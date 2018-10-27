package protect.videotranscoder.media;

import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * List of audio codecs which the application may support
 */
public enum AudioCodec
{
    AAC("aac", "aac", Collections.EMPTY_LIST, Arrays.asList("1", "2")),
    MP3("mp3", "mp3", Collections.EMPTY_LIST, Arrays.asList("1", "2")),
    OPUS("libopus", "opus", Collections.EMPTY_LIST, Arrays.asList("1", "2")),

    // The vorbis encode is experimental, and needs other flags to enable
    VORBIS("vorbis", "vorbus", Arrays.asList("-strict", "-2"), Collections.singletonList("2")),
    NONE("none", "none", Collections.EMPTY_LIST, Collections.EMPTY_LIST),
    ;

    public final String ffmpegName;
    public final String prettyName;
    public final List<String> extraFfmpegArgs;
    public final List<String> supportedChannels;

    AudioCodec(String ffmpegName, String prettyName, List<String> extraFfmpegArgs, List<String> supportedChannels)
    {
        this.ffmpegName = ffmpegName;
        this.prettyName = prettyName;
        this.extraFfmpegArgs = extraFfmpegArgs;
        this.supportedChannels = Collections.unmodifiableList(supportedChannels);
    }

    @Nullable
    public static AudioCodec fromName(String name)
    {
        if(name != null)
        {
            for (AudioCodec item : values())
            {
                if (item.ffmpegName.equals(name) || item.prettyName.equalsIgnoreCase(name))
                {
                    return item;
                }
            }
        }

        return null;
    }

    @Override
    public String toString()
    {
        return ffmpegName;
    }

}
