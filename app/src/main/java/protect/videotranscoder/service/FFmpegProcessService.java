package protect.videotranscoder.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;

import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import protect.videotranscoder.FFmpegUtil;
import protect.videotranscoder.R;
import protect.videotranscoder.ResultCallbackHandler;
import protect.videotranscoder.activity.MainActivity;

import static protect.videotranscoder.activity.MainActivity.FFMPEG_ENCODE_ARGS;
import static protect.videotranscoder.activity.MainActivity.FFMPEG_FAILURE_MSG;
import static protect.videotranscoder.activity.MainActivity.FFMPEG_OUTPUT_FILE;
import static protect.videotranscoder.activity.MainActivity.MESSENGER_INTENT_KEY;
import static protect.videotranscoder.activity.MainActivity.OUTPUT_DURATION_MS;
import static protect.videotranscoder.activity.MainActivity.OUTPUT_MIMETYPE;

public class FFmpegProcessService extends JobService
{
    private static final String TAG = "VideoTranscoder";
    private static final int NOTIFICATION_ID = 1;

    private Messenger _activityMessenger;
    NotificationManager _notificationManager;

    @Override
    public void onCreate()
    {
        super.onCreate();

        _notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Log.i(TAG, "Service created");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.i(TAG, "Service destroyed");
    }

    /**
     * When the app's MainActivity is created, it starts this service. This is so that the
     * activity and this service can communicate back and forth. See "setUiCallback()"
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        _activityMessenger = intent.getParcelableExtra(MESSENGER_INTENT_KEY);
        return START_NOT_STICKY;
    }

    private void setNotification(String filename)
    {
        String message = String.format(getString(R.string.encodingNotification), filename);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setOngoing(true)
                        .setSmallIcon(R.drawable.encoding_notification)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(message);

        // Creates an explicit intent for the Activity
        Intent resultIntent = new Intent(this, MainActivity.class);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
                resultIntent, 0);
        builder.setContentIntent(resultPendingIntent);

        _notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void clearNotification()
    {
        _notificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public boolean onStartJob(final JobParameters params)
    {
        boolean result = FFmpegUtil.init(getApplicationContext());

        if(result)
        {
            sendMessage(MessageId.JOB_START_MSG, params.getJobId());

            final String [] args = params.getExtras().getStringArray(FFMPEG_ENCODE_ARGS);
            final String outputFile = params.getExtras().getString(FFMPEG_OUTPUT_FILE);
            final String mimetype = params.getExtras().getString(OUTPUT_MIMETYPE);
            final Integer durationMs = params.getExtras().getInt(OUTPUT_DURATION_MS);

            ExecuteBinaryResponseHandler handler = new ExecuteBinaryResponseHandler()
            {
                @Override
                public void onFailure(String s)
                {
                    Log.d(TAG, "Failed with output : " + s);
                    jobFinished(params, false);
                    clearNotification();

                    // The last line of the output should be the failure message
                    String [] lines = s.split("\n");
                    String failureMg = lines[lines.length-1].trim();

                    Bundle bundle = new Bundle();
                    bundle.putString(FFMPEG_FAILURE_MSG, failureMg);
                    sendMessage(MessageId.JOB_FAILED_MSG, bundle);
                }

                @Override
                public void onSuccess(String s)
                {
                    Log.d(TAG, "Success with output : " +s);
                    jobFinished(params, false);
                    clearNotification();

                    Bundle bundle = new Bundle();
                    bundle.putString(FFMPEG_OUTPUT_FILE, outputFile);
                    bundle.putString(OUTPUT_MIMETYPE, mimetype);
                    sendMessage(MessageId.JOB_SUCCEDED_MSG, bundle);
                }

                @Override
                public void onProgress(String s)
                {
                    // Progress updates look like the following:
                    // frame=   15 fps=7.1 q=2.0 size=      26kB time=00:00:00.69 bitrate= 309.4kbits/s dup=6 drop=0 speed=0.329x
                    Long currentTimeMs = null;

                    String [] split = s.split(" ");
                    for(String item : split)
                    {
                        if(item.startsWith("time="))
                        {
                            item = item.replace("time=", "");
                            currentTimeMs = FFmpegUtil.timestampToMs(item);
                            break;
                        }
                    }

                    Integer percentComplete = null;

                    if(currentTimeMs != null && currentTimeMs > 0)
                    {
                        percentComplete = (int)Math.floor((currentTimeMs * 100) / (float)durationMs);
                    }

                    sendMessage(MessageId.JOB_PROGRESS_MSG, percentComplete);
                }
            };

            if(outputFile != null)
            {
                setNotification(new File(outputFile).getName());
            }

            FFmpegUtil.call(args, handler);
        }
        else
        {
            sendMessage(MessageId.FFMPEG_UNSUPPORTED_MSG, params.getJobId());
        }

        // Return true as there's more work to be done with this job.
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params)
    {
        // Stop tracking these job parameters, as we've 'finished' executing.
        Log.i(TAG, "on stop job: " + params.getJobId());

        clearNotification();

        FFmpegUtil.cancelCall();

        Bundle bundle = new Bundle();
        bundle.putString(FFMPEG_FAILURE_MSG, getResources().getString(R.string.encodeCanceled));
        sendMessage(MessageId.JOB_FAILED_MSG, bundle);

        // Return false to drop the job.
        return false;
    }

    private void sendMessage(MessageId messageId, @Nullable Object params) {
        // If this service is launched by the JobScheduler, there's no callback Messenger. It
        // only exists when the MainActivity calls startService() with the callback in the Intent.
        if (_activityMessenger == null)
        {
            Log.d(TAG, "Service is bound, not started. There's no callback to send a message to.");
            return;
        }

        Message m = Message.obtain();
        m.what = messageId.ordinal();
        m.obj = params;
        try
        {
            _activityMessenger.send(m);
        }
        catch (RemoteException e)
        {
            Log.e(TAG, "Error passing service object back to activity.");
        }
    }
}
