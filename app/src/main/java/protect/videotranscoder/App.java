package protect.videotranscoder;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;
import org.acra.sender.ReportSenderFactory;

import protect.videotranscoder.report.AcraReportSenderFactory;
import protect.videotranscoder.report.ErrorActivity;
import protect.videotranscoder.report.UserAction;

public class App extends Application
{
    private static final Class<? extends ReportSenderFactory>[] reportSenderFactoryClasses = new Class[]{AcraReportSenderFactory.class};

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        initACRA();
    }

    private void initACRA()
    {
        try
        {
            final ACRAConfiguration acraConfig = new ConfigurationBuilder(this)
                    .setReportSenderFactoryClasses(reportSenderFactoryClasses)
                    .setBuildConfigClass(BuildConfig.class)
                    .build();
            ACRA.init(this, acraConfig);
        }
        catch (ACRAConfigurationException ace)
        {
            ErrorActivity.reportError(this, ace, null, ErrorActivity.ErrorInfo.make(UserAction.SOMETHING_ELSE, "none",
                    "Could not initialize ACRA crash report", R.string.app_ui_crash));
        }
    }
}
