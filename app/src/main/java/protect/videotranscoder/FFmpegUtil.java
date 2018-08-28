package protect.videotranscoder;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;
import nl.bravobit.ffmpeg.FFprobe;
import nl.bravobit.ffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import nl.bravobit.ffmpeg.exceptions.FFprobeCommandAlreadyRunningException;
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
    private static FFprobe ffprobe;

    /**
     * Load FFmpeg binary
     */
    public static boolean init(final Context context)
    {
        if (ffmpeg == null)
        {
            Log.d(TAG, "Creating FFmpeg instance");
            final FFmpeg tmpFfmpeg = FFmpeg.getInstance(context.getApplicationContext());
            final FFprobe tmpFfprobe = FFprobe.getInstance(context.getApplicationContext());

            if(tmpFfmpeg.isSupported() && tmpFfprobe.isSupported())
            {
                ffmpeg = tmpFfmpeg;
                ffprobe = tmpFfprobe;
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

    /**
     * Executing FFprobe binary with arguments
     */
    public static void probe(final String[] command, @NonNull final ExecuteBinaryResponseHandler handler)
    {
        if(ffprobe == null || ffprobe.isCommandRunning())
        {
            String message = "Command failed, FFprobe " + (ffprobe == null ? "not initialized" : "still running");
            Log.d(TAG, message);

            handler.onFailure(message);
            return;
        }

        try
        {
            Log.d(TAG, "Executing command: " + commandToString(command));
            ffprobe.execute(command, handler);
        }
        catch (FFprobeCommandAlreadyRunningException e)
        {
            String message = "Command failed, FFprobe already running";
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

    public static void probeGetOutput(@NonNull final String [] command,
                                      @NonNull final ResultCallbackHandler<String> resultHandler)
    {
        ExecuteBinaryResponseHandler handler = new ExecuteBinaryResponseHandler()
        {
            @Override
            public void onSuccess(String message)
            {
                // On some systems some warnings are emitted right before the json, such as:
                //   WARNING: linker: /data/data/protect.videoeditor/files/ffprobe: unused DT entry: type 0x6ffffffe arg 0x22c0
                // Skip past this to the start of the json, if any exists.

                int jsonStartIndex = message.indexOf('{');
                if(jsonStartIndex != -1)
                {
                    message = message.substring(jsonStartIndex);
                }

                resultHandler.onResult(message);
            }

            @Override
            public void onFailure(String message)
            {
                resultHandler.onResult(message);
            }
        };

        probe(command, handler);
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

    public static boolean isFFmpegRunning()
    {
        return ffmpeg != null && ffmpeg.isCommandRunning();
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
        String [] command = {"-i",  mediaFile.getAbsolutePath(),
                "-v", "quiet", "-print_format", "json", "-show_streams", "-show_format"};

        probeGetOutput(command, new ResultCallbackHandler<String>()
        {
            @Override
            public void onResult(String mediaDetailsJsonStr)
            {
                MediaInfo info = parseMediaInfo(mediaFile, mediaDetailsJsonStr);
                resultHandler.onResult(info);
            }
        });
    }

    static MediaInfo parseMediaInfo(File mediaFile, String mediaDetailsJsonStr)
    {
        long durationMs = 0;
        Integer totalBitrateK = null;
        MediaContainer container = null;
        VideoCodec videoCodec = null;
        String videoResolution = null;
        Integer videoBitrateK = null;
        String videoFramerate = null;
        AudioCodec audioCodec = null;
        Integer audioSampleRate = null;
        Integer audioBitrateK = null;
        int audioChannels = 2;

        /*
         * Example output:
         {
            "streams": [
                {
                    "index": 0,
                    "codec_name": "h264",
                    "codec_long_name": "H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10",
                    "profile": "Main",
                    "codec_type": "video",
                    "codec_time_base": "1/50",
                    "codec_tag_string": "[0][0][0][0]",
                    "codec_tag": "0x0000",
                    "width": 640,
                    "height": 360,
                    "coded_width": 640,
                    "coded_height": 360,
                    "has_b_frames": 0,
                    "sample_aspect_ratio": "1:1",
                    "display_aspect_ratio": "16:9",
                    "pix_fmt": "yuv420p",
                    "level": 30,
                    "chroma_location": "left",
                    "refs": 1,
                    "is_avc": "true",
                    "nal_length_size": "4",
                    "r_frame_rate": "25/1",
                    "avg_frame_rate": "25/1",
                    "time_base": "1/1000",
                    "start_pts": 0,
                    "start_time": "0.000000",
                    "bits_per_raw_sample": "8",
                    "disposition": {
                        "default": 0,
                        "dub": 0,
                        "original": 0,
                        "comment": 0,
                        "lyrics": 0,
                        "karaoke": 0,
                        "forced": 0,
                        "hearing_impaired": 0,
                        "visual_impaired": 0,
                        "clean_effects": 0,
                        "attached_pic": 0
                    }
                },
                {
                    "index": 1,
                    "codec_name": "aac",
                    "codec_long_name": "AAC (Advanced Audio Coding)",
                    "profile": "LC",
                    "codec_type": "audio",
                    "codec_time_base": "1/44100",
                    "codec_tag_string": "[0][0][0][0]",
                    "codec_tag": "0x0000",
                    "sample_fmt": "fltp",
                    "sample_rate": "48000",
                    "channels": 6,
                    "channel_layout": "5.1",
                    "bits_per_sample": 0,
                    "r_frame_rate": "0/0",
                    "avg_frame_rate": "0/0",
                    "time_base": "1/1000",
                    "start_pts": 3,
                    "start_time": "0.003000",
                    "disposition": {
                        "default": 0,
                        "dub": 0,
                        "original": 0,
                        "comment": 0,
                        "lyrics": 0,
                        "karaoke": 0,
                        "forced": 0,
                        "hearing_impaired": 0,
                        "visual_impaired": 0,
                        "clean_effects": 0,
                        "attached_pic": 0
                    }
                }
            ],
            "format": {
                "filename": "/Users/brarcher/Downloads/big_buck_bunny_360p_1mb.flv",
                "nb_streams": 2,
                "nb_programs": 0,
                "format_name": "flv",
                "format_long_name": "FLV (Flash Video)",
                "start_time": "0.000000",
                "duration": "6.893000",
                "size": "1048720",
                "bit_rate": "1217142",
                "probe_score": 100,
                "tags": {
                    "encoder": "Lavf53.24.2"
                }
            }
        }
         */

        ObjectMapper mapper = new ObjectMapper();
        try
        {
            JsonNode root = mapper.readTree(mediaDetailsJsonStr);
            if(root != null)
            {

                JsonNode format = root.get("format");
                if(format != null)
                {
                    JsonNode duration = format.get("duration");
                    if(duration != null)
                    {
                        durationMs = (int)(duration.asDouble() * 1000);
                    }

                    JsonNode bitrate = format.get("bit_rate");
                    if(bitrate != null)
                    {
                        totalBitrateK = bitrate.asInt()/1000;
                    }

                    JsonNode formatName = format.get("format_name");
                    if(formatName != null)
                    {
                        String formatNameStr = formatName.asText();

                        for(MediaContainer item : MediaContainer.values())
                        {
                            if(formatNameStr.contains(item.ffmpegName))
                            {
                                container = item;
                                break;
                            }
                        }
                    }
                }

                JsonNode streams = root.get("streams");
                if(streams != null)
                {
                    for (int index = 0; index < streams.size(); index++)
                    {
                        JsonNode stream = streams.get(index);
                        JsonNode codecType = stream.get("codec_type");
                        if (codecType == null)
                        {
                            continue;
                        }

                        if (codecType.asText().equals("video"))
                        {
                            JsonNode codecName = stream.get("codec_name");
                            if (codecName == null)
                            {
                                continue;
                            }
                            videoCodec = VideoCodec.fromName(codecName.asText());

                            JsonNode width = stream.get("width");
                            JsonNode height = stream.get("height");
                            if (width == null || height == null)
                            {
                                continue;
                            }
                            videoResolution = width + "x" + height;

                            // bit_rate may not always be available, so do not require it.
                            JsonNode bitrate = stream.get("bit_rate");
                            if (bitrate != null)
                            {
                                videoBitrateK = bitrate.asInt() / 1000;
                            }

                            JsonNode frameRate = stream.get("avg_frame_rate");
                            if (frameRate == null)
                            {
                                continue;
                            }

                            try
                            {
                                String frameRateStr = frameRate.asText();
                                String[] frameRateSplit = frameRateStr.split("/");
                                int frameRateNum = Integer.parseInt(frameRateSplit[0]);
                                int frameRateDem = 1;
                                if (frameRateSplit.length > 1)
                                {
                                    frameRateDem = Integer.parseInt(frameRateSplit[1]);
                                }

                                double frameRateValue = frameRateNum / (double) frameRateDem;

                                videoFramerate = String.format(Locale.US, "%.2f", frameRateValue);
                                if (videoFramerate.contains(".00"))
                                {
                                    videoFramerate = videoFramerate.replace(".00", "");
                                }
                            } catch (NumberFormatException e)
                            {
                                continue;
                            }
                        }

                        if (codecType.asText().equals("audio"))
                        {
                            JsonNode codecName = stream.get("codec_name");
                            if (codecName == null)
                            {
                                continue;
                            }
                            audioCodec = AudioCodec.fromName(codecName.asText());

                            JsonNode sampleRate = stream.get("sample_rate");
                            if (sampleRate == null)
                            {
                                continue;
                            }
                            audioSampleRate = sampleRate.asInt();

                            // bit_rate may not always be available, so do not require it.
                            JsonNode bitrate = stream.get("bit_rate");
                            if (bitrate != null)
                            {
                                audioBitrateK = bitrate.asInt() / 1000;
                            }

                            JsonNode channelLaoyout = stream.get("channel_layout");
                            if (channelLaoyout == null)
                            {
                                continue;
                            }
                            audioChannels = channelLaoyout.asText().equals("mono") ? 1 : 2;
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            Log.w(TAG, "Failed to read media details for file : " + mediaFile.getAbsolutePath() + "\n" + mediaDetailsJsonStr);
        }

        if(totalBitrateK != null)
        {
            if(videoBitrateK == null)
            {
                if(audioBitrateK != null)
                {
                    // We know the audio bitrate, we can calculate the video bitrate
                    videoBitrateK = totalBitrateK - audioBitrateK;
                }

                if(videoBitrateK == null)
                {
                    // We do not know any of the separate bitrates. Lets guess 100 kb/s for the audio,
                    // and subtract that from the total to guess the video bitrate.

                    // As a guess, subtract 100 kb/s from the bitrate for audio, and
                    // assume that the video is the rest. This should be a decent-ish
                    // estimate if the video bitrate cannot be found later.
                    videoBitrateK = totalBitrateK - 100;
                }
            }
        }

        MediaInfo info = new MediaInfo(mediaFile, durationMs, container, videoCodec, videoResolution,
                videoBitrateK, videoFramerate, audioCodec, audioSampleRate, audioBitrateK, audioChannels);
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
