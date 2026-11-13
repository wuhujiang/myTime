package com.mytime.android;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

/** Network work runs outside the widget broadcast and is deferred when offline. */
public final class WeatherUpdateService extends JobService {
    private static final int PERIODIC_JOB_ID = 2001;
    private static final int INITIAL_JOB_ID = 2002;
    private final ConcurrentHashMap<Integer, Thread> workers = new ConcurrentHashMap<>();

    static void schedule(Context context) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        ComponentName service = new ComponentName(context, WeatherUpdateService.class);
        if (scheduler == null) {
            return;
        }
        if (!hasPendingJob(scheduler, PERIODIC_JOB_ID)) {
            scheduler.schedule(new JobInfo.Builder(PERIODIC_JOB_ID, service)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(WeatherRepository.REFRESH_INTERVAL_MILLIS)
                    .setPersisted(true)
                    .build());
        }
        if (WeatherRepository.needsRefresh(context) && !hasPendingJob(scheduler, INITIAL_JOB_ID)) {
            scheduler.schedule(new JobInfo.Builder(INITIAL_JOB_ID, service)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build());
        }
    }

    private static boolean hasPendingJob(JobScheduler scheduler, int id) {
        // getPendingJob(id) requires API 24; this widget also supports Android 6 (API 23).
        for (JobInfo job : scheduler.getAllPendingJobs()) {
            if (job.getId() == id) {
                return true;
            }
        }
        return false;
    }

    static void cancel(Context context) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler != null) {
            scheduler.cancel(PERIODIC_JOB_ID);
            scheduler.cancel(INITIAL_JOB_ID);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Thread worker = new Thread(() -> {
            try {
                ComponentName provider = new ComponentName(this, ClockWidgetProvider.class);
                if (AppWidgetManager.getInstance(this).getAppWidgetIds(provider).length == 0) {
                    cancel(this);
                    return;
                }
                WeatherRepository.refresh(this);
            } catch (Exception error) {
                Log.w("myTimeWeather", "Weather unavailable; keeping cached weather", error);
            } finally {
                if (!Thread.currentThread().isInterrupted()) {
                    ClockWidgetProvider.updateAllWidgets(this);
                    jobFinished(params, false);
                }
                workers.remove(params.getJobId(), Thread.currentThread());
            }
        }, "myTime-weather-" + params.getJobId());
        workers.put(params.getJobId(), worker);
        worker.start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Thread worker = workers.remove(params.getJobId());
        if (worker != null) {
            worker.interrupt();
        }
        return true;
    }

    @Override
    public void onDestroy() {
        for (Thread worker : workers.values()) {
            worker.interrupt();
        }
        workers.clear();
        super.onDestroy();
    }
}
