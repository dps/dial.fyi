package io.singleton.wearface;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.app.job.JobParameters;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ACJobService extends JobService {

    private static final String TAG = "ACJobService";
    private static final long TWENTY_FOUR_HOURS_MILLIS = 24 * 60 * 60 * 1000;
    private static final int JOB_ID_UPDATE = 1;


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Override
    public boolean onStartJob(JobParameters params) {
        if (params.getJobId() == JOB_ID_UPDATE) {
            return onUpdateJobStart(params);
        }
        return false;
    }

    private boolean onUpdateJobStart(JobParameters params) {
        ImageDownloadingStore.getInstance(this).updateUrls();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    /** Send job to the JobScheduler. */
    private static void scheduleJob(Context ctx, JobInfo t) {
        JobScheduler tm =
                (JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        tm.schedule(t);
    }

    public static void scheduleUpdateJob(Context ctx) {
        JobInfo.Builder bld = new JobInfo.Builder(JOB_ID_UPDATE,
                new ComponentName(ctx, ACJobService.class));
        bld.setPeriodic(TWENTY_FOUR_HOURS_MILLIS);
        bld.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        bld.setPersisted(true);
        scheduleJob(ctx, bld.build());
    }

    public void cancelAllJobs() {
        JobScheduler tm =
                (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        tm.cancelAll();
    }

}
