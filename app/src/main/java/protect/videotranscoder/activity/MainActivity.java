package protect.videotranscoder.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.VideoView;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.google.common.collect.ImmutableMap;

import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import protect.videotranscoder.FFmpegResponseHandler;
import protect.videotranscoder.FFmpegUtil;
import protect.videotranscoder.R;
import protect.videotranscoder.ResultCallbackHandler;

public class MainActivity extends AppCompatActivity
{
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 100;
    private VideoView videoView;
    private CrystalRangeSeekbar rangeSeekBar;
    private Runnable r;
    private ProgressDialog progressDialog;
    private Uri selectedVideoUri;
    private static final String TAG = "VideoTranscoder";
    private int stopPosition;
    private TextView tvLeft, tvRight;
    private String filePath;
    private int durationMs;

    final List<Integer> BASIC_SETTINGS_IDS = Collections.unmodifiableList(Arrays.asList(
            R.id.basicSettingsText,
            R.id.basicSettingsTopDivider,
            R.id.containerTypeContainer,
            R.id.containerTypeContainerDivider));
    final List<Integer> VIDEO_SETTINGS_IDS = Collections.unmodifiableList(Arrays.asList(
            R.id.videoSettingsText,
            R.id.videoSettingsTextTopDivider,
            R.id.videoCodecContainer,
            R.id.videoCodecContainerDivider,
            R.id.fpsContainer,
            R.id.fpsContainerDivider,
            R.id.resolutionContainer,
            R.id.resolutionContainerDivider,
            R.id.videoBitrateContainer,
            R.id.videoBitrateContainerDivider));
    final List<Integer> AUDIO_SETTINGS_IDS = Collections.unmodifiableList(Arrays.asList(
            R.id.audioSettingsText,
            R.id.audioSettingsTextTopDivider,
            R.id.audioCodecContainer,
            R.id.audioCodecContainerDivider,
            R.id.audioBitrateContainer,
            R.id.audioBitrateContainerDivider,
            R.id.audioSampleRateContainer,
            R.id.audioSampleRateContainerDivider,
            R.id.audioChannelContainer,
            R.id.audioChannelContainerDivider));

    private static final int READ_WRITE_PERMISSION_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView selectVideo = findViewById(R.id.selectVideo);

        tvLeft = findViewById(R.id.tvLeft);
        tvRight = findViewById(R.id.tvRight);

        videoView =  findViewById(R.id.videoView);
        rangeSeekBar =  findViewById(R.id.rangeSeekBar);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(null);
        progressDialog.setCancelable(false);
        rangeSeekBar.setEnabled(false);

        FFmpegUtil.init(this, new ResultCallbackHandler<Boolean>()
        {
            @Override
            public void onResult(Boolean result)
            {
                if(result == false)
                {
                    showUnsupportedExceptionDialog();
                }
            }
        });

        selectVideo.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (Build.VERSION.SDK_INT >= 23)
                {
                    getPermission();
                }
                else
                {
                    selectVideo();
                }
            }
        });
    }

    private void getPermission()
    {
        String[] params = null;
        String writeExternalStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        String readExternalStorage = Manifest.permission.READ_EXTERNAL_STORAGE;

        int hasWriteExternalStoragePermission = ActivityCompat.checkSelfPermission(this, writeExternalStorage);
        int hasReadExternalStoragePermission = ActivityCompat.checkSelfPermission(this, readExternalStorage);
        List<String> permissions = new ArrayList<String>();

        if (hasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED)
        {
            permissions.add(writeExternalStorage);
        }
        if (hasReadExternalStoragePermission != PackageManager.PERMISSION_GRANTED)
        {
            permissions.add(readExternalStorage);
        }

        if (!permissions.isEmpty())
        {
            params = permissions.toArray(new String[permissions.size()]);
        }
        if (params != null && params.length > 0)
        {
            ActivityCompat.requestPermissions(MainActivity.this,
                    params,
                    READ_WRITE_PERMISSION_REQUEST);
        }
        else
        {
            selectVideo();
        }
    }

    /**
     * Handling response for permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults)
    {
        if(requestCode == READ_WRITE_PERMISSION_REQUEST)
        {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                selectVideo();
            }
            else
            {
                new AlertDialog.Builder(MainActivity.this)
                    .setMessage(R.string.writePermissionExplanation)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.permissionRequestAgain, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                            getPermission();
                        }
                    })
                    .show();
            }
        }
    }

    /**
     * Opening gallery for uploading video
     */
    private void selectVideo()
    {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        String message = getResources().getString(R.string.selectVideo);
        startActivityForResult(Intent.createChooser(intent, message), REQUEST_TAKE_GALLERY_VIDEO);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        stopPosition = videoView.getCurrentPosition(); //stopPosition is an int
        videoView.pause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        videoView.seekTo(stopPosition);
        videoView.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO)
            {
                for(int id : BASIC_SETTINGS_IDS)
                {
                    findViewById(id).setVisibility(View.VISIBLE);
                }
                for(int id : VIDEO_SETTINGS_IDS)
                {
                    findViewById(id).setVisibility(View.VISIBLE);
                }
                for(int id : AUDIO_SETTINGS_IDS)
                {
                    findViewById(id).setVisibility(View.VISIBLE);
                }

                selectedVideoUri = data.getData();
                videoView.setVideoURI(selectedVideoUri);
                videoView.start();

                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
                {

                    @Override
                    public void onPrepared(MediaPlayer mp)
                    {
                        durationMs = mp.getDuration();
                        tvLeft.setVisibility(View.VISIBLE);
                        tvLeft.setText("00:00:00");
                        tvRight.setVisibility(View.VISIBLE);
                        tvRight.setText(getTime(durationMs / 1000));
                        mp.setLooping(true);

                        rangeSeekBar.setMinValue(0);
                        rangeSeekBar.setMaxValue(durationMs / 1000f);
                        rangeSeekBar.setEnabled(true);
                        rangeSeekBar.setVisibility(View.VISIBLE);

                        rangeSeekBar.setOnRangeSeekbarChangeListener(new OnRangeSeekbarChangeListener()
                        {
                            @Override
                            public void valueChanged(Number minValue, Number maxValue)
                            {
                                videoView.seekTo(minValue.intValue() * 1000);
                                tvLeft.setText(getTime(minValue.intValue()));
                                tvRight.setText(getTime(maxValue.intValue()));
                            }
                        });

                        final Handler handler = new Handler();
                        handler.postDelayed(r = new Runnable()
                        {
                            @Override
                            public void run()
                            {

                                if (videoView.getCurrentPosition() >= rangeSeekBar.getSelectedMaxValue().intValue() * 1000)
                                    videoView.seekTo(rangeSeekBar.getSelectedMinValue().intValue() * 1000);
                                handler.postDelayed(r, 1000);
                            }
                        }, 1000);

                    }
                });
            }
        }
    }

    private String getTime(int seconds)
    {
        int hr = seconds / 3600;
        int rem = seconds % 3600;
        int mn = rem / 60;
        int sec = rem % 60;
        return String.format("%02d", hr) + ":" + String.format("%02d", mn) + ":" + String.format("%02d", sec);
    }

    private void showUnsupportedExceptionDialog()
    {
        new AlertDialog.Builder(MainActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Not Supported")
                .setMessage("Device Not Supported")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        MainActivity.this.finish();
                    }
                })
                .create()
                .show();

    }

    private ResultCallbackHandler<Boolean> _transcodeResultHandler = new ResultCallbackHandler<Boolean>()
    {
        @Override
        public void onResult(Boolean result)
        {
            String message;

            if(result)
            {
                message = getResources().getString(R.string.transcodeSuccess, filePath);
            }
            else
            {
                message = getResources().getString(R.string.transcodeFailed);
            }

            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(message)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    })
                    .show();

            videoView.seekTo(stopPosition);
            videoView.start();
        }
    };

    /**
     * Command for cutting video
     */
    private void executeCutVideoCommand(int startMs, int endMs)
    {
        File moviesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
        );

        String filePrefix = "cut_video";
        String fileExtn = ".mp4";
        String yourRealPath = getPath(MainActivity.this, selectedVideoUri);
        File dest = new File(moviesDir, filePrefix + fileExtn);
        int fileNo = 0;
        while (dest.exists())
        {
            fileNo++;
            dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        }

        Log.d(TAG, "startTrim: src: " + yourRealPath);
        Log.d(TAG, "startTrim: dest: " + dest.getAbsolutePath());
        Log.d(TAG, "startTrim: startMs: " + startMs);
        Log.d(TAG, "startTrim: endMs: " + endMs);
        filePath = dest.getAbsolutePath();

        final String[] complexCommand = {"-ss", "" + startMs / 1000, "-y", "-i", yourRealPath, "-t", "" + (endMs - startMs) / 1000,"-vcodec", "mpeg4", "-b:v", "2097152", "-b:a", "48000", "-ac", "2", "-ar", "22050", filePath};

        FFmpegResponseHandler handler = new FFmpegResponseHandler(durationMs, progressDialog, _transcodeResultHandler);
        FFmpegUtil.call(complexCommand, handler);

        stopPosition = videoView.getCurrentPosition(); //stopPosition is an int
        videoView.pause();
    }

    /**
     * Command for compressing video
     */
    private void executeCompressCommand()
    {
        File moviesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
        );

        String filePrefix = "compress_video";
        String fileExtn = ".mp4";
        String yourRealPath = getPath(MainActivity.this, selectedVideoUri);


        File dest = new File(moviesDir, filePrefix + fileExtn);
        int fileNo = 0;
        while (dest.exists())
        {
            fileNo++;
            dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        }

        Log.d(TAG, "startTrim: src: " + yourRealPath);
        Log.d(TAG, "startTrim: dest: " + dest.getAbsolutePath());
        filePath = dest.getAbsolutePath();
        String[] complexCommand = {"-y", "-i", yourRealPath, "-s", "160x120", "-r", "25", "-vcodec", "mpeg4", "-b:v", "150k", "-b:a", "48000", "-ac", "2", "-ar", "22050", filePath};

        FFmpegResponseHandler handler = new FFmpegResponseHandler(durationMs, progressDialog, _transcodeResultHandler);
        FFmpegUtil.call(complexCommand, handler);

        stopPosition = videoView.getCurrentPosition();
        videoView.pause();
    }

    /**
     * Command for extracting audio from video
     */
    private void extractAudioVideo()
    {
        File moviesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC
        );

        String filePrefix = "extract_audio";
        String fileExtn = ".mp3";
        String yourRealPath = getPath(MainActivity.this, selectedVideoUri);
        File dest = new File(moviesDir, filePrefix + fileExtn);

        int fileNo = 0;
        while (dest.exists())
        {
            fileNo++;
            dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        }
        Log.d(TAG, "startTrim: src: " + yourRealPath);
        Log.d(TAG, "startTrim: dest: " + dest.getAbsolutePath());
        filePath = dest.getAbsolutePath();

        String[] complexCommand = {"-y", "-i", yourRealPath, "-vn", "-ar", "44100", "-ac", "2", "-b:a", "256k", "-f", "mp3", filePath};

        FFmpegResponseHandler handler = new FFmpegResponseHandler(durationMs, progressDialog, _transcodeResultHandler);
        FFmpegUtil.call(complexCommand, handler);

        stopPosition = videoView.getCurrentPosition(); //stopPosition is an int
        videoView.pause();
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     */
    private String getPath(final Context context, final Uri uri)
    {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri))
        {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri))
            {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type))
                {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri))
            {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri))
            {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type))
                {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                }
                else if ("video".equals(type))
                {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                }
                else if ("audio".equals(type))
                {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]
                {
                    split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme()))
        {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme()))
        {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri.
     */
    private String getDataColumn(Context context, Uri uri, String selection,
                                 String[] selectionArgs)
    {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection =
        {
            column
        };

        try
        {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst())
            {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private boolean isExternalStorageDocument(Uri uri)
    {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private boolean isDownloadsDocument(Uri uri)
    {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private boolean isMediaDocument(Uri uri)
    {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if(id == R.id.action_about)
        {
            displayAboutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void displayAboutDialog()
    {
        final Map<String, String> USED_LIBRARIES = ImmutableMap.of
        (
            "Commons IO", "https://commons.apache.org/proper/commons-io/",
            "FFmpeg", "https://www.ffmpeg.org/",
            "FFmpeg Android", "http://writingminds.github.io/ffmpeg-android/",
            "Guava", "https://github.com/google/guava",
            "Crystal Range Seekbar", "https://github.com/syedowaisali/crystal-range-seekbar"
        );

        final Map<String, String> USED_ASSETS = ImmutableMap.of
        (
            "Film by Mint Shirt", "https://thenounproject.com/term/film/395618/"
        );

        StringBuilder libs = new StringBuilder().append("<ul>");
        for (Map.Entry<String, String> entry : USED_LIBRARIES.entrySet())
        {
            libs.append("<li><a href=\"").append(entry.getValue()).append("\">").append(entry.getKey()).append("</a></li>");
        }
        libs.append("</ul>");

        StringBuilder resources = new StringBuilder().append("<ul>");
        for (Map.Entry<String, String> entry : USED_ASSETS.entrySet())
        {
            resources.append("<li><a href=\"").append(entry.getValue()).append("\">").append(entry.getKey()).append("</a></li>");
        }
        resources.append("</ul>");

        String appName = getString(R.string.app_name);
        int year = Calendar.getInstance().get(Calendar.YEAR);

        String version = "?";
        try
        {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Log.w(TAG, "Package name not found", e);
        }

        WebView wv = new WebView(this);
        String html =
            "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />" +
            "<h1>" +
            String.format(getString(R.string.about_title_fmt),
                    "<a href=\"" + getString(R.string.app_webpage_url)) + "\">" +
            appName +
            "</a>" +
            "</h1><p>" +
            appName +
            " " +
            String.format(getString(R.string.debug_version_fmt), version) +
            "</p><p>" +
            String.format(getString(R.string.app_revision_fmt),
                    "<a href=\"" + getString(R.string.app_revision_url) + "\">" +
                            getString(R.string.app_revision_url) +
                            "</a>") +
            "</p><hr/><p>" +
            String.format(getString(R.string.app_copyright_fmt), year) +
            "</p><hr/><p>" +
            getString(R.string.app_license) +
            "</p><hr/><p>" +
            String.format(getString(R.string.app_libraries), appName, libs.toString()) +
            "</p><hr/><p>" +
            String.format(getString(R.string.app_resources), appName, resources.toString());

        wv.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "utf-8", null);
        new AlertDialog.Builder(this)
                .setView(wv)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                })
                .show();
    }

}
