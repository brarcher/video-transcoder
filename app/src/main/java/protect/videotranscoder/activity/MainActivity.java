package protect.videotranscoder.activity;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.google.common.collect.ImmutableMap;

import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;

import org.javatuples.Triplet;

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

import protect.videoeditor.IFFmpegProcessService;
import protect.videotranscoder.BuildConfig;
import protect.videotranscoder.FFmpegUtil;
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
    public static final String FFMPEG_OUTPUT_FILE = BuildConfig.APPLICATION_ID + ".FFMPEG_OUTPUT_FILE";
    public static final String FFMPEG_FAILURE_MSG = BuildConfig.APPLICATION_ID + ".FFMPEG_FAILURE_MSG";
    public static final String OUTPUT_MIMETYPE = BuildConfig.APPLICATION_ID + ".OUTPUT_MIMETYPE";

    private static final int READ_WRITE_PERMISSION_REQUEST = 1;

    private static final int SELECT_FILE_REQUEST = 2;

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
    private ImageView startJumpBack;
    private ImageView startJumpForward;
    private ImageView endJumpBack;
    private ImageView endJumpForward;
    private MediaInfo videoInfo;

    private IFFmpegProcessService ffmpegService;
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

        startJumpBack = findViewById(R.id.startJumpBack);
        startJumpForward = findViewById(R.id.startJumpForward);
        endJumpBack = findViewById(R.id.endJumpBack);
        endJumpForward = findViewById(R.id.endJumpForward);

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

        selectVideoButton.setEnabled(false);

        if(FFmpegUtil.init(getApplicationContext()) == false)
        {
            showUnsupportedExceptionDialog();
        }

        Intent serviceIntent = new Intent(this, FFmpegProcessService.class);
        startService(serviceIntent);
        bindService(serviceIntent, ffmpegServiceConnection, BIND_AUTO_CREATE);
    }

    private ServiceConnection ffmpegServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            Log.i(TAG, "Bound to service");
            ffmpegService = IFFmpegProcessService.Stub.asInterface(service);

            try
            {
                // Check if a job is still being processed in the background from a previous
                // launch. If so, skip to that UI layout.
                if(ffmpegService.isEncoding())
                {
                    updateUiForEncoding();
                }
                else
                {
                    selectVideoButton.setEnabled(true);
                }

                Intent intent = getIntent();
                if(intent != null)
                {
                    String action = intent.getAction();
                    if(action != null && action.equals("protect.videotranscoder.ENCODE"))
                    {
                        handleEncodeIntent(intent);
                    }
                }
            }
            catch (RemoteException e)
            {
                Log.w(TAG, "Failed to query service", e);
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className)
        {
            ffmpegService = null;
            // This method is only invoked when the service quits from the other end or gets killed
            // Invoking exit() from the AIDL interface makes the Service kill itself, thus invoking this.
            Log.i(TAG, "Service disconnected");
        }
    };

    @Override
    public void onNewIntent(Intent intent)
    {
        setIntent(intent);
        String action = intent.getAction();
        if(action != null && action.contains("ENCODE"))
        {
            handleEncodeIntent(intent);
        }
    }

    private void handleEncodeIntent(Intent intent)
    {
        final String inputFilePath = intent.getStringExtra("inputVideoFilePath");
        final String destinationFilePath = intent.getStringExtra("outputFilePath");

        String mediaContainerStr = intent.getStringExtra("mediaContainer");
        final MediaContainer container = MediaContainer.fromName(mediaContainerStr);

        String videoCodecStr = intent.getStringExtra("videoCodec");
        final VideoCodec videoCodec = VideoCodec.fromName(videoCodecStr);

        int tmpCideoBitrateK = intent.getIntExtra("videoBitrateK", -1);
        final Integer videoBitrateK = tmpCideoBitrateK != -1 ? tmpCideoBitrateK : null;

        final String resolution = intent.getStringExtra("resolution");
        final String fps = intent.getStringExtra("fps");

        String audioCodecStr = intent.getStringExtra("audioCodec");
        final AudioCodec audioCodec = AudioCodec.fromName(audioCodecStr);

        int tmpAudioSampleRate = intent.getIntExtra("audioSampleRate", -1);
        final Integer audioSampleRate = tmpAudioSampleRate != -1 ? tmpAudioSampleRate : null;

        final String audioChannel = intent.getStringExtra("audioChannel");

        int tmpAudioBitrateK = intent.getIntExtra("audioBitrateK", -1);
        final Integer audioBitrateK = tmpAudioBitrateK != -1 ? tmpAudioBitrateK : null;

        boolean skipDialog = intent.getBooleanExtra("skipDialog", false);

        List<Triplet<Object, Integer, String>> nullChecks = new LinkedList<>();
        nullChecks.add(new Triplet<>((Object)inputFilePath, R.string.fieldMissingError, "inputFilePath"));
        nullChecks.add(new Triplet<>((Object)destinationFilePath, R.string.fieldMissingError, "outputFilePath"));
        nullChecks.add(new Triplet<>((Object)container, R.string.fieldMissingOrInvalidError, "mediaContainer"));
        if(container != null && container.supportedVideoCodecs.size() > 0)
        {
            nullChecks.add(new Triplet<>((Object)videoCodec, R.string.fieldMissingOrInvalidError, "videoCodec"));
            nullChecks.add(new Triplet<>((Object)videoBitrateK, R.string.fieldMissingError, "videoBitrateK missing"));
            nullChecks.add(new Triplet<>((Object)resolution, R.string.fieldMissingError, "resolution"));
            nullChecks.add(new Triplet<>((Object)fps, R.string.fieldMissingError, "fps"));
        }
        if(container != null && container.supportedAudioCodecs.size() > 0)
        {
            nullChecks.add(new Triplet<>((Object)audioCodec, R.string.fieldMissingOrInvalidError, "audioCodec"));
            nullChecks.add(new Triplet<>((Object)audioSampleRate, R.string.fieldMissingError, "audioSampleRate"));
            nullChecks.add(new Triplet<>((Object)audioChannel, R.string.fieldMissingError, "audioChannel"));
            nullChecks.add(new Triplet<>((Object)audioBitrateK, R.string.fieldMissingError, "audioBitrateK"));
        }

        for(Triplet<Object, Integer, String> check : nullChecks)
        {
            if(check.getValue0() == null)
            {
                String submsg = String.format(getString(check.getValue1()), check.getValue2());
                String message = String.format(getString(R.string.cannotEncodeFile), submsg);
                Log.i(TAG, message);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        String message = String.format(getString(R.string.encodeStartConfirmation), inputFilePath, destinationFilePath);

        DialogInterface.OnClickListener positiveButtonListener = new DialogInterface.OnClickListener()
        {
            public void onClick(final DialogInterface dialog, int which)
            {
                FFmpegUtil.getMediaDetails(new File(inputFilePath), new ResultCallbackHandler<MediaInfo>()
                {
                    @Override
                    public void onResult(MediaInfo result)
                    {
                        if(result != null)
                        {
                            int durationSec = (int)(result.durationMs/1000);
                            startEncode(inputFilePath, 0, durationSec, durationSec, container, videoCodec, videoBitrateK,
                                    resolution, fps, audioCodec, audioSampleRate, audioChannel, audioBitrateK, destinationFilePath);
                        }
                        else
                        {
                            String message = String.format(getString(R.string.transcodeFailed), getString(R.string.couldNotFindFileSubmsg));
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                            finish();
                            dialog.dismiss();
                        }
                    }
                });
            }
        };

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setMessage(message)
            .setCancelable(false)
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    finish();
                    dialog.dismiss();
                }
            })
            .setPositiveButton(R.string.encode, positiveButtonListener).create();

        dialog.show();

        if(skipDialog)
        {
            positiveButtonListener.onClick(dialog, 0);
            dialog.dismiss();
        }
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
     * Opening gallery for selecting video file
     */
    private void selectVideo()
    {
        Intent i = new Intent(this, FilePickerActivity.class);

        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(i, SELECT_FILE_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if (requestCode == SELECT_FILE_REQUEST && resultCode == Activity.RESULT_OK)
        {
            // Use the provided utility method to parse the result
            List<Uri> files = Utils.getSelectedFilesFromResult(intent);

            // There should be at most once result
            if(files.size() > 0)
            {
                File file = Utils.getFileForUri(files.get(0));

                Log.i(TAG, "Selected file: " + file.getAbsolutePath());
                setSelectMediaFile(file.getAbsolutePath());
            }
        }
    }

    private List<String> getFfmpegEncodingArgs(String inputFilePath, Integer startTimeSec, Integer endTimeSec, Integer fullDurationSec,
                                               MediaContainer container, VideoCodec videoCodec, Integer videoBitrateK,
                                               String resolution, String fps, AudioCodec audioCodec, Integer audioSampleRate,
                                               String audioChannel, Integer audioBitrateK, String destinationFilePath)
    {
        List<String> command = new LinkedList<>();

        // If the output exists, overwrite it
        command.add("-y");

        // Input file
        command.add("-i");
        command.add(inputFilePath);

        if (startTimeSec != null && startTimeSec != 0)
        {
            // Start time offset
            command.add("-ss");
            command.add(Integer.toString(startTimeSec));
        }

        if(startTimeSec != null && endTimeSec != null)
        {
            int subDurationSec = endTimeSec - startTimeSec;

            if (fullDurationSec != subDurationSec)
            {
                // Duration of media file
                command.add("-t");
                command.add(Integer.toString(subDurationSec));
            }
        }

        if (container.supportedVideoCodecs.size() > 0)
        {
            // These options only apply when not using GIF
            if (videoCodec != VideoCodec.GIF)
            {
                // Video codec
                command.add("-vcodec");
                command.add(videoCodec.ffmpegName);

                // Video bitrate
                command.add("-b:v");
                command.add(videoBitrateK + "k");
            }

            // Frame size
            command.add("-s");
            command.add(resolution);

            // Frame rate
            command.add("-r");
            command.add(fps);
        } else
        {
            // No video
            command.add("-vn");
        }

        if (container.supportedAudioCodecs.size() > 0 && audioCodec != AudioCodec.NONE)
        {
            // Audio codec
            command.add("-acodec");
            command.add(audioCodec.ffmpegName);

            if (audioCodec == AudioCodec.VORBIS)
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
            command.add(audioBitrateK + "k");
        }
        else
        {
            // No audio
            command.add("-an");
        }

        if (container == MediaContainer.GIF)
        {
            command.add("-filter_complex");
            command.add("fps=" + fps + ",split [o1] [o2];[o1] palettegen [p]; [o2] fifo [o3];[o3] [p] paletteuse");
        }

        // Output file
        command.add(destinationFilePath);

        return command;
    }

    private void startEncode(String inputFilePath, Integer startTimeSec, Integer endTimeSec, Integer fullDurationSec,
                             MediaContainer container, VideoCodec videoCodec, Integer videoBitrateK,
                             String resolution, String fps, AudioCodec audioCodec, Integer audioSampleRate,
                             String audioChannel, Integer audioBitrateK, String destinationFilePath)
    {
        List<String> args = getFfmpegEncodingArgs(inputFilePath, startTimeSec, endTimeSec, fullDurationSec,
                container, videoCodec, videoBitrateK, resolution, fps, audioCodec, audioSampleRate,
                audioChannel, audioBitrateK, destinationFilePath);

        updateUiForEncoding();

        boolean success = false;
        try
        {
            Log.d(TAG, "Sending encode request to service");
            int durationSec = endTimeSec - startTimeSec;
            success = ffmpegService.startEncode(args, destinationFilePath, container.mimetype, durationSec*1000);
        }
        catch (RemoteException e)
        {
            Log.w(TAG, "Failed to send encode request to service", e);
        }

        if(success == false)
        {
            updateUiForVideoSettings();
        }
    }

    private void startEncode()
    {
        MediaContainer container = (MediaContainer)containerSpinner.getSelectedItem();
        VideoCodecWrapper videoCodecWrapper = (VideoCodecWrapper)videoCodecSpinner.getSelectedItem();
        VideoCodec videoCodec = videoCodecWrapper != null ? videoCodecWrapper.codec : null;
        String fps = (String)fpsSpinner.getSelectedItem();
        String resolution = (String)resolutionSpinner.getSelectedItem();
        AudioCodec audioCodec = (AudioCodec) audioCodecSpinner.getSelectedItem();
        Integer audioBitrateK = (Integer) audioBitrateSpinner.getSelectedItem();
        Integer audioSampleRate = (Integer) audioSampleRateSpinner.getSelectedItem();
        String audioChannel = (String) audioChannelSpinner.getSelectedItem();
        int videoBitrateK;

        if(videoInfo == null)
        {
            Toast.makeText(this, R.string.selectFileFirst, Toast.LENGTH_LONG).show();
            return;
        }

        try
        {
            String videoBitrateKStr = videoBitrateValue.getText().toString();
            videoBitrateK = Integer.parseInt(videoBitrateKStr);
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

        String filePrefix = videoInfo.file.getName();
        if(filePrefix.contains("."))
        {
            filePrefix = filePrefix.substring(0, filePrefix.lastIndexOf("."));
        }

        String extension = "." + container.extension;
        String inputFilePath = videoInfo.file.getAbsolutePath();

        File destination = new File(outputDir, filePrefix + extension);
        int fileNo = 0;
        while (destination.exists())
        {
            fileNo++;
            destination = new File(outputDir, filePrefix + "_" + fileNo + extension);
        }

        int startTimeSec = rangeSeekBar.getSelectedMinValue().intValue();
        int endTimeSec = rangeSeekBar.getSelectedMaxValue().intValue();

        int durationSec = (int)(videoInfo.durationMs/1000);

        startEncode(inputFilePath, startTimeSec, endTimeSec, durationSec, container, videoCodec,
                    videoBitrateK, resolution, fps, audioCodec, audioSampleRate, audioChannel,
                    audioBitrateK, destination.getAbsolutePath());
    }

    private void updateUiForEncoding()
    {
        stopVideoPlayback();

        progressBar.setProgress(0);
        progressBar.setIndeterminate(true);

        selectVideoButton.setVisibility(View.GONE);
        encodeButton.setVisibility(View.GONE);
        startJumpBack.setVisibility(View.GONE);
        startJumpForward.setVisibility(View.GONE);
        endJumpBack.setVisibility(View.GONE);
        endJumpForward.setVisibility(View.GONE);

        cancelButton.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void updateUiForVideoSettings()
    {
        selectVideoButton.setVisibility(View.VISIBLE);
        encodeButton.setVisibility(View.VISIBLE);
        startJumpBack.setVisibility(View.VISIBLE);
        startJumpForward.setVisibility(View.VISIBLE);
        endJumpBack.setVisibility(View.VISIBLE);
        endJumpForward.setVisibility(View.VISIBLE);

        cancelButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    private void cancelEncode()
    {
        try
        {
            ffmpegService.cancel();
        }
        catch (RemoteException e)
        {
            Log.w(TAG, "Failed to cancel encoding", e);
            e.printStackTrace();
        }

        updateUiForVideoSettings();
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

    private void startVideoPlayback(Integer positionSec)
    {
        stopVideoPlayback();

        int startTimeSec = rangeSeekBar.getSelectedMinValue().intValue();
        int stopTimeSec = rangeSeekBar.getSelectedMaxValue().intValue();

        if(positionSec == null)
        {
            positionSec = startTimeSec;
        }

        int durationSec = stopTimeSec - positionSec;

        videoView.seekTo(positionSec * 1000);
        videoView.start();

        videoTimer = new Timer();
        videoTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                startVideoPlayback(null);
            }
        }, durationSec * 1000);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Log.i(TAG, "Establishing messenger with service");
        // Start service and provide it a way to communicate with this class.
        Intent startServiceIntent = new Intent(this, FFmpegProcessService.class);
        msgHandler = new IncomingMessageHandler(this);
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
            startVideoPlayback(null);
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

    private void setSpinnerSelection(Spinner spinner, VideoCodec value)
    {
        for(int index = 0; index < spinner.getCount(); index++)
        {
            VideoCodecWrapper item = (VideoCodecWrapper)spinner.getItemAtPosition(index);
            if(item.codec == value)
            {
                spinner.setSelection(index);
                break;
            }
        }
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
        startJumpBack.setVisibility(View.VISIBLE);
        startJumpForward.setVisibility(View.VISIBLE);
        endJumpBack.setVisibility(View.VISIBLE);
        endJumpForward.setVisibility(View.VISIBLE);

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

                visibility = container.supportedAudioCodecs.size() > 0 ? View.VISIBLE : View.GONE;

                for(int resId : AUDIO_SETTINGS_IDS)
                {
                    findViewById(resId).setVisibility(visibility);
                }

                // Hide video bitrate for GIF, as it does not apply
                if(container == MediaContainer.GIF)
                {
                    findViewById(R.id.videoBitrateContainer).setVisibility(View.GONE);
                }

                VideoCodecWrapper currentVideoSelection = (VideoCodecWrapper)videoCodecSpinner.getSelectedItem();
                AudioCodec currentAudioSelection = (AudioCodec)audioCodecSpinner.getSelectedItem();

                videoCodecSpinner.setAdapter(new ArrayAdapter<>(MainActivity.this, R.layout.spinner_textview, VideoCodecWrapper.wrap(MainActivity.this, container.supportedVideoCodecs)));
                audioCodecSpinner.setAdapter(new ArrayAdapter<>(MainActivity.this, R.layout.spinner_textview, container.supportedAudioCodecs));

                // Attempt to set the same settings again, if they exist
                if(currentVideoSelection != null)
                {
                    setSpinnerSelection(videoCodecSpinner, currentVideoSelection.codec);
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

        LinkedList<String> fps = new LinkedList<>(Arrays.asList("15", "24", "23.98", "25", "29.97", "30", "50"));
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

            int width = Integer.parseInt(videoInfo.videoResolution.split("x")[0]);
            int height = Integer.parseInt(videoInfo.videoResolution.split("x")[1]);

            // Add a few derivatives of this resolution as well
            resolution.add( (width/2) + "x" + (height/2) );
            resolution.add( (width/4) + "x" + (height/4) );

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
                if(currentSelection != null)
                {
                    setSpinnerSelection(audioChannelSpinner, currentSelection);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView)
            {
                // Nothing to do
            }
        });

        List<Integer> audioBitrateK = new ArrayList<>(Arrays.asList(15, 24, 32, 64, 96, 128, 192, 256, 320, 384, 448, 512));
        if(videoInfo.audioBitrateK != null && audioBitrateK.contains(videoInfo.audioBitrateK) == false)
        {
            audioBitrateK.add(videoInfo.audioBitrateK);
            Collections.sort(audioBitrateK);
        }
        audioBitrateSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_textview, audioBitrateK));

        List<Integer> sampleRate = new ArrayList<>(Arrays.asList(8000, 11025, 16000, 22050, 24000, 32000, 44100, 48000));
        if(videoInfo.audioSampleRate != null && audioBitrateK.contains(videoInfo.audioSampleRate) == false)
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
            setSpinnerSelection(videoCodecSpinner, videoInfo.videoCodec);
        }

        if(videoInfo.videoFramerate != null)
        {
            setSpinnerSelection(fpsSpinner, videoInfo.videoFramerate);
        }

        if(videoInfo.videoResolution != null)
        {
            setSpinnerSelection(resolutionSpinner, videoInfo.videoResolution);
        }

        if(videoInfo.videoBitrateK != null)
        {
            videoBitrateValue.setText(Integer.toString(videoInfo.videoBitrateK));
        }

        if(videoInfo.audioCodec != null)
        {
            setSpinnerSelection(audioCodecSpinner, videoInfo.audioCodec.toString());
        }

        Integer defaultAudioBitrateK = videoInfo.audioBitrateK;
        if(defaultAudioBitrateK == null)
        {
            // Set some default if none is detected
            defaultAudioBitrateK = 128;
        }

        setSpinnerSelection(audioBitrateSpinner, defaultAudioBitrateK);

        if(videoInfo.audioSampleRate != null)
        {
            setSpinnerSelection(audioSampleRateSpinner, videoInfo.audioSampleRate);
        }

        setSpinnerSelection(audioChannelSpinner, Integer.toString(videoInfo.audioChannels));
    }

    private void setSelectMediaFile(String path)
    {
        videoView.setVideoPath(path);
        videoView.start();

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
        {
            @Override
            public void onPrepared(MediaPlayer mp)
            {
                final int durationMs = mp.getDuration();
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
                    Number prevMinValueSec = 0;
                    Number prevMaxValueSec = (int)(durationMs / 1000f);

                    // If the end time slider was moved, resume the playback
                    // this may seconds before the end
                    static final int END_PLAYBACK_HEADROOM_SEC = 3;

                    @Override
                    public void valueChanged(Number minValue, Number maxValue)
                    {
                        Integer playStartSec = null;

                        if(prevMaxValueSec.intValue() != maxValue.intValue())
                        {
                            // End time was changed
                            prevMaxValueSec = maxValue;
                            playStartSec = prevMaxValueSec.intValue();

                            // Resume playback a few seconds before the end
                            int headroom = Math.min(maxValue.intValue()-minValue.intValue(), END_PLAYBACK_HEADROOM_SEC);
                            playStartSec -= headroom;
                        }

                        if(prevMinValueSec.intValue() != minValue.intValue())
                        {
                            // Start time was changed
                            prevMinValueSec = minValue;
                            playStartSec = prevMinValueSec.intValue();
                        }

                        tvLeft.setText(getTime(minValue.intValue()));
                        tvRight.setText(getTime(maxValue.intValue()));

                        if(isEncoding() == false && playStartSec != null)
                        {
                            startVideoPlayback(playStartSec);
                        }
                    }
                });

                class RangeSeekChanger implements View.OnClickListener
                {
                    private final int startOffset;
                    private final int endOffset;
                    RangeSeekChanger(int startOffset, int endOffset)
                    {
                        this.startOffset = startOffset;
                        this.endOffset = endOffset;
                    }

                    @Override
                    public void onClick(View v)
                    {
                        rangeSeekBar.setMinValue(0);
                        rangeSeekBar.setMaxValue(durationMs / 1000f);

                        int selectedStart = rangeSeekBar.getSelectedMinValue().intValue();
                        int selectedEnd = rangeSeekBar.getSelectedMaxValue().intValue();

                        rangeSeekBar.setMinStartValue(selectedStart + startOffset);
                        rangeSeekBar.setMaxStartValue(selectedEnd + endOffset);
                        rangeSeekBar.apply();
                    }
                }

                startJumpForward.setOnClickListener(new RangeSeekChanger(1, 0));
                startJumpBack.setOnClickListener(new RangeSeekChanger(-1, 0));
                endJumpForward.setOnClickListener(new RangeSeekChanger(0, 1));
                endJumpBack.setOnClickListener(new RangeSeekChanger(0, -1));
            }
        });

        final File videoFile = new File(path);

        FFmpegUtil.getMediaDetails(new File(path), new ResultCallbackHandler<MediaInfo>()
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
                    String message = null;

                    if(messageId == MessageId.JOB_SUCCEDED_MSG)
                    {
                        result = true;
                        outputFile = ((Bundle)msg.obj).getString(FFMPEG_OUTPUT_FILE);
                        mimetype = ((Bundle)msg.obj).getString(OUTPUT_MIMETYPE);
                    }
                    else
                    {
                        message = ((Bundle)msg.obj).getString(FFMPEG_FAILURE_MSG);
                    }

                    Log.d(TAG, "Job complete, result: " + result);
                    showEncodeCompleteDialog(mainActivity, result, message, outputFile, mimetype);
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
                                              final String ffmpegMessage, final String outputFile,
                                              final String mimetype)
        {
            Log.d(TAG, "Encode result: " + result);

            Intent intent = mainActivity.getIntent();
            final String action = intent.getAction();
            boolean skipDialog = intent.getBooleanExtra("skipDialog", false);

            if(skipDialog)
            {
                mainActivity.finish();
                return;
            }

            String message;

            if(result)
            {
                message = mainActivity.getResources().getString(R.string.transcodeSuccess, outputFile);
            }
            else
            {
                message = mainActivity.getResources().getString(R.string.transcodeFailed, ffmpegMessage);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity)
                .setMessage(message)
                .setCancelable(true)
                .setOnDismissListener(new DialogInterface.OnDismissListener()
                {
                    @Override
                    public void onDismiss(DialogInterface dialog)
                    {
                        if(action != null && action.contains("ENCODE"))
                        {
                            // This activity was launched to encode this file,
                            // finish it off when complete.
                            mainActivity.finish();
                        }
                        else
                        {
                            mainActivity.startVideoPlayback(null);
                        }
                    }
                })
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

            mainActivity.updateUiForVideoSettings();
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
        final Map<String, String> USED_LIBRARIES = new ImmutableMap.Builder<String, String>()
            .put("Commons IO", "https://commons.apache.org/proper/commons-io/")
            .put("FFmpeg", "https://www.ffmpeg.org/")
            .put("FFmpeg Android", "https://github.com/bravobit/FFmpeg-Android")
            .put("Guava", "https://github.com/google/guava")
            .put("Crystal Range Seekbar", "https://github.com/syedowaisali/crystal-range-seekbar")
            .put("NoNonsense-FilePicker", "https://github.com/spacecowboy/NoNonsense-FilePicker")
            .put("javatuples", "https://www.javatuples.org/")
            .put("jackson-databind", "https://github.com/FasterXML/jackson-databind")
            .put("ACRA", "https://github.com/ACRA/acra")
            .build();

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
