package protect.videotranscoder;

import android.app.Activity;
import android.os.IBinder;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import protect.videoeditor.IFFmpegProcessService;
import protect.videotranscoder.activity.MainActivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "com.sun.org.apache.xerces.*", "com.crystal.crystalrangeseekbar.*" })
@PrepareForTest(IFFmpegProcessService.Stub.class)
public class MainActivityTest
{
    // Loads the mocks without the need of a @RunWith annotation
    @Rule
    public PowerMockRule rule = new PowerMockRule();

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
}