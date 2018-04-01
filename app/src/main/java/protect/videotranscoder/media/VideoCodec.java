package protect.videotranscoder.media;

import android.support.annotation.Nullable;

import protect.videotranscoder.R;

/**
 * List of video codecs which the application may support
 */
public enum VideoCodec
{
    H264("h264", "H.264", R.string.codecSlowExcellent),
    MPEG4("mpeg4", "MPEG-4", R.string.codecFastGood),
    MPEG2("mpeg2video", "MPEG-2", R.string.codecFastOk),
    MPEG1("mpeg1video", "MPEG-1", R.string.codecFastLow),
    GIF("gif", "GIF", null),
    ;

    public final String ffmpegName;
    public final String prettyName;
    public final Integer helperTextId;

    VideoCodec(String ffmpegName, String prettyName, Integer helperTextId)
    {
        this.ffmpegName = ffmpegName;
        this.prettyName = prettyName;
        this.helperTextId = helperTextId;
    }

    @Nullable
    public static VideoCodec fromName(String name)
    {
        if(name != null)
        {
            for (VideoCodec item : values())
            {
                if (item.ffmpegName.equals(name))
                {
                    return item;
                }
            }
        }

        return null;
    }
}
