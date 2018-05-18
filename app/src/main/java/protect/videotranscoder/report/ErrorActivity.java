package protect.videotranscoder.report;

/*
 * Originally created by Christian Schabesberger on 24.10.15.
 * Adapted by Branden Archer.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * Copyright (C) Branden Archer 2018.
 * ErrorActivity.java is originally part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import protect.videotranscoder.BuildConfig;
import protect.videotranscoder.R;
import protect.videotranscoder.activity.MainActivity;

public class ErrorActivity extends AppCompatActivity
{
    // LOG TAGS
    public static final String TAG = "VideoTranscoder";
    // BUNDLE TAGS
    public static final String ERROR_INFO = "error_info";
    public static final String ERROR_LIST = "error_list";

    public static final String ERROR_EMAIL_ADDRESS = "protect.github+" + TAG + "-" + BuildConfig.VERSION_NAME + "@gmail.com";
    public static final String ERROR_EMAIL_SUBJECT = "Exception in " + TAG + " " + BuildConfig.VERSION_NAME;
    private String[] errorList;
    private ErrorInfo errorInfo;
    private String currentTimeStamp;
    // views
    private EditText userCommentBox;

    public static void reportUiError(final AppCompatActivity activity, final Throwable el)
    {
        reportError(activity, el, null,
                ErrorInfo.make(UserAction.UI_ERROR, "none", "", R.string.app_ui_crash));
    }

    public static void reportError(final Context context, final List<Throwable> el,
                                   View rootView, final ErrorInfo errorInfo)
    {
        if (rootView != null)
        {
            Snackbar.make(rootView, R.string.error_snackbar_message, 3 * 1000)
                    .setActionTextColor(Color.YELLOW)
                    .setAction(R.string.error_snackbar_action, v ->
                            startErrorActivity(context, errorInfo, el)).show();
        }
        else
        {
            startErrorActivity(context, errorInfo, el);
        }
    }

    private static void startErrorActivity(Context context, ErrorInfo errorInfo, List<Throwable> el)
    {
        Intent intent = new Intent(context, ErrorActivity.class);
        intent.putExtra(ERROR_INFO, errorInfo);
        intent.putExtra(ERROR_LIST, elToSl(el));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void reportError(final Context context, final Throwable e,
                                   View rootView, final ErrorInfo errorInfo)
    {
        List<Throwable> el = null;
        if (e != null)
        {
            el = new Vector<>();
            el.add(e);
        }
        reportError(context, el, rootView, errorInfo);
    }

    // async call
    public static void reportError(Handler handler, final Context context, final Throwable e,
                                   final View rootView, final ErrorInfo errorInfo)
    {
        List<Throwable> el = null;
        if (e != null) {
            el = new Vector<>();
            el.add(e);
        }
        reportError(handler, context, el, rootView, errorInfo);
    }

    // async call
    public static void reportError(Handler handler, final Context context, final List<Throwable> el,
                                   final View rootView, final ErrorInfo errorInfo)
    {
        handler.post(() -> reportError(context, el, rootView, errorInfo));
    }

    public static void reportError(final Context context, final CrashReportData report, final ErrorInfo errorInfo)
    {
        // get key first (don't ask about this solution)
        ReportField key = null;
        for (ReportField k : report.keySet())
        {
            if (k.toString().equals("STACK_TRACE"))
            {
                key = k;
            }
        }
        String[] el = new String[]{report.get(key).toString()};

        Intent intent = new Intent(context, ErrorActivity.class);
        intent.putExtra(ERROR_INFO, errorInfo);
        intent.putExtra(ERROR_LIST, el);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static String getStackTrace(final Throwable throwable)
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    // errorList to StringList
    private static String[] elToSl(List<Throwable> stackTraces)
    {
        String[] out = new String[stackTraces.size()];
        for (int i = 0; i < stackTraces.size(); i++)
        {
            out[i] = getStackTrace(stackTraces.get(i));
        }
        return out;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error);

        Intent intent = getIntent();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.error_report_title);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        Button reportButton = findViewById(R.id.errorReportButton);
        userCommentBox = findViewById(R.id.errorCommentBox);
        TextView errorView = findViewById(R.id.errorView);
        TextView errorMessageView = findViewById(R.id.errorMessageView);

        errorInfo = intent.getParcelableExtra(ERROR_INFO);
        errorList = intent.getStringArrayExtra(ERROR_LIST);

        currentTimeStamp = getCurrentTimeStamp();

        reportButton.setOnClickListener((View v) ->
        {
            Intent i = new Intent(Intent.ACTION_SENDTO);
            i.setData(Uri.parse("mailto:" + ERROR_EMAIL_ADDRESS))
                    .putExtra(Intent.EXTRA_SUBJECT, ERROR_EMAIL_SUBJECT)
                    .putExtra(Intent.EXTRA_TEXT, buildText());

            startActivity(Intent.createChooser(i, getString(R.string.send_email_title)));
        });

        // normal bugreport
        buildInfo(errorInfo);
        if (errorInfo.message != 0)
        {
            errorMessageView.setText(errorInfo.message);
        }
        else
        {
            errorMessageView.setVisibility(View.GONE);
            findViewById(R.id.messageWhatHappenedView).setVisibility(View.GONE);
        }

        errorView.setText(formErrorText(errorList));

        // print stack trace once again for debugging:
        for (String e : errorList)
        {
            Log.e(TAG, e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.error_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if(id == android.R.id.home)
        {
            goToReturnActivity();
        }

        if(id == R.id.menu_item_share_error)
        {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, buildText());
            intent.setType("text/plain");
            startActivity(Intent.createChooser(intent, getString(R.string.share_dialog_title)));
        }

        return false;
    }

    private String formErrorText(String[] el)
    {
        StringBuilder sb = new StringBuilder();
        if (el != null)
        {
            for (String e : el)
            {
                sb.append("-------------------------------------\n");
                sb.append(e);
            }
        }
        sb.append("-------------------------------------");
        return sb.toString();
    }

    /**
     * Get the activity to return to
     */
    static Class<? extends Activity> getReturnActivity()
    {
        return MainActivity.class;
    }

    private void goToReturnActivity()
    {
        Class<? extends Activity> checkedReturnActivity = getReturnActivity();

        Intent intent = new Intent(this, checkedReturnActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        NavUtils.navigateUpTo(this, intent);
    }

    private void buildInfo(ErrorInfo info)
    {
        TextView infoLabelView = findViewById(R.id.errorInfoLabelsView);
        TextView infoView = findViewById(R.id.errorInfosView);
        String text = "";

        infoLabelView.setText(getString(R.string.info_labels).replace("\\n", "\n"));

        text += getUserActionString(info.userAction)
                + "\n" + info.request
                + "\n" + getContentLangString()
                + "\n" + info.serviceName
                + "\n" + currentTimeStamp
                + "\n" + getPackageName()
                + "\n" + BuildConfig.VERSION_NAME
                + "\n" + getOsString();

        infoView.setText(text);
    }

    private String buildText()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("user_action: ").append(getUserActionString(errorInfo.userAction)).append("\n");
        sb.append("request: ").append(errorInfo.request).append("\n");
        sb.append("content_language: ").append(getContentLangString()).append("\n");
        sb.append("service: ").append(errorInfo.serviceName).append("\n");
        sb.append("package: ").append(getPackageName()).append("\n");
        sb.append("version: ").append(BuildConfig.VERSION_NAME).append("\n");
        sb.append("os: ").append(getOsString()).append("\n");
        sb.append("time: ").append(currentTimeStamp).append("\n");

        sb.append("\nexceptions:\n");
        if (errorList != null)
        {
            for (String e : errorList)
            {
                sb.append(e).append("\n");
            }
        }

        sb.append("user_comment:\n").append(userCommentBox.getText().toString());

        return sb.toString();

    }

    private String getUserActionString(UserAction userAction)
    {
        if (userAction == null)
        {
            return "Your description is in another castle.";
        }
        else
        {
            return userAction.getMessage();
        }
    }

    private String getContentLangString()
    {
        return Locale.getDefault().getLanguage();
    }

    private String getOsString() {
        String osBase = Build.VERSION.SDK_INT >= 23 ? Build.VERSION.BASE_OS : "Android";
        return System.getProperty("os.name")
                + " " + (osBase.isEmpty() ? "Android" : osBase)
                + " " + Build.VERSION.RELEASE
                + " - " + Integer.toString(Build.VERSION.SDK_INT);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        goToReturnActivity();
    }

    public String getCurrentTimeStamp()
    {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(new Date());
    }

    public static class ErrorInfo implements Parcelable
    {
        public static final Parcelable.Creator<ErrorInfo> CREATOR = new Parcelable.Creator<ErrorInfo>()
        {
            @Override
            public ErrorInfo createFromParcel(Parcel source)
            {
                return new ErrorInfo(source);
            }

            @Override
            public ErrorInfo[] newArray(int size)
            {
                return new ErrorInfo[size];
            }
        };
        final UserAction userAction;
        final String request;
        final String serviceName;
        @StringRes
        final int message;

        private ErrorInfo(UserAction userAction, String serviceName, String request, @StringRes int message)
        {
            this.userAction = userAction;
            this.serviceName = serviceName;
            this.request = request;
            this.message = message;
        }

        ErrorInfo(Parcel in)
        {
            this.userAction = UserAction.valueOf(in.readString());
            this.request = in.readString();
            this.serviceName = in.readString();
            this.message = in.readInt();
        }

        public static ErrorInfo make(UserAction userAction, String serviceName, String request, @StringRes int message)
        {
            return new ErrorInfo(userAction, serviceName, request, message);
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeString(this.userAction.name());
            dest.writeString(this.request);
            dest.writeString(this.serviceName);
            dest.writeInt(this.message);
        }
    }
}
