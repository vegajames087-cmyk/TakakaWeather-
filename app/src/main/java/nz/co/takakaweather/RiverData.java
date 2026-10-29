package nz.co.takakaweather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RiverData {
    public static final String REPORT =
        "https://www.tasman.govt.nz/my-region/environment/environmental-data/river-flow/water-level-and-flow-report";

    private RiverData() {}

    public static JSONObject fetchReport() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(REPORT).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(12000);
        connection.setRequestProperty("User-Agent", "WeatherAndRiversNZ/1.3 (https://github.com/vegajames087-cmyk/TakakaWeather-)");
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml");
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) throw new Exception("Council report HTTP " + code);
            return parseReportHtml(readAll(connection.getInputStream()));
        } finally {
            connection.disconnect();
        }
    }

    static JSONObject parseReportHtml(String html) throws Exception {
        JSONObject result = new JSONObject();
        result.put("source", REPORT);
        result.put("fetchedAt", System.currentTimeMillis());
        JSONArray gauges = new JSONArray();

        Matcher rowMatcher = Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>").matcher(html);
        while (rowMatcher.find()) {
            String row = rowMatcher.group(1);
            Matcher cellMatcher = Pattern.compile("(?is)<t[dh]\\b[^>]*>(.*?)</t[dh]>").matcher(row);
            JSONArray cells = new JSONArray();
            while (cellMatcher.find()) cells.put(strip(cellMatcher.group(1)).replaceAll("\\s+", " ").trim());
            if (cells.length() < 6) continue;

            String site = cells.optString(0, "").trim();
            if (!looksLikeRiverGauge(site)) continue;
            JSONObject gauge = parseCells(cells);
            if (!isValid(gauge)) continue;

            String key = keyFor(site);
            gauge.put("id", key);
            gauge.put("name", site);
            gauges.put(gauge);
            if ("kotinga".equals(key)) result.put("kotinga", gauge);
            if ("harwoods".equals(key)) result.put("harwoods", gauge);
            if (!result.has(key)) result.put(key, gauge);
        }

        // Preserve legacy gauges if the page's table markup is flattened.
        if (!result.has("kotinga") || !result.has("harwoods")) {
            String rowsOnly = html.replaceAll("(?is)</tr\\s*>", "\\n")
                .replaceAll("(?is)<tr\\b[^>]*>", "")
                .replaceAll("(?is)<t[dh]\\b[^>]*>", "|")
                .replaceAll("(?is)</t[dh]\\s*>", "")
                .replaceAll("(?is)<script\\b.*?</script>", "")
                .replaceAll("(?is)<style\\b.*?</style>", "");
            Matcher fallbackRows = Pattern.compile("(?im)^\\s*([^\\n]*(?:Takaka at (?:Kotinga|Harwoods)|Aorere at)[^\\n]*)").matcher(rowsOnly);
            while (fallbackRows.find()) {
                String line = strip(fallbackRows.group(1)).replace("&nbsp;", " ").trim();
                String[] columns = line.replaceAll("^\\s*\\|+", "").replaceAll("\\|+\\s*$", "").split("\\s*\\|\\s*");
                String site = columns.length > 0 ? columns[0].trim() : "";
                String key = keyFor(site + " " + line);
                JSONObject gauge = parseFlat(columns);
                if (key != null && isValid(gauge)) {
                    gauge.put("id", key);
                    gauge.put("name", site.isEmpty() ? siteNameFromLine(line) : site);
                    if (!result.has(key)) gauges.put(gauge);
                    result.put(key, gauge);
                }
            }
        }
        if (!result.has("kotinga") || !result.has("harwoods")) {
            throw new Exception("Council report loaded, but its Kotinga and Harwoods rows could not both be read.");
        }
        result.put("gauges", gauges);
        return result;
    }

    private static boolean looksLikeRiverGauge(String site) {
        String s = site == null ? "" : site.toLowerCase(Locale.ROOT);
        return s.contains(" at ") && !s.contains("peak") && !s.contains("time");
    }

    private static String keyFor(String site) {
        if (site == null) return null;
        String s = site.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (s.contains("takaka") && s.contains("kotinga")) return "kotinga";
        if (s.contains("takaka") && s.contains("harwood")) return "harwoods";
        return s.isEmpty() ? null : s;
    }

    private static String siteNameFromLine(String line) {
        String cleaned = line.replaceAll("^\\s*\\|+", "").trim();
        int split = cleaned.indexOf('|');
        return (split >= 0 ? cleaned.substring(0, split) : cleaned).trim();
    }

    private static JSONObject parseCells(JSONArray cells) {
        String[] values = new String[cells.length()];
        for (int i = 0; i < cells.length(); i++) values[i] = cells.optString(i, "");
        return parseFlat(values);
    }

    private static JSONObject parseFlat(String[] columns) {
        try {
            if (columns.length < 5) return null;
            int offset = isSiteName(columns[0]) ? 1 : 0;
            if (columns.length - offset < 5) return null;
            String timestamp = columns[offset].trim();
            if (!timestamp.matches("\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{2}")) return null;
            double level = number(columns[offset + 1]);
            double change = number(columns[offset + 2]);
            double flow = number(columns[offset + 3]);
            double peakFlow = number(columns[offset + 4]);
            JSONObject gauge = new JSONObject();
            gauge.put("time", timestamp + " NZST");
            gauge.put("level", level);
            gauge.put("delta", change);
            gauge.put("flow", flow);
            gauge.put("peak", peakFlow);
            if (columns.length > offset + 5) gauge.put("peakLevel", number(columns[offset + 5]));
            if (columns.length > offset + 6) gauge.put("peakTime", columns[offset + 6].trim());
            return gauge;
        } catch (Exception ignored) { return null; }
    }

    private static boolean isValid(JSONObject gauge) {
        return gauge != null && gauge.has("time") && gauge.has("flow") && gauge.has("level")
            && finite(gauge.optDouble("flow", Double.NaN)) && finite(gauge.optDouble("level", Double.NaN));
    }
    private static boolean finite(double value) { return !Double.isNaN(value) && !Double.isInfinite(value); }
    private static boolean isSiteName(String value) {
        String s = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return s.contains(" at ") && !s.matches(".*\\d{1,2}/\\d{1,2}/\\d{4}.*");
    }
    private static double number(String value) {
        Matcher m = Pattern.compile("[-+]?[0-9]+(?:\\.[0-9]+)?").matcher(value.replace(",", "").trim());
        if (!m.find()) throw new IllegalArgumentException("Not numeric: " + value);
        return Double.parseDouble(m.group());
    }
    private static String strip(String s) {
        return s.replaceAll("(?is)<script\\b.*?</script>", "").replaceAll("(?is)<style\\b.*?</style>", "")
            .replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]+>", " ")
            .replace("&nbsp;", " ").replace("&#160;", " ").replace("&amp;", "&")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'");
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