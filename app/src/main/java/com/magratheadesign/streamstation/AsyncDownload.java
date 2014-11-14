package com.magratheadesign.streamstation;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.List;

/**
 * Created by ejntoo on 22/10/14.
 * Class to define background download tasks
 */
public class AsyncDownload extends AsyncTask<Void, Integer, String> {
    final String TAG = Constants.APPLICATION_TAG;

    /*
    Public fields, for web access
     */
    public int fileSize;
    public int speed;
    public int progress;
    public String fileName = "";

    File destinationFile;

    String stringFileSize = "";
    String url = "";
    private final String destination;
    private final Context context;
    private final SQLiteDatabase db;

    public boolean canceled    = false;
    public boolean paused      = false;
    public boolean completed   = false;

    public AsyncDownload(Context _context, String _url, String _destination, SQLiteDatabase _db) {
        url  = _url;
        destination = _destination;
        context = _context;
        db = _db;
    }

    public String toString() {
        return fileName;
    }

    /* Notifies the new/progress download to the main activity */
    public void sendDownloadBroadcast() {
        Intent intent = new Intent();
        intent.setAction(Constants.INTENT_DOWNLOAD_ACTION);
        intent.putExtra("index", ((StreamStationService) context).downloads.indexOf(this));
        intent.putExtra("name", fileName);
        intent.putExtra("progress", progress);
        intent.putExtra("speed", speed);
        intent.putExtra("size", fileSize);
        String status = (completed) ? "completed" : ((canceled) ? "canceled" : ((paused) ? "paused" : "active"));
        intent.putExtra("status", status);
        context.sendBroadcast(intent);
    }

    @Override
    protected void onPreExecute() {
        String[] urlArray = url.split("/");
        fileName = urlArray[urlArray.length - 1];
        try {
            fileName = URLDecoder.decode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            fileName = "generic_download";
        }
        destinationFile = new File(destination + File.separator + fileName);
        while (destinationFile.exists()) {
            destinationFile = DownloadUtils.getAlternativeName(destinationFile); // changes name if same filename exists
            Log.i(TAG, "File exists, new name: " + destinationFile);
        }
        fileName = destinationFile.getName();
        Log.i(TAG, "Downloading file: " + fileName);

        sendDownloadBroadcast();
    }

    @Override
    public String doInBackground(Void... params) {
        db.execSQL("INSERT INTO history (name, url) VALUES ('" + fileName + "', '" + url + "');");
        URL url;
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            url = new URL(this.url);
            URLConnection connection = url.openConnection();

            // Returns the file size:
            List values = connection.getHeaderFields().get("content-Length");
            if (values != null && !values.isEmpty()) {
                String sLength = (String) values.get(0);
                if (sLength != null) fileSize = Integer.parseInt(sLength);
            }
            Log.i(TAG, "File size is " + fileSize);
            stringFileSize = DownloadUtils.formatSize(fileSize); // gets the size with measure units

            is = connection.getInputStream();
            fos = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[4096];
            int len;
            int progress = 0;

            long start = System.nanoTime();
            int downloadedInOneSec = 0;
            while ((len = is.read(buffer)) > 0 && !canceled) {
                if (paused) {
                    Thread.sleep(1000);
                } else {
                    progress += len;
                    downloadedInOneSec += len;

                    fos.write(buffer, 0, len);

                    //Thread.sleep(2000); // debug, to slow down download

                    long end = System.nanoTime();
                    int elapsedSeconds = (int) (end - start);
                    if (elapsedSeconds > 1000000000) {
                        publishProgress(progress, downloadedInOneSec);
                        start = System.nanoTime();
                        downloadedInOneSec = 0;
                    } else {
                        publishProgress(progress, -1);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in the download: " + e.getMessage());
            e.printStackTrace();
            return "Error in the download: " + e.getMessage();
        } finally {
            try {
                if (is  != null) is.close();
                if (fos != null) fos.close();
            } catch (IOException e) {Log.w(TAG, "Could not close stream: " + e.getMessage());}
        }

        if (canceled && destinationFile != null) {
            boolean deleted = destinationFile.delete();
            if (deleted) Log.i(TAG, "Download canceled");
            return "Download canceled";
        }

        completed = true;
        Log.d(TAG, "Download completed");
        return "Download completed";
    }

    @Override
    protected void onProgressUpdate(Integer... args) {
        int progress = args[0];
        int speed    = args[1];

        this.progress = progress;
        this.progress = progress;
        if (speed != -1) {
            this.speed = speed;
        }

        sendDownloadBroadcast();
    }

    @Override
    protected void onPostExecute(String result) {
        completed = true;
    }

    public void cancel() {
        canceled = true;
    }

    public void pause() {
        paused = true;
    }

    public void play() {
        paused = false;
    }
}