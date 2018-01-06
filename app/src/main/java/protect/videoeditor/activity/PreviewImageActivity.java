package protect.videoeditor.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import protect.videoeditor.R;
import protect.videoeditor.adapter.PreviewImageAdapter;

public class PreviewImageActivity extends AppCompatActivity
{
    private static final String FILEPATH = "filepath";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        TextView tvInstruction=(TextView)findViewById(R.id.tvInstruction) ;

        GridLayoutManager lLayoutlLayout = new GridLayoutManager(PreviewImageActivity.this, 4);
        RecyclerView rView = (RecyclerView)findViewById(R.id.recycler_view);
        rView.setHasFixedSize(true);
        rView.setLayoutManager(lLayoutlLayout);
        String filePath = getIntent().getStringExtra(FILEPATH);
        ArrayList<String> f = new ArrayList<String>();

        File dir = new File(filePath);
        tvInstruction.setText("Images stored at path "+filePath);
        File[] listFile;

        listFile = dir.listFiles();

        if(listFile != null)
        {
            for(File e:listFile)
            {
                f.add(e.getAbsolutePath());
            }
        }

        PreviewImageAdapter rcAdapter = new PreviewImageAdapter( f);
        rView.setAdapter(rcAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home)
        {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }
}
