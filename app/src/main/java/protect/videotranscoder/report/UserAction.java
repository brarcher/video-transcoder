package protect.videotranscoder.report;

/**
 * The user actions that can cause an error.
 */
public enum UserAction {
    USER_REPORT("user report"),
    UI_ERROR("ui error"),
    VIDEO_LOAD("video load"),
    ENCODE("encode"),
    SOMETHING_ELSE("something"),
    ;

    private final String message;

    UserAction(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
