package com.mpaun.game;

import java.util.List;

import android.content.Context;

public class HighScoreTable {
    private DataHelper mDBsql;
    private HighScoreEntry[] mHighScoreArray;
    public static final int HIGHSCORE_NUM_ENTRIES    = 5;
    private static final int HIGHSCORE_DEFAULT_SCORE = 10;
    
    public HighScoreTable(Context context) {
    	mHighScoreArray = new HighScoreEntry[HIGHSCORE_NUM_ENTRIES];
 	    mDBsql = new DataHelper(context);
 	    List<HighScoreEntry> hsList = mDBsql.selectAll();

 	    if (hsList.size() < HIGHSCORE_NUM_ENTRIES) {
 	        // create default empty high scores
 	        // starting at index 0 = highest score
 	        mDBsql.deleteAll();
 	        for (int i=0; i<HIGHSCORE_NUM_ENTRIES; i++) {
 	            mHighScoreArray[i]=new HighScoreEntry("Anonymous", (HIGHSCORE_NUM_ENTRIES-i)*HIGHSCORE_DEFAULT_SCORE);
                mDBsql.insertData(mHighScoreArray[i]);
 	        }
 	    } else {
 	        //Load High scores
 	        for (int i=0; i < HIGHSCORE_NUM_ENTRIES; i++) {
 	            mHighScoreArray[i]=hsList.get(i);
 	        }
 	    }
    }
    
    public void addNewHighScore(String name, int score) {
    	HighScoreEntry nHighScore = new HighScoreEntry(name, score);
		mDBsql.insertData(nHighScore);
    }
    
    public int getLowestHighScore() {
    	return mHighScoreArray[HIGHSCORE_NUM_ENTRIES-1].score.intValue();
    }
    
    public HighScoreEntry[] getHighScores() {
    	return mHighScoreArray;
    }
    
    public void closeDB() {
    	mDBsql.closeDB();
    }
}