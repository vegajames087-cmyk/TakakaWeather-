package nz.co.weatherandrivers;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
public class MainActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView text = new TextView(this);
        text.setText("Weather & Rivers\n\nFoundation build only. Live forecast, location selection and river gauges are not connected yet.");
        text.setTextSize(20);
        text.setPadding(32,48,32,32);
        setContentView(text);
    }
}
