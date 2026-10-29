package nz.co.takakaweather;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
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
        String failure = null;
        try { data = RiverData.fetchReport(); }
        catch (Exception e) { failure = e.getMessage() == null ? "FEED UNAVAILABLE" : e.getMessage(); }
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_river);
            Intent launch = new Intent(context, MainActivity.class);
            PendingIntent pending = PendingIntent.getActivity(context, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root, pending);
            if (data != null) {
                JSONObject k = data.optJSONObject("kotinga");
                JSONObject h = data.optJSONObject("harwoods");
                if (k != null) {
                    views.setTextViewText(R.id.widget_k_flow, format(k.optDouble("flow", Double.NaN)) + " m³/s");
                    views.setTextViewText(R.id.widget_k_level, "LEVEL " + format(k.optDouble("level", Double.NaN)) + " mm");
                }
                if (h != null) {
                    views.setTextViewText(R.id.widget_h_flow, format(h.optDouble("flow", Double.NaN)) + " m³/s");
                    views.setTextViewText(R.id.widget_h_level, "LEVEL " + format(h.optDouble("level", Double.NaN)) + " mm");
                }
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                views.setTextViewText(R.id.widget_status, "● LIVE");
                views.setTextViewText(R.id.widget_updated, "UPDATED " + time + " / TASMAN DC TELEMETRY");
            } else {
                views.setTextViewText(R.id.widget_status, "OFFLINE");
                views.setTextViewText(R.id.widget_updated, "NO NEW DATA / " + (failure == null ? "CHECK CONNECTION" : "CHECK APP FOR STATUS"));
            }
            manager.updateAppWidget(id, views);
        }
    }

    private static String format(double n) {
        if (Double.isNaN(n) || Double.isInfinite(n)) return "—";
        return String.format(Locale.getDefault(), "%.2f", n);
    }
}