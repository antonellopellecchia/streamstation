package com.magratheadesign.streamstation;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by ejntoo on 13/10/14.
 * Helps you creating the database
 */
public class DBHelper extends SQLiteOpenHelper {

    private final static String DB_NAME = "StreamStationDB";
    private static final int DB_VERSION = 1;
    private static final String DICTIONARY_TABLE_CREATE = "CREATE TABLE history (" +
                    "_id INTEGER PRIMARY KEY NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "url TEXT);";

    DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

    }
}