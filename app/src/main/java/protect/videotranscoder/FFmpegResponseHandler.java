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
    private final ProgressDialog _progressDialog;

    public FFmpegResponseHandler(Context context, Intent intent, ProgressDialog progressDialog)
    {
        _context = context;
        _intentToLaunch = intent;
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
        _progressDialog.setMessage("progress : " + s);
        Log.d(TAG, "progress : " + s);
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
