package protect.videotranscoder.media;

import java.io.File;

/**
 * Container with information about a media file
 */
public class MediaInfo
{
    public final File file;
    public final long durationMs;
    public final VideoCodec videoCodec;
    public final String videoResolution;
    public final String videoBitrate;
    public final String videoFramerate;
    public final AudioCodec audioCodec;
    public final String audioSampleRate;
    public final String audioBitrate;
    public final int audioChannels;


    public MediaInfo(File file, long durationMs, VideoCodec videoCodec, String videoResolution,
                     String videoBitrate, String videoFramerate, AudioCodec audioCodec,
                     String audioSampleRate, String audioBitrate, int audioChannels)
    {
        this.file = file;
        this.durationMs = durationMs;
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
