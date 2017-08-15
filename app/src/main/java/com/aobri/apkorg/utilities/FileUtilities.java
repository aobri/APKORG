package com.aobri.apkorg.utilities;

import android.app.AlertDialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.aobri.apkorg.activities.OrganizeActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A Class that provides general file management utilities for android apps.
 * Created by Ahmed on 022 22-07-2016.
 */
public class FileUtilities {

    private List<String> supportedFileExtensions;

    /**
     * Default Constructor for instantiation.
     */
    public FileUtilities() {
    }

    /**
     * Constructor that Initializes supported file extensions.
     *
     * @param fileExtensions list of supported extensions.
     */
    public FileUtilities(List<String> fileExtensions) {
        this.supportedFileExtensions = fileExtensions;
    }

    /**
     * Validates that the given file at the file path has one of the provided file extensions.
     *
     * @param filePath       the path of a file within storage.
     * @param fileExtensions a list of supported file extensions to be checked against.
     * @return true if file is supported , false otherwise.
     */
    public boolean isSupportedFile(String filePath, List<String> fileExtensions) {
        // Check supported file extensions
        String ext = filePath.substring((filePath.lastIndexOf(".") + 1), filePath.length());
        return fileExtensions.contains(ext.toLowerCase(Locale.getDefault()));
    }

    /**
     * Creates a new directory in the given path with the specified name.
     *
     * @param directoryName the new directory name
     * @param directoryPath the path to create the directory inside.
     * @return the path to the newly created directory
     */
    public String createNewDirectory(String directoryName, String directoryPath) {
        File folder = new File(directoryPath, directoryName);
        if (!folder.exists()) {
            if (folder.mkdirs() || folder.isDirectory()) {
                Log.d("APKORG", "Folder created successfully: " + directoryName);
            } else {
                Log.e("APKORG", "Failed to make folder:" + directoryName + " in path : " + directoryPath);
                return null;
            }
        }
        return folder.getPath();
    }

    /**
     * Moves the specified file from it's path to the new path.
     *
     * @param filePath     the filePath including it's name and extension.
     * @param newDirectory the new directory (not a file with extension).
     */
    public void moveFileToDirectory(String filePath, String newDirectory) {
        /* Using channels is the fastest way for moving and copying apparently ,
         but the new Files is implemented in a good exception handling way and not that far from channels.
          */
        try {
            File apkFile = new File(filePath);
            FileChannel source = new FileInputStream(apkFile).getChannel();
            FileChannel destination = new FileOutputStream(newDirectory + File.separator + apkFile.getName()).getChannel();
            source.transferTo(0, source.size(), destination);
            boolean deleted = apkFile.delete();
            if (deleted) {
                Log.d("APKORG", apkFile.getName() + " Successfully moved to: " + newDirectory);
//                Toast.makeText(THIS, apkFile.getName()+" Successfully moved",Toast.LENGTH_LONG).show();
            } else {
                Log.e("APKORG", apkFile.getName() + "'s old file was copied but not deleted");
//                Toast.makeText(THIS, apkFile.getName()+"'s old file was not deleted",Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Log.e("APKORG", "Error in moving file");
        }
    }

    /**
     * Retrieves a list of file paths in the given directory.
     * Support is determined depending on the list of supported file extensions provided in
     * the constructor of fileUtils.
     *
     * @param appsDirectory the directory that includes supported file extensions
     * @return list of supported filepaths.
     */
    public ArrayList<String> getFilePaths(String appsDirectory) {
    /*
     Read file paths from SDCard from given directory
     */
        ArrayList<String> appPaths = new ArrayList<String>();
//        File directory = new File(android.os.Environment.getExternalStorageDirectory() + File.separator + appsDirectory);
        File directory = new File(appsDirectory);

        // check for directory
        if (directory.isDirectory()) {
            // getting list of file paths
            File[] listFiles = directory.listFiles();
            // Check for count
            if (listFiles.length > 0) {
                // loop through all files
                for (File listFile : listFiles) {
                    // get file path
                    String filePath = listFile.getAbsolutePath();
                    // check for supported file extension
                    if (isSupportedFile(filePath, supportedFileExtensions)) {
                        // Add apk path to array list
//                        appPaths.add("file://" + filePath);
                        appPaths.add(filePath);
                    }
                }
//                organized = true;
            } else {
                //directory is empty
                Log.d("APKORG", "Empty directory : " + appsDirectory);
                Toast.makeText(OrganizeActivity.THIS, appsDirectory + " is empty. Please load some APKs in it !", Toast.LENGTH_LONG).show();
//                organized = false;
                return null;
            }
        } else {
            Log.d("APKORG", "invalid directory path: " + appsDirectory);
            AlertDialog.Builder alert = new AlertDialog.Builder(OrganizeActivity.THIS);
            alert.setTitle("Error!");
            alert.setMessage(appsDirectory + " directory path is not valid! Please choose a valid path");
            alert.setPositiveButton("OK", null);
            alert.show();
//            organized = false;
            return null;
        }
//        listingProgress.dismiss();
        return appPaths;
    }

    /**
     * Returns a string representing the Label + Version from an APK file.
     *
     * @param appPath the full path to the apk file.
     * @return the label of an app + it's version.
     */
    public String getAppLabelAndVersion(String appPath) {
        final PackageManager packageManager = OrganizeActivity.THIS.getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(appPath, 0);

        if (Build.VERSION.SDK_INT >= 8) {
            // those two lines do the magic: someone commented these two lines within the package
            // manager's parser ,that's why we need them here - AOBRI - 7/7/2016
            packageInfo.applicationInfo.sourceDir = appPath;
            packageInfo.applicationInfo.publicSourceDir = appPath;
        }
        String label = packageManager.getApplicationLabel(packageInfo.applicationInfo) + " " + packageInfo.versionName;
        Log.i("AOKORG", "The Label is :" + label);
        return label;
    }
}