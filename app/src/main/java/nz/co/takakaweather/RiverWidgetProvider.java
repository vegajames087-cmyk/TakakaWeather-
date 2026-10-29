package nz.co.takakaweather;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RiverWidgetProvider extends AppWidgetProvider {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) showLoading(context, manager, id);
        IO.execute(() -> refreshAll(context.getApplicationContext()));
    }

    @Override public void onEnabled(Context context) {
        super.onEnabled(context);
        IO.execute(() -> refreshAll(context.getApplicationContext()));
    }

    private static void showLoading(Context context, AppWidgetManager manager, int id) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_river);
        views.setTextViewText(R.id.widget_status, "SYNC");
        Intent launch = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, 0, launch,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pending);
        manager.updateAppWidget(id, views);
    }

    private static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, RiverWidgetProvider.class));
        if (ids == null || ids.length == 0) return;
        JSONObject data = null;
        JSONObject weather = null;
        String failure = null;
        try { data = RiverData.fetchReport(); }
        catch (Exception e) { failure = e.getMessage() == null ? "FEED UNAVAILABLE" : e.getMessage(); }
        try { weather = WeatherData.fetchEcmwf(-40.859, 172.806); } catch (Exception ignored) {}
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_river);
            Intent launch = new Intent(context, MainActivity.class);
            PendingIntent pending = PendingIntent.getActivity(context, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root, pending);
            if (weather != null) {
                JSONObject current = weather;
                if (current != null) {
                    views.setTextViewText(R.id.widget_weather_temp, format(current.optDouble("temperature", Double.NaN)) + "°C");
                    views.setTextViewText(R.id.widget_weather_wind, "WIND " + format(current.optDouble("wind", Double.NaN)) + " km/h");
                    views.setTextViewText(R.id.widget_weather_rain, "RAIN " + format(current.optDouble("rain", Double.NaN)) + " mm / ECMWF");
                }
            }
            if (data != null) {
                JSONObject k = data.optJSONObject("kotinga");
                JSONObject h = data.optJSONObject("harwoods");
                if (k != null) {
                    views.setTextViewText(R.id.widget_k_flow, format(k.optDouble("flow", Double.NaN)) + " m³/s");
                    views.setTextViewText(R.id.widget_k_level, "LEVEL " + formatLevel(k.optDouble("level", Double.NaN)) + " mm");
                }
                if (h != null) {
                    views.setTextViewText(R.id.widget_h_flow, format(h.optDouble("flow", Double.NaN)) + " m³/s");
                    views.setTextViewText(R.id.widget_h_level, "LEVEL " + formatLevel(h.optDouble("level", Double.NaN)) + " mm");
                }
                String time = k != null ? k.optString("time", "SOURCE TIME UNKNOWN") : "SOURCE TIME UNKNOWN";
                views.setTextViewText(R.id.widget_status, "● DATA");
                views.setTextViewText(R.id.widget_updated, "COUNCIL SOURCE " + time);
            } else {
                views.setTextViewText(R.id.widget_status, "OFFLINE");
                views.setTextViewText(R.id.widget_updated, "NO NEW DATA / " + (failure == null ? "CHECK CONNECTION" : "CHECK APP FOR STATUS"));
            }
            manager.updateAppWidget(id, views);
        }
    }

    private static String formatLevel(double n) {
        if (Double.isNaN(n) || Double.isInfinite(n)) return "—";
        return String.format(Locale.getDefault(), "%.0f", n);
    }

    private static String format(double n) {
        if (Double.isNaN(n) || Double.isInfinite(n)) return "—";
        return String.format(Locale.getDefault(), "%.2f", n);
    }
}