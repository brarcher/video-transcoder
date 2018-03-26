// IFFmpegProcessService.aidl
package protect.videoeditor;

// Declare any non-default types here with import statements

interface IFFmpegProcessService
{
    /**
     * Start encoding a file using the given arguments
     */
    boolean startEncode(in List<String> args, String outputFile, String mimetype, int durationMs);

    /**
     * Cancel the current encoding, if any
     */
    void cancel();

    /**
     * Returns true if a file is currently being encoded, false otherwise.
     */
    boolean isEncoding();
}
