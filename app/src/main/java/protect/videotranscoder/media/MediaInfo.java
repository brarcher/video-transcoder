package protect.videotranscoder.media;

import java.io.File;

/**
 * Container with information about a media file
 */
public class MediaInfo
{
    public final File file;
    public final long durationMs;
    public final MediaContainer container;
    public final VideoCodec videoCodec;
    public final String videoResolution;
    public final Integer videoBitrate;
    public final String videoFramerate;
    public final AudioCodec audioCodec;
    public final Integer audioSampleRate;
    public final Integer audioBitrate;
    public final Integer audioChannels;


    public MediaInfo(File file, long durationMs, MediaContainer container, VideoCodec videoCodec,
                     String videoResolution, Integer videoBitrate, String videoFramerate,
                     AudioCodec audioCodec, Integer audioSampleRate, Integer audioBitrate,
                     Integer audioChannels)
    {
        this.file = file;
        this.durationMs = durationMs;
        this.container = container;
        this.videoCodec = videoCodec;
        this.videoResolution = videoResolution;
        this.videoBitrate = videoBitrate;
        this.videoFramerate = videoFramerate;
        this.audioCodec = audioCodec;
        this.audioSampleRate = audioSampleRate;
        this.audioBitrate = audioBitrate;
        this.audioChannels = audioChannels;
    }
}
