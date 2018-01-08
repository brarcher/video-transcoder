package protect.videotranscoder;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import protect.videotranscoder.media.MediaInfo;

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
    public static void init(final Context context, @NonNull final ResultCallbackHandler<Boolean> resultHandler)
    {
        if (ffmpeg == null)
        {
            Log.d(TAG, "Creating FFmpeg instance");
            final FFmpeg tmpFfmpeg = FFmpeg.getInstance(context.getApplicationContext());

            try
            {
                tmpFfmpeg.loadBinary(new LoadBinaryResponseHandler()
                {
                    @Override
                    public void onFailure()
                    {
                        Log.d(TAG, "FFmpeg load failed");
                        resultHandler.onResult(false);
                    }

                    @Override
                    public void onSuccess()
                    {
                        Log.d(TAG, "FFmpeg load succeeded");
                        ffmpeg = tmpFfmpeg;
                        resultHandler.onResult(true);
                    }
                });
            }
            catch (FFmpegNotSupportedException e)
            {
                Log.d(TAG, "Failed to load FFmpeg", e);
                resultHandler.onResult(false);
            }
        }
    }

    /**
     * Executing Ffmpeg binary with arguments
     */
    public static void call(final String[] command, @NonNull final ExecuteBinaryResponseHandler handler)
    {
        if(ffmpeg == null)
        {
            String message = "Command failed, FFmpeg not initialized";
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

    public static void getMediaDetails(final File mediaFile, final ResultCallbackHandler<MediaInfo> resultHandler)
    {
        if(ffmpeg == null)
        {
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
        String videoCodec = null;
        String videoResolution = null;
        String videoBitrate = null;
        String videoFramerate = null;
        String audioCodec = null;
        String audioSampleRate = null;
        String audioBitrate = null;
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

                SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SS", Locale.US);
                Date parsedTimestamp;
                Date baseTimestamp;

                try
                {
                    parsedTimestamp = dateFormat.parse(valueStr);
                    baseTimestamp = dateFormat.parse("00:00:00.00");
                }
                catch (ParseException e)
                {
                    Log.d(TAG, "Failed to parse timestamp: " + valueStr);
                    continue;
                }

                // The timestamps are from epoch, so subtracting out the time
                // at midnight gets only the duration we are looking for.
                durationMs = parsedTimestamp.getTime() - baseTimestamp.getTime();
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
                videoCodec = split[3];

                // Looking for resolution
                Pattern p = Pattern.compile("[0-9]+x[0-9]+");

                split = line.split(",");
                for(String piece : split)
                {
                    piece = piece.trim();

                    if(piece.contains("kb/s"))
                    {
                        videoBitrate = piece;
                    }

                    if(piece.contains("fps"))
                    {
                        videoFramerate = piece;
                    }

                    Matcher m = p.matcher(piece);
                    if(m.matches())
                    {
                        videoResolution = piece;
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
                audioCodec = split[3];

                split = line.split(",");
                for(String piece : split)
                {
                    piece = piece.trim();

                    if(piece.contains("Hz"))
                    {
                        audioSampleRate = piece;
                    }

                    if(piece.contains("kb/s"))
                    {
                        audioBitrate = piece;

                        if(audioBitrate.contains("(default)"))
                        {
                            audioBitrate = audioBitrate.replace("(default)", "").trim();
                        }
                    }

                    if(piece.contains("mono"))
                    {
                        audioChannels = 1;
                    }
                }
            }
        }

        MediaInfo info = new MediaInfo(mediaFile, durationMs, videoCodec, videoResolution,
                videoBitrate, videoFramerate, audioCodec, audioSampleRate, audioBitrate, audioChannels);
        return info;
    }
}
