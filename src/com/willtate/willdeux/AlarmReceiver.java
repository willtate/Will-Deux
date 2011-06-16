package com.willtate.willdeux;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class AlarmReceiver extends BroadcastReceiver {
	private NotificationManager mNM;
    private ItemDbAdapter mDbHelper;
	private Long mRowId;
	private Context context;
	@Override
	public void onReceive(Context c, Intent i) {
		context = c;
		Bundle extras = i.getExtras();
		//Extract any extras that came in
		mRowId = extras.getLong(ItemDbAdapter.KEY_ROWID);
		mNM = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		showNotification();
	}
	
	private void showNotification() {
		mDbHelper = new ItemDbAdapter(context);
		mDbHelper.open();
		Cursor item = mDbHelper.fetchItem(mRowId);
        String title = item.getString(item.getColumnIndexOrThrow(ItemDbAdapter.KEY_TITLE));
        item.close();
		// Set the icon, scrolling text and time stamp
		Notification notification = new Notification(android.R.drawable.ic_popup_reminder,
				context.getText(R.string.notification_title), System.currentTimeMillis());
		Intent i = new Intent(context, ItemEdit.class);
		i.putExtra(ItemDbAdapter.KEY_ROWID, mRowId);
		i.setAction("AlarmService:" + System.currentTimeMillis());
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP );
		//Grab preferences so we can check the notifications preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, i, 0);
		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(context, context.getText(R.string.notification_title),
				title, contentIntent);
		
		if(prefs.getBoolean("reminderSound", true)) {
			notification.defaults |= Notification.DEFAULT_SOUND;
		}
		if(prefs.getBoolean("reminderVibrate",true)) {
			notification.defaults |= Notification.DEFAULT_VIBRATE;
		}
		if(prefs.getBoolean("reminderLED",true)) {
			notification.defaults |= Notification.DEFAULT_LIGHTS;
		}
		// Send the notification.
		// We use a layout id because it is a unique number.  We use it later to cancel.
		mNM.notify(mRowId.intValue(), notification);
		mDbHelper.close();
	}
}
