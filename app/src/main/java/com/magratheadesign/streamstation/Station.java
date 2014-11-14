package com.magratheadesign.streamstation;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.jraf.android.backport.switchwidget.Switch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ejntoo on 03/10/14.
 * Main activity
 */

public class Station extends ActionBarActivity {
    final String TAG = Constants.APPLICATION_TAG;

    DownloadMapAdapter downloadAdapter;
    ArrayList<HashMap<String, String>> downloadList;

    String destination = "";
    Switch serverToggle;
    Intent serviceIntent;

    IntentFilter filter = new IntentFilter(Constants.INTENT_DOWNLOAD_ACTION);
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            HashMap<String, String> downloadMap = new HashMap<String, String>();
            int index = intent.getIntExtra("index", -1);
            if (index != -1) {
                downloadMap.put("name", intent.getStringExtra("name"));
                downloadMap.put("progress", intent.getIntExtra("progress", 0) + "");
                downloadMap.put("speed", intent.getIntExtra("speed", 0) + "");
                downloadMap.put("size", intent.getIntExtra("size", 0) + "");
                String status = intent.getStringExtra("status");
                downloadMap.put("status", status);
                if (status.equals("canceled")) {
                    if (downloadList.size() > index) downloadList.remove(index);
                } else {
                    if (downloadList.size() > index) {
                        downloadList.set(index, downloadMap);
                        Log.i(TAG, "Setting download");
                    } else {
                        downloadList.add(index, downloadMap);
                        Log.i(TAG, "Adding download");
                    }
                }
                downloadAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        destination = sharedPref.getString("pref_dir", "");
        File sdCard = Environment.getExternalStorageDirectory();
        File destinationDir;
        if (destination.equals("")) {
            destinationDir = new File(sdCard, "download/StreamStation");
            if (!destinationDir.exists()) destinationDir.mkdirs();
            destination = destinationDir.getAbsolutePath();

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("pref_dir", destination);
            editor.commit();
        } else {
            destinationDir = new File(destination);
            if (!destinationDir.exists()) destinationDir.mkdirs();
            destination = destinationDir.getAbsolutePath();
        }

        serviceIntent = new Intent(this, StreamStationService.class);
        serviceIntent.putExtra("destination", destination);

        ListView downloadListView = (ListView) findViewById(R.id.download_list_view);
        downloadList = new ArrayList<HashMap<String, String>>();
        downloadAdapter = new DownloadMapAdapter(this, R.layout.download_layout, downloadList);
        downloadListView.setAdapter(downloadAdapter);

        String stringIpAddress;
        TextView localIpView = (TextView) findViewById(R.id.local_ip);

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
            stringIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
            localIpView.setText(getString(R.string.local_ip) + " " + stringIpAddress);
        } else {
            localIpView.setText(getString(R.string.wifi_not_connected));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(receiver, filter);
    }

    @Override
    public void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.station, menu);
        serverToggle = (Switch) MenuItemCompat.getActionView(menu.findItem(R.id.switch_layout)).findViewById(R.id.server_toggle);
        serverToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if (isChecked) startService(serviceIntent);
                else requestStopService();
            }
        });

        /* Detects whether the activity was started by the service notification */
        Intent startedByServiceIntent = getIntent();
        boolean startedByService = startedByServiceIntent.getBooleanExtra(Constants.STARTED_BY_SERVICE, false);
        if (startedByService) serverToggle.setChecked(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent prefsIntent = new Intent(this, Preferences.class);
                startActivity(prefsIntent);
                return true;
            case R.id.action_history:
                Intent historyIntent = new Intent(this, HistoryActivity.class);
                startActivity(historyIntent);
                return true;
            case R.id.action_help:
                Intent helpIntent = new Intent(this, HelpActivity.class);
                startActivity(helpIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void requestStopService() {
        boolean pending = false;
        for (HashMap<String, String> download : downloadList) {
            if (download.get("status").equals("active") || download.get("status").equals("paused")) {
                pending = true;
                break;
            }
        }
        if (pending) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.dialog_pending_text).setTitle(R.string.dialog_pending_title);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    stopService();
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // the button remains checked if the user didn't accept the dialog:
                    serverToggle.setChecked(true);
                    dialog.dismiss();
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    serverToggle.setChecked(true);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            stopService();
        }
    }

    public void stopService() {
        stopService(serviceIntent);
    }

    public class DownloadMapAdapter extends BaseAdapter {
        private final Context context;
        private final int layoutRes;
        private final ArrayList<HashMap<String, String>> data;

        public DownloadMapAdapter(Context _context, int _layoutRes, ArrayList<HashMap<String, String>> _data) {
            context = _context;
            layoutRes = _layoutRes;
            data = _data;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int i) {
            return data.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int index, View convertView, ViewGroup viewGroup) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(layoutRes, viewGroup, false);
            }
            TextView downloadName = (TextView) convertView.findViewById(R.id.download_name);
            TextView downloadSpeed = (TextView) convertView.findViewById(R.id.download_speed);
            ProgressBar downloadProgress = (ProgressBar) convertView.findViewById(R.id.download_progress);
            TextView downloadInfo = (TextView) convertView.findViewById(R.id.download_info);

            HashMap<String, String> downloadMap = data.get(index);
            String status = downloadMap.get("status");
            int size = Integer.parseInt(downloadMap.get("size"));
            int progress = Integer.parseInt(downloadMap.get("progress"));

            downloadName.setText(downloadMap.get("name"));
            if (status.equals("active")) downloadSpeed.setText(DownloadUtils.formatSize(Integer.parseInt(downloadMap.get("speed"))) + "/s");

            if (status.equals("completed") || status.equals("canceled")) {
                downloadProgress.setVisibility(View.GONE);
            } else {
                downloadProgress.setMax(size);
                downloadProgress.setProgress(progress);
            }

            String readableSize = DownloadUtils.formatSize(size);
            String readableProgress = DownloadUtils.formatSize(progress);
            int percent = (int) ((double) progress * 100.0 / (double) size);
            if (status.equals("active")) {
                if (size == 0) {
                    downloadInfo.setText("Downloading, but unknown file size");
                } else {
                    downloadInfo.setText("Downloaded " + readableProgress + " on " + readableSize + ", " + percent + "%");
                }
            } else if (status.equals("paused")) {
                downloadInfo.setText("Paused, " + percent + "% completed");
            } else if (status.equals("completed")) {
                downloadInfo.setText("Completed, " + readableSize);
            } else if (status.equals("canceled")) {
                downloadInfo.setText("Download canceled");
            } else {
                downloadInfo.setText("Unknown status " + status);
            }

            return convertView;
        }
    }

    public static class MainFragment extends Fragment {

        public MainFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main, container, false);
        }
    }

    public static class AdFragment extends Fragment {

        private AdView adView;

        public AdFragment() {
        }

        @Override
        public void onActivityCreated(Bundle bundle) {
            super.onActivityCreated(bundle);
            View view = getView();
            try {
                adView = (AdView) view.findViewById(R.id.adView);
            } catch (NullPointerException e) {return;}
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_admob, container, true);
        }

        /** Called when leaving the activity */
        @Override
        public void onPause() {
            if (adView != null) {
                adView.pause();
            }
            super.onPause();
        }

        /** Called when returning to the activity */
        @Override
        public void onResume() {
            super.onResume();
            if (adView != null) {
                adView.resume();
            }
        }

        /** Called before the activity is destroyed */
        @Override
        public void onDestroy() {
            if (adView != null) {
                adView.destroy();
            }
            super.onDestroy();
        }

    }

}
