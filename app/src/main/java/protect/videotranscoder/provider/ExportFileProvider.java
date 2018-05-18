package protect.videotranscoder.provider;

import android.support.v4.content.FileProvider;

/**
 * This file picker exists only to avoid a name conflict with
 * a used package that uses the FileProvider directly.
 */
public class ExportFileProvider extends FileProvider
{
}
