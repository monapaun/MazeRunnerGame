package com.mpaun.game;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

//  customized by Alexander Biemann for high-score table implementation
//  credit for original code:
//  http://www.screaming-penguin.com/node/7742

//  SQLlite references:
//  http://www.sqlite.org/docs.html
//  http://www.sqlite.org/lang.html
//  http://www.sqlite.org/datatype3.html

//  Tutorial for working with SQL Databases on Android
//  http://www.brighthub.com/mobile/google-android/articles/25881.aspx

//  Recommended app to edit SQLlite database
//  http://sqlitestudio.one.pl/index.rvt

public class DataHelper {
	private static final String DATABASE_NAME = "gamehighscores.db";
	private static final int DATABASE_VERSION = 1;
	private static final String TABLE_NAME_HIGHSCORES = "highscores";
	private final Context context;
	private final SQLiteDatabase db;
	private SQLiteStatement insertStmt;
	private static final String INSERT_DATA = "insert into " + TABLE_NAME_HIGHSCORES + "(name, hiscore) values (?, ?)";
	private boolean isDBOpen;

	public DataHelper(Context context) {
		this.context = context;
		final OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();
		this.isDBOpen = true;
		this.insertStmt = this.db.compileStatement(INSERT_DATA);
	}

	public long insertData(String name, Integer hiscore) {
		this.insertStmt.bindString(1, name);
		this.insertStmt.bindLong(2, hiscore);
		return this.insertStmt.executeInsert();
	}
	
	public long insertData(HighScoreEntry hsEntry) {
	    this.insertStmt.bindString(1, hsEntry.name);
	    this.insertStmt.bindLong(2, hsEntry.score);
	    return this.insertStmt.executeInsert();
	}
	
	public void deleteAll() {
		this.db.delete(TABLE_NAME_HIGHSCORES, null, null);
	}

	// Query database to get high-score list
	// 'DESC' parameter used to sort the list from high to low (descending)
	public List<HighScoreEntry> selectAll() {
		final List<HighScoreEntry> list = new ArrayList<HighScoreEntry>();
		
		if (!isDBOpen) 
			return list;
		final Cursor cursor = this.db.query(TABLE_NAME_HIGHSCORES, 
				new String[] { "name","hiscore" }, null, null, null, null, "hiscore DESC");
		
		//loop through result set if data exists
		if (cursor.moveToFirst()) {
			do {
			    HighScoreEntry hsEntry = new HighScoreEntry(cursor.getString(0), cursor.getInt(1));
				list.add(hsEntry);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return list;
	}
	
	public void closeDB() {
		isDBOpen = false;
		db.close();
	}

	private static class OpenHelper extends SQLiteOpenHelper {
		OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		// create a table with two columns (excluding the id key): "name" and "hiscore" 
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_NAME_HIGHSCORES + " (id INTEGER PRIMARY KEY, name TEXT, hiscore INTEGER)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w("Example","Upgrading database, this will drop tables and recreate.");
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_HIGHSCORES);
			onCreate(db);
		}
	}
}