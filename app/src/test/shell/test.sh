#!/bin/bash

ASSETS="$(dirname $0)/assets"

encode_file()
{
    args=""
    for arg in "$@"
    do
        args="${args} $arg"
    done
    
    # Clear the logs, so we know when the encoding is complete
    adb shell logcat -c
    
    echo "Encoding start: ${args}"
    adb shell am start -a "protect.videotranscoder.ENCODE" --ez skipDialog true ${args}
    
    for i in `seq 1 300`; do
        result=$(adb logcat -d | grep VideoTranscoder | grep "Encode result")
        if [ ! -z "${result}" ]; then
            echo "Encoding complete"
            
            if [ ! -z "$(echo ${result} | grep 'Encode result: false')" ]; then
                echo "Encoding failed"
                exit 1
            fi
            return
        fi
        sleep 1
    done
    
    echo "Encoding did not complete before timeout"
    exit 1
}

pull_file()
{
    adb pull "$1" .
}

remove_file()
{
    adb shell rm "$1"
}

push_asset()
{
    adb push "$ASSETS/$1" /sdcard
}


echo "test mp4 -> mp4"
FILE=SampleVideo_360x240_1mb.mp4
OUTPUT=output.mp4
push_asset ${FILE}
encode_file --es inputVideoFilePath "/sdcard/${FILE}" --es outputFilePath /sdcard/${OUTPUT} --es mediaContainer mp4 --es videoCodec h264 --ei videoBitrateK 2000 --es resolution 360x240 --es fps 19 --es audioCodec aac --ei audioSampleRate 22050 --ei audioBitrateK 100 --es audioChannel 2
pull_file /sdcard/${OUTPUT}
remove_file /sdcard/${OUTPUT}
ffprobe -i ${OUTPUT}

echo "test mp4 -> flv"
FILE=SampleVideo_360x240_1mb.mp4
OUTPUT=output.flv
push_asset ${FILE}
encode_file --es inputVideoFilePath "/sdcard/${FILE}" --es outputFilePath /sdcard/${OUTPUT} --es mediaContainer flv --es videoCodec h264 --ei videoBitrateK 2000 --es resolution 360x240 --es fps 19 --es audioCodec aac --ei audioSampleRate 22050 --ei audioBitrateK 100 --es audioChannel 2
pull_file /sdcard/${OUTPUT}
remove_file /sdcard/${OUTPUT}
ffprobe -i ${OUTPUT}

echo "test mp4 -> mkv"
FILE=SampleVideo_360x240_1mb.mp4
OUTPUT=output.mkv
push_asset ${FILE}
encode_file --es inputVideoFilePath "/sdcard/${FILE}" --es outputFilePath /sdcard/${OUTPUT} --es mediaContainer matroska --es videoCodec h264 --ei videoBitrateK 2000 --es resolution 360x240 --es fps 19 --es audioCodec aac --ei audioSampleRate 22050 --ei audioBitrateK 100 --es audioChannel 2
pull_file /sdcard/${OUTPUT}
remove_file /sdcard/${OUTPUT}
ffprobe -i ${OUTPUT}

echo "test mp4 -> gif"
FILE=SampleVideo_360x240_1mb.mp4
OUTPUT=output.gif
push_asset ${FILE}
encode_file --es inputVideoFilePath "/sdcard/${FILE}" --es outputFilePath /sdcard/${OUTPUT} --es mediaContainer gif --es videoCodec gif --ei videoBitrateK 2000 --es resolution 360x240 --es fps 19
pull_file /sdcard/${OUTPUT}
remove_file /sdcard/${OUTPUT}
ffprobe -i ${OUTPUT}

echo "test mp4 -> mp3"
FILE=SampleVideo_360x240_1mb.mp4
OUTPUT=output.mp3
push_asset ${FILE}
encode_file --es inputVideoFilePath "/sdcard/${FILE}" --es outputFilePath /sdcard/${OUTPUT} --es mediaContainer mp3 --es audioCodec mp3 --ei audioSampleRate 22050 --ei audioBitrateK 100 --es audioChannel 2
pull_file /sdcard/${OUTPUT}
remove_file /sdcard/${OUTPUT}
ffprobe -i ${OUTPUT}

echo "test mp4 -> ogg"
FILE=SampleVideo_360x240_1mb.mp4
OUTPUT=output.ogg
push_asset ${FILE}
encode_file --es inputVideoFilePath "/sdcard/${FILE}" --es outputFilePath /sdcard/${OUTPUT} --es mediaContainer ogg --es audioCodec vorbis --ei audioSampleRate 22050 --ei audioBitrateK 100 --es audioChannel 2
pull_file /sdcard/${OUTPUT}
remove_file /sdcard/${OUTPUT}
ffprobe -i ${OUTPUT}

echo "test mp4 -> opus"
FILE=SampleVideo_360x240_1mb.mp4
OUTPUT=output.opus
push_asset ${FILE}
encode_file --es inputVideoFilePath "/sdcard/${FILE}" --es outputFilePath /sdcard/${OUTPUT} --es mediaContainer opus --es audioCodec libopus --ei audioSampleRate 48000 --ei audioBitrateK 100 --es audioChannel 2
pull_file /sdcard/${OUTPUT}
remove_file /sdcard/${OUTPUT}
ffprobe -i ${OUTPUT}
