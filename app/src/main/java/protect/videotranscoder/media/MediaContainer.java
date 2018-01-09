package protect.videotranscoder.media;

import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * List of video and audio containers the application may support
 */
public enum MediaContainer
{
    // Video and audio:
    FLV("flv", "flv", Collections.singletonList(VideoCodec.H264), Arrays.asList(AudioCodec.AAC, AudioCodec.MP3)),
    MKV("matroska", "mkv", Collections.singletonList(VideoCodec.H264), Arrays.asList(AudioCodec.AAC, AudioCodec.MP3)),
    MP4("mp4", "mp4", Arrays.asList(VideoCodec.H264, VideoCodec.MPEG4), Arrays.asList(AudioCodec.AAC, AudioCodec.MP3)),

    // Audio only
    MP3("mp3", "mp3", new ArrayList<VideoCodec>(), Collections.singletonList(AudioCodec.MP3)),
    OGG("ogg", "ogg", new ArrayList<VideoCodec>(), Arrays.asList(AudioCodec.VORBIS)),
    ;

    public final String ffmpegName;
    public final String extension;
    public final List<VideoCodec> supportedVideoCodecs;
    public final List<AudioCodec> supportedAudioCodecs;

    MediaContainer(String ffmpegName, String extension, List<VideoCodec> videoCodecs, List<AudioCodec> audioCodecs)
    {
        this.ffmpegName = ffmpegName;
        this.extension = extension;
        this.supportedVideoCodecs = Collections.unmodifiableList(videoCodecs);
        this.supportedAudioCodecs = Collections.unmodifiableList(audioCodecs);
    }

    public static MediaContainer fromName(String ffmpegName)
    {
        for(MediaContainer item : MediaContainer.values())
        {
            if(item.ffmpegName.endsWith(ffmpegName))
            {
                return item;
            }
        }

        return null;
    }
}
