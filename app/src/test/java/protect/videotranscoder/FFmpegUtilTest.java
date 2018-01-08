package protect.videotranscoder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

import protect.videotranscoder.media.MediaInfo;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class FFmpegUtilTest
{
    @Test
    public void parseMediaInfo() throws Exception
    {
        File file = new File("/dev/null");

        String string = "Input #0, mov,mp4,m4a,3gp,3g2,mj2, from 'ExampleVideo.mp4'\n" +
                        "Metadata:\n" +
                        "major_brand     : mp42\n" +
                        "minor_version   : 0\n" +
                        "compatible_brands: isommp42\n" +
                        "creation_time   : 2018-01-02 00:09:32\n" +
                        "com.android.version: 7.1.2\n" +
                        "Duration: 00:02:22.86, start: 0.000000, bitrate: 4569 kb/s\n" +
                        "Stream #0:0(eng): Video: h264 (Constrained Baseline) (avc1 / 0x31637661), yuv420p(tv, bt709), 1080x1920, 4499 kb/s, SAR 1:1 DAR 9:16, 19.01 fps, 90k tbr, 90k tbn, 180k tbc (default)\n" +
                        "Metadata:\n" +
                        "creation_time   : 2018-01-02 00:09:32\n" +
                        "handler_name    : VideoHandle\n" +
                        "Stream #0:1(eng): Audio: aac (LC) (mp4a / 0x6134706D), 22050 Hz, mono, fltp, 63 kb/s (default)\n" +
                        "Metadata:\n" +
                        "creation_time   : 2018-01-02 00:09:32\n" +
                        "handler_name    : SoundHandle\n";

        MediaInfo info = FFmpegUtil.parseMediaInfo(file, string);

        assertEquals(file, info.file);
        assertEquals( (2*60+22)*1000 + 86, info.durationMs);

        assertEquals("h264", info.videoCodec);
        assertEquals("1080x1920", info.videoResolution);
        assertEquals("4499 kb/s", info.videoBitrate);
        assertEquals("19.01 fps", info.videoFramerate);
        assertEquals("aac", info.audioCodec);
        assertEquals("22050 Hz", info.audioSampleRate);
        assertEquals("63 kb/s", info.audioBitrate);
        assertEquals(1, info.audioChannels);

        string = "libavutil      55. 17.103 / 55. 17.103\n" +
                "libavcodec     57. 24.102 / 57. 24.102\n" +
                "libavformat    57. 25.100 / 57. 25.100\n" +
                "libavdevice    57.  0.101 / 57.  0.101\n" +
                "libavfilter     6. 31.100 /  6. 31.100\n" +
                "libswscale      4.  0.100 /  4.  0.100\n" +
                "libswresample   2.  0.101 /  2.  0.101\n" +
                "libpostproc    54.  0.100 / 54.  0.100\n" +
                "Input #0, mov,mp4,m4a,3gp,3g2,mj2, from '/storage/self/primary/Movies/cut_video.mp4':\n" +
                "Metadata:\n" +
                "  major_brand     : isom\n" +
                "  minor_version   : 512\n" +
                "  compatible_brands: isomiso2mp41\n" +
                "  encoder         : Lavf57.25.100\n" +
                "Duration: 00:00:10.05, start: 0.035420, bitrate: 754 kb/s\n" +
                "  Stream #0:0(und): Video: mpeg4 (Simple Profile) (mp4v / 0x7634706D), yuv420p, 320x240 [SAR 1:1 DAR 4:3], 705 kb/s, 25 fps, 25 tbr, 12800 tbn, 25 tbc (default)\n" +
                "  Metadata:\n" +
                "    handler_name    : VideoHandler\n" +
                "  Stream #0:1(und): Audio: aac (LC) (mp4a / 0x6134706D), 22050 Hz, stereo, fltp, 47 kb/s (default)\n" +
                "  Metadata:\n" +
                "    handler_name    : SoundHandler";

        info = FFmpegUtil.parseMediaInfo(file, string);

        assertEquals(file, info.file);
        assertEquals( (10)*1000 + 5, info.durationMs);

        assertEquals("mpeg4", info.videoCodec);
        assertEquals("320x240", info.videoResolution);
        assertEquals("705 kb/s", info.videoBitrate);
        assertEquals("25 fps", info.videoFramerate);
        assertEquals("aac", info.audioCodec);
        assertEquals("22050 Hz", info.audioSampleRate);
        assertEquals("47 kb/s", info.audioBitrate);
        assertEquals(2, info.audioChannels);
    }
}