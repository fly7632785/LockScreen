package com.jafir.lockscreen;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by jafir on 16/11/16.
 */

public class WordsDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "/data/data/com.jafir.lockscreen/files/databases/words.db";
    private static final int DATABASE_VERSION = 1;

    /**
     * Create a helper object for the Events database
     */
    public WordsDbHelper(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}