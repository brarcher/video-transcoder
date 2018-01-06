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
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import protect.videotranscoder.R;

/*
ffmpeg version n3.0.1 Copyright (c) 2000-2016 the FFmpeg developers
built with gcc 4.8 (GCC)
configuration: --target-os=linux
               --cross-prefix=/home/vagrant/SourceCode/ffmpeg-android/toolchain-android/bin/arm-linux-androideabi-
               --arch=arm
               --cpu=cortex-a8
               --enable-runtime-cpudetect
               --sysroot=/home/vagrant/SourceCode/ffmpeg-android/toolchain-android/sysroot
               --enable-pic
               --enable-libx264
               --enable-libass
               --enable-libfreetype
               --enable-libfribidi
               --enable-libmp3lame
               --enable-fontconfig
               --enable-pthreads
               --disable-debug
               --disable-ffserver
               --enable-version3
               --enable-hardcoded-tables
               --disable-ffplay
               --disable-ffprobe
               --enable-gpl
               --enable-yasm
               --disable-doc
               --disable-shared
               --enable-static
               --pkg-config=/home/vagrant/SourceCode/ffmpeg-android/ffmpeg-pkg-config
               --prefix=/home/vagrant/SourceCode/ffmpeg-android/build/armeabi-v7a
               --extra-cflags='-I/home/vagrant/SourceCode/ffmpeg-android/toolchain-android/include
                -U_FORTIFY_SOURCE
               -D_FORTIFY_SOURCE=2
               -fno-strict-overflow
               -fstack-protector-all'
               --extra-ldflags='-L/home/vagrant/SourceCode/ffmpeg-android/toolchain-android/lib
               -Wl,-z,relro
               -Wl,-z,now -pie'
               --extra-libs='-lpng -lexpat -lm' --extra-cxxflags=
libavutil      55. 17.103 / 55. 17.103
libavcodec     57. 24.102 / 57. 24.102
libavformat    57. 25.100 / 57. 25.100
libavdevice    57.  0.101 / 57.  0.101
libavfilter     6. 31.100 /  6. 31.100
libswscale      4.  0.100 /  4.  0.100
libswresample   2.  0.101 /  2.  0.101
libpostproc    54.  0.100 / 54.  0.100


 */

public class MainActivity extends AppCompatActivity
{
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 100;
    private VideoView videoView;
    private RangeSeekBar rangeSeekBar;
    private Runnable r;
    private FFmpeg ffmpeg;
    private ProgressDialog progressDialog;
    private Uri selectedVideoUri;
    private static final String TAG = "VideoEditor";
    private static final String FILEPATH = "filepath";
    private int choice = 0;
    private int stopPosition;
    private ScrollView mainlayout;
    private TextView tvLeft, tvRight;
    private String filePath;
    private int duration;

    private static final int READ_WRITE_PERMISSION_REQUEST = 1;
    private static final int AUDIO_PERMISSION_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView uploadVideo = findViewById(R.id.uploadVideo);
        TextView cutVideo = findViewById(R.id.cropVideo);
        TextView compressVideo = findViewById(R.id.compressVideo);
        TextView extractImages = findViewById(R.id.extractImages);

        tvLeft = findViewById(R.id.tvLeft);
        tvRight = findViewById(R.id.tvRight);

        final TextView extractAudio = findViewById(R.id.extractAudio);
        videoView =  findViewById(R.id.videoView);
        rangeSeekBar =  findViewById(R.id.rangeSeekBar);
        mainlayout =  findViewById(R.id.mainlayout);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(null);
        progressDialog.setCancelable(false);
        rangeSeekBar.setEnabled(false);
        loadFFMpegBinary();

        uploadVideo.setOnClickListener(new View.OnClickListener()
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
                    uploadVideo();
                }
            }
        });

        compressVideo.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                choice = 1;

                if (selectedVideoUri != null)
                {
                    executeCompressCommand();
                }
                else
                {
                    Snackbar.make(mainlayout, "Please upload a video", 4000).show();
                }

            }
        });
        cutVideo.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                choice = 2;

                if (selectedVideoUri != null)
                {
                    executeCutVideoCommand(rangeSeekBar.getSelectedMinValue().intValue() * 1000, rangeSeekBar.getSelectedMaxValue().intValue() * 1000);
                }
                else
                {
                    Snackbar.make(mainlayout, "Please upload a video", 4000).show();
                }
            }
        });

        extractImages.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                choice = 3;

                if (selectedVideoUri != null)
                {
                    extractImagesVideo(rangeSeekBar.getSelectedMinValue().intValue() * 1000, rangeSeekBar.getSelectedMaxValue().intValue() * 1000);
                }
                else
                {
                    Snackbar.make(mainlayout, "Please upload a video", 4000).show();
                }

            }
        });
        extractAudio.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                choice = 4;

                if (selectedVideoUri != null)
                {
                    if (Build.VERSION.SDK_INT >= 23)
                    {
                        getAudioPermission();
                    }
                    else
                    {
                        extractAudioVideo();
                    }
                }
                else
                {
                    Snackbar.make(mainlayout, "Please upload a video", 4000).show();
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
            uploadVideo();
        }
    }

    private void getAudioPermission()
    {
        String[] params = null;
        String recordAudio = Manifest.permission.RECORD_AUDIO;
        String modifyAudio = Manifest.permission.MODIFY_AUDIO_SETTINGS;

        int hasRecordAudioPermission = ActivityCompat.checkSelfPermission(this, recordAudio);
        int hasModifyAudioPermission = ActivityCompat.checkSelfPermission(this, modifyAudio);
        List<String> permissions = new ArrayList<String>();

        if (hasRecordAudioPermission != PackageManager.PERMISSION_GRANTED)
        {
            permissions.add(recordAudio);
        }
        if (hasModifyAudioPermission != PackageManager.PERMISSION_GRANTED)
        {
            permissions.add(modifyAudio);
        }

        if (!permissions.isEmpty())
        {
            params = permissions.toArray(new String[permissions.size()]);
        }
        if (params != null && params.length > 0)
        {
            ActivityCompat.requestPermissions(MainActivity.this,
                    params,
                    AUDIO_PERMISSION_REQUEST);
        } else
        {
            extractAudioVideo();
        }
    }

    /**
     * Handling response for permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults)
    {
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            if(requestCode == READ_WRITE_PERMISSION_REQUEST)
            {
                uploadVideo();
            }

            if(requestCode == AUDIO_PERMISSION_REQUEST)
            {
                extractAudioVideo();
            }
        }
    }

    /**
     * Opening gallery for uploading video
     */
    private void uploadVideo()
    {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_TAKE_GALLERY_VIDEO);
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
                selectedVideoUri = data.getData();
                videoView.setVideoURI(selectedVideoUri);
                videoView.start();

                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
                {

                    @Override
                    public void onPrepared(MediaPlayer mp)
                    {
                        duration = mp.getDuration() / 1000;
                        tvLeft.setText("00:00:00");

                        tvRight.setText(getTime(mp.getDuration() / 1000));
                        mp.setLooping(true);
                        rangeSeekBar.setRangeValues(0, duration);
                        rangeSeekBar.setSelectedMinValue(0);
                        rangeSeekBar.setSelectedMaxValue(duration);
                        rangeSeekBar.setEnabled(true);

                        rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener()
                        {
                            @Override
                            public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue)
                            {
                                videoView.seekTo((int) minValue * 1000);

                                tvLeft.setText(getTime((int) bar.getSelectedMinValue()));

                                tvRight.setText(getTime((int) bar.getSelectedMaxValue()));

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

    /**
     * Load FFmpeg binary
     */
    private void loadFFMpegBinary()
    {
        try
        {
            if (ffmpeg == null)
            {
                Log.d(TAG, "ffmpeg : era nulo");
                ffmpeg = FFmpeg.getInstance(this);
            }
            ffmpeg.loadBinary(new LoadBinaryResponseHandler()
            {
                @Override
                public void onFailure()
                {
                    showUnsupportedExceptionDialog();
                }

                @Override
                public void onSuccess()
                {
                    Log.d(TAG, "ffmpeg : correct Loaded");
                    execFFmpegBinary(new String []{"ffmpeg", "-formats"});
                }
            });
        }
        catch (FFmpegNotSupportedException e)
        {
            showUnsupportedExceptionDialog();
        }
        catch (Exception e)
        {
            Log.d(TAG, "EXception no controlada : " + e);
        }
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
        //String[] complexCommand = {"-i", yourRealPath, "-ss", "" + startMs / 1000, "-t", "" + endMs / 1000, dest.getAbsolutePath()};
        String[] complexCommand = {"-ss", "" + startMs / 1000, "-y", "-i", yourRealPath, "-t", "" + (endMs - startMs) / 1000,"-vcodec", "mpeg4", "-b:v", "2097152", "-b:a", "48000", "-ac", "2", "-ar", "22050", filePath};

        execFFmpegBinary(complexCommand);

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
        execFFmpegBinary(complexCommand);

    }

    /**
     * Command for extracting images from video
     */
    private void extractImagesVideo(int startMs, int endMs)
    {
        File moviesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
        );

        String filePrefix = "extract_picture";
        String fileExtn = ".jpg";
        String yourRealPath = getPath(MainActivity.this, selectedVideoUri);

        File dir = new File(moviesDir, "VideoEditor");
        int fileNo = 0;
        while (dir.exists())
        {
            fileNo++;
            dir = new File(moviesDir, "VideoEditor" + fileNo);

        }

        boolean result = dir.mkdir();
        if(result == false)
        {
            Toast.makeText(this, "Failed to create directory", Toast.LENGTH_LONG).show();
            return;
        }

        filePath = dir.getAbsolutePath();
        File dest = new File(dir, filePrefix + "%03d" + fileExtn);


        Log.d(TAG, "startTrim: src: " + yourRealPath);
        Log.d(TAG, "startTrim: dest: " + dest.getAbsolutePath());

        String[] complexCommand = {"-y", "-i", yourRealPath, "-an", "-r", "1", "-ss", "" + startMs / 1000, "-t", "" + (endMs - startMs) / 1000, dest.getAbsolutePath()};
  /*   Remove -r 1 if you want to extract all video frames as images from the specified time duration.*/
        execFFmpegBinary(complexCommand);

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

        execFFmpegBinary(complexCommand);
    }

    /**
     * Command for concating reversed segmented videos
     */
    private void concatVideoCommand()
    {
        File moviesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
        );
        File srcDir = new File(moviesDir, ".VideoPartsReverse");
        File[] files = srcDir.listFiles();
        if(files == null || files.length == 0)
        {
            Toast.makeText(this, "There are no files to reverse", Toast.LENGTH_LONG).show();
            return;
        }

        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);

        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder filterComplex = new StringBuilder();
        filterComplex.append("-filter_complex,");
        for (int i = 0; i < files.length; i++)
        {
            stringBuilder.append("-i" + "," + files[i].getAbsolutePath() + ",");
            filterComplex.append("[").append(i).append(":v").append(i).append("] [").append(i).append(":a").append(i).append("] ");

        }
        filterComplex.append("concat=n=").append(files.length).append(":v=1:a=1 [v] [a]");
        String[] inputCommand = stringBuilder.toString().split(",");
        String[] filterCommand = filterComplex.toString().split(",");

        String filePrefix = "reverse_video";
        String fileExtn = ".mp4";
        File dest = new File(moviesDir, filePrefix + fileExtn);
        int fileNo = 0;
        while (dest.exists())
        {
            fileNo++;
            dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        }
        filePath = dest.getAbsolutePath();
        String[] destinationCommand = {"-map", "[v]", "-map", "[a]", dest.getAbsolutePath()};
        execFFmpegBinary(combine(inputCommand, filterCommand, destinationCommand));
    }

    public static String[] combine(String[] arg1, String[] arg2, String[] arg3)
    {
        String[] result = new String[arg1.length + arg2.length + arg3.length];
        System.arraycopy(arg1, 0, result, 0, arg1.length);
        System.arraycopy(arg2, 0, result, arg1.length, arg2.length);
        System.arraycopy(arg3, 0, result, arg1.length + arg2.length, arg3.length);
        return result;
    }

    /**
     * Executing ffmpeg binary
     */
    private void execFFmpegBinary(final String[] command)
    {
        final StringBuilder commandBuilder = new StringBuilder();
        for(String part : command)
        {
            commandBuilder.append(part);
            commandBuilder.append(" ");
        }
        final String commandString = commandBuilder.toString().trim();

        try
        {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler()
            {
                @Override
                public void onFailure(String s) {
                    Log.d(TAG, "FAILED with output : " + s);
                }

                @Override
                public void onSuccess(String s)
                {
                    Log.d(TAG, "SUCCESS with output : " + s.replace("\r", "\n"));
                    if (choice == 1 || choice == 2)
                    {
                        Intent intent = new Intent(MainActivity.this, PreviewActivity.class);
                        intent.putExtra(FILEPATH, filePath);
                        startActivity(intent);
                    }
                    else if (choice == 3)
                    {
                        Intent intent = new Intent(MainActivity.this, PreviewImageActivity.class);
                        intent.putExtra(FILEPATH, filePath);
                        startActivity(intent);
                    }
                    else if (choice == 4)
                    {
                        Intent intent = new Intent(MainActivity.this, AudioPreviewActivity.class);
                        intent.putExtra(FILEPATH, filePath);
                        startActivity(intent);
                    }
                }

                @Override
                public void onProgress(String s)
                {
                    Log.d(TAG, "Started command : ffmpeg " + commandString);
                    progressDialog.setMessage("progress : " + s);
                    Log.d(TAG, "progress : " + s);
                }

                @Override
                public void onStart()
                {
                    Log.d(TAG, "Started command : ffmpeg " + commandString);
                    progressDialog.setMessage("Processing...");
                    progressDialog.show();
                }

                @Override
                public void onFinish()
                {
                    Log.d(TAG, "Finished command : ffmpeg " + commandString);
                    if (choice != 8 && choice != 9 && choice != 10)
                    {
                        progressDialog.dismiss();
                    }
                }
            });
        }
        catch (FFmpegCommandAlreadyRunningException e)
        {
            // do nothing for now
        }
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
            "Range SeekBar", "https://github.com/anothem/android-range-seek-bar"
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
