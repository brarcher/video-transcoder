package protect.videotranscoder;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

/**
 * Utility class for executing ffmpeg
 */
public class FFmpegUtil
{
    private static final String TAG = "VideoTranscoder";
    private static FFmpeg ffmpeg;

    /**
     * Load FFmpeg binary
     */
    public static void init(final Context context, @NonNull final ResultCallbackHandler<Boolean> resultHandler)
    {
        if (ffmpeg == null)
        {
            Log.d(TAG, "Creating FFmpeg instance");
            final FFmpeg tmpFfmpeg = FFmpeg.getInstance(context.getApplicationContext());

            try
            {
                tmpFfmpeg.loadBinary(new LoadBinaryResponseHandler()
                {
                    @Override
                    public void onFailure()
                    {
                        Log.d(TAG, "FFmpeg load failed");
                        resultHandler.onResult(false);
                    }

                    @Override
                    public void onSuccess()
                    {
                        Log.d(TAG, "FFmpeg load succeeded");
                        ffmpeg = tmpFfmpeg;
                        resultHandler.onResult(true);
                    }
                });
            }
            catch (FFmpegNotSupportedException e)
            {
                Log.d(TAG, "Failed to load FFmpeg", e);
                resultHandler.onResult(false);
            }
        }
    }

    /**
     * Executing Ffmpeg binary with arguments
     */
    public static void call(final String[] command, @NonNull final ExecuteBinaryResponseHandler handler)
    {
        if(ffmpeg == null)
        {
            String message = "Command failed, FFmpeg not initialized";
            Log.d(TAG, message);

            handler.onFailure(message);
            return;
        }

        try
        {
            Log.d(TAG, "Executing command: " + commandToString(command));
            ffmpeg.execute(command, handler);
        }
        catch (FFmpegCommandAlreadyRunningException e)
        {
            String message = "Command failed, FFmpeg already running";
            Log.d(TAG, message);

            handler.onFailure(message);
            // do nothing for now
        }
    }

    @NonNull
    private static String commandToString(String [] cmd)
    {
        final StringBuilder commandBuilder = new StringBuilder();
        for(String part : cmd)
        {
            commandBuilder.append(part);
            commandBuilder.append(" ");
        }

        return commandBuilder.toString().trim();
    }
}
