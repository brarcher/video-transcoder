package protect.videotranscoder;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;
import nl.bravobit.ffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import protect.videotranscoder.media.AudioCodec;
import protect.videotranscoder.media.MediaInfo;
import protect.videotranscoder.media.MediaContainer;
import protect.videotranscoder.media.VideoCodec;

/**
 * Utility class for executing ffmpeg
 */
public class FFmpegUtil
{
    private static final String TAG = "VideoTranscoder";
    private static FFmpeg ffmpeg;

    /**
     * Load FFmpeg binary
     */
    public static boolean init(final Context context)
    {
        if (ffmpeg == null)
        {
            Log.d(TAG, "Creating FFmpeg instance");
            final FFmpeg tmpFfmpeg = FFmpeg.getInstance(context.getApplicationContext());

            if(tmpFfmpeg.isSupported())
            {
                ffmpeg = tmpFfmpeg;
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return true;
        }
    }

    /**
     * Executing Ffmpeg binary with arguments
     */
    public static void call(final String[] command, @NonNull final ExecuteBinaryResponseHandler handler)
    {
        if(ffmpeg == null || ffmpeg.isCommandRunning())
        {
            String message = "Command failed, FFmpeg " + (ffmpeg == null ? "not initialized" : "still running");
            Log.d(TAG, message);

            handler.onFailure(message);
            return;
        }

        try
        {
            Log.d(TAG, "Executing command: " + commandToString(command));
            ffmpeg.execute(command, handler);
        }
        catch (FFmpegCommandAlreadyRunningException e)
        {
            String message = "Command failed, FFmpeg already running";
            Log.d(TAG, message);

            handler.onFailure(message);
            // do nothing for now
        }
    }

    public static void callGetOutput(@NonNull final String [] command,
                                     @NonNull final ResultCallbackHandler<String> resultHandler)
    {
        ExecuteBinaryResponseHandler handler = new ExecuteBinaryResponseHandler()
        {
            @Override
            public void onSuccess(String message)
            {
                resultHandler.onResult(message);
            }

            @Override
            public void onFailure(String message)
            {
                resultHandler.onResult(message);
            }
        };

        call(command, handler);
    }

    /**
     * Cancel the current in-progress invocation of FFmpeg
     */
    public static void cancelCall()
    {
        String message;

        if(ffmpeg == null || ffmpeg.isCommandRunning() == false)
        {
            message = "Cancel failed, FFmpeg not running";
        }
        else
        {
            boolean result = ffmpeg.killRunningProcesses();
            message = "Attempt to cancel FFmpeg: " + (result ? "success" : "failed");
        }

        Log.d(TAG, message);
    }

    @NonNull
    private static String commandToString(String [] cmd)
    {
        final StringBuilder commandBuilder = new StringBuilder();
        for(String part : cmd)
        {
            commandBuilder.append(part);
            commandBuilder.append(" ");
        }

        return commandBuilder.toString().trim();
    }

    /**
     * Parses a FFmpeg timestamp and returns its value in milliseconds.
     * If a timestamp could not be parsed, null is returned.
     */
    public static Long timestampToMs(String timestamp)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SS", Locale.US);
        Date parsedTimestamp;
        Date baseTimestamp;

        try
        {
            parsedTimestamp = dateFormat.parse(timestamp);
            baseTimestamp = dateFormat.parse("00:00:00.00");
        }
        catch (ParseException e)
        {
            Log.d(TAG, "Failed to parse timestamp: " + timestamp);
            return null;
        }

        // The timestamps are from epoch, so subtracting out the time
        // at midnight gets only the duration we are looking for.
        return parsedTimestamp.getTime() - baseTimestamp.getTime();
    }

    public static void getMediaDetails(final File mediaFile, final ResultCallbackHandler<MediaInfo> resultHandler)
    {
        if(ffmpeg == null || ffmpeg.isCommandRunning())
        {
            Log.d(TAG, "Failed to get media details, FFmpeg " +
                    (ffmpeg == null ? "is not initialized" : "is already running"));
            resultHandler.onResult(null);
            return;
        }

        String [] command = {"-i", mediaFile.getAbsolutePath()};
        callGetOutput(command, new ResultCallbackHandler<String>()
        {
            @Override
            public void onResult(String mediaDetailsStr)
            {
                Log.d(TAG, "Media details on " + mediaFile.getAbsolutePath() + "\n");
                for(String line : mediaDetailsStr.split("\n"))
                {
                    Log.d(TAG, line);
                }
                MediaInfo info = parseMediaInfo(mediaFile, mediaDetailsStr);
                resultHandler.onResult(info);
            }
        });
    }

    static MediaInfo parseMediaInfo(File mediaFile, String string)
    {
        long durationMs = 0;
        Integer totalBitrate = null;
        MediaContainer container = null;
        VideoCodec videoCodec = null;
        String videoResolution = null;
        Integer videoBitrate = null;
        String videoFramerate = null;
        AudioCodec audioCodec = null;
        Integer audioSampleRate = null;
        Integer audioBitrate = null;
        int audioChannels = 2;

        /*
         * Example output:
         Input #0, mov,mp4,m4a,3gp,3g2,mj2, from 'ExampleVideo.mp4':
          Metadata:
            major_brand     : mp42
            minor_version   : 0
            compatible_brands: isommp42
            creation_time   : 2018-01-02 00:09:32
            com.android.version: 7.1.2
          Duration: 00:02:22.86, start: 0.000000, bitrate: 4569 kb/s
            Stream #0:0(eng): Video: h264 (Constrained Baseline) (avc1 / 0x31637661), yuv420p(tv, bt709), 1080x1920, 4499 kb/s, SAR 1:1 DAR 9:16, 19.01 fps, 90k tbr, 90k tbn, 180k tbc (default)
            Metadata:
              creation_time   : 2018-01-02 00:09:32
              handler_name    : VideoHandle
            Stream #0:1(eng): Audio: aac (LC) (mp4a / 0x6134706D), 22050 Hz, mono, fltp, 63 kb/s (default)
            Metadata:
              creation_time   : 2018-01-02 00:09:32
              handler_name    : SoundHandle
         */

        for(String line : string.split("\n"))
        {
            line = line.trim();
            String [] split;

            if(line.startsWith("Duration:"))
            {
                // Duration: 00:02:22.86, start: 0.000000, bitrate: 4569 kb/s

                split = line.split(" ");
                if(split.length <= 1)
                {
                    continue;
                }

                String valueStr = split[1];
                valueStr = valueStr.replace(",", "");

                Long time = timestampToMs(valueStr);
                if(time == null)
                {
                    continue;
                }

                durationMs = time;

                split = line.split(",");
                for(String item : split)
                {
                    if(item.contains("bitrate:"))
                    {
                        item = item.replace("bitrate:", "").replace("kb/s", "").trim();
                        try
                        {
                            // This may be used later if the video bitrate cannot be determined.
                            totalBitrate = Integer.parseInt(item);
                        }
                        catch(NumberFormatException e)
                        {
                            continue;
                        }
                    }
                }
            }

            if(line.startsWith("Input"))
            {
                for(MediaContainer item : MediaContainer.values())
                {
                    if(line.contains(item.ffmpegName))
                    {
                        container = item;
                        break;
                    }
                }
            }

            if(line.startsWith("Stream") && line.contains("Video:"))
            {
                // Stream #0:0: Video: h264 (Main), yuv420p, 640x360 [SAR 1:1 DAR 16:9], 25 fps, 25 tbr, 1k tbn, 50 tbc
                // Stream #0:0(eng): Video: h264 (Constrained Baseline) (avc1 / 0x31637661), yuv420p(tv, bt709), 1080x1920, 4499 kb/s, SAR 1:1 DAR 9:16, 19.01 fps, 90k tbr, 90k tbn, 180k tbc (default)

                split = line.split(" ");
                if(split.length <= 4)
                {
                    continue;
                }

                String videoCodecName = split[3];
                videoCodec = VideoCodec.fromName(videoCodecName);

                // Looking for resolution. There are sometimes items such as:
                //  (mp4a / 0x6134706D)
                // that have numbers and an 'x', that need to be avoided.
                Pattern p = Pattern.compile("[0-9]+x[0-9]+[ ,]{1}");
                Matcher m = p.matcher(line);

                if(m.find())
                {
                    videoResolution = m.group(0);
                    // There will be an extra space or , at the end; strip it
                    videoResolution = videoResolution.trim().replace(",","");
                }

                split = line.split(",");
                for(String piece : split)
                {
                    piece = piece.trim();

                    if(piece.contains("kb/s"))
                    {
                        try
                        {
                            String videoBitrateStr = piece.replace("kb/s", "").trim();
                            videoBitrate = Integer.parseInt(videoBitrateStr);
                        }
                        catch(NumberFormatException e)
                        {
                            // Nothing to do
                        }
                    }

                    if(piece.contains("fps"))
                    {
                        videoFramerate = piece.replace("fps", "").trim();
                    }
                }
            }

            if(line.startsWith("Stream") && line.contains("Audio:"))
            {
                // Stream #0:1: Audio: aac (LC), 48000 Hz, 5.1, fltp
                // Stream #0:1(eng): Audio: aac (LC) (mp4a / 0x6134706D), 22050 Hz, mono, fltp, 63 kb/s (default)

                split = line.split(" ");
                if(split.length <= 4)
                {
                    continue;
                }

                String audioCodecName = split[3];
                audioCodec = AudioCodec.fromName(audioCodecName);

                split = line.split(",");
                for(String piece : split)
                {
                    piece = piece.trim();

                    if(piece.contains("Hz"))
                    {
                        try
                        {
                            String audioSampeRateStr = piece.replace("Hz", "").trim();
                            audioSampleRate = Integer.parseInt(audioSampeRateStr);
                        }
                        catch(NumberFormatException e)
                        {
                            // Nothing to do
                        }
                    }

                    if(piece.contains("kb/s"))
                    {
                        String audioBitrateStr = piece.replace("kb/s", "").trim();

                        if(audioBitrateStr.contains("(default)"))
                        {
                            audioBitrateStr = audioBitrateStr.replace("(default)", "").trim();
                        }

                        try
                        {
                            audioBitrate = Integer.parseInt(audioBitrateStr);
                        }
                        catch(NumberFormatException e)
                        {
                            // Nothing to do
                        }
                    }

                    if(piece.contains("mono"))
                    {
                        audioChannels = 1;
                    }
                }
            }
        }

        if(totalBitrate != null)
        {
            if(videoBitrate == null)
            {
                if(audioBitrate != null)
                {
                    // We know the audio bitrate, we can calculate the video bitrate
                    videoBitrate = totalBitrate - audioBitrate;
                }

                if(videoBitrate == null)
                {
                    // We do not know any of the separate bitrates. Lets guess 100 kb/s for the audio,
                    // and subtract that from the total to guess the video bitrate.

                    // As a guess, subtract 100 kb/s from the bitrate for audio, and
                    // assume that the video is the rest. This should be a decent-ish
                    // estimate if the video bitrate cannot be found later.
                    videoBitrate = totalBitrate - 100;
                }
            }
        }

        MediaInfo info = new MediaInfo(mediaFile, durationMs, container, videoCodec, videoResolution,
                videoBitrate, videoFramerate, audioCodec, audioSampleRate, audioBitrate, audioChannels);
        return info;
    }

    public static void getSupportedContainers(final ResultCallbackHandler<List<MediaContainer>> resultHandler)
    {
        if(ffmpeg == null || ffmpeg.isCommandRunning())
        {
            resultHandler.onResult(null);
            return;
        }

        String [] command = {"-formats"};

        callGetOutput(command, new ResultCallbackHandler<String>()
        {
            @Override
            public void onResult(String formatsStr)
            {
                List<MediaContainer> containers = parseSupportedFormats(formatsStr);

                Log.d(TAG, "Supported containers: " + containers.size());

                for(MediaContainer container : containers)
                {
                    Log.d(TAG, container.name());
                }

                resultHandler.onResult(containers);
            }
        });
    }

    @NonNull
    static List<MediaContainer> parseSupportedFormats(String ffmpegOutput)
    {
        List<MediaContainer> containers = new ArrayList<>();

        // The FFmpeg output has all the supported formats, but on one line.
        // They appear as the following.
        // Video containers:
        // " DE avi             AVI (Audio Video Interleaved)"
        // " DE flv             FLV (Flash Video)"
        // "  E matroska        Matroska"
        // "  E mov             QuickTime / MOV"
        // "  E mp4             MP4 (MPEG-4 Part 14)"
        // " DE mpeg            MPEG-1 Systems / MPEG program stream"
        // Audio containers:
        // " DE flac            raw FLAC"
        // " DE mp3             MP3 (MPEG audio layer 3)"
        // " DE ogg             Ogg"
        // " DE wav             WAV / WAVE (Waveform Audio)"

        for(MediaContainer video : MediaContainer.values())
        {
            if(ffmpegOutput.contains(video.ffmpegName))
            {
                containers.add(video);
            }
        }

        return containers;
    }
}
