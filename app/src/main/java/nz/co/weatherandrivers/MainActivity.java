package nz.co.weatherandrivers;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.content.SharedPreferences;
import android.widget.*;
import android.view.View;
public class MainActivity extends Activity {
  static final String[] NAMES={"Tākaka","Pōhara","Wainui Bay","Collingwood","Bainham","Pākawau","Puponga","Anatori","Paturau","Tōtaranui","Awaroa"};
  static final double[] LATS={-40.86,-40.83,-40.80,-40.68,-40.74,-40.61,-40.51,-40.55,-40.65,-40.82,-40.87};
  static final double[] LONS={172.81,172.89,173.02,172.68,172.60,172.68,172.71,172.43,172.43,173.00,173.06};
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(32,32,32,32);scroll.addView(root);
    TextView title=new TextView(this);title.setText("Weather & Rivers\nGolden Bay & Kahurangi coast");title.setTextSize(22);root.addView(title);
    Spinner spinner=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,NAMES);spinner.setAdapter(adapter);root.addView(spinner);
    SharedPreferences prefs=getSharedPreferences("settings",MODE_PRIVATE);spinner.setSelection(prefs.getInt("location",0));
    Button save=new Button(this);save.setText("Save location & refresh widget");root.addView(save);
    TextView note=new TextView(this);note.setText("Live forecasts: ECMWF and MET Norway. River readings are provisional. Open the council source for official 7-day and 30-day flow graphs.");root.addView(note);
    Button riverHistory=new Button(this);riverHistory.setText("Open official river-flow history");root.addView(riverHistory);riverHistory.setOnClickListener(v->{Intent web=new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.tasman.govt.nz/my-region/environment/environmental-data/river-flow/takaka-at-kotinga"));startActivity(web);});
    Button allRivers=new Button(this);allRivers.setText("Open all live river gauges");root.addView(allRivers);allRivers.setOnClickListener(v->{Intent web=new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.tasman.govt.nz/my-region/environment/environmental-data/river-flow/water-level-and-flow-report"));startActivity(web);});
    Button warnings=new Button(this);warnings.setText("Open official weather warnings");root.addView(warnings);warnings.setOnClickListener(v->{Intent web=new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.metservice.com/warnings/home"));startActivity(web);});
    save.setOnClickListener(v->{prefs.edit().putInt("location",spinner.getSelectedItemPosition()).apply();AppWidgetManager manager=AppWidgetManager.getInstance(this);int[] ids=manager.getAppWidgetIds(new ComponentName(this,WeatherWidget.class));sendBroadcast(new Intent(this,WeatherWidget.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids));Toast.makeText(this,"Location saved",Toast.LENGTH_SHORT).show();});
    setContentView(scroll);
  }
}