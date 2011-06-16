/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.willtate.willdeux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.format.Time;
import android.util.Log;

/**
 * Simple notes database access helper class. Defines the basic CRUD operations
 * for the notepad example, and gives the ability to list all notes as well as
 * retrieve or modify a specific note.
 * 
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 */
public class ItemDbAdapter {

    public static final String KEY_TITLE = "title";
    public static final String KEY_BODY = "body";
    public static final String KEY_ROWID = "_id";
    public static final String KEY_DATE = "date";
    public static final String KEY_EDIT = "edit";
    public static final String KEY_IMG = "img_source";
    public static final String KEY_REMINDER = "reminder";
    public static final String KEY_DELETION = "deletion";
    public static final String KEY_PRIORITY = "priority";
    
	public static final int PRIORITY_HIGH = 1;
	public static final int PRIORITY_NORMAL = 0;
	public static final int PRIORITY_LOW = -1;

    private static final String TAG = "WillDeux.Datebase";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
        "create table notes (_id integer primary key autoincrement, "
        + "title text not null, "
        + "body text not null, "
        + "date text not null, "
        + "edit text not null, "
        + "reminder text, "
        + "img_source text, "
        + "deletion, "
        + "priority)";
    
    private static final String DATABASE_UPGRADE =
    	"notes (_id integer primary key autoincrement, "
    	+ "title text not null, "
    	+ "body text not null, "
    	+ "date text not null, "
    	+ "edit text not null, "
    	+ "reminder text, "
    	+ "img_source text, "
    	+ "deletion, "
    	+ "priority)";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "notes";
    private static final int DATABASE_VERSION = 4;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
            
            db.beginTransaction();
            try {
            	db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_UPGRADE);
            	List<String> columns = GetColumns(db, DATABASE_TABLE);
            	db.execSQL("ALTER table " + DATABASE_TABLE + " RENAME TO 'temp_" + DATABASE_TABLE + "'");
            	db.execSQL("create table " + DATABASE_UPGRADE);
            	columns.retainAll(GetColumns(db, DATABASE_TABLE));
            	String cols = join(columns, ","); 
            	db.execSQL(String.format( "INSERT INTO %s (%s) SELECT %s from temp_%s", DATABASE_TABLE, cols, cols, DATABASE_TABLE));
            	db.execSQL("DROP table 'temp_" + DATABASE_TABLE + "'");
            	db.setTransactionSuccessful();
            } finally {
            	db.endTransaction();
            }
        }
    }
    
    public static List<String> GetColumns(SQLiteDatabase db, String tableName) {
        List<String> ar = null;
        Cursor c = null;
        try {
            c = db.rawQuery("select * from " + tableName + " limit 1", null);
            if (c != null) {
                ar = new ArrayList<String>(Arrays.asList(c.getColumnNames()));
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return ar;
    }

    public static String join(List<String> list, String delim) {
        StringBuilder buf = new StringBuilder();
        int num = list.size();
        for (int i = 0; i < num; i++) {
            if (i != 0)
                buf.append(delim);
            buf.append((String) list.get(i));
        }
        return buf.toString();
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public ItemDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public ItemDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new note using the title and body provided. If the note is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     * 
     * @param title the title of the note
     * @param body the body of the note
     * @return rowId or -1 if failed
     */
    public long createItem(String title, String body, String imgSource, String reminder) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TITLE, title);
        initialValues.put(KEY_BODY, body);
        Time currentTime = new Time();
        currentTime.setToNow();
        initialValues.put(KEY_DATE, currentTime.format("%m/%d/%Y %H:%M:%S"));
        initialValues.put(KEY_EDIT, currentTime.format("%m/%d/%Y %H:%M:%S"));
        initialValues.put(KEY_IMG, imgSource);
        initialValues.put(KEY_REMINDER, reminder);
        initialValues.put(KEY_DELETION, 0);
        initialValues.put(KEY_PRIORITY, PRIORITY_NORMAL);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Delete the note with the given rowId
     * 
     * @param rowId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteItem(long rowId) {
        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    /**
     * Delete all items currently marked for deletion
     * @author will
     * @return number of items deleted
     */
    
    public int deleteItems() {
    	return mDb.delete(DATABASE_TABLE, KEY_DELETION+"=1", null);
    }

    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAllItems(String orderBy) {
        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE,
                KEY_BODY, KEY_DATE, KEY_EDIT, KEY_IMG, KEY_REMINDER, KEY_DELETION, KEY_PRIORITY}, 
                null, null, null, null, orderBy);
    }

    /**
     * Return a Cursor positioned at the note that matches the given rowId
     * 
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchItem(long rowId) throws SQLException {

        Cursor cursor = mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_TITLE, KEY_BODY, KEY_DATE, KEY_EDIT, KEY_IMG, 
                    KEY_REMINDER, KEY_DELETION, KEY_PRIORITY},
                    KEY_ROWID + "=" + rowId, null,
                    null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;

    }

    /**
     * Update the note using the details provided. The note to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     * 
     * @param rowId id of note to update
     * @param title value to set note title to
     * @param body value to set note body to
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateItem(long rowId, String title, String body, String imgSource, String reminder) {
        ContentValues args = new ContentValues();
        args.put(KEY_TITLE, title);
        args.put(KEY_BODY, body);
        Time currentTime = new Time();
        currentTime.setToNow();
        args.put(KEY_EDIT, currentTime.format("%m/%d/%Y %H:%M:%S"));
        args.put(KEY_IMG, imgSource);
        args.put(KEY_REMINDER, reminder);
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    /**
     * Marks a particular item for deletion
     * @author will
     * @param rowId
     * @return true if successful, false if not
     */
    
    public int toggleItemDeletion(long rowId) {
    	/** Grab current deletion value for this item */
    	Cursor cursor = mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,  KEY_DELETION},
                KEY_ROWID + "=" + rowId, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
    	int currentVal = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_DELETION));
    	cursor.close();
    	if (currentVal == 0) {
    		currentVal = 1;
    	} else {
    		currentVal = 0;
    	}
    	/** Toggle the current value */
    	ContentValues args = new ContentValues();
    	args.put(KEY_DELETION, currentVal);
    	mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null);
    	return currentVal;
    }
    
    public boolean updateItemPriority(long rowId, int priority) {
    	ContentValues args = new ContentValues();
    	args.put(KEY_PRIORITY, priority);
    	return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
}
