package protect.videotranscoder.media;

/**
 * List of video and audio containers the application may support
 */
public enum MediaContainer
{
    // Video and audio:
    FLV("flv"), // flv is compatible with vp6 (default) and h264 video, and aac (default) and mp3 audio. Valid values and file extensions: flv
    MKV("matroska"), // mkv is compatible with h264 video and aac (default), mp3, ac3 or eac3 audio. Valid values and file extensions: mkv
    MP4("mp4"), // compatible with h264 (default), hevc, and mpeg4 video, and aac (default), mp3, ac3, and eac3 audio. Valid values and file extensions: mp4, m4a, m4v, f4v, f4a, m4b, m4r, f4b

    // Audio only
    MP3("mp3"), // mp3 is compatible with mp3 audio and no video. Valid values and file extensions: mp3
    OGG("ogg"), // ogg is compatible with theora video and vorbis audio. Valid values and file extensions: ogg, oga, ogv, ogx
    ;

    public final String ffmpegName;

    MediaContainer(String ffmpegName)
    {
        this.ffmpegName = ffmpegName;
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
