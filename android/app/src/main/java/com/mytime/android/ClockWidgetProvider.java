package com.mytime.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.TypedValue;
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
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_UPDATE_CLOCK.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName provider = new ComponentName(context, ClockWidgetProvider.class);
            int[] appWidgetIds = manager.getAppWidgetIds(provider);
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, manager, appWidgetId);
            }
        }
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        Calendar beijing = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
        Calendar tindouf = Calendar.getInstance(TimeZone.getTimeZone("Africa/Algiers"));

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_clock);
        applyWidgetSize(context, manager, appWidgetId, views);
        views.setTextViewText(R.id.beijing_date, formatDate(beijing));
        views.setTextViewText(R.id.beijing_time, formatTime(beijing));
        views.setTextViewText(R.id.tindouf_date, formatDate(tindouf));
        views.setTextViewText(R.id.tindouf_time, formatTime(tindouf));
        views.setContentDescription(
                R.id.clock_content,
                "北京时间 " + formatDate(beijing) + " " + formatTime(beijing)
                        + "，阿尔及利亚廷杜夫 " + formatDate(tindouf) + " " + formatTime(tindouf));
        manager.updateAppWidget(appWidgetId, views);
    }

    private static void applyWidgetSize(Context context, AppWidgetManager manager,
                                        int appWidgetId, RemoteViews views) {
        int height = manager.getAppWidgetOptions(appWidgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40);
        boolean compact = height < 64;
        float compactScale = height > 0 && height < 48 ? height / 48f : 1f;
        float placeSize = compact ? 10 * compactScale : 16;
        float dateSize = compact ? 8 * compactScale : 13;
        float timeSize = compact ? 18 * compactScale : 30;
        views.setTextViewTextSize(R.id.beijing_place, TypedValue.COMPLEX_UNIT_SP, placeSize);
        views.setTextViewTextSize(R.id.tindouf_place, TypedValue.COMPLEX_UNIT_SP, placeSize);
        views.setTextViewTextSize(R.id.beijing_date, TypedValue.COMPLEX_UNIT_SP, dateSize);
        views.setTextViewTextSize(R.id.tindouf_date, TypedValue.COMPLEX_UNIT_SP, dateSize);
        views.setTextViewTextSize(R.id.beijing_time, TypedValue.COMPLEX_UNIT_SP, timeSize);
        views.setTextViewTextSize(R.id.tindouf_time, TypedValue.COMPLEX_UNIT_SP, timeSize);
        float density = context.getResources().getDisplayMetrics().density;
        int horizontalPadding = Math.round(14 * density);
        int verticalPadding = Math.round((compact ? 1 : 10) * density);
        views.setViewPadding(R.id.clock_content, horizontalPadding, verticalPadding,
                horizontalPadding, verticalPadding);
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
