# Video Transcoder
[![Build Status](https://travis-ci.org/brarcher/video-transcoder.svg?branch=master)](https://travis-ci.org/brarcher/video-transcoder)

<a href="https://f-droid.org/packages/protect.videoeditor/" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="90"/></a>
<a href="https://play.google.com/store/apps/details?id=protect.videoeditor" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="90"/></a>

Do you want to encode videos on your phone into different formats, trim videos, or extract audio? Are you looking for a free solution which will not take your information?

Video Transcoder is an application which uses the open source program FFmpeg to transcode video files from one format to another. By selecting the video to process, details for the video are provided and the desired settings can be configured.

The following media containers are supported: Avi, Flv, Gif, Matroska, Mp3, Mp4, Ogg, Opus, WebM. Additionally, these are the supported video codecs: H.264, MPEG-1, MPEG-2, MPEG-4, VP8, VP9, Xvid.

The application requires very few permissions, and never attempts to access the Internet.

# Screenshots

[<img src="https://github.com/brarcher/video-transcoder/raw/master/metadata/en-US/images/phoneScreenshots/screenshot-01.png" width=250>](https://github.com/brarcher/video-transcoder/raw/master/metadata/en-US/images/phoneScreenshots/screenshot-01.png)
[<img src="https://github.com/brarcher/video-transcoder/raw/master/metadata/en-US/images/phoneScreenshots/screenshot-02.png" width=250>](https://github.com/brarcher/video-transcoder/raw/master/metadata/en-US/images/phoneScreenshots/screenshot-02.png)
[<img src="https://github.com/brarcher/video-transcoder/raw/master/metadata/en-US/images/phoneScreenshots/screenshot-03.png" width=250>](https://github.com/brarcher/video-transcoder/raw/master/metadata/en-US/images/phoneScreenshots/screenshot-03.png)

# Building

To build, use the gradle wrapper scripts provided in the top level directory of the project. The following will
compile the application and run all unit tests:

GNU/Linux, OSX, UNIX:
```
./gradlew build
```

Windows:
```
./gradlew.bat build
```

# Note from Developer
The developer is currently on hiatus from the project. Questions and pull requests may not be looked at for some time. Apologies in advance.
