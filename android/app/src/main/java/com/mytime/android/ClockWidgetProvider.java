package com.mytime.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
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
        String beijingTime = formatTime("Asia/Shanghai");
        String tindoufTime = formatTime("Africa/Algiers");

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_clock);
        views.setTextViewText(R.id.beijing_time, beijingTime);
        views.setTextViewText(R.id.tindouf_time, tindoufTime);
        views.setContentDescription(
                R.id.clock_content,
                "北京时间 " + beijingTime + "，阿尔及利亚廷杜夫 " + tindoufTime);
        manager.updateAppWidget(appWidgetId, views);
    }

    private static String formatTime(String timeZoneId) {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId));
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.getTime());
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
