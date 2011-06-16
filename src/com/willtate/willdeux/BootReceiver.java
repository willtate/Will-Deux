package com.willtate.willdeux;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

/**
 * This class is used to register all alarms/reminders for items
 * that had them as the AlarmManager doesn't retain this after a
 * reboot.
 * 
 * @author will
 *
 */

public class BootReceiver extends BroadcastReceiver {

	private Context mContext;
	private ItemDbAdapter mDbHelper;
	
	@Override
	public void onReceive(Context c, Intent i) {
		mContext = c;
		registerReminders();
	}
	
	/**
	 * Register any alarms/reminders
	 * @author will
	 */
	
	private void registerReminders() {
		mDbHelper = new ItemDbAdapter(mContext);
        mDbHelper.open();
        /* Grab all items from database */
        Cursor itemsCursor = mDbHelper.fetchAllItems(null);
        /* Start with first item */
        itemsCursor.moveToFirst();
        /* Loop over all items */
        while (itemsCursor.isAfterLast() == false) {
        	/* Grab reminder string of current item */
            String rawDate = itemsCursor.getString(
            		itemsCursor.getColumnIndexOrThrow(ItemDbAdapter.KEY_REMINDER));
            /* Grab _id of current item */
            Long rowId = itemsCursor.getLong(
            		itemsCursor.getColumnIndexOrThrow(ItemDbAdapter.KEY_ROWID));
            /* If there is a valid reminder date parse it to get millis */
            if(rawDate.length() != 0 && rawDate != null) {
            	Long millis = parseDateString(rawDate);
            	if(millis != null && millis > System.currentTimeMillis()) {
            		/* if milli value isn't borked set a reminder for that time */
            		setAlarm(rowId, millis);
            	}
            }
            /* go to next item */
       	    itemsCursor.moveToNext();
        }
        /* clean up our mess */
        itemsCursor.close();
        mDbHelper.close();	
	}
	
	/**
	 * Set alarm for the given item and time
	 * @author 	will
	 * @param 	rowId
	 * 			The particular item this notification is for and will launch when clicked
	 * @param 	alarmTime
	 * 			The time in millis that this alarm will trigger
	 */
	
	private void setAlarm(Long rowId, Long alarmTime) {
    	Intent i = new Intent(mContext, AlarmReceiver.class);
    	i.putExtra(ItemDbAdapter.KEY_ROWID, rowId);
    	i.setAction("Item: " + rowId);
    	PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, i, 0);
    	AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
    	am.set(AlarmManager.RTC_WAKEUP, alarmTime, sender);
	}
	
    /**
     * Parse the reminder string
     * 
     * @author will
     * @throws java.text.ParseException
     */
    private Long parseDateString(String rawDate) {
    	try {
    		Long millis = Long.decode(rawDate);
    		return millis;
    	} catch (NumberFormatException e) {
    		e.printStackTrace();
    		return null;
    	}
    }
}
