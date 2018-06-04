/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package protect.videotranscoder.picker;

import android.os.Environment;
import android.support.annotation.Nullable;

import com.nononsenseapps.filepicker.AbstractFilePickerActivity;
import com.nononsenseapps.filepicker.AbstractFilePickerFragment;

import java.io.File;

/**
 * All this class does is return a suitable fragment.
 */
public class FastScrollerFilePickerActivity extends AbstractFilePickerActivity {

    public FastScrollerFilePickerActivity() {
        super();
    }

    @Override
    protected AbstractFilePickerFragment getFragment(@Nullable String startPath, int mode, boolean allowMultiple, boolean allowCreateDir, boolean allowExistingFile, boolean singleClick)
    {
        return getFragment(startPath, mode, allowMultiple, allowCreateDir);
    }

    protected AbstractFilePickerFragment<File> getFragment(
            @Nullable final String startPath, final int mode, final boolean allowMultiple,
            final boolean allowCreateDir) {
        AbstractFilePickerFragment<File> fragment = new FastScrollerFilePickerFragment();
        // startPath is allowed to be null. In that case, default folder should be SD-card and not "/"
        fragment.setArgs(startPath != null ? startPath : Environment.getExternalStorageDirectory().getPath(),
                mode, allowMultiple, allowCreateDir, true, false);
        return fragment;
    }
}