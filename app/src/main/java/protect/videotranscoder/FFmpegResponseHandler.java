package protect.videotranscoder;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;

public class FFmpegResponseHandler extends ExecuteBinaryResponseHandler
{
    private static final String TAG = "VideoTranscoder";

    private final Context _context;
    private final long _durationMs;
    private final ProgressDialog _progressDialog;
    private final ResultCallbackHandler<Boolean> _resultHandler;

    public FFmpegResponseHandler(Context context, long durationMs, ProgressDialog progressDialog,
                                 ResultCallbackHandler<Boolean> resultHandler)
    {
        _context = context;
        _durationMs = durationMs;
        _progressDialog = progressDialog;
        _resultHandler = resultHandler;
    }

    @Override
    public void onFailure(String s)
    {
        Log.d(TAG, "Failed with output : " + s);
        _resultHandler.onResult(false);
    }

    @Override
    public void onSuccess(String s)
    {
        Log.d(TAG, "Success with output : " +s);
        _resultHandler.onResult(true);
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
