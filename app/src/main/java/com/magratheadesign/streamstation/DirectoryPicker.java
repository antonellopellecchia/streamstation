package com.magratheadesign.streamstation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by ejntoo on 13/10/14.
 * Preference to choose file download directory
 */
public class DirectoryPicker extends Activity {

    String currentDir;
    String helpDir; // to use in case of NullPointerException
    String[] directoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filepicker_layout);

        Intent intent = getIntent();
        currentDir = intent.getStringExtra("currentDir");

        ListView fileListView = (ListView) findViewById(R.id.filepicker_view);
        directoryList = getDirectoryList(currentDir);
        fileListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, directoryList));
        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parentView, View view, int i, long l) {
                helpDir = currentDir;
                try {
                    if (directoryList[i].equals("(Parent directory)")) {
                        currentDir = new File(currentDir).getParent();
                    } else {
                        currentDir = currentDir + File.separator + directoryList[i];
                    }
                    directoryList = getDirectoryList(currentDir);
                } catch (NullPointerException e) {
                    Toast.makeText(DirectoryPicker.this, R.string.filepicker_nullpointer_toast, Toast.LENGTH_SHORT).show();
                    currentDir = helpDir;
                    directoryList = getDirectoryList(currentDir);
                    return;
                }
                ((ListView)parentView).setAdapter(new ArrayAdapter<String>(DirectoryPicker.this, android.R.layout.simple_list_item_1, directoryList));
            }
        });

        findViewById(R.id.filepicker_yes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create intent to deliver some kind of result data
                Intent result = new Intent();
                result.putExtra("resultDir", currentDir);
                setResult(RESULT_OK, result);
                finish();
            }
        });

        findViewById(R.id.filepicker_no).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Intent result = new Intent("com.magratheadesign.streamstation.RESULT_ACTION", Uri.parse("content://result_uri");
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    public String[] getDirectoryList(String directory) {
        ArrayList<String> list = new ArrayList<String>();
        File parent = new File(directory);
        File[] fileList = parent.listFiles();
        list.add("(Parent directory)");
        for (File f:fileList) {
            if (f.isDirectory()) list.add(f.getName());
        }
        return list.toArray(new String[list.size()]);
    }
}
