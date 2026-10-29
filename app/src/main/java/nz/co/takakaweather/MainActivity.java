package nz.co.takakaweather;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.graphics.Color;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {
    private WebView webView;
    private final ExecutorService io = Executors.newFixedThreadPool(3);
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(17, 21, 16));
        getWindow().setNavigationBarColor(Color.rgb(17, 21, 16));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(17, 21, 16));
        ViewCompat.setOnApplyWindowInsetsListener(webView, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new DashboardBridge(), "AndroidData");
        setContentView(webView);
        ViewCompat.requestApplyInsets(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private final class DashboardBridge {
        @JavascriptInterface
        public void loadDashboardData(double latitude, double longitude) {
            final JSONObject result = new JSONObject();
            final AtomicInteger remaining = new AtomicInteger(3);

            requestPart(result, remaining, "ecmwf", "ecmwfError",
                () -> WeatherData.fetchEcmwf(latitude, longitude));
            requestPart(result, remaining, "metNorway", "metNorwayError",
                () -> WeatherData.fetchMetNorway(latitude, longitude));
            requestPart(result, remaining, "rivers", "riverError",
                RiverData::fetchReport);
        }

        private void requestPart(JSONObject result, AtomicInteger remaining,
                                  String dataKey, String errorKey,
                                  Callable<JSONObject> action) {
            io.execute(() -> {
                try {
                    JSONObject payload = action.call();
                    synchronized (result) { result.put(dataKey, payload); }
                } catch (Exception e) {
                    String message = e.getMessage() == null ? "Data feed unavailable" : e.getMessage();
                    try { synchronized (result) { result.put(errorKey, message); } }
                    catch (Exception ignored) {}
                } finally {
                    if (remaining.decrementAndGet() == 0) {
                        final String payload;
                        synchronized (result) { payload = result.toString(); }
                        main.post(() -> {
                            if (webView != null) {
                                webView.evaluateJavascript(
                                    "window.receiveDashboardData(" + JSONObject.quote(payload) + ")", null);
                            }
                        });
                    }
                }
            });
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidData");
            webView.destroy();
            webView = null;
        }
        io.shutdownNow();
        super.onDestroy();
    }
}