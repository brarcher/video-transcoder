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
    public final Integer videoBitrateK;
    public final String videoFramerate;
    public final AudioCodec audioCodec;
    public final Integer audioSampleRate;
    public final Integer audioBitrateK;
    public final Integer audioChannels;


    public MediaInfo(File file, long durationMs, MediaContainer container, VideoCodec videoCodec,
                     String videoResolution, Integer videoBitrateK, String videoFramerate,
                     AudioCodec audioCodec, Integer audioSampleRate, Integer audioBitrateK,
                     Integer audioChannels)
    {
        this.file = file;
        this.durationMs = durationMs;
        this.container = container;
        this.videoCodec = videoCodec;
        this.videoResolution = videoResolution;
        this.videoBitrateK = videoBitrateK;
        this.videoFramerate = videoFramerate;
        this.audioCodec = audioCodec;
        this.audioSampleRate = audioSampleRate;
        this.audioBitrateK = audioBitrateK;
        this.audioChannels = audioChannels;
    }
}
