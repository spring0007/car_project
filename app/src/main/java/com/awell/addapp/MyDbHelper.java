package com.awell.addapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class MyDbHelper extends SQLiteOpenHelper {

    private String sqlContact = "create table showapp(_id integer primary key autoincrement," +
            "packagename text)";
    private String sqlContact2 = "create table showapp2(_id integer primary key autoincrement," +
            "packagename text)";

    /* private String sqlCallHistory =  "create table callhistory(_id integer primary key autoincrement," +
             "name text,number text,type integer)";
 */
    public MyDbHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        Log.d("MyDbHelper", "public MyDbHelper(");

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(sqlContact);
        db.execSQL(sqlContact2);
        Log.d("MyDbHelper", "onCreate");
//        db.execSQL(sqlCallHistory);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("MyDbHelper", "onUpgrade");
//        db.execSQL("drop table if exists showapp");
//        db.execSQL("drop table if exists Category");
//        onCreate(db);
    }
}
