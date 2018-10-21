package protect.videotranscoder;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.io.File;
import java.util.List;

import protect.videoeditor.IFFmpegProcessService;
import protect.videotranscoder.activity.MainActivity;
import protect.videotranscoder.media.AudioCodec;
import protect.videotranscoder.media.MediaContainer;
import protect.videotranscoder.media.MediaInfo;
import protect.videotranscoder.media.VideoCodec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "com.sun.org.apache.xerces.*", "com.crystal.crystalrangeseekbar.*" })
@PrepareForTest({IFFmpegProcessService.Stub.class, FFmpegUtil.class})
public class MainActivityTest
{
    // Loads the mocks without the need of a @RunWith annotation
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    // Note the ROOT in the File pathname; the file picker can only decode Uris that start with /ROOT
    private static final MediaInfo FAKE_MP4_MEDIA_INFO = new MediaInfo(new File("/ROOT/file.mp4"), 0,
        MediaContainer.MP4, VideoCodec.H264, "100x100", 100, "10", AudioCodec.AAC,
        41000, 100, 2);
    private static final MediaInfo FAKE_GIF_MEDIA_INFO = new MediaInfo(new File("/ROOT/file.mp4"), 0,
            MediaContainer.GIF, VideoCodec.GIF, "100x100", 100, "10", AudioCodec.NONE,
            0, 0, 0);

    private MockFFmpegService mockFFmpegService;

    class MockFFmpegService implements IFFmpegProcessService
    {
        @Override
        public boolean startEncode(List<String> args, String outputFile, String mimetype, int durationMs)
        {
            return false;
        }

        @Override
        public void cancel()
        {

        }

        @Override
        public boolean isEncoding()
        {
            return false;
        }

        @Override
        public IBinder asBinder()
        {
            return null;
        }
    }

    @Before
    public void setUp()
    {
        // Mock the AIDL interface for the FFmpeg service to return a mocked Service object
        // when requested. This is done through mocking the asInterface() method through
        // PowerMockito.
        mockStatic(IFFmpegProcessService.Stub.class);
        mockFFmpegService = new MockFFmpegService();
        PowerMockito.when(IFFmpegProcessService.Stub.asInterface(any())).thenReturn(mockFFmpegService);

        mockStatic(FFmpegUtil.class);
    }

    private void checkVisibility(Activity activity, List<Integer> ids, int mode)
    {
        for(int index = 0; index < ids.size(); index++)
        {
            View view = activity.findViewById(ids.get(index));
            assertNotNull(view);
            assertEquals(view.getVisibility(), mode);
        }
    }

    @Test
    public void initiallyNothingLoaded() throws Exception
    {
        Activity activity = Robolectric.setupActivity(MainActivity.class);
        assertTrue(activity != null);

        Button selectVideo = activity.findViewById(R.id.selectVideo);
        assertNotNull(selectVideo);
        assertEquals(selectVideo.getVisibility(), View.VISIBLE);
        assertTrue(selectVideo.isEnabled());

        Button encode = activity.findViewById(R.id.encode);
        assertNotNull(encode);
        assertEquals(encode.getVisibility(), View.GONE);

        checkVisibility(activity, MainActivity.BASIC_SETTINGS_IDS, View.GONE);
        checkVisibility(activity, MainActivity.AUDIO_SETTINGS_IDS, View.GONE);
        checkVisibility(activity, MainActivity.VIDEO_SETTINGS_IDS, View.GONE);
    }

    @Test
    public void onCreateShouldInflateLayout() throws Exception
    {
        final MainActivity activity = Robolectric.setupActivity(MainActivity.class);

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        // The settings and add button should be present
        assertEquals(menu.size(), 2);

        assertEquals("About", menu.findItem(R.id.action_about).getTitle().toString());
        assertEquals("Settings", menu.findItem(R.id.action_settings).getTitle().toString());
    }

    private void openFilePickerFromLoadSelectFile(Activity activity, MediaInfo mediaInfo) throws Exception
    {
        shadowOf(activity).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE);
        shadowOf(activity).grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        Button selectVideo = activity.findViewById(R.id.selectVideo);
        assertNotNull(selectVideo);

        selectVideo.performClick();

        ShadowActivity.IntentForResult intentForResult = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(intentForResult);

        Intent intent = intentForResult.intent;
        assertNotNull(intent);
        ComponentName componentName = intent.getComponent();
        assertNotNull(componentName);
        assertEquals("protect.videotranscoder/.picker.FastScrollerFilePickerActivity", componentName.flattenToShortString());

        Intent resultIntent = new Intent();
        resultIntent.setData(Uri.fromFile(mediaInfo.file));

        final MediaInfo mediaInfoToUse = mediaInfo;

        // When the media details are queried for the selected file, capture the call and
        // return the passed (or stubbed) MediaInfo instead of calling ffprobe.
        PowerMockito.when(FFmpegUtil.class, "getMediaDetails", any(File.class), any(ResultCallbackHandler.class)).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation)
            {
                Object [] args = invocation.getArguments();
                ResultCallbackHandler<MediaInfo> resultHandler = (ResultCallbackHandler<MediaInfo>) args[1];
                resultHandler.onResult(mediaInfoToUse);
                return null;
            }
        });

        shadowOf(activity).receiveResult(
                intent,
                Activity.RESULT_OK,
                resultIntent);
    }

    private void checkSelectedSettings(Activity activity, MediaInfo expected)
    {
        Spinner containerSpinner = activity.findViewById(R.id.containerSpinner);
        assertEquals(containerSpinner.getVisibility(), View.VISIBLE);
        assertEquals(containerSpinner.getSelectedItem().toString(), expected.container.ffmpegName);

        Spinner videoCodecSpinner = activity.findViewById(R.id.videoCodecSpinner);
        Spinner fpsSpinner = activity.findViewById(R.id.fpsSpinner);
        Spinner resolutionSpinner = activity.findViewById(R.id.resolutionSpinner);
        EditText videoBitrateValue = activity.findViewById(R.id.videoBitrateValue);
        Spinner audioCodecSpinner = activity.findViewById(R.id.audioCodecSpinner);
        Spinner audioBitrateSpinner = activity.findViewById(R.id.audioBitrateSpinner);
        Spinner audioSampleRateSpinner = activity.findViewById(R.id.audioSampleRateSpinner);
        Spinner audioChannelSpinner = activity.findViewById(R.id.audioChannelSpinner);
        View resolutionCustomContainer = activity.findViewById(R.id.resolutionCustomContainer);

        // Note, the resolution customer container is checked by itself later on
        int [] videoContainerIds = new int[]{R.id.videoCodecContainer, R.id.fpsContainer, R.id.resolutionContainer,
                R.id.videoBitrateContainer};
        int [] audioContainerIds = new int[]{R.id.audioCodecContainer, R.id.audioBitrateContainer,
                R.id.audioSampleRateContainer, R.id.audioChannelContainer};

        if(expected.container.supportedVideoCodecs.size() > 0)
        {
            // Check that all video containers are visible
            for(int id : videoContainerIds)
            {
                View view = activity.findViewById(id);

                // The video bitrate container does not apply for GIF
                if(expected.container == MediaContainer.GIF && id == R.id.videoBitrateContainer)
                {
                    assertEquals(view.toString(), view.getVisibility(), View.GONE);
                    continue;
                }

                assertEquals(view.toString(), view.getVisibility(), View.VISIBLE);
            }

            String expectedVideoCodecText;
            if(expected.videoCodec.helperTextId != null)
            {
                expectedVideoCodecText = String.format("%s (%s)",
                        expected.videoCodec.prettyName, activity.getString(expected.videoCodec.helperTextId));
            }
            else
            {
                expectedVideoCodecText = expected.videoCodec.prettyName;
            }

            assertEquals(videoCodecSpinner.getSelectedItem().toString(), expectedVideoCodecText);
            assertEquals(fpsSpinner.getSelectedItem().toString(), expected.videoFramerate);
            assertEquals(resolutionSpinner.getSelectedItem().toString(), expected.videoResolution);
            assertEquals(videoBitrateValue.getText().toString(), String.valueOf(expected.videoBitrateK));
        }
        else
        {
            // Check that all video entries are gone; video is not supported in this container
            for(int id : videoContainerIds)
            {
                View view = activity.findViewById(id);
                assertEquals(view.getVisibility(), View.GONE);
            }
        }

        if(expected.container.supportedAudioCodecs.size() > 0)
        {
            for(int id : audioContainerIds)
            {
                View view = activity.findViewById(id);
                assertEquals(view.getVisibility(), View.VISIBLE);
            }

            assertEquals(audioCodecSpinner.getSelectedItem().toString(), expected.audioCodec.ffmpegName);
            assertEquals(audioBitrateSpinner.getSelectedItem().toString(), String.valueOf(expected.audioBitrateK));
            assertEquals(audioSampleRateSpinner.getSelectedItem().toString(), String.valueOf(expected.audioSampleRate));
            assertEquals(audioChannelSpinner.getSelectedItem().toString(), String.valueOf(expected.audioChannels));
        }
        else
        {
            // Check that all audio entries are gone; audio is not supported in this container
            for(int id : audioContainerIds)
            {
                View view = activity.findViewById(id);
                assertEquals(view.getVisibility(), View.GONE);
            }
        }

        // The custom resolution entry should only be present if the resolution is set to 'custom'
        if(resolutionSpinner.getSelectedItem().toString().equals(activity.getString(R.string.custom)))
        {
            assertEquals(resolutionCustomContainer.getVisibility(), View.VISIBLE);
        }
        else
        {
            assertEquals(resolutionSpinner.getSelectedItem().toString(), expected.videoResolution);
            assertEquals(resolutionCustomContainer.getVisibility(), View.GONE);
        }
    }

    private void setSpinnerSelection(Spinner spinner, String text)
    {
        for(int index = 0; index < spinner.getCount(); index++)
        {
            String item = spinner.getItemAtPosition(index).toString();
            if(item.equals(text))
            {
                spinner.setSelection(index);
                return;
            }
        }

        fail("Could not find text '" + text + "' in spinner");
    }

    @Test
    public void selectVideoWithPermission() throws Exception
    {
        Activity activity = Robolectric.setupActivity(MainActivity.class);

        openFilePickerFromLoadSelectFile(activity, FAKE_MP4_MEDIA_INFO);

        checkSelectedSettings(activity, FAKE_MP4_MEDIA_INFO);
    }

    @Test
    public void selectMp4ChangeToGif() throws Exception
    {
        Activity activity = Robolectric.setupActivity(MainActivity.class);

        openFilePickerFromLoadSelectFile(activity, FAKE_MP4_MEDIA_INFO);

        checkSelectedSettings(activity, FAKE_MP4_MEDIA_INFO);

        Spinner containerSpinner = activity.findViewById(R.id.containerSpinner);
        setSpinnerSelection(containerSpinner, MediaContainer.GIF.ffmpegName);

        checkSelectedSettings(activity, FAKE_GIF_MEDIA_INFO);
    }

}