# Video Transcoder
[![Build Status](https://travis-ci.org/brarcher/video-transcoder.svg?branch=master)](https://travis-ci.org/brarcher/video-transcoder)

Do you want to encode videos on your phone into different formats, trim videos, or extract audio? Are you looking for a free solution which will not take your information?

Video Transcoder is an application which uses the open source program FFmpeg to transcode video files from one format to another. By selecting the video to process, details for the video are provided and the desired settings can be configured.

The application requires very few permissions, and never attempts to access the Internet.

If there is any interest in improving this project, kindly submit a pull request with proposed changes.

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

# Thanks

This application uses the following image:
- [Film](https://thenounproject.com/term/film/395618/) by [Mint Shirt](https://thenounproject.com/mint10/)
