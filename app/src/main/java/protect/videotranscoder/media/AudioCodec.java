package protect.videotranscoder.media;

import android.support.annotation.Nullable;

/**
 * List of audio codecs which the application may support
 */
public enum AudioCodec
{
    AAC("aac"),
    MP3("mp3"),
    VORBIS("vorbis"),
    ;

    public final String ffmpegName;

    AudioCodec(String ffmpegName)
    {
        this.ffmpegName = ffmpegName;
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

}
