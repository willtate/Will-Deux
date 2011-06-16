package com.willtate.willdeux;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class ItemAdapter extends CursorAdapter {
	private LayoutInflater mLayoutInflater;
	private Context mContext;
	public ItemAdapter(Context context, Cursor c) {
		super(context, c);
		mContext = context;
		mLayoutInflater = LayoutInflater.from(context); 
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View v = mLayoutInflater.inflate(R.layout.items_row, parent, false);
		return v;
	}
	
	/**
	 * @author will
	 * 
	 * @param	v
	 * 			The view in which the elements we set up here will be displayed.
	 * 
	 * @param	context
	 * 			The running context where this ListView adapter will be active.
	 * 
	 * @param	c
	 * 			The Cursor containing the query results we will display.
	 */
	
	@Override
	public void bindView(View v, Context context, Cursor c) {
		String title = c.getString(c.getColumnIndexOrThrow(ItemDbAdapter.KEY_TITLE));
		String date = c.getString(c.getColumnIndexOrThrow(ItemDbAdapter.KEY_DATE));
		String imagePath = c.getString(c.getColumnIndexOrThrow(ItemDbAdapter.KEY_IMG));
		String reminder = c.getString(c.getColumnIndexOrThrow(ItemDbAdapter.KEY_REMINDER));
		int deletion = c.getInt(c.getColumnIndexOrThrow(ItemDbAdapter.KEY_DELETION));
		int priority = c.getInt(c.getColumnIndexOrThrow(ItemDbAdapter.KEY_PRIORITY));
		
		/**
		 * Next set the title of the entry.
		 */
		
		TextView title_text = (TextView) v.findViewById(R.id.item_text);
		if (title_text != null) {
			title_text.setText(title);
		}
		
		setPriorityColor(title_text, priority);
		
		/**
		 * Set Date
		 */
		
		TextView date_text = (TextView) v.findViewById(R.id.item_date);
		if (date_text != null) {
			date_text.setText(date);
		}		
		
		/**
		 * Decide if we should display the paper clip icon denoting image attachment
		 */
		
		ImageView item_image = (ImageView) v.findViewById(R.id.item_attachment);
		item_image.setVisibility(ImageView.INVISIBLE);
		if (imagePath != null && imagePath.length() != 0 && item_image != null) {
			item_image.setVisibility(ImageView.VISIBLE);
		}
		
		/**
		 * Decide if we should display the deletion indicator
		 */
		ImageView del_image = (ImageView) v.findViewById(R.id.item_deletion);
		del_image.setVisibility(ImageView.INVISIBLE);
		if (deletion == 1) {
			del_image.setVisibility(ImageView.VISIBLE);
		}
		
		/**
		 * Decide if we should display the reminder indicator
		 */
		ImageView rem_image = (ImageView) v.findViewById(R.id.item_reminder);
		rem_image.setVisibility(ImageView.INVISIBLE);
		if(reminder != null && reminder.length() != 0 && rem_image != null) {
			rem_image.setVisibility(ImageView.VISIBLE);
		}
	}
	
	/**
	 * Set the priority colors based on the SharedPreferences
	 * 
	 * @author will
	 * 
	 * @param	title 
	 * 			The particular TextView item we are handling
	 * @param 	priority 
	 * 			The current TextView item's associated priority level
	 */
	
	private void setPriorityColor(TextView title, int priority) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		Resources res = mContext.getResources();
		
		switch(priority) {
		case ItemDbAdapter.PRIORITY_HIGH:
			title.setTextColor(prefs.getInt("highColor", res.getColor(R.color.high_priority)));
			break;
		case ItemDbAdapter.PRIORITY_NORMAL:
			title.setTextColor(prefs.getInt("normColor", res.getColor(R.color.norm_priority)));
			break;
		case ItemDbAdapter.PRIORITY_LOW:
			title.setTextColor(prefs.getInt("lowColor", res.getColor(R.color.low_priority)));
			break;
		}
	}
}
