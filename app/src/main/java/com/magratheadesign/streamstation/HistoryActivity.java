package com.magratheadesign.streamstation;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by ejntoo on 13/10/14.
 * Download history
 */
public class HistoryActivity extends ActionBarActivity {

    ArrayList<String> historyList = new ArrayList<String>();
    ArrayList<String> historyIndexes = new ArrayList<String>();
    DBHelper dbHelper;
    SQLiteDatabase db;
    ListView historyListView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        setTitle(R.string.history_title);

        dbHelper = new DBHelper(this);

        historyListView = (ListView) findViewById(R.id.history_list_view);
        updateList();
        registerForContextMenu(historyListView);

        findViewById(R.id.history_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dbHelper.getWritableDatabase().execSQL("DELETE FROM history");
                finish();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (db != null && db.isOpen()) {
            db.close();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_contextual, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int id = info.position;
        db = dbHelper.getWritableDatabase();
        switch (item.getItemId()) {
            case R.id.history_contextual_delete:
                db.delete("history", "_id=?", new String[]{historyIndexes.get(id)});
                updateList();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public void updateList() {
        db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query("history", new String[]{"_id", "name"}, null, null, null, null, null);
        historyList.clear();
        historyIndexes.clear();

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            historyIndexes.add(cursor.getString(0));
            historyList.add(cursor.getString(1));
            cursor.moveToNext();
        }
        historyListView.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                historyList));

        cursor.close();
        db.close();
    }
}
