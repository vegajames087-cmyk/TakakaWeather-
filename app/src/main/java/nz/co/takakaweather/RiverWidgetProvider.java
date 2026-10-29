package nz.co.takakaweather;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class RiverWidgetProvider extends AppWidgetProvider {

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) showLoading(context, manager, id);
        enqueueOneTime(context);
    }

    @Override public void onEnabled(Context context) {
        super.onEnabled(context);
        Constraints constraints = networkConstraints();
        PeriodicWorkRequest periodic = new PeriodicWorkRequest.Builder(RiverRefreshWorker.class, 30, TimeUnit.MINUTES)
            .setConstraints(constraints).build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
            "weather-rivers-widget-periodic", ExistingPeriodicWorkPolicy.KEEP, periodic);
        enqueueOneTime(context);
    }

    @Override public void onDisabled(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork("weather-rivers-widget-refresh");
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork("weather-rivers-widget-periodic");
        super.onDisabled(context);
    }

    private static Constraints networkConstraints() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    private static void enqueueOneTime(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(RiverRefreshWorker.class)
            .setConstraints(networkConstraints()).build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
            "weather-rivers-widget-refresh", ExistingWorkPolicy.REPLACE, request);
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

    public static boolean refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, RiverWidgetProvider.class));
        if (ids == null || ids.length == 0) return true;
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
                    views.setTextViewText(R.id.widget_weather_rain, "RAIN " + format(current.optDouble("rain", Double.NaN)) + " mm / " + current.optString("time", "ECMWF FORECAST"));
                }
            } else {
                views.setTextViewText(R.id.widget_weather_temp, "—°C");
                views.setTextViewText(R.id.widget_weather_wind, "WIND —");
                views.setTextViewText(R.id.widget_weather_rain, "RAIN — mm / ECMWF");
            }
            if (data != null) {
                JSONObject k = data.optJSONObject("kotinga");
                JSONObject h = data.optJSONObject("harwoods");
                if (k != null) {
                    views.setTextViewText(R.id.widget_k_flow, format(k.optDouble("flow", Double.NaN)) + " m³/s");
                    views.setTextViewText(R.id.widget_k_level, "LEVEL " + formatLevel(k.optDouble("level", Double.NaN)) + " mm");
                    views.setProgressBar(R.id.widget_k_bar, 100, ratio(k.optDouble("flow", Double.NaN), k.optDouble("peak", Double.NaN)), false);
                }
                if (h != null) {
                    views.setTextViewText(R.id.widget_h_flow, format(h.optDouble("flow", Double.NaN)) + " m³/s");
                    views.setTextViewText(R.id.widget_h_level, "LEVEL " + formatLevel(h.optDouble("level", Double.NaN)) + " mm");
                    views.setProgressBar(R.id.widget_h_bar, 100, ratio(h.optDouble("flow", Double.NaN), h.optDouble("peak", Double.NaN)), false);
                }
                String time = k != null ? k.optString("time", "SOURCE TIME UNKNOWN") : "SOURCE TIME UNKNOWN";
                views.setTextViewText(R.id.widget_status, "● DATA");
                views.setTextViewText(R.id.widget_updated, "COUNCIL SOURCE " + time);
            } else {
                views.setTextViewText(R.id.widget_k_flow, "— m³/s");
                views.setTextViewText(R.id.widget_k_level, "LEVEL —");
                views.setTextViewText(R.id.widget_h_flow, "— m³/s");
                views.setTextViewText(R.id.widget_h_level, "LEVEL —");
                views.setProgressBar(R.id.widget_k_bar, 100, 0, false);
                views.setProgressBar(R.id.widget_h_bar, 100, 0, false);
                views.setTextViewText(R.id.widget_status, "OFFLINE");
                views.setTextViewText(R.id.widget_updated, "NO NEW DATA / " + (failure == null ? "CHECK CONNECTION" : "CHECK APP FOR STATUS"));
            }
            manager.updateAppWidget(id, views);
        }
        return data != null && weather != null;
    }

    private static int ratio(double flow, double peak) {
        if (Double.isNaN(flow) || Double.isInfinite(flow) || Double.isNaN(peak) || Double.isInfinite(peak) || peak <= 0) return 0;
        return Math.max(0, Math.min(100, (int)Math.round(flow / peak * 100.0)));
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