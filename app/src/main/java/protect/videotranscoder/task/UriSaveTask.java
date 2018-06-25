package protect.videotranscoder.task;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ProgressBar;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import protect.videotranscoder.R;
import protect.videotranscoder.ResultCallbackHandler;

public class UriSaveTask extends AsyncTask<Void, Void, Boolean>
{
    private static final String TAG = "VideoTranscoder";

    private Context context;
    private Uri uri;
    private File output;
    private ResultCallbackHandler<Boolean> callback;

    private AlertDialog dialog;
    private boolean wasCanceled;

    public UriSaveTask(Context context, Uri uri, File output, ResultCallbackHandler<Boolean> callback)
    {
        this.context = context;
        this.uri = uri;
        this.output = output;
        this.callback = callback;
    }

    @Override
    protected void onPreExecute()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        ProgressBar progress = new ProgressBar(context,null, android.R.attr.progressBarStyleLarge);

        builder.setTitle(R.string.receivingSharedData);
        builder.setView(progress);
        builder.setCancelable(false);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                wasCanceled = true;
            }
        });

        wasCanceled = false;
        dialog = builder.show();
    }

    @Override
    protected Boolean doInBackground(Void... voids)
    {
        boolean result = false;

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try
        {
            inputStream = context.getContentResolver().openInputStream(uri);
            if(inputStream != null)
            {
                outputStream = new FileOutputStream(output);

                ByteStreams.copy(inputStream, outputStream);
                outputStream.flush();

                result = true;
            }

        }
        catch (IOException e)
        {
            Log.w(TAG, "Failed to open stream for share intent data", e);
        }
        finally
        {
            if(inputStream != null)
            {
                Closeables.closeQuietly(inputStream);
            }

            if(outputStream != null)
            {
                try
                {
                    outputStream.close();
                }
                catch (IOException e)
                {
                    Log.w(TAG, "Failed to close stream for share intent data", e);
                }
            }

        }

        return result;
    }

    protected void onPostExecute(Boolean result)
    {
        dialog.dismiss();

        if(wasCanceled)
        {
            boolean deleted = output.delete();
            if(deleted == false)
            {
                Log.w(TAG, "Uri save canceled, but output file failed to be deleted");
            }
            result = false;
        }

        callback.onResult(result);
    }
}
