package protect.videotranscoder;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;

public class FFmpegResponseHandler extends ExecuteBinaryResponseHandler
{
    private static final String TAG = "VideoTranscoder";

    private final Context _context;
    private final Intent _intentToLaunch;
    private final long _durationMs;
    private final ProgressDialog _progressDialog;

    public FFmpegResponseHandler(Context context, Intent intent, long durationMs, ProgressDialog progressDialog)
    {
        _context = context;
        _intentToLaunch = intent;
        _durationMs = durationMs;
        _progressDialog = progressDialog;
    }

    @Override
    public void onFailure(String s)
    {
        Log.d(TAG, "Failed with output : " + s);
    }

    @Override
    public void onSuccess(String s)
    {
        Log.d(TAG, "Success with output : " +s);
        _context.startActivity(_intentToLaunch);
    }

    @Override
    public void onProgress(String s)
    {
        // Progress updates look like the following:
        // frame=   15 fps=7.1 q=2.0 size=      26kB time=00:00:00.69 bitrate= 309.4kbits/s dup=6 drop=0 speed=0.329x

        Log.d(TAG, "progress : " + s);

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

        if(currentTimeMs != null)
        {
            int percentComplete = (int)Math.floor((currentTimeMs * 100) / (float)_durationMs);

            _progressDialog.setMessage(percentComplete + "%");
        }
    }

    @Override
    public void onStart()
    {
        Log.d(TAG, "Started FFmpeg command");
        _progressDialog.setMessage("Processing...");
        _progressDialog.show();
    }

    @Override
    public void onFinish()
    {
        Log.d(TAG, "Finished command");
        _progressDialog.dismiss();
    }
}
