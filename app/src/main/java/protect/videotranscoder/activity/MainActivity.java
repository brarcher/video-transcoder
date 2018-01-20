package protect.videotranscoder.activity;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.google.common.collect.ImmutableMap;

import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import protect.videotranscoder.BuildConfig;
import protect.videotranscoder.FFmpegUtil;
import protect.videotranscoder.FileUtil;
import protect.videotranscoder.R;
import protect.videotranscoder.ResultCallbackHandler;
import protect.videotranscoder.media.AudioCodec;
import protect.videotranscoder.media.MediaContainer;
import protect.videotranscoder.media.MediaInfo;
import protect.videotranscoder.media.VideoCodec;
import protect.videotranscoder.service.FFmpegProcessService;
import protect.videotranscoder.service.MessageId;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "VideoTranscoder";

    public static final String MESSENGER_INTENT_KEY = BuildConfig.APPLICATION_ID + ".MESSENGER_INTENT_KEY";
    public static final String FFMPEG_ENCODE_ARGS = BuildConfig.APPLICATION_ID + ".FFMPEG_ENCODE_ARGS";
    public static final String FFMPEG_OUTPUT_FILE = BuildConfig.APPLICATION_ID + ".FFMPEG_OUTPUT_FILE";
    public static final String OUTPUT_MIMETYPE = BuildConfig.APPLICATION_ID + ".OUTPUT_MIMETYPE";
    public static final String OUTPUT_DURATION_MS = BuildConfig.APPLICATION_ID + ".OUTPUT_DURATION_MS";

    private static final int REQUEST_TAKE_GALLERY_VIDEO = 100;
    private static final int READ_WRITE_PERMISSION_REQUEST = 1;

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

    private VideoView videoView;
    private CrystalRangeSeekbar rangeSeekBar;
    private Timer videoTimer = null;
    private ProgressBar progressBar;

    private Spinner containerSpinner;
    private Spinner videoCodecSpinner;
    private Spinner fpsSpinner;
    private Spinner resolutionSpinner;
    private EditText videoBitrateValue;
    private Spinner audioCodecSpinner;
    private Spinner audioBitrateSpinner;
    private Spinner audioSampleRateSpinner;
    private Spinner audioChannelSpinner;

    private TextView tvLeft, tvRight;
    private Button selectVideoButton;
    private Button encodeButton;
    private Button cancelButton;
    private MediaInfo videoInfo;

    private JobScheduler schedulerService;
    private ComponentName serviceComponent;
    // Handler for incoming messages from the service.
    private IncomingMessageHandler msgHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectVideoButton = findViewById(R.id.selectVideo);
        encodeButton = findViewById(R.id.encode);
        cancelButton = findViewById(R.id.cancel);

        tvLeft = findViewById(R.id.tvLeft);
        tvRight = findViewById(R.id.tvRight);

        videoView =  findViewById(R.id.videoView);
        rangeSeekBar =  findViewById(R.id.rangeSeekBar);
        progressBar = findViewById(R.id.encodeProgress);
        rangeSeekBar.setEnabled(false);

        containerSpinner = findViewById(R.id.containerSpinner);
        videoCodecSpinner = findViewById(R.id.videoCodecSpinner);
        fpsSpinner = findViewById(R.id.fpsSpinner);
        resolutionSpinner = findViewById(R.id.resolutionSpinner);
        videoBitrateValue = findViewById(R.id.videoBitrateValue);
        audioCodecSpinner = findViewById(R.id.audioCodecSpinner);
        audioBitrateSpinner = findViewById(R.id.audioBitrateSpinner);
        audioSampleRateSpinner = findViewById(R.id.audioSampleRateSpinner);
        audioChannelSpinner = findViewById(R.id.audioChannelSpinner);

        serviceComponent = new ComponentName(this, FFmpegProcessService.class);
        msgHandler = new IncomingMessageHandler(this);

        selectVideoButton.setOnClickListener(new View.OnClickListener()
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

        encodeButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startEncode();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                cancelEncode();
            }
        });

        schedulerService = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(schedulerService != null)
        {
            // Check if a job is still being processed in the background from a previous
            // launch. If so, skip to that UI layout.
            if(schedulerService.getAllPendingJobs().size() > 0)
            {
                updateUiForEncoding();
            }
        }

        selectVideoButton.setEnabled(false);

        FFmpegUtil.init(getApplicationContext(), new ResultCallbackHandler<Boolean>()
        {
            @Override
            public void onResult(Boolean result)
            {
                if(result)
                {
                    selectVideoButton.setEnabled(true);
                }
                else
                {
                    showUnsupportedExceptionDialog();
                }
            }
        });
    }

    private void showUnsupportedExceptionDialog()
    {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.notSupportedTitle)
                .setMessage(R.string.notSupportedMessage)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        finish();
                    }
                })
                .create()
                .show();
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

    private void startEncode()
    {
        MediaContainer container = (MediaContainer)containerSpinner.getSelectedItem();
        VideoCodec videoCodec = (VideoCodec)videoCodecSpinner.getSelectedItem();
        String fps = (String)fpsSpinner.getSelectedItem();
        String resolution = (String)resolutionSpinner.getSelectedItem();
        AudioCodec audioCodec = (AudioCodec) audioCodecSpinner.getSelectedItem();
        Integer audioBitrate = (Integer) audioBitrateSpinner.getSelectedItem();
        Integer audioSampleRate = (Integer) audioSampleRateSpinner.getSelectedItem();
        String audioChannel = (String) audioChannelSpinner.getSelectedItem();
        int videoBitrate;

        if(videoInfo == null)
        {
            Toast.makeText(this, R.string.selectFileFirst, Toast.LENGTH_LONG).show();
            return;
        }

        try
        {
            String videoBitrateStr = videoBitrateValue.getText().toString();
            videoBitrate = Integer.parseInt(videoBitrateStr);
        }
        catch(NumberFormatException e)
        {
            Toast.makeText(this, R.string.videoBitrateValueInvalid, Toast.LENGTH_LONG).show();
            return;
        }

        File outputDir;

        if(container.supportedVideoCodecs.size() > 0)
        {
            outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        }
        else
        {
            outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        }

        String filePrefix = "Video_Transcoder_Output";
        String extension = "." + container.extension;
        String inputFilePath = videoInfo.file.getAbsolutePath();

        File destination = new File(outputDir, filePrefix + extension);
        int fileNo = 0;
        while (destination.exists())
        {
            fileNo++;
            destination = new File(outputDir, filePrefix + "_" + fileNo + extension);
        }

        List<String> command = new LinkedList<>();

        // If the output exists, overwrite it
        command.add("-y");

        // Input file
        command.add("-i");
        command.add(inputFilePath);

        int startTimeSec = rangeSeekBar.getSelectedMinValue().intValue();
        if(startTimeSec != 0)
        {
            // Start time offset
            command.add("-ss");
            command.add(Integer.toString(startTimeSec));
        }

        int endTimeSec = rangeSeekBar.getSelectedMaxValue().intValue();
        int durationSec = endTimeSec - startTimeSec;
        if( (videoInfo.durationMs)/1000 != endTimeSec)
        {
            // Duration of media file
            command.add("-t");
            command.add(Integer.toString(durationSec));
        }

        if(container.supportedVideoCodecs.size() > 0)
        {
            // These options only apply when not using GIF
            if(videoCodec != VideoCodec.GIF)
            {
                // Video codec
                command.add("-vcodec");
                command.add(videoCodec.ffmpegName);

                // Video bitrate
                command.add("-b:v");
                command.add(videoBitrate + "k");
            }

            // Frame size
            command.add("-s");
            command.add(resolution);

            // Frame rate
            command.add("-r");
            command.add(fps);
        }
        else
        {
            // No video
            command.add("-vn");
        }

        if(container.supportedAudioCodecs.size() > 0)
        {
            // Audio codec
            command.add("-acodec");
            command.add(audioCodec.ffmpegName);

            if(audioCodec == AudioCodec.VORBIS)
            {
                // The vorbis encode is experimental, and needs other
                // flags to enable
                command.add("-strict");
                command.add("-2");
            }

            // Sample rate
            command.add("-ar");
            command.add(Integer.toString(audioSampleRate));

            // Channels
            command.add("-ac");
            command.add(audioChannel);

            // Audio bitrate
            command.add("-b:a");
            command.add(audioBitrate + "k");
        }

        if(container == MediaContainer.GIF)
        {
            command.add("-filter_complex");
            command.add("fps=" + fps + ",split [o1] [o2];[o1] palettegen [p]; [o2] fifo [o3];[o3] [p] paletteuse");
        }

        // Output file
        command.add(destination.getAbsolutePath());


        updateUiForEncoding();


        JobInfo.Builder builder = new JobInfo.Builder(1, serviceComponent);
        builder.setOverrideDeadline(0);

        // Extras, work duration.
        PersistableBundle extras = new PersistableBundle();
        extras.putStringArray(FFMPEG_ENCODE_ARGS, command.toArray(new String[command.size()]));
        extras.putString(FFMPEG_OUTPUT_FILE, destination.getAbsolutePath());
        extras.putString(OUTPUT_MIMETYPE, container.mimetype);
        extras.putInt(OUTPUT_DURATION_MS, durationSec*1000);
        builder.setExtras(extras);

        // Schedule job
        Log.d(TAG, "Scheduling job");
        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(scheduler == null)
        {
            Toast.makeText(this, R.string.transcodeFailed, Toast.LENGTH_LONG).show();
            return;
        }

        scheduler.schedule(builder.build());
    }

    private void updateUiForEncoding()
    {
        stopVideoPlayback();

        progressBar.setProgress(0);
        progressBar.setIndeterminate(true);

        selectVideoButton.setVisibility(View.GONE);
        encodeButton.setVisibility(View.GONE);
        cancelButton.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void cancelEncode()
    {
        FFmpegUtil.cancelCall();

        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(scheduler == null)
        {
            Log.w(TAG, "Scheduler was null when canceling encode");
            return;
        }

        scheduler.cancelAll();
    }

    private boolean isEncoding()
    {
        return cancelButton.getVisibility() == View.VISIBLE;
    }

    private void stopVideoPlayback()
    {
        videoView.pause();
        if(videoTimer != null)
        {
            videoTimer.cancel();
            videoTimer = null;
        }
    }

    private void startVideoPlayback()
    {
        stopVideoPlayback();

        int startTimeSec = rangeSeekBar.getSelectedMinValue().intValue();
        int stopTimeSec = rangeSeekBar.getSelectedMaxValue().intValue();
        int durationSec = stopTimeSec - startTimeSec;

        videoView.seekTo(startTimeSec * 1000);
        videoView.start();

        videoTimer = new Timer();
        videoTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                startVideoPlayback();
            }
        }, durationSec * 1000);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        // Start service and provide it a way to communicate with this class.
        Intent startServiceIntent = new Intent(this, FFmpegProcessService.class);
        Messenger messengerIncoming = new Messenger(msgHandler);
        startServiceIntent.putExtra(MESSENGER_INTENT_KEY, messengerIncoming);
        startService(startServiceIntent);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        stopVideoPlayback();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if(isEncoding() == false)
        {
            startVideoPlayback();
        }
    }

    @Override
    protected void onStop() {
        // A service can be "started" and/or "bound". In this case, it's "started" by this Activity
        // and "bound" to the JobScheduler (also called "Scheduled" by the JobScheduler). This call
        // to stopService() won't prevent scheduled jobs to be processed. However, failing
        // to call stopService() would keep it alive indefinitely.
        stopService(new Intent(this, FFmpegProcessService.class));
        super.onStop();
    }

    private void setSpinnerSelection(Spinner spinner, Object value)
    {
        for(int index = 0; index < spinner.getCount(); index++)
        {
            String item = spinner.getItemAtPosition(index).toString();
            if(item.equals(value.toString()))
            {
                spinner.setSelection(index);
                break;
            }
        }
    }

    private void populateOptionDefaults()
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

        encodeButton.setVisibility(View.VISIBLE);

        containerSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_textview, MediaContainer.values()));
        containerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
            {
                MediaContainer container = (MediaContainer)parentView.getItemAtPosition(position);
                int visibility = container.supportedVideoCodecs.size() > 0 ? View.VISIBLE : View.GONE;

                for(int resId : VIDEO_SETTINGS_IDS)
                {
                    findViewById(resId).setVisibility(visibility);
                }

                VideoCodec currentVideoSelection = (VideoCodec)videoCodecSpinner.getSelectedItem();
                AudioCodec currentAudioSelection = (AudioCodec)audioCodecSpinner.getSelectedItem();

                videoCodecSpinner.setAdapter(new ArrayAdapter<>(MainActivity.this, R.layout.spinner_textview, container.supportedVideoCodecs));
                audioCodecSpinner.setAdapter(new ArrayAdapter<>(MainActivity.this, R.layout.spinner_textview, container.supportedAudioCodecs));

                // Attempt to set the same settings again, if they exist
                if(currentVideoSelection != null)
                {
                    setSpinnerSelection(videoCodecSpinner, currentVideoSelection.toString());
                }
                if(currentAudioSelection != null)
                {
                    setSpinnerSelection(audioCodecSpinner, currentAudioSelection.toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView)
            {
                // Nothing to do
            }
        });

        LinkedList<String> fps = new LinkedList<>(Arrays.asList("24", "23.98", "25", "29.97", "30", "50"));
        if(videoInfo.videoFramerate != null && fps.contains(videoInfo.videoFramerate) == false)
        {
            fps.add(videoInfo.videoFramerate);
            Collections.sort(fps);
        }
        fpsSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_textview, fps));

        LinkedList<String> resolution = new LinkedList<>(Arrays.asList("176x144", "320x240", "480x360", "640x360", "640x480", "800x600", "960x720", "1024x768", "1280x720", "1920x1080", "2048x1080", "2048x858", "2560x1440", "2560x1600", "4096x2160"));
        if(videoInfo.videoResolution != null && resolution.contains(videoInfo.videoResolution) == false)
        {
            resolution.add(videoInfo.videoResolution);

            Collections.sort(resolution, new Comparator<String>()
            {
                @Override
                public int compare(String o1, String o2)
                {
                    int width1 = Integer.parseInt(o1.split("x")[0]);
                    int height1 = Integer.parseInt(o1.split("x")[1]);
                    int width2 = Integer.parseInt(o2.split("x")[0]);
                    int height2 = Integer.parseInt(o2.split("x")[1]);

                    if(width1 != width2)
                    {
                        return width1 - width2;
                    }
                    else
                    {
                        return height1 - height2;
                    }
                }
            });
        }
        resolutionSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_textview, resolution));

        audioCodecSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
            {
                AudioCodec audioCodec = (AudioCodec) parentView.getItemAtPosition(position);

                String currentSelection = (String)audioChannelSpinner.getSelectedItem();
                audioChannelSpinner.setAdapter(new ArrayAdapter<>(MainActivity.this, R.layout.spinner_textview, audioCodec.supportedChannels));

                // Attempt to set the same setting as before, if it exists
                setSpinnerSelection(audioChannelSpinner, currentSelection);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView)
            {
                // Nothing to do
            }
        });

        List<Integer> audioBitrate = new ArrayList<>(Arrays.asList(15, 24, 32, 64, 96, 128, 192, 256, 320, 384, 448, 512));
        if(videoInfo.audioBitrate != null && audioBitrate.contains(videoInfo.audioBitrate) == false)
        {
            audioBitrate.add(videoInfo.audioBitrate);
            Collections.sort(audioBitrate);
        }
        audioBitrateSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_textview, audioBitrate));

        List<Integer> sampleRate = new ArrayList<>(Arrays.asList(8000, 11025, 16000, 22050, 24000, 32000, 44100, 48000));
        if(videoInfo.audioSampleRate != null && audioBitrate.contains(videoInfo.audioSampleRate) == false)
        {
            sampleRate.add(videoInfo.audioSampleRate);
            Collections.sort(sampleRate);
        }
        audioSampleRateSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_textview, sampleRate));

        String [] channels = new String[] {"1", "2"};
        audioChannelSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_textview, channels));

        if(videoInfo.container != null)
        {
            setSpinnerSelection(containerSpinner, videoInfo.container.toString());
        }

        if(videoInfo.videoCodec != null)
        {
            setSpinnerSelection(videoCodecSpinner, videoInfo.videoCodec.toString());
        }

        if(videoInfo.videoFramerate != null)
        {
            setSpinnerSelection(fpsSpinner, videoInfo.videoFramerate);
        }

        if(videoInfo.videoResolution != null)
        {
            setSpinnerSelection(resolutionSpinner, videoInfo.videoResolution);
        }

        if(videoInfo.videoBitrate != null)
        {
            videoBitrateValue.setText(Integer.toString(videoInfo.videoBitrate));
        }

        if(videoInfo.audioCodec != null)
        {
            setSpinnerSelection(audioCodecSpinner, videoInfo.audioCodec.toString());
        }

        Integer defaultAudioBitrate = videoInfo.audioBitrate;
        if(defaultAudioBitrate == null)
        {
            // Set some default if none is detected
            defaultAudioBitrate = 128;
        }

        setSpinnerSelection(audioBitrateSpinner, defaultAudioBitrate);

        if(videoInfo.audioSampleRate != null)
        {
            setSpinnerSelection(audioSampleRateSpinner, videoInfo.audioSampleRate);
        }

        setSpinnerSelection(audioChannelSpinner, Integer.toString(videoInfo.audioChannels));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO)
            {
                Uri selectedVideoUri = data.getData();
                String videoPath = FileUtil.getPath(MainActivity.this, selectedVideoUri);

                if(videoPath == null)
                {
                    Toast.makeText(this, R.string.fileLocationLookupFailed, Toast.LENGTH_LONG).show();
                    return;
                }

                videoView.setVideoURI(selectedVideoUri);
                videoView.start();

                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
                {
                    @Override
                    public void onPrepared(MediaPlayer mp)
                    {
                        int durationMs = mp.getDuration();
                        tvLeft.setVisibility(View.VISIBLE);
                        tvLeft.setText(getTime(0));
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
                                tvLeft.setText(getTime(minValue.intValue()));
                                tvRight.setText(getTime(maxValue.intValue()));

                                if(isEncoding() == false)
                                {
                                    startVideoPlayback();
                                }
                            }
                        });
                    }
                });

                final File videoFile = new File(videoPath);

                FFmpegUtil.getMediaDetails(new File(videoPath), new ResultCallbackHandler<MediaInfo>()
                {
                    @Override
                    public void onResult(MediaInfo result)
                    {
                        if(result == null)
                        {
                            Log.d(TAG, "Failed to query media file, filling in defaults");
                            // Could not query the file, fill in what we know.
                            result = new MediaInfo(videoFile, 0, MediaContainer.MP4, VideoCodec.MPEG4, "640x480",
                                800, "25", AudioCodec.MP3,
                                44100, 128, 2);
                        }

                        videoInfo = result;

                        populateOptionDefaults();
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

        return String.format(Locale.US, "%02d:%02d:%02d", hr, mn, sec);
    }

    /**
     * A {@link Handler} allows you to send messages associated with a thread. A {@link Messenger}
     * uses this handler to communicate from {@link protect.videotranscoder.service.FFmpegProcessService}.
     * It's also used to make the start and stop views blink for a short period of time.
     */
    private static class IncomingMessageHandler extends Handler
    {
        // Prevent possible leaks with a weak reference.
        private WeakReference<MainActivity> activityRef;

        IncomingMessageHandler(MainActivity activity)
        {
            super(/* default looper */);
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            final MainActivity mainActivity = activityRef.get();
            if (mainActivity == null)
            {
                // Activity is no longer available, exit.
                return;
            }

            MessageId messageId = MessageId.fromInt(msg.what);

            switch (MessageId.fromInt(msg.what))
            {
                /*
                 * Receives callback from the service when a job has landed
                 * on the app. Turns on indicator and sends a message to turn it off after
                 * a second.
                 */
                case JOB_START_MSG:
                    Log.d(TAG, "JOB_START_MSG: " + msg.obj.toString());
                    break;

                /*
                 * Receives callback from the service when a job that previously landed on the
                 * app must stop executing. Turns on indicator and sends a message to turn it
                 * off after two seconds.
                 */
                case JOB_PROGRESS_MSG:
                    Integer percentComplete = (Integer)msg.obj;

                    if(percentComplete != null && percentComplete > 0)
                    {
                        Log.d(TAG, "JOB_PROGRESS_MSG: " + percentComplete);

                        ProgressBar progressBar = mainActivity.findViewById(R.id.encodeProgress);
                        progressBar.setIndeterminate(false);
                        progressBar.setProgress(percentComplete);
                    }
                    break;

                case JOB_SUCCEDED_MSG:
                case JOB_FAILED_MSG:
                    boolean result = false;
                    String outputFile = null;
                    String mimetype = null;

                    if(messageId == MessageId.JOB_SUCCEDED_MSG)
                    {
                        result = true;
                        outputFile = ((Bundle)msg.obj).getString(FFMPEG_OUTPUT_FILE);
                        mimetype = ((Bundle)msg.obj).getString(OUTPUT_MIMETYPE);
                    }

                    Log.d(TAG, "Job complete, result: " + result);
                    showEncodeCompleteDialog(mainActivity, result, outputFile, mimetype);
                    break;

                case FFMPEG_UNSUPPORTED_MSG:
                    Log.d(TAG, "FFMPEG_UNSUPPORTED_MSG");
                    break;

                case UNKNOWN_MSG:
                    Log.w(TAG, "UNKNOWN_MSG received");
                    break;
            }
        }

        private void showEncodeCompleteDialog(final MainActivity mainActivity, final boolean result,
                                              final String outputFile, final String mimetype)
        {
            ProgressBar progressBar = mainActivity.findViewById(R.id.encodeProgress);
            Button selectVideoButton = mainActivity.findViewById(R.id.selectVideo);
            Button encodeButton = mainActivity.findViewById(R.id.encode);
            Button cancelButton = mainActivity.findViewById(R.id.cancel);

            Log.d(TAG, "Encode result: " + result);

            String message;

            if(result)
            {
                message = mainActivity.getResources().getString(R.string.transcodeSuccess, outputFile);
            }
            else
            {
                message = mainActivity.getResources().getString(R.string.transcodeFailed);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });

            if(result)
            {
                final Uri outputUri = FileProvider.getUriForFile(mainActivity, BuildConfig.APPLICATION_ID, new File(outputFile));

                final CharSequence sendLabel = mainActivity.getResources().getText(R.string.sendLabel);
                builder.setNeutralButton(sendLabel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Intent sendIntent = new Intent(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_STREAM, outputUri);
                        sendIntent.setType(mimetype);

                        // set flag to give temporary permission to external app to use the FileProvider
                        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        mainActivity.startActivity(Intent.createChooser(sendIntent, sendLabel));
                    }
                });
            }

            builder.show();

            //startVideoPlayback();

            selectVideoButton.setVisibility(View.VISIBLE);
            encodeButton.setVisibility(View.VISIBLE);
            cancelButton.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        }
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
