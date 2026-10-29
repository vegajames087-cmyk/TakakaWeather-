package nz.co.takakaweather;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private WebView webView;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(17,21,16));
        getWindow().setNavigationBarColor(Color.rgb(17,21,16));
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(17,21,16));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new RiverBridge(), "AndroidRiver");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private class RiverBridge {
        @JavascriptInterface
        public void loadRiverReport(String callbackName) {
            io.execute(() -> {
                String payload;
                try {
                    payload = RiverData.fetchReport().toString();
                } catch (Exception e) {
                    JSONObject error = new JSONObject();
                    try { error.put("error", e.getMessage() == null ? "River feed unavailable" : e.getMessage()); } catch (Exception ignored) {}
                    payload = error.toString();
                }
                final String result = payload;
                main.post(() -> {
                    if (webView != null) {
                        String callback = "receiveRiverData";
                        webView.evaluateJavascript("window." + callback + "(" + JSONObject.quote(result) + ")", null);
                    }
                });
            });
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) { webView.removeJavascriptInterface("AndroidRiver"); webView.destroy(); webView = null; }
        io.shutdownNow();
        super.onDestroy();
    }
}