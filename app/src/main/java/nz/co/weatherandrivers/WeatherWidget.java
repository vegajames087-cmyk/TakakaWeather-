package nz.co.weatherandrivers;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.widget.RemoteViews;
import org.json.JSONObject;
import org.json.JSONArray;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class WeatherWidget extends AppWidgetProvider {
    private static final String API="https://api.open-meteo.com/v1/forecast?latitude=-40.86&longitude=172.81&current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,wind_direction_10m&wind_speed_unit=kmh&timezone=Pacific%2FAuckland&models=ecmwf_ifs025";
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for(int id:ids) {
            RemoteViews v=new RemoteViews(context.getPackageName(),R.layout.weather_widget);
            Intent intent=new Intent(context,MainActivity.class);
            PendingIntent p=PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            v.setOnClickPendingIntent(R.id.widget_title,p);
            v.setTextViewText(R.id.widget_conditions,"Updating ECMWF forecast…");
            manager.updateAppWidget(id,v);
        }
        final Context app=context.getApplicationContext();
        new Thread(()->{
            String message="ECMWF unavailable — tap to retry";
            try {
                HttpURLConnection c=(HttpURLConnection)new URL(API).openConnection();
                c.setConnectTimeout(12000);c.setReadTimeout(12000);
                c.setRequestProperty("User-Agent","WeatherAndRivers/0.2");
                try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
                    JSONObject current=new JSONObject(out.toString(StandardCharsets.UTF_8.name())).getJSONObject("current");
                    message=String.format(Locale.US,"Tākaka · ECMWF\n%.1f°C  Wind %.0f km/h\nRain %.1f mm · Humidity %.0f%%\nUpdated %s",current.getDouble("temperature_2m"),current.getDouble("wind_speed_10m"),current.getDouble("precipitation"),current.getDouble("relative_humidity_2m"),current.getString("time").replace('T',' '));
                }finally{c.disconnect();}
                try { HttpURLConnection yr=(HttpURLConnection)new URL("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=-40.86&lon=172.81").openConnection();yr.setConnectTimeout(10000);yr.setReadTimeout(10000);yr.setRequestProperty("User-Agent","WeatherAndRivers/0.2 (github.com/vegajames087-cmyk/TakakaWeather-)");try(InputStream yin=yr.getInputStream();ByteArrayOutputStream yout=new ByteArrayOutputStream()){byte[] ybuf=new byte[4096];int yn;while((yn=yin.read(ybuf))!=-1)yout.write(ybuf,0,yn);JSONObject instant=new JSONObject(yout.toString(StandardCharsets.UTF_8.name())).getJSONObject("properties").getJSONArray("timeseries").getJSONObject(0).getJSONObject("data").getJSONObject("instant").getJSONObject("details");message+="\\nYr.no "+String.format(Locale.US,"%.1f°C · %.1f m/s",instant.getDouble("air_temperature"),instant.getDouble("wind_speed"));}finally{yr.disconnect();}}catch(Exception ignored){message+="\\nYr.no unavailable";}
            }catch(Exception e){message="ECMWF unavailable — check connection";}
            AppWidgetManager m=AppWidgetManager.getInstance(app);
            android.content.ComponentName name=new android.content.ComponentName(app,WeatherWidget.class);
            int[] active=m.getAppWidgetIds(name);
            for(int id:active){RemoteViews v=new RemoteViews(app.getPackageName(),R.layout.weather_widget);v.setTextViewText(R.id.widget_conditions,message);m.partiallyUpdateAppWidget(id,v);}
        }).start();
    }
}