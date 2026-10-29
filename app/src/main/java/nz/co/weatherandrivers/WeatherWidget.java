package nz.co.weatherandrivers;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;
public class WeatherWidget extends AppWidgetProvider {
 private static final String MET="https://alerts.metservice.com/cap/rss";
 private static final String RIVERS="https://www.tasman.govt.nz/my-region/environment/environmental-data/river-flow/water-level-and-flow-report";
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){
  for(int id:ids){RemoteViews v=new RemoteViews(context.getPackageName(),R.layout.weather_widget);Intent i=new Intent(context,MainActivity.class);PendingIntent p=PendingIntent.getActivity(context,0,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);v.setOnClickPendingIntent(R.id.widget_title,p);v.setTextViewText(R.id.widget_conditions,"Loading live forecasts…");manager.updateAppWidget(id,v);}
  Context app=context.getApplicationContext();new Thread(()->refresh(app)).start();
 }
 private static byte[] fetch(String url)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(12000);c.setRequestProperty("User-Agent","WeatherAndRivers/0.3 (github.com/vegajames087-cmyk/TakakaWeather-)");
  try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){if(c.getResponseCode()!=200)throw new Exception("HTTP "+c.getResponseCode());byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>1500000)throw new Exception("Response too large");}return out.toByteArray();}finally{c.disconnect();}
 }
 private static void refresh(Context app){
  int s=app.getSharedPreferences("settings",Context.MODE_PRIVATE).getInt("location",0);if(s<0||s>=MainActivity.NAMES.length)s=0;
  String town=MainActivity.NAMES[s];double lat=MainActivity.LATS[s],lon=MainActivity.LONS[s];
  String ecmwf="ECMWF unavailable",yr="Yr.no unavailable",river="River gauges: unavailable",warning="Warnings: unable to check";Bitmap graph=null;
  try{
   String url=String.format(Locale.US,"https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m&hourly=temperature_2m&forecast_days=2&timezone=Pacific%%2FAuckland&models=ecmwf_ifs025",lat,lon);
   JSONObject root=new JSONObject(new String(fetch(url),"UTF-8"));JSONObject now=root.getJSONObject("current");
   ecmwf=String.format(Locale.US,"ECMWF %.1f°C · Wind %.0f km/h · Rain %.1f mm · RH %.0f%%",now.getDouble("temperature_2m"),now.getDouble("wind_speed_10m"),now.getDouble("precipitation"),now.getDouble("relative_humidity_2m"));
   graph=plot(root.getJSONObject("hourly").getJSONArray("temperature_2m"),root.getJSONObject("hourly").getJSONArray("time"),now.getString("time"));
  }catch(Exception ignored){}
  try{String url=String.format(Locale.US,"https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.4f&lon=%.4f",lat,lon);JSONObject instant=new JSONObject(new String(fetch(url),"UTF-8")).getJSONObject("properties").getJSONArray("timeseries").getJSONObject(0).getJSONObject("data").getJSONObject("instant").getJSONObject("details");yr=String.format(Locale.US,"Yr.no %.1f°C · Wind %.1f m/s",instant.getDouble("air_temperature"),instant.getDouble("wind_speed"));}catch(Exception ignored){}
  try{
   String html=new String(fetch(RIVERS),"UTF-8");String[] names={"Takaka at Kotinga","Takaka at Harwoods","Anatoki at Happy Sams","Aorere at Devils Boots"};StringBuilder sb=new StringBuilder();
   for(String name:names){
    java.util.regex.Matcher rows=java.util.regex.Pattern.compile("(?is)<tr\\b[^>]*>.*?</tr>").matcher(html);
    while(rows.find()){
     String row=rows.group().replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1>","").replaceAll("(?is)<[^>]+>","|").replace("&nbsp;"," ").replace("&#160;"," ");
     row=row.replaceAll("\\s+"," ").replaceAll("\\|+","|");
     if(!row.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))continue;
     String[] cells=row.split("\\|");java.util.ArrayList<String> values=new java.util.ArrayList<>();for(String cell:cells)if(!cell.trim().isEmpty())values.add(cell.trim());
     if(values.size()>=5){if(sb.length()>0)sb.append("\n");sb.append(name).append(": ").append(values.get(4)).append(" m³/s · ").append(values.get(1));}break;
    }
   }
   if(sb.length()>0)river="TDC provisional flows:\n"+sb;
  }catch(Exception ignored){}
  try{
   DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);f.setFeature("http://xml.org/sax/features/external-general-entities",false);f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
   Document d=f.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(fetch(MET)));NodeList items=d.getElementsByTagName("item");warning="MetService: check regional alerts";
   for(int i=0;i<items.getLength();i++){Element item=(Element)items.item(i);NodeList titles=item.getElementsByTagName("title");if(titles.getLength()==0)continue;String title=titles.item(0).getTextContent();String lower=(title+" "+item.getTextContent()).toLowerCase(Locale.ROOT);if(lower.contains("nelson")||lower.contains("tasman")||lower.contains("golden bay")||lower.contains("buller")||lower.contains("northwest")){warning="MetService: "+title;break;}}
  }catch(Exception ignored){}
  AppWidgetManager m=AppWidgetManager.getInstance(app);int[] ids=m.getAppWidgetIds(new ComponentName(app,WeatherWidget.class));
  for(int id:ids){RemoteViews v=new RemoteViews(app.getPackageName(),R.layout.weather_widget);v.setTextViewText(R.id.widget_conditions,town+"\n"+ecmwf+"\n"+yr);v.setTextViewText(R.id.widget_rivers,river);v.setTextViewText(R.id.widget_warning,warning);if(graph!=null)v.setImageViewBitmap(R.id.widget_graph,graph);else v.setImageViewResource(R.id.widget_graph,android.R.color.transparent);m.updateAppWidget(id,v);}
 }
 private static Bitmap plot(JSONArray arr,JSONArray times,String current)throws Exception{
  int start=0;String hour=current.length()>=13?current.substring(0,13):current;for(int i=0;i<times.length();i++){if(times.getString(i).startsWith(hour)){start=i;break;}}int count=Math.min(24,arr.length()-start);if(count<2)return null;float[] values=new float[count];float lo=Float.MAX_VALUE,hi=-Float.MAX_VALUE;
  for(int i=0;i<count;i++){values[i]=(float)arr.getDouble(start+i);lo=Math.min(lo,values[i]);hi=Math.max(hi,values[i]);}
  Bitmap bmp=Bitmap.createBitmap(600,110,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bmp);c.drawColor(Color.rgb(20,43,54));Paint p=new Paint(3);p.setColor(Color.rgb(80,211,201));p.setStrokeWidth(4);p.setStyle(Paint.Style.STROKE);
  for(int i=1;i<count;i++){float x1=10+(i-1)*580f/(count-1),x2=10+i*580f/(count-1);float y1=95-(values[i-1]-lo)*75f/Math.max(1,hi-lo),y2=95-(values[i]-lo)*75f/Math.max(1,hi-lo);c.drawLine(x1,y1,x2,y2,p);}return bmp;
 }
}
