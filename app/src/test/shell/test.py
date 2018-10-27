#!/bin/env python

import subprocess
import os
import json
import collections
import time

ASSETS = os.path.dirname(os.path.realpath(__file__)) + os.sep + "assets"

VideoTest = collections.namedtuple("VideoTest", ["filename", "mediaContainer", "formatName", "extension", "videoCodec", "videoBitrateK", "resolution", "fps", "audioCodec", "audioCodecName", "audioSampleRate", "audioBitrateK", "audioChannel"])
AudioTest = collections.namedtuple("AudioTest", ["filename", "mediaContainer", "formatName", "extension", "audioCodec", "audioCodecName", "audioSampleRate", "audioBitrateK", "audioChannel"])

def adb(args):
    cmd = ["adb"]
    cmd += args
    result = subprocess.call(cmd)
    if result != 0:
        raise Exception("adb failed with " + str(result) + ": " + str(cmd))

def logcatClear():
    p = subprocess.Popen(["adb", "logcat", "-c"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = p.communicate()
    rc = p.wait()
    if rc != 0:
        raise Exception("logcat failed with " + str(rc) + ": " + stderr)

def logcat():
    p = subprocess.Popen(["adb", "logcat", "-d"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = p.communicate()
    rc = p.wait()
    if rc != 0:
        raise Exception("logcat failed with " + str(rc) + ": " + stderr)
    return stdout

def pullFile(filename):
    adb(["pull", "/sdcard/" + filename, "."])

def removeFile(filename):
    adb(["shell", "rm", "/sdcard/" + filename])

def pushAsset(filename):
    adb(["push", ASSETS + os.sep + filename, "/sdcard"])

def encodeVideoTest(test, output):
    pushAsset(test.filename)

    logcatClear()
    encodeVideo(test.filename, output, test.mediaContainer, test.videoCodec, test.videoBitrateK, test.resolution, test.fps, test.audioCodec, test.audioSampleRate, test.audioBitrateK, test.audioChannel)

    logs = None
    for count in range(1, 300):
        logs = logcat()
        if "Encode result" in logs:
            break
        time.sleep(1)

    if logs == None:
        raise Exception("Failed to encode file before timeout: " + test.filename)

    if "Encode result: false" in logs:
        raise Exception("Failed to encode file: " + test.filename)

    pullFile(output)
    removeFile(output)

def encodeVideo(filename, output, mediaContainer, videoCodec, videoBitrateK, resolution, fps, audioCodec, audioSampleRate, audioBitrateK, audioChannel):
    args = []
    args.append("shell")
    args.append("am")
    args.append("start")
    args.append("-a")
    args.append("protect.videotranscoder.ENCODE")
    args.append("--ez")
    args.append("skipDialog")
    args.append("true")

    args.append("--es")
    args.append("inputVideoFilePath")
    args.append("/sdcard/" + filename)

    args.append("--es")
    args.append("outputFilePath")
    args.append("/sdcard/" + output)

    args.append("--es")
    args.append("mediaContainer")
    args.append(mediaContainer)

    args.append("--es")
    args.append("videoCodec")
    args.append(videoCodec)

    args.append("--ei")
    args.append("videoBitrateK")
    args.append(str(videoBitrateK))

    args.append("--es")
    args.append("resolution")
    args.append(resolution)

    args.append("--es")
    args.append("fps")
    args.append(fps)

    if audioCodec:
        args.append("--es")
        args.append("audioCodec")
        args.append(audioCodec)

    if audioSampleRate:
        args.append("--ei")
        args.append("audioSampleRate")
        args.append(audioSampleRate)

    if audioBitrateK:
        args.append("--ei")
        args.append("audioBitrateK")
        args.append(audioBitrateK)

    if audioChannel:
        args.append("--es")
        args.append("audioChannel")
        args.append(audioChannel)

    adb(args)

def encodeAudioTest(test, output):
    pushAsset(test.filename)

    logcatClear()
    encodeAudio(test.filename, output, test.mediaContainer, test.audioCodec, test.audioSampleRate, test.audioBitrateK, test.audioChannel)

    logs = None
    for count in range(1, 300):
        logs = logcat()
        if "Encode result" in logs:
            break
        time.sleep(1)

    if logs == None:
        raise Exception("Failed to encode file before timeout: " + test.filename)

    if "Encode result: false" in logs:
        raise Exception("Failed to encode file: " + test.filename)

    pullFile(output)
    removeFile(output)

def encodeAudio(filename, output, mediaContainer, audioCodec, audioSampleRate, audioBitrateK, audioChannel):
    args = []
    args.append("shell")
    args.append("am")
    args.append("start")
    args.append("-a")
    args.append("protect.videotranscoder.ENCODE")
    args.append("--ez")
    args.append("skipDialog")
    args.append("true")

    args.append("--es")
    args.append("inputVideoFilePath")
    args.append("/sdcard/" + filename)

    args.append("--es")
    args.append("outputFilePath")
    args.append("/sdcard/" + output)

    args.append("--es")
    args.append("mediaContainer")
    args.append(mediaContainer)

    args.append("--es")
    args.append("audioCodec")
    args.append(audioCodec)

    args.append("--ei")
    args.append("audioSampleRate")
    args.append(audioSampleRate)

    args.append("--ei")
    args.append("audioBitrateK")
    args.append(audioBitrateK)

    args.append("--es")
    args.append("audioChannel")
    args.append(audioChannel)

    adb(args)

def ffprobe(filename):
    p = subprocess.Popen(["ffprobe", "-i", filename, "-print_format", "json", "-show_streams", "-show_format", "-v", "quiet"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = p.communicate()
    rc = p.wait()
    if rc != 0:
        raise Exception("ffprobe failed with " + str(rc) + " on " + filename + ":\n" + stdout + "\n" + stderr)
    data = json.loads(stdout)
    return data

def verifyVideo(test, data):
    streams = data["streams"]
    format = data["format"]

    expectedStreams = 2
    if test.mediaContainer == "gif":
        expectedStreams = 1

    if len(streams) != expectedStreams:
        raise Exception("Expected %d stream but found %d" % (expectedStreams, len(streams)))

    formatName = format["format_name"]
    if test.formatName not in formatName:
        raise Exception("Did not get expected format of " + test.formatName + ": " + formatName)

    videoStream = None
    audioStream = None

    for stream in streams:
        if stream["codec_type"] == "video":
            videoStream = stream
        else:
            audioStream = stream

    verifyVideoStream(test, videoStream)

    # A gif file will not have an audio stream
    if audioStream:
        verifyAudioStream(test, audioStream)

def verifyAudio(test, data):
    streams = data["streams"]
    format = data["format"]
    if len(streams) != 1:
        raise Exception("Expected 1 stream")

    formatName = format["format_name"]
    if test.formatName not in formatName:
        raise Exception("Did not get expected format of " + test.formatName + ": " + formatName)

    verifyAudioStream(test, streams[0])

def verifyVideoStream(test, stream):
    codecName = stream["codec_name"]
    avgFrameRate = stream["avg_frame_rate"]

    if "/" in avgFrameRate:
        num = avgFrameRate[0:avgFrameRate.index("/")]
        den = avgFrameRate[avgFrameRate.index("/")+1:]
        # End up with a string that is of an integer
        avgFrameRate = unicode(int(int(num)/int(den)))

    width = stream["width"]
    height = stream["height"]

    resolution = str(width) + "x" + str(height)

    if codecName != test.videoCodec:
        raise Exception("Unexpected video codec: '%s' vs '%s'" % (test.videoCodec, codecName))

    if avgFrameRate != test.fps:
        raise Exception("Unexpected video frame rate: '%s' vs '%s'" % (test.fps, avgFrameRate))

    if resolution != test.resolution:
        raise Exception("Unexpected video resolution: '%s' vs '%s'" % (test.resolution, resolution))

def verifyAudioStream(test, stream):
    codecName = stream["codec_name"]
    sampleRate = stream["sample_rate"]
    channels = str(stream["channels"])

    if codecName != test.audioCodecName:
        raise Exception("Unexpected audio codec: '%s' vs '%s'" % (test.audioCodecName, codecName))

    if sampleRate != test.audioSampleRate:
        raise Exception("Unexpected audio sample rate: '%s' vs '%s'" % (test.audioSampleRate, sampleRate))

    if channels != test.audioChannel:
        raise Exception("Unexpected audio channels: '%s' vs '%s'" % (test.audioChannel, channels))



# VideoTest = collections.namedtuple("VideoTest", ["filename", "mediaContainer", "formatName", "extension", "videoCodec", "videoBitrateK", "resolution", "fps", "audioCodec", "audioCodecName", "audioSampleRate", "audioBitrateK", "audioChannel"])
videoTests = []
videoTests.append(VideoTest("SampleVideo_360x240_1mb.mp4", "avi", "avi", "avi", "mpeg4", "2000", "360x240", "19", "mp3", "mp3", "22050", "100", "2"))
videoTests.append(VideoTest("SampleVideo_360x240_1mb.mp4", "mp4", "mp4", "mp4", "h264", "2000", "360x240", "19", "aac", "aac", "22050", "100", "2"))
videoTests.append(VideoTest("SampleVideo_360x240_1mb.mp4", "matroska", "matroska", "mkv", "h264", "2000", "180x120", "24", "aac", "aac", "22050", "50", "1"))
videoTests.append(VideoTest("SampleVideo_360x240_1mb.mp4", "flv", "flv", "flv", "h264", "2000", "180x120", "24", "aac", "aac", "22050", "50", "1"))
videoTests.append(VideoTest("SampleVideo_360x240_1mb.mp4", "gif", "gif", "gif", "gif", "2000", "360x240", "10", None, None, None, None, None))

# AudioTest = collections.namedtuple("AudioTest", ["filename", "mediaContainer", "formatName", "extension", "audioCodec", "audioCodecName", "audioSampleRate", "audioBitrateK", "audioChannel"])
audioTests = []
audioTests.append(AudioTest("SampleVideo_360x240_1mb.mp4", "mp3", "mp3", "mp3", "mp3", "mp3", "22050", "100", "2"))
audioTests.append(AudioTest("SampleVideo_360x240_1mb.mp4", "ogg", "ogg", "ogg", "vorbis", "vorbis", "22050", "100", "2"))
audioTests.append(AudioTest("SampleVideo_360x240_1mb.mp4", "opus", "ogg", "opus", "libopus", "opus", "48000", "100", "2"))

for videoTest in videoTests:
    print "Testing " + str(videoTest)
    output = "output." + videoTest.extension
    encodeVideoTest(videoTest, output)
    data = ffprobe(output)
    print data
    verifyVideo(videoTest, data)
    print

for audioTest in audioTests:
    print "Testing " + str(audioTest)
    output = "output." + audioTest.extension
    encodeAudioTest(audioTest, output)
    data = ffprobe(output)
    print data
    verifyAudio(audioTest, data)
    print
