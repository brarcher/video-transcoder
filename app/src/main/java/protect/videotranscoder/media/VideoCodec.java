package protect.videotranscoder.media;

import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import protect.videotranscoder.R;

/**
 * List of video codecs which the application may support
 */
public enum VideoCodec
{
    H264("h264", "H.264", Collections.EMPTY_LIST, R.string.codecSlowExcellent),
    MPEG4("mpeg4", "MPEG-4", Collections.EMPTY_LIST, R.string.codecFastGood),
    MPEG2("mpeg2video", "MPEG-2", Collections.EMPTY_LIST, R.string.codecFastOk),
    MPEG1("mpeg1video", "MPEG-1", Collections.EMPTY_LIST, R.string.codecFastLow),
    GIF("gif", "GIF", Collections.EMPTY_LIST, null),
    ;

    public final String ffmpegName;
    public final String prettyName;
    public final List<String> extraFfmpegArgs;
    public final Integer helperTextId;

    VideoCodec(String ffmpegName, String prettyName, List<String> extraFfmpegArgs, Integer helperTextId)
    {
        this.ffmpegName = ffmpegName;
        this.prettyName = prettyName;
        this.extraFfmpegArgs = extraFfmpegArgs;
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
