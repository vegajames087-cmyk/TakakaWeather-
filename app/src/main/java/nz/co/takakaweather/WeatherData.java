package nz.co.takakaweather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class WeatherData {
    private WeatherData() {}

    public static JSONObject fetchEcmwf(double latitude, double longitude) throws Exception {
        String url = "https://api.open-meteo.com/v1/ecmwf?latitude=" + latitude
            + "&longitude=" + longitude
            + "&hourly=temperature_2m,precipitation,wind_speed_10m,wind_gusts_10m,relative_humidity_2m"
            + "&timezone=Pacific%2FAuckland&forecast_days=2";
        JSONObject root = getJson(url, "WeatherAndRiversNZ/1.2");
        JSONObject hourly = root.optJSONObject("hourly");
        if (hourly == null) throw new Exception("ECMWF response did not include hourly forecast values");
        JSONArray times = hourly.optJSONArray("time");
        if (times == null || times.length() == 0) throw new Exception("ECMWF returned no forecast timestamps");

        SimpleDateFormat localFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
        localFormat.setTimeZone(TimeZone.getTimeZone("Pacific/Auckland"));
        long now = System.currentTimeMillis();
        int selected = -1;
        for (int i = 0; i < times.length(); i++) {
            String timestamp = times.optString(i, "");
            try {
                Date date = localFormat.parse(timestamp);
                if (date != null && date.getTime() >= now) { selected = i; break; }
            } catch (ParseException ignored) {}
        }
        if (selected < 0) selected = times.length() - 1;

        JSONObject out = new JSONObject();
        out.put("time", times.optString(selected, ""));
        putNumber(out, "temperature", valueAt(hourly, "temperature_2m", selected));
        putNumber(out, "rain", valueAt(hourly, "precipitation", selected));
        putNumber(out, "wind", valueAt(hourly, "wind_speed_10m", selected));
        putNumber(out, "gust", valueAt(hourly, "wind_gusts_10m", selected));
        putNumber(out, "humidity", valueAt(hourly, "relative_humidity_2m", selected));
        return out;
    }

    private static double valueAt(JSONObject object, String key, int index) {
        JSONArray values = object.optJSONArray(key);
        return values == null || index < 0 || index >= values.length()
            ? Double.NaN : values.optDouble(index, Double.NaN);
    }

    public static JSONObject fetchMetNorway(double latitude, double longitude) throws Exception {
        String url = "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=" + latitude + "&lon=" + longitude;
        JSONObject root = getJson(url, "WeatherAndRiversNZ/1.2 (https://github.com/vegajames087-cmyk/TakakaWeather-)");
        JSONObject properties = root.optJSONObject("properties");
        JSONArray series = properties == null ? null : properties.optJSONArray("timeseries");
        if (series == null || series.length() == 0) throw new Exception("MET Norway returned no forecast points");

        JSONObject selected = null;
        long now = System.currentTimeMillis();
        for (int i = 0; i < series.length(); i++) {
            JSONObject item = series.optJSONObject(i);
            if (item == null) continue;
            String time = item.optString("time", "");
            try {
                if (parseIsoEpoch(time) >= now) { selected = item; break; }
            } catch (Exception ignored) {}
            if (selected == null) selected = item;
        }
        if (selected == null) throw new Exception("MET Norway forecast had no usable point");

        JSONObject data = selected.optJSONObject("data");
        JSONObject instant = data == null ? null : data.optJSONObject("instant");
        JSONObject details = instant == null ? null : instant.optJSONObject("details");
        if (details == null) throw new Exception("MET Norway forecast point was incomplete");
        JSONObject nextHour = data.optJSONObject("next_1_hours");
        JSONObject hourDetails = nextHour == null ? null : nextHour.optJSONObject("details");

        JSONObject out = new JSONObject();
        out.put("time", selected.optString("time", ""));
        putNumber(out, "temperature", details.optDouble("air_temperature", Double.NaN));
        putNumber(out, "rain", hourDetails == null ? Double.NaN : hourDetails.optDouble("precipitation_amount", Double.NaN));
        putNumber(out, "wind", multiply(details.optDouble("wind_speed", Double.NaN), 3.6));
        putNumber(out, "gust", multiply(details.optDouble("wind_speed_of_gust", details.optDouble("wind_speed", Double.NaN)), 3.6));
        putNumber(out, "humidity", details.optDouble("relative_humidity", Double.NaN));
        return out;
    }

    private static JSONObject getJson(String address, String userAgent) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(9000);
        connection.setReadTimeout(11000);
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", "application/json");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new Exception("Forecast service HTTP " + status);
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            JSONObject result = new JSONObject(body.toString());
            if (result.has("error") && result.optBoolean("error")) throw new Exception(result.optString("reason", "Forecast service error"));
            return result;
        } finally {
            connection.disconnect();
        }
    }

    private static long parseIsoEpoch(String value) {
        try {
            String normalized = value.replaceAll("\\.\\d+Z$", "Z");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date parsed = formatter.parse(normalized);
            return parsed == null ? Long.MIN_VALUE : parsed.getTime();
        } catch (ParseException | IllegalArgumentException ignored) { return Long.MIN_VALUE; }
    }

    private static double multiply(double value, double multiplier) {
        return Double.isNaN(value) || Double.isInfinite(value) ? Double.NaN : value * multiplier;
    }

    private static void putNumber(JSONObject object, String key, double value) throws Exception {
        object.put(key, Double.isNaN(value) || Double.isInfinite(value) ? JSONObject.NULL : value);
    }
}