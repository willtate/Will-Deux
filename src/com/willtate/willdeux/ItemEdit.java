/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.willtate.willdeux;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class ItemEdit extends Activity {
	public static final String TAG = "WillDeux";
	final CharSequence[] mImageDialogItemsWithSD = {"From Gallery", "From Camera"};
	final CharSequence[] mImageDialogItemsSansSD = {"From Gallery"};
	public static final int ADD_IMAGE_GALLERY = 0;
	public static final int ADD_IMAGE_CAMERA = 1;
	public static final int REMOVE_IMAGE = 2;
	final CharSequence[] reminderDialogItems = {"Add Reminder", "Remove Reminder"};
	public static final int ADD_REMINDER = 0;
	public static final int REMOVE_REMINDER = 1;
    /* Options for when user hit menu button */
    private static final int ATTACH_IMG_ID = Menu.FIRST;
    private static final int REMINDER_ID = Menu.FIRST + 1;
    /* Misc Constants */
    private static final int SELECT_IMAGE = 1;
    private static final int REMINDER_DATE_DIALOG = 2;
    private static final int REMINDER_TIME_DIALOG = 3;
    private static final int IMAGE_DIALOG = 4;
    private static final int REMINDER_DIALOG=5;
    private static final int TAKE_PICTURE = 6;
    private static final int GALLERY_DIALOG = 7;
    public static final int ALARM_ID = 99;
	/*Variables*/
    private Context mContext;
    private Long mRowId;
    private ItemDbAdapter mDbHelper;
    private String mTempImagePath;
    private String mImagePath;
    private String mRawDate;
    private File mImageFile;
    private Calendar mReminderCal;
    private boolean mNewReminderDate = false;
    private boolean mNewReminderTime = false;
    private int mGalleryItemToRemove;
    /*User Interface Objects*/
    private ImageView mReminderIcon;
    private TextView mReminderText;
    private EditText mTitleText;
    private EditText mBodyText;
    private Button mConfirmButton;
    private Gallery mGallery;
    /*items for date picker */
    private int mYear;
    private int mMonth;
    private int mDay;
    /*items for time picker */
    private int mHour;
    private int mMinute;
    /* Used for date picker */
    private DatePickerDialog.OnDateSetListener mDateSetListener =
    	new DatePickerDialog.OnDateSetListener() {
    		@Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mYear = year;
                mMonth = monthOfYear;
                mDay = dayOfMonth;
                mNewReminderDate=true;
            }
        };
    /* Used for time picker */
    private TimePickerDialog.OnTimeSetListener mTimeSetListener =
        new TimePickerDialog.OnTimeSetListener() {
    		@Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                mHour = hourOfDay;
                mMinute = minute;
                mNewReminderTime=true;
                mReminderCal = Calendar.getInstance();
                mReminderCal.set(mYear, mMonth, mDay, mHour, mMinute, 0);
                
                saveState();
				populateFields();
				if(mReminderCal.getTimeInMillis() > System.currentTimeMillis()) {
					controlAlarm(true);
				} else {
					Toast.makeText(mContext, "Reminder time is in the past, alarm NOT set.", Toast.LENGTH_SHORT).show();
				}
            }
        };
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        Bundle extras;
        mDbHelper = new ItemDbAdapter(this);
        mDbHelper.open();
        setContentView(R.layout.item_edit);
        setTitle(R.string.edit_item);
        viewObjectInit();
        //Extract any extras that came in
		if (savedInstanceState == null) {
			extras = getIntent().getExtras();
			if(extras == null) {
				mRowId = null;
			} else {
				mRowId = extras.getLong(ItemDbAdapter.KEY_ROWID);
			}
		} else {
			mRowId = (Long) savedInstanceState.getSerializable(ItemDbAdapter.KEY_ROWID);
			mTempImagePath = (String) savedInstanceState.getSerializable(ItemDbAdapter.KEY_IMG);
		}
		//Clear notification if we came from one
		if(mRowId != null) {
			NotificationManager NM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
			NM.cancel(mRowId.intValue());
		}
	    
		populateFields();
		setupButtonListeners();
    }
    
    /**
     * Initialize the listeners for any clickable UI objects
     * @author will
     */
    
    private void setupButtonListeners() {
    	mConfirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setResult(RESULT_OK);
                finish();
            }
        });
        
	    mGallery.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	            launchImageViewer(position);
	        }
	    });
	    
	    mGallery.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
				//Toast.makeText(mContext, "item position"+position, Toast.LENGTH_SHORT).show();
				mGalleryItemToRemove=position;
				showDialog(GALLERY_DIALOG);
				return false;
			}
	    });
	    
	    mBodyText.setOnFocusChangeListener(new OnFocusChangeListener() {
	    	@Override
	    	public void onFocusChange(View v, boolean hasFocus) {
	    		;
	    	}
	    });
    }
    
    /**
     * Initialize all of the UI objects
     * @author will
     */
    
    private void viewObjectInit() {
    	mTitleText = (EditText) findViewById(R.id.title);
        mBodyText = (EditText) findViewById(R.id.body);
        mReminderIcon = (ImageView) findViewById(R.id.reminder);
        mReminderIcon.setVisibility(ImageView.GONE);
        mReminderText = (TextView) findViewById(R.id.item_reminder);
        mReminderText.setVisibility(TextView.GONE);
        mConfirmButton = (Button) findViewById(R.id.confirm);
        mGallery = (Gallery) findViewById(R.id.gallery);
    }
    
    /**
     * @author 	will
     * @throws java.text.ParseException 
     */
    
    private void populateFields() {
        if (mRowId != null) {
            Cursor item = mDbHelper.fetchItem(mRowId);
            startManagingCursor(item);
            mTitleText.setText(item.getString(item.getColumnIndexOrThrow(ItemDbAdapter.KEY_TITLE)));
            mBodyText.setText(item.getString(item.getColumnIndexOrThrow(ItemDbAdapter.KEY_BODY)));
            mImagePath = item.getString(item.getColumnIndexOrThrow(ItemDbAdapter.KEY_IMG));
            mRawDate = item.getString(item.getColumnIndexOrThrow(ItemDbAdapter.KEY_REMINDER));
                        
            if(mRawDate.length() != 0 && mRawDate != null) {
            	parseDateString();
            } else {
            	mReminderIcon.setVisibility(ImageView.GONE);
            	mReminderText.setVisibility(TextView.GONE);
            	mReminderText.setText("");
            }

            GalleryAdapter gallery = new GalleryAdapter(this, mRowId);
            mGallery.setAdapter(gallery);
            gallery = null;
        }
    }
    /**
     * Enable/Disable an AlarmService reminder for this item set for 
     * the time given by the user.
     * @author will
     */
    private void controlAlarm(boolean toggle) {
    	Intent i = new Intent(this, AlarmReceiver.class);
    	i.putExtra(ItemDbAdapter.KEY_ROWID, mRowId);
    	i.setAction("Item: " + mRowId);
    	PendingIntent sender = PendingIntent.getBroadcast(this, 0, i, 0);
    	AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
    	if(toggle) {
    		am.set(AlarmManager.RTC_WAKEUP, mReminderCal.getTimeInMillis(), sender);
    	} else {
    		am.cancel(sender);
    	}
    }
    /**
     * Parse the reminder string
     * 
     * @author will
     * @throws java.text.ParseException
     */
    private void parseDateString() {
    	Long reminderMillis = Long.decode(mRawDate);
    	mReminderCal = Calendar.getInstance();
    	mReminderCal.setTimeInMillis(reminderMillis);
    	Date dateText = new Date(mReminderCal.get(Calendar.YEAR)-1900,
    			mReminderCal.get(Calendar.MONTH),
    			mReminderCal.get(Calendar.DAY_OF_MONTH),
    			mReminderCal.get(Calendar.HOUR_OF_DAY),
    			mReminderCal.get(Calendar.MINUTE));
    	mReminderText.setText(android.text.format.DateFormat.format("MM/dd/yyyy hh:mm", dateText));
    	mReminderText.setVisibility(TextView.VISIBLE);
    	mReminderIcon.setVisibility(TextView.VISIBLE);
    }
    
    /**
     * 	Determine which item in the gallery the user has selected and display the item
     * 	for the user in ImageViewer
     * 
     * @author 	will
     * 
     * @param	position
     * 
     * 			The position of the item in the gallery that has been clicked by the user
     */

    public void launchImageViewer(int position) {
    	String imagePaths, pathArray[];
    	Cursor item = mDbHelper.fetchItem(mRowId);
    	/*Grab string of image pathes from database*/
    	startManagingCursor(item);
		imagePaths = item.getString(item.getColumnIndexOrThrow(ItemDbAdapter.KEY_IMG));
		/*split the string into an array of paths*/
		pathArray = imagePaths.split(",");
		/*Create a file out of the image the user clicked on*/
    	File file = new File(pathArray[position]);
    	/*create ACTION_VIEW intent to view the image*/
    	Intent i = new Intent();
    	i.setAction(android.content.Intent.ACTION_VIEW);
    	i.setDataAndType(Uri.fromFile(file), "image/*");
    	startActivity(i);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ATTACH_IMG_ID, 0, R.string.attach_img).setIcon(android.R.drawable.ic_menu_gallery);
        menu.add(0, REMINDER_ID, 0, R.string.reminder).setIcon(android.R.drawable.ic_popup_reminder);
        return true;
    }
    
    /**
     * @author	will
     * 
     * @param	featureId
     * 
     * 			Not entirely sure, used to send back to the super.onMenuItemSelected()
     * 
     * @param	item
     * 
     * 			The item selected from the UI
     */
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case ATTACH_IMG_ID:
            	showDialog(IMAGE_DIALOG);
            	break;
            case REMINDER_ID:
            	showDialog(REMINDER_DIALOG);
            	break;
        }
        return super.onMenuItemSelected(featureId, item);
    }
    
    /**
     * @author	will
     * 
     * @param 	requestCode
     * 			
     *			Code associated with the returning activity, helps us know which
     *			activity has returned.
     *
     * @param	resultCode
     * 
     * 			Code returned by the activity indicating the status.
     * 
     * @param	data
     * 
     * 			The data returned by the activity
     */
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
        	switch(requestCode) {
        	case SELECT_IMAGE:
        		if(mImagePath == null || mImagePath.length() == 0) {
        			mImagePath = getPath(data.getData());
        		} else {
        			mImagePath = mImagePath + "," + getPath(data.getData());
        		}
        		saveState();
				populateFields();
        		break;
        	case TAKE_PICTURE:
        		if(mImagePath == null || mImagePath.length() == 0) {
        			mImagePath = mTempImagePath;
        		} else {
        			mImagePath = mImagePath + "," + mTempImagePath;
        		}
           		saveState();
        		populateFields();
        		break;
        	}
        }
    }
    
    /**
     * @author	will
     * 
     * @param	uri
     * 
     * 			URI from which to extract the path of an image file.
     * 
     * @return	String
     * 
     * 			Path name to the user selected image.
     */
    
    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        startManagingCursor(cursor);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState()");
        outState.putSerializable(ItemDbAdapter.KEY_ROWID, mRowId);
        outState.putSerializable(ItemDbAdapter.KEY_IMG, mTempImagePath);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }

    @Override
    protected void onResume() {
        super.onResume();
		populateFields();
    }

    private void saveState() {
        String title = mTitleText.getText().toString();
        String body = mBodyText.getText().toString();
        String imgPath;
        String reminder;
        if(mNewReminderDate && mNewReminderTime) {
        	Calendar reminderCal = Calendar.getInstance();
            reminderCal.set(mYear, mMonth, mDay, mHour, mMinute, 0);
            Long reminderMillis = reminderCal.getTimeInMillis();
        	reminder = reminderMillis.toString();
        } else if (mReminderCal != null){
        	reminder = Long.toString(mReminderCal.getTimeInMillis());
        } else {
        	reminder = "";
        }
        /*Validate mImagePath*/
        if(mImagePath != null){
        	imgPath = mImagePath;
        } else {
        	imgPath = "";
        }
        /* If the user hasn't modified the note but is leaving, save this as a draft */
        if ((title == "" || title.length() == 0)) {
        	Toast.makeText(this, "Item saved as draft.", Toast.LENGTH_SHORT).show();
        	title="Draft";
        }
        /* Item wasn't previously in the database, create a new note */
        if (mRowId == null) {
            long id = mDbHelper.createItem(title, body, imgPath, reminder);
            if (id > 0) {
                mRowId = id;
            }
        } else { /* Item already existed, edit the current database entry */
            mDbHelper.updateItem(mRowId, title, body, imgPath, reminder);
        }
    }

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	/**
	 * @author will
	 * 
	 * @note This should display the date picker on top of the time picker!
	 * @return Dialog	The dialog to choose a reminder date/time
	 */
	public Dialog reminderDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Add or Remove Reminder?");
		builder.setItems(reminderDialogItems, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		        if(item == ADD_REMINDER) {
		    		/* Set current date */
		    		final Calendar c = Calendar.getInstance();
		            mYear = c.get(Calendar.YEAR);
		            mMonth = c.get(Calendar.MONTH);
		            mDay = c.get(Calendar.DAY_OF_MONTH);
		            mHour = c.get(Calendar.HOUR_OF_DAY);
		            mMinute = c.get(Calendar.MINUTE);
		            /* Get reminder time */
		            showDialog(REMINDER_TIME_DIALOG);
		            /* Get reminder date */
		            showDialog(REMINDER_DATE_DIALOG);
		        } else if (item == REMOVE_REMINDER) {
		        	controlAlarm(false);
	            	mReminderCal = null;
	            	saveState();
	            	populateFields();
		        } else {
		        	return;
		        }
		    }
		});
		AlertDialog alert = builder.create();
		return alert;
	}
	
	
	/**
	 * @author will
	 * @return Dialog	The dialog from which the user chooses to add or remove an image.
	 */
	
	public Dialog imageDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		CharSequence[] dialogItems;
		builder.setTitle(R.string.image_options);
		
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
	    	dialogItems = mImageDialogItemsSansSD;
	    } else {
	    	dialogItems = mImageDialogItemsWithSD;
	    }
		builder.setItems(dialogItems, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		        switch(item) {
		        case ADD_IMAGE_GALLERY:
		        	imageFromGallery();
	            	break;
		        case ADD_IMAGE_CAMERA:
		        	imageFromCamera();
		        	break;
		        default:
		        	break;
		        }
		    }
		});
		AlertDialog alert = builder.create();
		return alert;
	}
	
	/**
	 * @author will
	 * @return AlertDialog
	 */
	
	public Dialog galleryRemoveDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Remove this image from gallery?")
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   galleryRemoveItem();
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
		return alert;
	}
	
	public void galleryRemoveItem() {
		String imgPaths, pathArray[], newPaths = "";
   		Cursor item = mDbHelper.fetchItem(mRowId);
   		startManagingCursor(item);
   		imgPaths = item.getString(item.getColumnIndexOrThrow(ItemDbAdapter.KEY_IMG));
   		pathArray = imgPaths.split(",");
   		for(int i = 0; i < pathArray.length; i++) {
   			if(i == mGalleryItemToRemove) {
   				continue;
   			} else {
   				if(newPaths == "") {
   					newPaths = newPaths + pathArray[i];
   				} else {
   					newPaths = newPaths + "," + pathArray[i];
   				}
   			}
   		}
   		mImagePath = newPaths;
   		saveState();
   		populateFields();
        mGallery.invalidate();
	}
	
	/**
	 * @author will
	 * Launch gallery to select an image
	 */
	
	public void imageFromCamera() {
		saveState();
	    Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
	    if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
	    	Log.d(TAG, "No SDCARD");
	    } else {
	    	mImageFile = new File(Environment.getExternalStorageDirectory()+File.separator+"WillDeux",  
	    		"PIC"+System.currentTimeMillis()+".jpg");
	    	mTempImagePath = mImageFile.getAbsolutePath();
	    	intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mImageFile));
	    	startActivityForResult(intent, TAKE_PICTURE);
	    }
	}
	
	/**
	 * @author will
	 * Launch camera to take picture
	 */
	
	public void imageFromGallery() {
		saveState();
		Intent getImageFromGalleryIntent = 
  		  new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
  		startActivityForResult(getImageFromGalleryIntent, SELECT_IMAGE);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
	    switch (id) {
	    case REMINDER_DATE_DIALOG:
	        return new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay);
	    case REMINDER_TIME_DIALOG:
	    	return new TimePickerDialog(this, mTimeSetListener, mHour, mMinute, false);
	    case IMAGE_DIALOG:
	    	return imageDialog();
	    case REMINDER_DIALOG:
	    	return reminderDialog();
	    case GALLERY_DIALOG:
	    	return galleryRemoveDialog();
	    }
	    return null;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mDbHelper != null) {
			mDbHelper.close();
		}
	}
}
