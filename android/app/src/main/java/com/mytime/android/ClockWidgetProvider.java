package com.mytime.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class ClockWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_UPDATE_CLOCK = "com.mytime.android.ACTION_UPDATE_CLOCK";
    private static final int UPDATE_REQUEST_CODE = 1001;
    private static final long UPDATE_INTERVAL_MILLIS = 60 * 1000L;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
        scheduleUpdates(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        updateWidget(context, appWidgetManager, appWidgetId);
    }

    @Override
    public void onEnabled(Context context) {
        scheduleUpdates(context);
    }

    @Override
    public void onDisabled(Context context) {
        cancelUpdates(context);
        WeatherUpdateService.cancel(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_UPDATE_CLOCK.equals(intent.getAction())) {
            updateAllWidgets(context);
        }
        if (AppWidgetManager.getInstance(context).getAppWidgetIds(
                new ComponentName(context, ClockWidgetProvider.class)).length > 0) {
            WeatherUpdateService.schedule(context);
        }
    }

    static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, ClockWidgetProvider.class));
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        Calendar xianyang = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
        Calendar tindouf = Calendar.getInstance(TimeZone.getTimeZone("Africa/Algiers"));

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_clock);
        views.setTextViewText(R.id.xianyang_date, formatDate(xianyang));
        views.setTextViewText(R.id.xianyang_time, formatTime(xianyang));
        views.setTextViewText(R.id.tindouf_date, formatDate(tindouf));
        views.setTextViewText(R.id.tindouf_time, formatTime(tindouf));
        views.setTextViewText(R.id.xianyang_weather, WeatherRepository.summary(context, "xianyang"));
        views.setTextViewText(R.id.tindouf_weather, WeatherRepository.summary(context, "tindouf"));
        views.setContentDescription(R.id.xianyang_weather,
                WeatherRepository.description(context, "xianyang"));
        views.setContentDescription(R.id.tindouf_weather,
                WeatherRepository.description(context, "tindouf"));
        Intent attribution = new Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/"));
        views.setOnClickPendingIntent(R.id.weather_attribution, PendingIntent.getActivity(
                context, 2003, attribution, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        views.setContentDescription(
                R.id.clock_content,
                "咸阳 " + formatDate(xianyang) + " " + formatTime(xianyang)
                        + "，" + WeatherRepository.description(context, "xianyang")
                        + "；阿尔及利亚廷杜夫 " + formatDate(tindouf) + " " + formatTime(tindouf)
                        + "，" + WeatherRepository.description(context, "tindouf"));
        manager.updateAppWidget(appWidgetId, views);
    }

    private static String formatTime(Calendar calendar) {
        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE));
    }

    private static String formatDate(Calendar calendar) {
        String weekdays = "日一二三四五六";
        return String.format(
                Locale.CHINA,
                "%d月%d日 周%c",
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                weekdays.charAt(calendar.get(Calendar.DAY_OF_WEEK) - 1));
    }

    private static void scheduleUpdates(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        long elapsedNow = SystemClock.elapsedRealtime();
        long firstUpdate = elapsedNow + (UPDATE_INTERVAL_MILLIS - (elapsedNow % UPDATE_INTERVAL_MILLIS));
        alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                firstUpdate,
                UPDATE_INTERVAL_MILLIS,
                updatePendingIntent(context));
    }

    private static void cancelUpdates(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(updatePendingIntent(context));
        }
    }

    private static PendingIntent updatePendingIntent(Context context) {
        Intent intent = new Intent(context, ClockWidgetProvider.class);
        intent.setAction(ACTION_UPDATE_CLOCK);
        return PendingIntent.getBroadcast(
                context,
                UPDATE_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
