package konross.wideorejestrator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Konrad on 2017-02-02.
 */

public class VideosListActivity extends Activity{

    private ArrayList<String> myList = new ArrayList<String>();
    private String filesDirectory=null;
    private String filesDirectory2[];
    public SharedPreferences preferences;
    public SharedPreferences.Editor preferencesEditor;
    private File mediaFile = new File("The output file's absolutePath");
    private final Context context = this;
    private static final String PREF_FILE_NAME = "PrefFile";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.videos_list_activity);

        preferences = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        preferencesEditor = preferences.edit();
        filesDirectory = preferences.getString("filesDirectory", null);

        File f = new File(filesDirectory);
        File file[] = f.listFiles();
        filesDirectory2= new String[file.length];
        for (int i=0; i < file.length; i++)
        {
            //here populate your listview
            filesDirectory2[i]=file[i].getName();
            myList.add( file[i].getName());
        }
        Toast.makeText(getApplicationContext(), "Ilość nagrań to: "+filesDirectory2[0], Toast.LENGTH_SHORT).show();

        ListView mListView = (ListView) findViewById(R.id.listView);
        mListView.setAdapter(new CustomAdapter(this,filesDirectory2,filesDirectory));
        
    }
}
