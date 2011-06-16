package com.willtate.willdeux;

import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;

public class ItemDetails extends Activity {

    private ItemDbAdapter mDbHelper;
    
    private Long mRowId;
    
    private TextView mTitle;
    private TextView mDateCreated;
    private TextView mDateEdited;
    private TextView mImageAttached;
    private TextView mReminderAttached;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new ItemDbAdapter(this);
        mDbHelper.open();

        setContentView(R.layout.item_details);
        setTitle(R.string.details_title);
        
        mTitle = (TextView) findViewById(R.id.details_title);
        mDateCreated = (TextView) findViewById(R.id.date_created);
        mDateEdited = (TextView) findViewById(R.id.date_edited);
        mImageAttached = (TextView) findViewById(R.id.image_attached);
        mReminderAttached = (TextView) findViewById(R.id.reminder_attached);
        
        mRowId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(ItemDbAdapter.KEY_ROWID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(ItemDbAdapter.KEY_ROWID)
									: null;
		}
		
        populateFields();
	}

	private void populateFields() {
		if (mRowId != null) {
            Cursor note = mDbHelper.fetchItem(mRowId);
            startManagingCursor(note);
            mTitle.setText(note.getString(
            		note.getColumnIndexOrThrow(ItemDbAdapter.KEY_TITLE)));
            mDateCreated.setText(note.getString(
                    note.getColumnIndexOrThrow(ItemDbAdapter.KEY_DATE)));
            mDateEdited.setText(note.getString(
                    note.getColumnIndexOrThrow(ItemDbAdapter.KEY_EDIT)));
            String imagePath = note.getString(
            		note.getColumnIndexOrThrow(ItemDbAdapter.KEY_IMG));
            String reminderTime = note.getString(
            		note.getColumnIndexOrThrow(ItemDbAdapter.KEY_REMINDER));
            
            if(imagePath.length() == 0 || imagePath == null) {
            	mImageAttached.setText("None");
            } else {
            	mImageAttached.setText("Yes");
            }
            
            if(reminderTime.length() == 0 || reminderTime == null) {
            	mReminderAttached.setText("None");
            } else{
            	/* Decode the raw date string into something human readable */
            	Calendar reminderCal = Calendar.getInstance();
            	reminderCal.setTimeInMillis(Long.decode(reminderTime));
            	Date dateText = new Date(reminderCal.get(Calendar.YEAR)-1900,
            			reminderCal.get(Calendar.MONTH),
            			reminderCal.get(Calendar.DAY_OF_MONTH),
            			reminderCal.get(Calendar.HOUR_OF_DAY),
            			reminderCal.get(Calendar.MINUTE));
            	mReminderAttached.setText(
            			android.text.format.DateFormat.format("MM/dd/yyyy hh:mm:ss", dateText));
            }
        }
	}
	
    @Override
    protected void onResume() {
        super.onResume();
		populateFields();
    }

	
    @Override
    protected void onPause() {
        super.onPause();
    }
    
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mDbHelper != null) {
			mDbHelper.close();
		}
	}
}
