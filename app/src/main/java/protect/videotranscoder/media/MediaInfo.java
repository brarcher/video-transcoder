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

    private String overrideBaseName;

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

    /**
     * Determines the name of the file with the extension stripped.
     * If the name has been overridden, that value is returned instead.
     */
    public String getFileBaseName()
    {
        String basename;

        if(overrideBaseName == null)
        {
            basename = file.getName();
            if(basename.contains("."))
            {
                basename = basename.substring(0, basename.lastIndexOf("."));
            }
        }
        else
        {
            basename = overrideBaseName;
        }

        return basename;
    }

    public void setFileBaseName(String name)
    {
        overrideBaseName = name;
    }
}
