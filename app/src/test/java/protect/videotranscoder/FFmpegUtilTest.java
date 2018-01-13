package protect.videotranscoder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.List;

import protect.videotranscoder.media.AudioCodec;
import protect.videotranscoder.media.MediaInfo;
import protect.videotranscoder.media.MediaContainer;
import protect.videotranscoder.media.VideoCodec;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class FFmpegUtilTest
{
    @Test
    public void parseMediaInfo() throws Exception
    {
        File file = new File("/dev/null");

        String string = "Input #0, mov,mp4,m4a,3gp,3g2,mj2, from 'ExampleVideo.mp4'\n" +
                        "Metadata:\n" +
                        "major_brand     : mp42\n" +
                        "minor_version   : 0\n" +
                        "compatible_brands: isommp42\n" +
                        "creation_time   : 2018-01-02 00:09:32\n" +
                        "com.android.version: 7.1.2\n" +
                        "Duration: 00:02:22.86, start: 0.000000, bitrate: 4569 kb/s\n" +
                        "Stream #0:0(eng): Video: h264 (Constrained Baseline) (avc1 / 0x31637661), yuv420p(tv, bt709), 1080x1920, 4499 kb/s, SAR 1:1 DAR 9:16, 19.01 fps, 90k tbr, 90k tbn, 180k tbc (default)\n" +
                        "Metadata:\n" +
                        "creation_time   : 2018-01-02 00:09:32\n" +
                        "handler_name    : VideoHandle\n" +
                        "Stream #0:1(eng): Audio: aac (LC) (mp4a / 0x6134706D), 22050 Hz, mono, fltp, 63 kb/s (default)\n" +
                        "Metadata:\n" +
                        "creation_time   : 2018-01-02 00:09:32\n" +
                        "handler_name    : SoundHandle\n";

        MediaInfo info = FFmpegUtil.parseMediaInfo(file, string);

        assertEquals(file, info.file);
        assertEquals( (2*60+22)*1000 + 86, info.durationMs);
        assertEquals(MediaContainer.MP4, info.container);
        assertEquals(VideoCodec.H264, info.videoCodec);
        assertEquals("1080x1920", info.videoResolution);
        assertEquals(Integer.valueOf(4499), info.videoBitrate);
        assertEquals("19.01", info.videoFramerate);
        assertEquals(AudioCodec.AAC, info.audioCodec);
        assertEquals(Integer.valueOf(22050), info.audioSampleRate);
        assertEquals(Integer.valueOf(63), info.audioBitrate);
        assertEquals(Integer.valueOf(1), info.audioChannels);

        string = "libavutil      55. 17.103 / 55. 17.103\n" +
                "libavcodec     57. 24.102 / 57. 24.102\n" +
                "libavformat    57. 25.100 / 57. 25.100\n" +
                "libavdevice    57.  0.101 / 57.  0.101\n" +
                "libavfilter     6. 31.100 /  6. 31.100\n" +
                "libswscale      4.  0.100 /  4.  0.100\n" +
                "libswresample   2.  0.101 /  2.  0.101\n" +
                "libpostproc    54.  0.100 / 54.  0.100\n" +
                "Input #0, mov,mp4,m4a,3gp,3g2,mj2, from '/storage/self/primary/Movies/cut_video.mp4':\n" +
                "Metadata:\n" +
                "  major_brand     : isom\n" +
                "  minor_version   : 512\n" +
                "  compatible_brands: isomiso2mp41\n" +
                "  encoder         : Lavf57.25.100\n" +
                "Duration: 00:00:10.05, start: 0.035420, bitrate: 754 kb/s\n" +
                "  Stream #0:0(und): Video: mpeg4 (Simple Profile) (mp4v / 0x7634706D), yuv420p, 320x240 [SAR 1:1 DAR 4:3], 705 kb/s, 25 fps, 25 tbr, 12800 tbn, 25 tbc (default)\n" +
                "  Metadata:\n" +
                "    handler_name    : VideoHandler\n" +
                "  Stream #0:1(und): Audio: aac (LC) (mp4a / 0x6134706D), 22050 Hz, stereo, fltp, 47 kb/s (default)\n" +
                "  Metadata:\n" +
                "    handler_name    : SoundHandler";

        info = FFmpegUtil.parseMediaInfo(file, string);

        assertEquals(file, info.file);
        assertEquals( (10)*1000 + 5, info.durationMs);
        assertEquals(MediaContainer.MP4, info.container);
        assertEquals(VideoCodec.MPEG4, info.videoCodec);
        assertEquals("320x240", info.videoResolution);
        assertEquals(Integer.valueOf(705), info.videoBitrate);
        assertEquals("25", info.videoFramerate);
        assertEquals(AudioCodec.AAC, info.audioCodec);
        assertEquals(Integer.valueOf(22050), info.audioSampleRate);
        assertEquals(Integer.valueOf(47), info.audioBitrate);
        assertEquals(Integer.valueOf(2), info.audioChannels);

        string = "Input #0, flv, from 'SampleVideo_360x240_1mb.flv':\n" +
                "  Metadata:\n" + "    encoder         : Lavf53.24.2\n" +
                "  Duration: 00:00:10.64, start: 0.000000, bitrate: 792 kb/s\n" +
                "    Stream #0:0: Audio: aac (LC), 48000 Hz, 5.1, fltp\n" +
                "    Stream #0:1: Video: flv1, yuv420p, 320x240, 25 fps, 25 tbr, 1k tbn";

        info = FFmpegUtil.parseMediaInfo(file, string);

        assertEquals(file, info.file);
        assertEquals( (10)*1000 + 64, info.durationMs);
        assertEquals(MediaContainer.FLV, info.container);
        assertEquals(null, info.videoCodec);
        assertEquals("320x240", info.videoResolution);
        assertEquals(null, info.videoBitrate);
        assertEquals("25", info.videoFramerate);
        assertEquals(AudioCodec.AAC, info.audioCodec);
        assertEquals(Integer.valueOf(48000), info.audioSampleRate);
        assertEquals(null, info.audioBitrate);
        assertEquals(Integer.valueOf(2), info.audioChannels);
    }

    @Test
    public void getSupportedFormats()
    {
        // Output from "ffmpeg -formats" on an Android device
        String string =
                "  libavutil      55. 17.103 / 55. 17.10\n" +
                "  libavcodec     57. 24.102 / 57. 24.10\n" +
                "  libavformat    57. 25.100 / 57. 25.10\n" +
                "  libavdevice    57.  0.101 / 57.  0.10\n" +
                "  libavfilter     6. 31.100 /  6. 31.10\n" +
                "  libswscale      4.  0.100 /  4.  0.10\n" +
                "  libswresample   2.  0.101 /  2.  0.10\n" +
                "  libpostproc    54.  0.100 / 54.  0.10\n" +
                // On the device, the following is one long line
                "File formats:" +
                " D. = Demuxing supported" +
                " .E = Muxing supported" +
                " --" +
                " D  3dostr          3DO STR " +
                " E 3g2             3GP2 (3GPP2 file format) " +
                " E 3gp             3GP (3GPP file format)" +
                " D  4xm             4X Technologies " +
                " E a64             a64 - video for Commodore 64" +
                " D  aa              Audible AA format files" +
                " D  aac             raw ADTS AAC (Advanced Audio Coding)" +
                " DE ac3             raw AC-3" +
                " D  acm             Interplay ACM" +
                " D  act             ACT Voice file format" +
                " D  adf             Artworx Data Format" +
                " D  adp             ADP" +
                " D  ads             Sony PS2 ADS " +
                " E adts            ADTS AAC (Advanced Audio Coding)" +
                " DE adx             CRI ADX" +
                " D  aea             MD STUDIO audio" +
                " D  afc             AFC" +
                " DE aiff            Audio IFF" +
                " DE alaw            PCM A-law" +
                " D  alias_pix       Alias/Wavefront PIX image" +
                " DE amr             3GPP AMR" +
                " D  anm             Deluxe Paint Animation" +
                " D  apc             CRYO APC" +
                " D  ape             Monkey's Audio" +
                " DE apng            Animated Portable Network Graphics" +
                " D  aqtitle         ATitle subtitles" +
                " DE asf             ASF (Advanced / Active Streaming Format)" +
                " D  asf_o           ASF (Advanced / Active Streaming Format) " +
                " E asf_stream      ASF (Advanced / Active Streaming Format)" +
                " DE ass             SSA (SubStation Alpha) subtitle" +
                " DE ast             AST (Audio Stream)" +
                " DE au              Sun AU" +
                " DE avi             AVI (Audio Video Interleaved) " +
                " E avm2            SWF (ShockWave Flash) (AVM2)" +
                " D  avr             AVR (Audio Visual Research)" +
                " D  avs             AVS" +
                " D  bethsoftvid     Bethesda Softworks VID" +
                " D  bfi             Brute Force & Ignorance" +
                " D  bfstm           BFSTM (Binary Cafe Stream)" +
                " D  bin             Binary text" +
                " D  bink            Bink" +
                " DE bit             G.729 BIT file format" +
                " D  bmp_pipe        piped bmp sequence" +
                " D  bmv             Discworld II BMV" +
                " D  boa             Black Ops Audio" +
                " D  brender_pix     BRender PIX image" +
                " D  brstm           BRSTM (Binary Revolution Stream)" +
                " D  c93             Interplay C93" +
                " DE caf             Apple CAF (Core Audio Format)" +
                " DE cavsvideo       raw Chinese AVS (Audio Video Standard) video" +
                " D  cdg             CD Graphics" +
                " D  cdxl            Commodore CDXL video" +
                " D  cine            Phantom Cine" +
                " D  concat          Virtual concatenation script " +
                " E crc             CRC testing " +
                " E dash            DASH Muxer" +
                " DE data            raw data" +
                " DE daud            D-Cinema audio" +
                " D  dcstr           Sega DC STR" +
                " D  dds_pipe        piped dds sequence" +
                " D  dfa             Chronomaster DFA" +
                " DE dirac           raw Dirac" +
                " DE dnxhd           raw DNxHD (SMPTE VC-3)" +
                " D  dpx_pipe        piped dpx sequence" +
                " D  dsf             DSD Stream File (DSF)" +
                " D  dsicin          Delphine Software International CIN" +
                " D  dss             Digital Speech Standard (DSS)" +
                " DE dts             raw DTS" +
                " D  dtshd           raw DTS-HD" +
                " DE dv              DV (Digital Video)" +
                " D  dv1394          DV1394 A/V grab" +
                " D  dvbsub          raw dvbsub " +
                " E dvd             MPEG-2 PS (DVD VOB)" +
                " D  dxa             DXA" +
                " D  ea              Electronic Arts Multimedia" +
                " D  ea_cdata        Electronic Arts cdata" +
                " DE eac3            raw E-AC-3" +
                " D  epaf            Ensoniq Paris Audio File" +
                " D  exr_pipe        piped exr sequence" +
                " DE f32be           PCM 32-bit floating-point big-endian" +
                " DE f32le           PCM 32-bit floating-point little-endian " +
                " E f4v             F4V Adobe Flash Video" +
                " DE f64be           PCM 64-bit floating-point big-endian" +
                " DE f64le           PCM 64-bit floating-point little-endian" +
                " DE fbdev           Linux framebuffer" +
                " DE ffm             FFM (FFserver live feed)" +
                " DE ffmetadata      FFmpeg metadata in text" +
                " D  film_cpk        Sega FILM / CPK" +
                " DE filmstrip       Adobe Filmstrip" +
                " DE flac            raw FLAC" +
                " D  flic            FLI/FLC/FLX animation" +
                " DE flv             FLV (Flash Video) " +
                " E framecrc        framecrc testing " +
                " E framemd5        Per-frame MD5 testing" +
                " D  frm             Megalux Frame" +
                " D  fsb             FMOD Sample Bank" +
                " DE g722            raw G.722" +
                " DE g723_1          raw G.723.1" +
                " D  g729            G.729 raw format demuxer" +
                " D  genh            GENeric Header" +
                " DE gif             GIF Animation" +
                " D  gsm            raw GSM" +
                " DE gxf             GXF (General eXchange Format)" +
                " DE h261            raw H.261" +
                " DE h263            raw H.263" +
                " DE h264            raw H.264 video " +
                " E hds             HDS Muxer" +
                " DE hevc            raw HEVC video " +
                " E hls             Apple HTTP Live Streaming" +
                " D  hls,applehttp   Apple HTTP Live Streaming" +
                " D  hnm             Cryo HNM v4" +
                " DE ico             Microsoft Windows ICO" +
                " D  idcin           id Cinematic" +
                " D  idf             iCE Draw File" +
                " D  iff             IFF (Interchange File Format)" +
                " DE ilbc            iLBC storage" +
                " DE image2          image2 sequence" +
                " DE image2pipe      piped image2 sequence" +
                " D  ingenient       raw Ingenient MJPEG" +
                " D  ipmovie         Interplay MVE " +
                " E ipod            iPod H.264 MP4 (MPEG-4 Part 14)" +
                " DE ircam           Berkeley/IRCAM/CARL Sound Format " +
                " E ismv            ISMV/ISMA (Smooth Streaming)" +
                " D  iss             Funcom ISS" +
                " D  iv8             IndigoVision 8000 video" +
                " DE ivf             On2 IVF" +
                " D  ivr             IVR (Internet Video Recording)" +
                " D  j2k_pipe        pipe j2k sequence" +
                " DE jacosub         JACOsub subtitle format" +
                " D  jpeg_pipe       piped jpeg sequence" +
                " D  jpegls_pipe     piped jpegls sequence" +
                " D  jv              Bitmap Brothers JV " +
                " E latm            LOAS/LATM" +
                " D  lavfi           Libavfilter virtual input device" +
                " D  live_flv        live RTMP FLV (Flash Video)" +
                " D  lmlm4           raw lmlm4" +
                " D  loas            LOAS AudioSyncStream" +
                " DE lrc             LRC lyrics" +
                " D  lvf             LVF" +
                " D  lxf             VR native stream (LXF)" +
                " DE m4v             raw MPEG-4 video " +
                " E matroska        Matroska" +
                " D  matroska,webm   Matroska / WebM " +
                " E md5             MD5 testing" +
                " D  mgsts           Metal Gear Solid: The Twin Snakes" +
                " DE microdvd        MicroDVD subtitle format" +
                " DE mjpeg           raw MJPEG video " +
                " E mkvtimestamp_v2 extract pts as timecode v2 format, as defined by mkvtoolnix" +
                " DE mlp             raw MLP" +
                " D  mlv             Magic Lantern Video (MLV)" +
                " D  mm              American Laser Games MM" +
                " DE mmf             Yamaha SMAF " +
                " E mov             QuickTime / MOV" +
                " D  mov,mp4,4a,3gp,3g2,mj2 QuickTime / MOV " +
                " E mp2             MP2 (MPEG audio layer 2)" +
                " DE mp3             MP3 (MPEG audio layer 3) " +
                " E mp4             MP4 (MPEG-4 Part 14)" +
                " D  mpc             Musepack" +
                " D  mpc8            Musepack SV8" +
                " DE mpeg            MPEG-1 Systems / MPEG program stream " +
                " E mpeg1video      raw MPEG-1 video " +
                " E mpeg2video      raw MPEG-2 video" +
                " DE mpegts          MPEG-TS (MPEG-2 Transport Stream)" +
                " D  mpegtsraw       raw MPEG-TS (MPEG-2 Transport Stream)" +
                " D  mpegvideo       raw MPEG video" +
                " DE mpjpeg          MIME multipart JPEG" +
                " D  mpl2            MPL2 subtitles" +
                " D  mpsub           MPlayer subtitles" +
                " D  msf             Sony PS3 MSF" +
                " D  msnwctcp        MSN TCP Webcam stream" +
                " D  mtv             MTV" +
                " DE mulaw           PCM mu-law" +
                " D  mv              Silicon Graphics Movie" +
                " D  mvi             Motion Pixels MVI" +
                " DE mxf             MXF (Material eXchange Format) " +
                " E mxf_d10         MXF (Material eXchange Format) D-10 Mapping " +
                " E mxf_opatom      MXF (Material eXchange Format) Operational Pattern Atom" +
                " D  mxg            MxPEG clip" +
                " D  nc              NC camera feed" +
                " D  nistsphere      NIST SPeech HEader REsources" +
                " D  nsv             Nullsoft Streaming Video " +
                " E null            raw null video" +
                " DE nut             NUT" +
                " D  nuv             NuppelVideo " +
                " E oga             Ogg Audio" +
                " DE ogg             Ogg" +
                " DE oma             Sony OpenMG audio " +
                " E opus            Ogg Opus" +
                " D  paf             Amazing Studio Packed Animation File" +
                " D  pictor_pipe     piped pictor sequence" +
                " D  pjs             PJS (Phoenix Japanimation Society) subtitles" +
                " D  pmp             Playstation Portable PMP" +
                " D  png_pipe        piped png sequence " +
                " E psp             PSP MP4 (MPEG-4 Part 14)" +
                " D  psxstr          Sony Playstation STR" +
                " D  pva             TechnoTrend PVA" +
                " D  pvf             PVF (Portable Voice Format)" +
                " D  qcp             QCP" +
                " D  qdraw_pipe      piped qdraw sequence" +
                " D  r3d             REDCODE R3D" +
                " DE rawvideo        raw video" +
                " D  realtext        RealText subtitle format" +
                " D  redspark        RedSpark" +
                " D  rl2             RL2" +
                " DE rm              RealMedia" +
                " DE roq             raw id RoQ" +
                " D  rpl             RPL / ARMovie" +
                " D  rsd             GameCube RSD" +
                " DE rso             Lego Mindstorms RSO" +
                " DE rtp             RTP output " +
                " E rtp_mpegts      RTP/mpegts output format" +
                " DE rtsp            RTSP output" +
                " DE s16be           PCM signed 16-bit big-endian" +
                " DE s16le           PCM signed 16-bit little-endian" +
                " DE s24be           PCM signed 24-bit big-endian" +
                " DE s24le           PCM signed 24-bit little-endian" +
                " DE s32be           PCM signed 32-bit big-endian" +
                " DE s32le           PCM signed 32-bit little-endian" +
                " DE s8              PCM signed 8-bit" +
                " D  sami            SAMI subtitle format" +
                " DE sap             SAP output" +
                " D  sbg             SBaGen binaural beats script" +
                " D  sdp             SDP" +
                " D  sdr2            SDR2 " +
                " E segment         segment" +
                " D  sgi_pipe        piped sgi sequence" +
                " D  shn             raw Shorten" +
                " D  siff            Beam Software SIFF " +
                " E singlejpeg      JPEG single image" +
                " D  sln             Asterisk raw pcm" +
                " DE smjpeg          Loki SDL MJPEG" +
                " D  smk             Smacker " +
                " E smoothstreaming Smooth Streaming Muxer" +
                " D  smush           LucasArts Smush" +
                " D  sol             Sierra SOL" +
                " DE sox             SoX native" +
                " DE spdif           IEC 61937 (used on S/PDIF - IEC958) " +
                " E spx             Ogg Speex" +
                " DE srt             SubRip subtitle" +
                " D  stl             Spruce subtitle format " +
                " E stream_segment,ssegment streaming segment muxer" +
                " D  subviewer       SubViewer subtitle format" +
                " D  subviewer1      SubViewer v1 subtitle format" +
                " D  sunrast_pipe    piped sunrast sequence" +
                " D  sup             raw HDMV Presentation Graphic Stream subtitles" +
                " D  svag            Konami PS2 SVAG " +
                " E svcd            MPEG-2 PS (SVCD)" +
                " DE swf             SWF (ShockWave Flash)" +
                " D  tak             raw TAK" +
                " D  tedcaptions     TED Talks captions " +
                " E tee             Multiple muxer tee" +
                " D  thp             THP" +
                " D  tiertexseq      Tiertex Limited SEQ" +
                " D  tiff_pipe       piped tiff sequence" +
                " D  tmv             8088flex TMV" +
                " DE truehd          raw TrueHD" +
                " D  tta             TTA (True Audio)" +
                " D  tty             Teletypewriter" +
                " D  txd             Renderware TeXture Dictionary" +
                " DE u16be           PCM unsigned 16-bit big-endian" +
                " DE u16le           PCM unsigned 16-bit little-endian" +
                " DE u24be           PCM unsigned 24-bit big-endian" +
                " DE u24le           PCM unsigned 24-bit little-endian" +
                " DE u32be           PCM unsigned 32-bit big-endian" +
                " DE u32le           PCM unsigned 32-bit little-endian" +
                " DE u8              PCM unsigned 8-bit " +
                " E uncodedframecrc uncoded framecrc testing" +
                " D  v210            Uncompressed 4:2:2 10-bit" +
                " D  v210x           Uncompressed 4:2:2 10-bit " +
                " E v4l2            Video4Linux2 output device" +
                " D  vag             Sony PS2 VAG" +
                " DE vc1             raw VC-1 video" +
                " DE vc1test         VC-1 test bitstream " +
                " E vcd             MPEG-1 Systems / MPEG program stream (VCD)" +
                " D  video4linux2,v4l2 Video4Linux2 device grab" +
                " D  vivo            Vivo" +
                " D  vmd             Sierra VMD " +
                " E vob             MPEG-2 PS (VOB" +
                " D  vobsub          VobSub subtitle format" +
                " DE voc             Creative Voice" +
                " D  vpk             Sony PS2 VPK" +
                " D player         VPlayer subtitles" +
                " D  vqf             Nippon Telegraph and Telephone Corporation (NTT) TwinVQ" +
                " DE w64             Sony Wave64" +
                " DE wav             WAV / WAVE (Waveform Audio)" +
                " D  wc3movie        Wing Commander III movie " +
                " E webm            WebM " +
                " E webm_chunk      WebM Chunk Muxer" +
                " DE webm_dash_manifest WebM DASH Manifest " +
                " E webp            WebP" +
                " D  webp_pipe       piped webp sequence" +
                " DE webvtt          WebVTT subtitle" +
                " D  wsaud           Westwood Studios audio" +
                " D  wsvqa           Westwood Studios VQA" +
                " DE wtv             Windows Television (WTV)" +
                " DE wv              raw WavPack" +
                " D  wve             Psion 3 audio" +
                " D  xa              Maxis XA" +
                " D  xbin            eXtended BINary text (XBIN)" +
                " D  xmv             Microsoft XMV" +
                " D  xvag            Sony PS3 XVAG" +
                " D  xwma            Microsoft xWMA" +
                " D  yop             Psygnosis YOP" +
                " DE yuv4mpegpipe    YUV4MPEG pip";

        List<MediaContainer> containers = FFmpegUtil.parseSupportedFormats(string);

        assertEquals(MediaContainer.values().length, containers.size());
    }
}