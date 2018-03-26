package protect.videotranscoder.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.util.List;

import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import protect.videotranscoder.FFmpegUtil;
import protect.videotranscoder.R;
import protect.videotranscoder.activity.MainActivity;
import protect.videoeditor.IFFmpegProcessService;

import static protect.videotranscoder.activity.MainActivity.FFMPEG_FAILURE_MSG;
import static protect.videotranscoder.activity.MainActivity.FFMPEG_OUTPUT_FILE;
import static protect.videotranscoder.activity.MainActivity.MESSENGER_INTENT_KEY;
import static protect.videotranscoder.activity.MainActivity.OUTPUT_MIMETYPE;

public class FFmpegProcessService extends Service
{
    private static final String TAG = "VideoTranscoder";
    private static final int NOTIFICATION_ID = 1;

    private Messenger _activityMessenger;


    /**
     * When the app's MainActivity is created, it starts this service. This is so that the
     * activity and this service can communicate back and forth. See "setUiCallback()"
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i(TAG, "Received start command from activity");
        _activityMessenger = intent.getParcelableExtra(MESSENGER_INTENT_KEY);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.i(TAG, "Received binding.");
        return mBinder;
    }

    private final IFFmpegProcessService.Stub mBinder = new IFFmpegProcessService.Stub()
    {
        @Override
        public boolean startEncode(final List<String> ffmpegArgs, final String outputFile, final String mimetype, final int durationMs) throws RemoteException
        {
            boolean result = FFmpegUtil.init(getApplicationContext());

            if (result)
            {
                final String[] args = ffmpegArgs.toArray(new String[ffmpegArgs.size()]);

                sendMessage(MessageId.JOB_START_MSG, outputFile);

                ExecuteBinaryResponseHandler handler = new ExecuteBinaryResponseHandler()
                {
                    @Override
                    public void onFailure(String s)
                    {
                        Log.d(TAG, "Failed with output : " + s);
                        clearNotification();

                        // The last line of the output should be the failure message
                        String[] lines = s.split("\n");
                        String failureMg = lines[lines.length - 1].trim();

                        Bundle bundle = new Bundle();
                        bundle.putString(FFMPEG_FAILURE_MSG, failureMg);
                        sendMessage(MessageId.JOB_FAILED_MSG, bundle);
                    }

                    @Override
                    public void onSuccess(String s)
                    {
                        Log.d(TAG, "Success with output : " + s);
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

                        String[] split = s.split(" ");
                        for (String item : split)
                        {
                            if (item.startsWith("time="))
                            {
                                item = item.replace("time=", "");
                                currentTimeMs = FFmpegUtil.timestampToMs(item);
                                break;
                            }
                        }

                        Integer percentComplete = null;

                        if (currentTimeMs != null && currentTimeMs > 0)
                        {
                            percentComplete = (int) Math.floor((currentTimeMs * 100) / (float) durationMs);
                        }

                        sendMessage(MessageId.JOB_PROGRESS_MSG, percentComplete);
                    }
                };

                if (outputFile != null)
                {
                    setNotification(new File(outputFile).getName());
                }

                FFmpegUtil.call(args, handler);
            }
            else
            {
                sendMessage(MessageId.FFMPEG_UNSUPPORTED_MSG, null);
            }

            return result;
        }

        @Override
        public void cancel() throws RemoteException
        {
            Log.i(TAG, "Received cancel");

            clearNotification();

            FFmpegUtil.cancelCall();

            Bundle bundle = new Bundle();
            bundle.putString(FFMPEG_FAILURE_MSG, getResources().getString(R.string.encodeCanceled));
            sendMessage(MessageId.JOB_FAILED_MSG, bundle);
        }

        @Override
        public boolean isEncoding() throws RemoteException
        {
            return FFmpegUtil.isFFmpegRunning();
        }
    };

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

        _activityMessenger = null;
        FFmpegUtil.cancelCall();

        Log.i(TAG, "Service destroyed");
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

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void clearNotification()
    {
        stopForeground(true);
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
            Log.e(TAG, "Error passing service object back to activity.", e);
        }
    }
}
