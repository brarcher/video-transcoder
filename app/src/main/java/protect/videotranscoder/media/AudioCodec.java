package protect.videotranscoder.media;

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

}
