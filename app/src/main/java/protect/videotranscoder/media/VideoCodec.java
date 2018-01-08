package protect.videotranscoder.media;

/**
 * List of video codecs which the application may support
 */
public enum VideoCodec
{
    H264("h264"),
    MPEG4("mpeg4"),
    ;

    public final String ffmpegName;

    VideoCodec(String ffmpegName)
    {
        this.ffmpegName = ffmpegName;
    }

}
