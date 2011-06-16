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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class WillDeux extends ListActivity implements SensorEventListener {
	
	private static final String TAG = "WillDeux";
	
    /* Menu for long click on an item */
    private static final int DELETE_ID = Menu.FIRST;
    private static final int DETAILS_ID = Menu.FIRST + 1;
    private static final int PRIORITY_ID = Menu.FIRST + 2;
    
    /* Menu for menu button */
    private static final int INSERT_ID = Menu.FIRST + 100;
    
    private static final int ORDER_ID = Menu.FIRST + 200;   
    private static final int CREATE_ASC = Menu.FIRST + 201;
    private static final int CREATE_DESC = Menu.FIRST + 202;
    private static final int EDIT_ASC = Menu.FIRST + 203;
    private static final int EDIT_DESC = Menu.FIRST + 204;
    private static final int TITLE_ASC = Menu.FIRST + 205;
    private static final int TITLE_DESC = Menu.FIRST + 206;
    private static final int PRI_ASC = Menu.FIRST + 207;
    private static final int PRI_DESC = Menu.FIRST + 208;
    
    private static final int SETTINGS_ID = Menu.FIRST + 300;
    
    private static final String ASCENDING = " ASC";
    private static final String DESCENDING = " DESC";
    private static final String ORDER_PREF = "order";
    private String mOrder;

    private ItemDbAdapter mDbHelper = null;
    
    /** Priority Crap **/
    
    private static final String PRIORITY_HIGH = "High";
    private static final String PRIORITY_NORM = "Normal";
    private static final String PRIORITY_LOW = "Low";
    private Long mPriorityItem;
    
    /** Sensor crap **/
    
    private SensorManager mSensorManager;
    private long lastUpdate = -1;
    private float x, y, z;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 800;
    private boolean sensorSupport;
    private boolean sensorOption;
    
    ImageView mAddButton;
    private Long mRowForDeletion = null;
    
    /** Preference **/
    
    private SharedPreferences mPrefs;

    /** 
     * Called when the activity is first created. 
     * @author will
     */
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.items_list);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        /* Sensor initialization */
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        /* create directory for camera images */
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
	    	Log.d(TAG, "No SDCARD");
	    } else {
	    	File directory = new File(Environment.getExternalStorageDirectory()+File.separator+TAG);
	    	directory.mkdirs();
	    }
        /* Start Database Stuff */
        openDatabase();
        /*Create add button */
        createAddButton();
        /*Fill List*/
        fillData();
        registerForContextMenu(getListView());
    }
    
    private void openDatabase() {	
    	if(mDbHelper == null) {
    		mDbHelper = new ItemDbAdapter(this);
        	mDbHelper.open();
    	}
    }
    
    /**
     * Create add item button in top right
     * @author will
     */
    
    private void createAddButton() {
    	mAddButton = (ImageView) findViewById(R.id.action_bar_add);
        mAddButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		createItem();
        	}
        });
    }

    /**
     * Redraw the list
     * @author will
     */
    
    private void fillData() {
    	mOrder = mPrefs.getString(ORDER_PREF, null);
        Cursor itemsCursor = mDbHelper.fetchAllItems(mOrder);
        startManagingCursor(itemsCursor);
        
        ItemAdapter itemAdapter = new ItemAdapter(this, itemsCursor);
        setListAdapter(itemAdapter);
        itemAdapter = null;
    }

    /**
     * Build the "menu button" menu
     * @author will
     */
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        SubMenu sub;
        menu.add(0, INSERT_ID, 0, R.string.menu_insert).setIcon(android.R.drawable.ic_menu_add);
        sub = menu.addSubMenu(0, ORDER_ID, 0, R.string.order).setIcon(android.R.drawable.ic_menu_sort_by_size);
        /**
         * SubMenu for ORDER_ID
         */
        sub.add(0, TITLE_ASC, 0, R.string.title_ascending);
        sub.add(0, TITLE_DESC, 0, R.string.title_descending);
        sub.add(0, CREATE_ASC, 0, R.string.create_ascending);
        sub.add(0, CREATE_DESC, 0, R.string.create_descending);
        sub.add(0, EDIT_ASC, 0, R.string.edit_ascending);
        sub.add(0, EDIT_DESC, 0, R.string.edit_descending);
        sub.add(0, PRI_ASC, 0, R.string.priority_ascending);
        sub.add(0, PRI_DESC, 0, R.string.priority_descending);
        
        menu.add(0, SETTINGS_ID, 0, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_manage);
        
        return true;
    }

    /**
     * Item from the "menu button" menu was clicked
     * @author will
     */
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	SharedPreferences.Editor editor = mPrefs.edit();
        switch(item.getItemId()) {
            case INSERT_ID:
                createItem();
                return true;
            case TITLE_ASC:
            	editor.putString(ORDER_PREF, ItemDbAdapter.KEY_TITLE + ASCENDING);
            	break;
            case TITLE_DESC:
            	editor.putString(ORDER_PREF, ItemDbAdapter.KEY_TITLE + DESCENDING);
            	break;
            case CREATE_ASC:
            	editor.putString(ORDER_PREF, ItemDbAdapter.KEY_DATE + ASCENDING);
            	break;
            case CREATE_DESC:
            	editor.putString(ORDER_PREF, ItemDbAdapter.KEY_DATE + DESCENDING);
            	break;
            case EDIT_ASC:
            	editor.putString(ORDER_PREF, ItemDbAdapter.KEY_EDIT + ASCENDING);
            	break;
            case EDIT_DESC:
            	editor.putString(ORDER_PREF, ItemDbAdapter.KEY_EDIT + DESCENDING);
            	break;
            case PRI_ASC:
            	editor.putString(ORDER_PREF, ItemDbAdapter.KEY_PRIORITY + ASCENDING);
            	break;
            case PRI_DESC:
            	editor.putString(ORDER_PREF, ItemDbAdapter.KEY_PRIORITY + DESCENDING);
            	break;
            case SETTINGS_ID:
            	launchSettings();
            	return true;
            default:
            	return super.onMenuItemSelected(featureId, item);
        }
        editor.commit();
        fillData();
        return true;
    }

    /**
     * Build the list item "long click" menu
     * @author will
     */
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
    	menu.add(0, DELETE_ID, 0, (sensorSupport&&sensorOption)?R.string.menu_delete_sensor:R.string.menu_delete_nosensor);
        menu.add(0, DETAILS_ID, 0, R.string.menu_details);
        menu.add(0, PRIORITY_ID, 0, R.string.menu_priority);
    }

    /**
     * List item has been "long clicked"
     * @author will
     * @param item Value indicating which item in the list has been clicked
     */
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
            case DELETE_ID:
            	if(sensorSupport && sensorOption) {
            		int status = mDbHelper.toggleItemDeletion(info.id);
            		fillData();
            		if (status == 1) {
            			Toast.makeText(this, R.string.shake, Toast.LENGTH_SHORT).show();
            		}
            	} else {
            		mRowForDeletion = info.id;
            		showDialog(DELETE_ID);
            	}
                return true;
            case DETAILS_ID:
            	Intent i = new Intent(this, ItemDetails.class);
            	i.putExtra(ItemDbAdapter.KEY_ROWID, info.id);
            	startActivity(i);
            	return true;
            case PRIORITY_ID:
            	mPriorityItem = info.id;
            	showDialog(PRIORITY_ID);
        }
        return super.onContextItemSelected(item);
    }
    
    /**
     * Launch Settings Activity
     * @author will
     */
    
    private void launchSettings() {
    	startActivity(new Intent(this, Settings.class));
    }

    /**
     * Launch Create Item Activity
     * @author will
     */
    
    private void createItem() {
        startActivity(new Intent(this, ItemEdit.class));
    }

    /**
     * Item in the list has been clicked
     * @author will
     */
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, ItemEdit.class);
        i.putExtra(ItemDbAdapter.KEY_ROWID, id);
        startActivity(i);
    }    

    /**
     * An Activity has returned
     * @author will
     */
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        fillData();
    }

    /**
     * @author will
     */
    
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
	}

	/**
	 * Sensors have changed
	 * @param event Structure containing information about the sensors
	 * @author will
	 */
	
	public void onSensorChanged(SensorEvent event) {
		synchronized (this) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
		    long curTime = System.currentTimeMillis();
		    // only allow one update every 100ms.
		    if ((curTime - lastUpdate) > 100) {
			long diffTime = (curTime - lastUpdate);
			lastUpdate = curTime;
	 
			x = event.values[SensorManager.DATA_X];
			y = event.values[SensorManager.DATA_Y];
			z = event.values[SensorManager.DATA_Z];
	 
			/** proper vector addition */
			double speed = Math.sqrt(
					Math.pow(x-last_x, 2) + 
					Math.pow(y-last_y, 2) + 
					Math.pow(z-last_z, 2)) 
					/ diffTime * 10000;
			if (speed > SHAKE_THRESHOLD) {
				mDbHelper.deleteItems();
				fillData();
			}
			last_x = x;
			last_y = y;
			last_z = z;
		    }
		}
		}
	}
	
	/**
	 * Create the delete confirmation Dialog.  This is only used for those without sensor Support, or
	 * those who disable sensor support in preferences.
	 * @return Dialog to display
	 */
	
	private Dialog deleteConfirmDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Really Delete Item?")
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   if(mRowForDeletion != null) {
		        		   mDbHelper.deleteItem(mRowForDeletion);
		        		   mRowForDeletion=null;
		        		   fillData();
		        	   } else {
		        		   Toast.makeText(WillDeux.this, "WTF!? That Item Doesnt Even Exist!", Toast.LENGTH_LONG).show();
		        		   dialog.cancel();
		        	   }
		        	   
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		return builder.create();
	}
	
	/**
	 * Build the priority dialog allowing user to select High, Normal and  Low priorities
	 * @author will
	 * @return Dialog to display
	 */
	
	private Dialog priorityDialog() {
		final CharSequence[] items = {PRIORITY_HIGH, PRIORITY_NORM, PRIORITY_LOW};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Set Priority");
		builder.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	int priority_val = ItemDbAdapter.PRIORITY_NORMAL;
		        if(items[item] == PRIORITY_HIGH) {
		        	priority_val = ItemDbAdapter.PRIORITY_HIGH;
		        } else if (items[item] == PRIORITY_LOW) {
		        	priority_val = ItemDbAdapter.PRIORITY_LOW;
		        }
		        mDbHelper.updateItemPriority(mPriorityItem, priority_val);
		        mPriorityItem = null;
		        fillData();
		    }
		});
		return builder.create();
	}
	
	/**
	 * Display a dialog
	 * @author will
	 * @param id Value indicating the dialog to display
	 */
	
	protected Dialog onCreateDialog(int id) {
	    Dialog dialog;
	    switch(id) {
	    case PRIORITY_ID:
	        dialog = priorityDialog();
	        break;
	    case DELETE_ID:
	    	dialog = deleteConfirmDialog();
	    	break;
	    default:
	        dialog = null;
	    }
	    return dialog;
	}

	/**
	 * Activity has resumed
	 * @author will
	 */
	
	@Override
	protected void onResume() {
		boolean shakeSupport;
		super.onResume();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        sensorOption = prefs.getBoolean("sensorOption", true);
		if(sensorOption) {
			shakeSupport = mSensorManager.registerListener(this,
					mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_FASTEST);
			sensorSupport=true;
			if (!shakeSupport) {
				sensorSupport = false;
				mSensorManager.unregisterListener(this);
			}
		}
	}

	/**
	 * Activity has stopped
	 * @author will
	 */
	
	@Override
	protected void onStop() {
		super.onStop();
		if(sensorSupport) {
			mSensorManager.unregisterListener(this);
		}
	}
	
	@Override
	protected void onDestroy () {
		super.onDestroy();
		if(mDbHelper != null) {
			mDbHelper.close();
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
}
