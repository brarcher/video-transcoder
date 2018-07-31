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
    AAC("aac", Collections.EMPTY_LIST, Arrays.asList("1", "2")),
    MP3("mp3", Collections.EMPTY_LIST, Arrays.asList("1", "2")),
    OPUS("libopus", Collections.EMPTY_LIST, Arrays.asList("1", "2")),

    // The vorbis encode is experimental, and needs other flags to enable
    VORBIS("vorbis", Arrays.asList("-strict", "-2"), Collections.singletonList("2")),
    NONE("none", Collections.EMPTY_LIST, Collections.EMPTY_LIST),
    ;

    public final String ffmpegName;
    public final List<String> extraFfmpegArgs;
    public final List<String> supportedChannels;

    AudioCodec(String ffmpegName, List<String> extraFfmpegArgs, List<String> supportedChannels)
    {
        this.ffmpegName = ffmpegName;
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
                if (item.ffmpegName.equals(name))
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
