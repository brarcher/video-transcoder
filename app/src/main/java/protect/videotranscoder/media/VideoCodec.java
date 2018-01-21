package protect.videotranscoder.media;

import android.support.annotation.Nullable;

/**
 * List of video codecs which the application may support
 */
public enum VideoCodec
{
    H264("h264"),
    MPEG4("mpeg4"),
    GIF("gif"),
    ;

    public final String ffmpegName;

    VideoCodec(String ffmpegName)
    {
        this.ffmpegName = ffmpegName;
    }

    @Nullable
    public static VideoCodec fromName(String name)
    {
        for(VideoCodec item : values())
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
