/**
 * APKORG is a simple Android tool to organize apk files into different directories depending on their
 * application label and version.
 *
 * @author Ahmed O. Nabri from Sudan.
 * @created 07-07-2016
 */
package com.aobri.apkorg.activities;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aobri.apkorg.R;
import com.aobri.apkorg.utilities.FileUtilities;
import com.aobri.apkorg.utilities.FragmentCounter;
import com.turhanoz.android.reactivedirectorychooser.event.OnDirectoryCancelEvent;
import com.turhanoz.android.reactivedirectorychooser.event.OnDirectoryChosenEvent;
import com.turhanoz.android.reactivedirectorychooser.ui.DirectoryChooserFragment;
import com.turhanoz.android.reactivedirectorychooser.ui.OnDirectoryChooserFragmentInteraction;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OrganizeActivity extends AppCompatActivity implements View.OnClickListener,
        OnDirectoryChooserFragmentInteraction {

    // CLASS STATICS:
    public static AppCompatActivity THIS;

    // CONSTANTS:
    private final List<String> FILE_EXTENSIONS = Arrays.asList("apk");  // supported app formats

    // INSTANCE VARIABLES:
    private FileUtilities fileUtils;
    private ArrayList<String> appPaths;
    private FragmentCounter fragmentCounter;
    private File currentRootDirectory = Environment.getExternalStorageDirectory();
    private boolean organized = true;
    private boolean external = false;
    private String usedPath = null ;

    // USER INTERFACE COMPONENTS/VIEWS:
    private Toolbar toolbar;
    private ImageButton selectFloatingDirectoryButton;
    private TextView appsDirectoryTextView;
    private TextView appNamesTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organize);

        // instantiating variables
        THIS = this;
        fileUtils = new FileUtilities(FILE_EXTENSIONS);
        prepareViews();
        initCurrentRootDirectory(savedInstanceState);
        initFragmentCounter(savedInstanceState);

        prepareAllPermissions();
    }

    private void prepareAllPermissions() {
        // checking permissions for android L AND ABOVE
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            // Do something for Lollipop , Marshmallow and above versions
            if (!(ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(
                        THIS,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        12345 // any unique permission request code , to be dealt with
                        // in onRequestPermissionsResults
                );
            }
        }
    }

    private void prepareViews() {
        // init the views
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        selectFloatingDirectoryButton = (ImageButton) findViewById(R.id.ibtn_appsPath2);
        appsDirectoryTextView = (TextView) findViewById(R.id.tv_appsPath);
//        appNamesTextView = (TextView) findViewById(R.id.tv_appNames);

        // configure each view properties
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.app_name);
    }

    private void initCurrentRootDirectory(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            currentRootDirectory = (File) savedInstanceState.getSerializable("currentRootDirectory");
        } else {
            currentRootDirectory = Environment.getExternalStorageDirectory();
        }
    }

    private void initFragmentCounter(Bundle savedInstanceState) {
        int fragmentCount = 0;
        if (savedInstanceState != null) {
            fragmentCount = savedInstanceState.getInt("fragmentCount");
        }
        fragmentCounter = new FragmentCounter(fragmentCount);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("fragmentCount", fragmentCounter.getCount());
        outState.putSerializable("currentRootDirectory", currentRootDirectory);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_organize, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_about:
                Dialog aboutDialog = new Dialog(this);
                aboutDialog.setCancelable(true);
                aboutDialog.setCanceledOnTouchOutside(true);
                aboutDialog.setContentView(R.layout.dialog_about);
                aboutDialog.setTitle(R.string.app_name);
                aboutDialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.ibtn_appsPath2):
                addDirectoryChooserAsFloatingFragment();
                break;
            case (R.id.btn_organize):
                organizeApps();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (fragmentCounter.getCount() == 0) {
            this.finish();
        }
    }

    void addDirectoryChooserAsFloatingFragment() {
        DialogFragment directoryChooserFragment = DirectoryChooserFragment.newInstance(currentRootDirectory);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        directoryChooserFragment.show(transaction, "RDC");
    }

    @Override
    public void onEvent(OnDirectoryChosenEvent event) {
        File directoryChosenByUser = event.getFile();
        appsDirectoryTextView.setText(directoryChosenByUser.getAbsolutePath());
    }

    @Override
    public void onEvent(OnDirectoryCancelEvent event) {
        onBackPressed();
    }

    /**
     * Organizes the apk files into different directories according to each app's name and version.
     * This method is invoked when Organize Apps Button is clicked.
     */
    private void organizeApps() {
        //1 take path from directory textView
        String appsDirectory = getAppsDirectory();
        //2 read all apk files from that path -->> for testing now just append to tv_names
        appPaths = fileUtils.getFilePaths(appsDirectory);
        if (appPaths != null) {
            if(isExternalPath(appsDirectory)){
                new OrganizeAsyncTask().execute(ContextCompat
                        .getExternalFilesDirs(getApplicationContext(),null)[1].getPath(),null , null);
            }else{
                new OrganizeAsyncTask().execute(appsDirectory, null, null);
            }
        } else {
            organized = false;
            Snackbar.make(getWindow().getDecorView(), "Selected path is not valid!", Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    /**
     * Checks if the given path is in external orr removable storage such as sdCard1 or extSdCard.
     * @param appsDirectory the path to be tested
     * @return true if directory is external , false otherwise.
     */
    private boolean isExternalPath(String appsDirectory) {
        return (appsDirectory.contains("extSdCard") || appsDirectory.contains("sdCard1"));
    }

    /**
     * Retrieves the correct apps directory , whither it's in sdCard or extSdCard.
     *
     * @return the appsDirectory containing apps to be organized.
     */
    private String getAppsDirectory() {
        return  appsDirectoryTextView.getText().toString().trim();
    }

    /**
     * OrganizeAsyncTask does the organizing of APK files in the background while showing a
     * progress dialog.
     */
    class OrganizeAsyncTask extends AsyncTask<String, Void, String> {

        ProgressDialog organizeProgress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            organizeProgress = new ProgressDialog(OrganizeActivity.this);
            organizeProgress.setMessage("Organizing apps ...");
            organizeProgress.setIndeterminate(false);
            organizeProgress.setMax(appPaths.size());
            organizeProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            organizeProgress.setCancelable(false);
            organizeProgress.show();
        }

        @Override
        protected String doInBackground(String... appsDir) {
            String directory = appsDir[0];
            if (appsDir[0] != null) {
                for (String apkPath : appPaths) {
                    String appLabel = fileUtils.getAppLabelAndVersion(apkPath);
                    //3 for each apk file in path ->> make folder with extracted info
                    String newAPKPath = fileUtils.createNewDirectory(appLabel,directory);
                    if (newAPKPath != null) {
                        //4 move corresponding apk file to that folder
                        fileUtils.moveFileToDirectory(apkPath, newAPKPath);
                    } else {
                        organized = false;
                    }
                    publishProgress();
                }
            }

            return appsDir[0];
        }

        protected void onProgressUpdate(Void... progress) {
//            organizeProgress.setProgress(organizeProgress.getProgress()+1);
            organizeProgress.incrementProgressBy(1);
        }

        @Override
        protected void onPostExecute(String result) {
            organizeProgress.dismiss();
            if (organized) {
                Snackbar snackbar = Snackbar.make(getWindow().getDecorView(),
                        "Your apps have been organized.\nfind them in the path: \n" + result,
                        Snackbar.LENGTH_LONG);
                ((TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text))
                        .setMaxLines(5);
                snackbar.show();
            } else {
                Snackbar.make(getWindow().getDecorView(), "There is an issue , not all apps organized.",
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }

}
