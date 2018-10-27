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
    // This is the built-in avi encoder instead of using the external libxvid library.
    AVI("mpeg4", "AVI", Arrays.asList("-vtag", "xvid"), null),
    // The 'preset' setting for h264 is changed from its default of 'medium'. The
    // 'faster' setting reduces encoding times by ~73% while only reducing quality
    // a near imperceptible amount. This seems like a good trade-off for encoding
    // on a mobile devices where power usage is a concern.
    H264("h264", "H.264", Arrays.asList("-preset", "faster"), R.string.codecSlowExcellent),
    MPEG4("mpeg4", "MPEG-4", Collections.EMPTY_LIST, R.string.codecFastGood),
    MPEG2("mpeg2video", "MPEG-2", Collections.EMPTY_LIST, R.string.codecFastOk),
    MPEG1("mpeg1video", "MPEG-1", Collections.EMPTY_LIST, R.string.codecFastLow),
    VP8("libvpx", "VP8", Collections.EMPTY_LIST, null),
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
