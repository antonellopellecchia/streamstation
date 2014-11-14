package com.magratheadesign.streamstation;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by ejntoo on 22/10/14.
 * Local service to run the server in background
 */

public class StreamStationService extends Service {
    final int NOTIFICATION_ID = 1;
    final String TAG = Constants.APPLICATION_TAG;
    String destination = "";
    StationServer server;
    public ArrayList<AsyncDownload> downloads = new ArrayList<AsyncDownload>();
    DBHelper dbHelper;
    SQLiteDatabase db;
    Notification notification;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            String action = b.getString("action");
            if (action.equals("download")) {
                String url = b.getString("url");
                startDownload(url);
            } else if (action.equals("cancel")) {
                int id = Integer.parseInt(b.getString("id"));
                downloads.get(id).cancel();
                Log.d(TAG, "Canceled download " + id);
            } else if (action.equals("play")) {
                int id = Integer.parseInt(b.getString("id"));
                downloads.get(id).play();
                Log.d(TAG, "Played download " + id);
            } else if (action.equals("pause")) {
                int id = Integer.parseInt(b.getString("id"));
                downloads.get(id).pause();
                Log.d(TAG, "Paused download " + id);
            } else if (action.equals("remove")) {
                int id = Integer.parseInt(b.getString("id"));
                downloads.remove(id);
                Log.d(TAG, "Removed download " + id);
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /* Initializing class fields: destination directory, database etc. */
        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();
        if (intent == null) return START_STICKY;
        destination = intent.getStringExtra("destination");
        server = new StationServer(this, handler);

        /* Runs service on foreground */
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(getText(R.string.service_running))
                        .setContentText(getText(R.string.service_info));
        Intent notificationIntent = new Intent(this, Station.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.putExtra(Constants.STARTED_BY_SERVICE, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notificationBuilder.setContentIntent(pendingIntent);
        notification = notificationBuilder.build();
        startForeground(NOTIFICATION_ID, notification);

        /* Starts the HTTP server */
        try {
            server.start();
        } catch (IllegalThreadStateException e) {
            Log.w(TAG, "Server warning: " + e.getMessage());
        }
        Toast.makeText(this, R.string.server_started, Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    public void onDestroy() {
        for (AsyncDownload download : downloads) {
            if (!download.canceled) download.cancel();
        }
        if (db != null) db.close();
        server.closeSocket();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void startDownload(String url) {
        AsyncDownload download = new AsyncDownload(this, url, destination, db);
        downloads.add(download);
        download.execute();
    }
}
