package nz.co.takakaweather;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RiverData {
    private static final String REPORT = "https://www.tasman.govt.nz/my-region/environment/environmental-data/river-flow/water-level-and-flow-report";
    private RiverData() {}

    public static JSONObject fetchReport() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(REPORT).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("User-Agent", "WeatherAndRiversNZ/1.2 (Android app)");
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml");
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) throw new Exception("Council report HTTP " + code);
            String html = readAll(connection.getInputStream());
            JSONObject result = new JSONObject();
            result.put("source", REPORT);
            result.put("fetchedAt", System.currentTimeMillis());
            JSONArray rows = new JSONArray();
            Matcher rowMatcher = Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>").matcher(html);
            while (rowMatcher.find()) {
                String row = rowMatcher.group(1);
                String plain = strip(row).replaceAll("\\s+", " ").trim();
                String lower = plain.toLowerCase();
                String key = lower.contains("takaka at kotinga") ? "kotinga" :
                    (lower.contains("takaka at harwoods") || lower.contains("takaka at harwood") ? "harwoods" : null);
                if (key == null) continue;
                Matcher cellMatcher = Pattern.compile("(?is)<t[dh]\\b[^>]*>(.*?)</t[dh]>").matcher(row);
                JSONArray cells = new JSONArray();
                while (cellMatcher.find()) cells.put(strip(cellMatcher.group(1)).replaceAll("\\s+", " ").trim());
                JSONObject gauge = parseCells(cells);
                if (gauge != null) result.put(key, gauge);
            }
            if (!result.has("kotinga") || !result.has("harwoods")) {
                // Some council page responses expose the table as pipe-delimited text.
                Matcher textRow = Pattern.compile("(?im)^\\s*(Takaka at Kotinga|Takaka at Harwoods)\\s*\\|\\s*([^\\n]+)").matcher(html.replaceAll("<[^>]+>", " "));
                while (textRow.find()) {
                    String key = textRow.group(1).toLowerCase().contains("kotinga") ? "kotinga" : "harwoods";
                    String[] parts = textRow.group(2).split("\\s*\\|\\s*");
                    JSONObject gauge = parseFlat(parts);
                    if (gauge != null) result.put(key, gauge);
                }
            }
            if (!result.has("kotinga") || !result.has("harwoods")) {
                throw new Exception("Official council report loaded, but the two Takaka gauge rows could not be parsed.");
            }
            return result;
        } finally {
            connection.disconnect();
        }
    }

    private static JSONObject parseCells(JSONArray cells) {
        if (cells.length() < 6) return null;
        String[] values = new String[cells.length()];
        for (int i = 0; i < cells.length(); i++) values[i] = cells.optString(i, "");
        return parseFlat(values);
    }

    private static JSONObject parseFlat(String[] c) {
        try {
            // Expected council columns: site, timestamp, level mm, level change mm/hr, flow m3/s, peak flow m3/s.
            if (c.length < 5) return null;
            int offset = c.length >= 6 ? 1 : 0;
            String timestamp = c[offset];
            double level = number(c[offset + 1]);
            double delta = number(c[offset + 2]);
            double flow = number(c[offset + 3]);
            double peak = number(c[offset + 4]);
            JSONObject out = new JSONObject();
            out.put("time", timestamp);
            out.put("level", level);
            out.put("delta", delta);
            out.put("flow", flow);
            out.put("peak", peak);
            return out;
        } catch (Exception ignored) { return null; }
    }

    private static double number(String value) {
        Matcher m = Pattern.compile("[-+]?[0-9]+(?:\\.[0-9]+)?").matcher(value.replace(",", ""));
        if (!m.find()) throw new IllegalArgumentException("Not a number: " + value);
        return Double.parseDouble(m.group());
    }

    private static String strip(String s) {
        return s.replaceAll("(?is)<script\\b.*?</script>", "")
                .replaceAll("(?is)<style\\b.*?</style>", "")
                .replaceAll("(?i)<br\\s*/?>", " ")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&#160;", " ")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'");
    }

    private static String readAll(InputStream stream) throws Exception {
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) out.append(line).append('\n');
        }
        return out.toString();
    }
}