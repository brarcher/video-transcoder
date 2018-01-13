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
    AAC("aac", Arrays.asList("1", "2")),
    MP3("mp3", Arrays.asList("1", "2")),
    VORBIS("vorbis", Collections.singletonList("2")),
    ;

    public final String ffmpegName;
    public final List<String> supportedChannels;

    AudioCodec(String ffmpegName, List<String> supportedChannels)
    {
        this.ffmpegName = ffmpegName;
        this.supportedChannels = Collections.unmodifiableList(supportedChannels);
    }

    @Nullable
    public static AudioCodec fromName(String name)
    {
        for(AudioCodec item : values())
        {
            if(item.ffmpegName.equals(name))
            {
                return item;
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
