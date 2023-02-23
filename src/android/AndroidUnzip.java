package com.platogo.cordova.androidunzip;

import android.net.Uri;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

public class AndroidUnzip extends CordovaPlugin {
    public static final String ACTION_UNZIP = "unzip";

    private static final int BUFFER_SIZE = 32 * 1024;

    private static final String LOG_TAG = "Unzip";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals(ACTION_UNZIP)) {
            unzip(args.getString(0), args.getString(1), callbackContext);
            return true;
        }
        return false;
    }

    private void readFile(CallbackContext callbackContext, File file, ZipFile zip, ZipEntry entry) {
        int count;
        byte data[] = new byte[BUFFER_SIZE];

        try {
            BufferedInputStream inputStream = new BufferedInputStream(zip.getInputStream(entry));
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream outputStream = new BufferedOutputStream(fos, BUFFER_SIZE);

            while ((count = inputStream.read(data, 0, BUFFER_SIZE)) != -1) {
                outputStream.write(data, 0, count);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (Exception e) {
            String message = e.getMessage();
            callbackContext.error(message);
            Log.e(LOG_TAG, message);
        }
    }

    private void unzip(final String zipFilePath, final String destPath, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    File zipFile = getFileForArg(zipFilePath);
                    if (zipFile == null || !zipFile.exists()) {
                        String message = "Zip file does not exist";
                        callbackContext.error(message);
                        Log.e(LOG_TAG, message);
                        return;
                    }

                    File destDir = getFileForArg(destPath);
                    if (destDir == null || (!destDir.exists() && !destDir.mkdirs())) {
                        String message = "Could not create output directory";
                        callbackContext.error(message);
                        Log.e(LOG_TAG, message);
                        return;
                    }

                    String outputDirectory = destDir.getAbsolutePath();
                    outputDirectory += outputDirectory.endsWith(File.separator) ? "" : File.separator;

                    ZipFile zip = new ZipFile(zipFile);
                    Enumeration<? extends ZipEntry> e = zip.entries();

                    ProgressEvent progress = new ProgressEvent(zip.size());

                    while (e.hasMoreElements()) {
                        ZipEntry entry = (ZipEntry)e.nextElement();
                        BufferedInputStream inputStream = new BufferedInputStream(zip.getInputStream(entry));


                        File outputFile = new File(outputDirectory + entry.getName());

                        String canonicalPath = outputFile.getCanonicalPath();
                        String absolutePath = outputFile.getAbsolutePath();

                        if (!canonicalPath.startsWith(outputDirectory) && !absolutePath.startsWith(outputDirectory)) {
                            String errorMessage = "Zip traversal security error";
                            callbackContext.error(errorMessage);
                            Log.e(LOG_TAG, errorMessage);
                            return;
                        }

                        if (entry.isDirectory()) {
                            outputFile.mkdirs();
                        } else {
                            File parentFile = new File(canonicalPath).getParentFile();
                            if (!parentFile.exists()) {
                                parentFile.mkdirs();
                            }

                            readFile(callbackContext, outputFile, zip, entry);
                        }

                        progress.increment();
                        updateProgress(callbackContext, progress);
                    }

                    zip.close();

                    Log.i(LOG_TAG, "Finishing unzip...");
                    callbackContext.success();
                } catch(Exception e) {
                    String message = e.getMessage();
                    callbackContext.error(message);
                    Log.e(LOG_TAG, message);
                }
            }
        });
    }

    private File getFileForArg(String arg) {
        CordovaResourceApi resourceApi = webView.getResourceApi();
        Uri uri = Uri.parse(arg);

        if (uri.getScheme() != null) {
            return resourceApi.mapUriToFile(uri);
        } else {
            return new File(arg);
        }
    }

    private void updateProgress(CallbackContext callbackContext, ProgressEvent progress) throws JSONException {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, progress.toJSONObject());
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }


    private static class ProgressEvent {
        private long loaded;
        private long total;

        public ProgressEvent(long total) {
            this.total = total;
        }

        public void increment() {
            this.loaded++;
        }

        public JSONObject toJSONObject() throws JSONException {
            return new JSONObject("{loaded:" + loaded +",total:" + total + "}");
        }
    }
}
