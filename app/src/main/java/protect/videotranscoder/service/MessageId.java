package protect.videotranscoder.service;

public enum MessageId
{
    JOB_START_MSG,
    JOB_PROGRESS_MSG,
    JOB_SUCCEDED_MSG,
    JOB_FAILED_MSG,
    FFMPEG_UNSUPPORTED_MSG,

    UNKNOWN_MSG,
    ;

    public static MessageId fromInt(int value)
    {
        for(MessageId id : values())
        {
            if(id.ordinal() == value)
            {
                return id;
            }
        }

        return UNKNOWN_MSG;
    }
}
