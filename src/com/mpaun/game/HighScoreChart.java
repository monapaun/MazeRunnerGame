package com.mpaun.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class HighScoreChart extends Activity {
		private HighScoreTable hsTable;
		private HighScoreEntry[] hsEntries;

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			LinearLayout layout = new LinearLayout(this);
			layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			setContentView(layout);
			
			hsTable = new HighScoreTable(this);
			hsEntries = hsTable.getHighScores();
			hsTable.closeDB();
			
			TableLayout table = new TableLayout(this);
			layout.addView(table);
			
			for (int i=0; i < HighScoreTable.HIGHSCORE_NUM_ENTRIES; i++) {
				TableRow row = new TableRow(this);
				
				TextView t = new TextView(this);
				t.setText(String.valueOf(i+1));
				t.setPadding(10, 10, 10, 10);
				row.addView(t);
				
				t = new TextView(this);
				t.setText(hsEntries[i].name);
				t.setPadding(10, 10, 10, 10);
				row.addView(t);
				
				t = new TextView(this);
				t.setText(String.valueOf(hsEntries[i].score));
				t.setPadding(10, 10, 10, 10);
				row.addView(t);			
				table.addView(row);
			}
		}
		
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			finish();
			return true;
		}
}