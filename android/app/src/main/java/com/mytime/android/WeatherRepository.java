package com.mytime.android;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Fixed city coordinates; no device location is collected. */
final class WeatherRepository {
    static final long REFRESH_INTERVAL_MILLIS = 30 * 60 * 1000L;
    private static final long RETRY_INTERVAL_MILLIS = 5 * 60 * 1000L;
    private static final long STALE_AFTER_MILLIS = 2 * 60 * 60 * 1000L;
    private static final String API_URL = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=34.3296,27.6711&longitude=108.7088,-8.1474"
            + "&current=temperature_2m,weather_code&temperature_unit=celsius"
            + "&timezone=auto&forecast_days=1";
    private static final String[] CITIES = {"xianyang", "tindouf"};

    private WeatherRepository() {}

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("weather", Context.MODE_PRIVATE);
    }

    static boolean needsRefresh(Context context) {
        SharedPreferences prefs = preferences(context);
        long now = System.currentTimeMillis();
        if (!elapsed(now, prefs.getLong("last_attempt", 0), RETRY_INTERVAL_MILLIS)) {
            return false;
        }
        for (String city : CITIES) {
            if (elapsed(now, prefs.getLong(city + "_updated", 0), REFRESH_INTERVAL_MILLIS)) {
                return true;
            }
        }
        return false;
    }

    private static boolean elapsed(long now, long then, long interval) {
        return then == 0 || now < then || now - then >= interval;
    }

    static String summary(Context context, String city) {
        SharedPreferences prefs = preferences(context);
        String text = prefs.getString(city + "_summary", null);
        if (text == null) {
            return "天气待更新";
        }
        return text + (elapsed(System.currentTimeMillis(),
                prefs.getLong(city + "_updated", 0), STALE_AFTER_MILLIS) ? "*" : "");
    }

    static String description(Context context, String city) {
        String text = summary(context, city);
        return text.endsWith("*") ? text.substring(0, text.length() - 1) + "，天气数据已超过两小时" : text;
    }

    static synchronized void refresh(Context context) throws IOException, JSONException {
        if (Thread.currentThread().isInterrupted() || !needsRefresh(context)) {
            return;
        }
        preferences(context).edit().putLong("last_attempt", System.currentTimeMillis()).apply();
        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("Accept", "application/json");
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Weather HTTP status " + connection.getResponseCode());
            }
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[2048];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new IOException("Weather update interrupted");
                    }
                    output.write(buffer, 0, length);
                    if (output.size() > 65536) {
                        throw new IOException("Weather response too large");
                    }
                }
                storeResponse(context, output.toString("UTF-8"));
            }
        } finally {
            connection.disconnect();
        }
    }

    static void storeResponse(Context context, String json) throws JSONException {
        JSONArray locations = new JSONArray(json);
        if (locations.length() != CITIES.length) {
            throw new JSONException("Expected two weather locations");
        }
        // Validate the entire response before replacing either city's cached weather.
        String[] summaries = new String[CITIES.length];
        for (int i = 0; i < CITIES.length; i++) {
            JSONObject location = locations.getJSONObject(i);
            JSONObject units = location.getJSONObject("current_units");
            if (!"°C".equals(units.getString("temperature_2m"))) {
                throw new JSONException("Expected Celsius temperatures");
            }
            JSONObject current = location.getJSONObject("current");
            double temperature = current.getDouble("temperature_2m");
            if (Double.isNaN(temperature) || Double.isInfinite(temperature)
                    || temperature < -100 || temperature > 70) {
                throw new JSONException("Invalid temperature");
            }
            summaries[i] = condition(current.getInt("weather_code"))
                    + " " + Math.round(temperature) + "°C";
        }
        SharedPreferences.Editor editor = preferences(context).edit();
        long now = System.currentTimeMillis();
        for (int i = 0; i < CITIES.length; i++) {
            editor.putString(CITIES[i] + "_summary", summaries[i]);
            editor.putLong(CITIES[i] + "_updated", now);
        }
        editor.apply();
    }

    static String condition(int code) {
        switch (code) {
            case 0: return "晴";
            case 1: return "晴间多云";
            case 2: return "多云";
            case 3: return "阴";
            case 45: case 48: return "雾";
            case 51: case 53: case 55: return "毛毛雨";
            case 56: case 57: case 66: case 67: return "冻雨";
            case 61: return "小雨";
            case 63: return "中雨";
            case 65: return "大雨";
            case 71: return "小雪";
            case 73: return "中雪";
            case 75: return "大雪";
            case 77: return "雪粒";
            case 80: case 81: case 82: return "阵雨";
            case 85: case 86: return "阵雪";
            case 95: return "雷雨";
            case 96: case 99: return "雷雨冰雹";
            default: return "天气未知";
        }
    }
}
