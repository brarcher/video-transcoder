package protect.videotranscoder.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;

import protect.videotranscoder.FFmpegUtil;
import protect.videotranscoder.ResultCallbackHandler;

import static protect.videotranscoder.activity.MainActivity.FFMPEG_ENCODE_ARGS;
import static protect.videotranscoder.activity.MainActivity.FFMPEG_OUTPUT_FILE;
import static protect.videotranscoder.activity.MainActivity.MESSENGER_INTENT_KEY;
import static protect.videotranscoder.activity.MainActivity.OUTPUT_DURATION_MS;
import static protect.videotranscoder.activity.MainActivity.OUTPUT_MIMETYPE;

public class FFmpegProcessService extends JobService
{
    private static final String TAG = "VideoTranscoder";

    private Messenger _activityMessenger;

    @Override
    public void onCreate()
    {
        super.onCreate();
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

    @Override
    public boolean onStartJob(final JobParameters params)
    {
        FFmpegUtil.init(getApplicationContext(), new ResultCallbackHandler<Boolean>()
        {
            @Override
            public void onResult(Boolean result)
            {
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

                            sendMessage(MessageId.JOB_FAILED_MSG, params.getJobId());
                        }

                        @Override
                        public void onSuccess(String s)
                        {
                            Log.d(TAG, "Success with output : " +s);
                            jobFinished(params, false);

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

                    FFmpegUtil.call(args, handler);
                }
                else
                {
                    sendMessage(MessageId.FFMPEG_UNSUPPORTED_MSG, params.getJobId());
                }
            }
        });

        // Return true as there's more work to be done with this job.
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params)
    {
        // Stop tracking these job parameters, as we've 'finished' executing.
        Log.i(TAG, "on stop job: " + params.getJobId());

        FFmpegUtil.cancelCall();

        sendMessage(MessageId.JOB_FAILED_MSG, params.getJobId());

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
