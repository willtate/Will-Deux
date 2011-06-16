package com.willtate.willdeux;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class Settings extends PreferenceActivity 
	implements ColorPickerDialog.OnColorChangedListener{
	
	Preference mHighColor;
	Preference mNormColor;
	Preference mLowColor;
	Preference mDefaultColors;
	
	Paint mPaint;
	
	SharedPreferences mPrefs;
	
	int mCaller;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mPaint = new Paint();
		setupOnClickHandlers();
	}
	
	/**
	 * Method to setup the click settings for the preferences
	 * 
	 * @author will
	 * 
	 */
	
	public void setupOnClickHandlers() {
		mHighColor = (Preference) findPreference("highColor");
		mNormColor = (Preference) findPreference("normColor");
		mLowColor = (Preference) findPreference("lowColor");
		mDefaultColors = (Preference) findPreference("defaultColors");
		mHighColor.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			 public boolean onPreferenceClick(Preference preference) {
				 mPaint.setColor(mPrefs.getInt("highColor", R.color.high_priority));
				 mCaller = 2;
				 launchColorChooser(mPaint.getColor());
				 return true;
			 }
		 });
		
		mNormColor.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			 public boolean onPreferenceClick(Preference preference) {
				 mPaint.setColor(mPrefs.getInt("normColor", R.color.norm_priority));
				 mCaller = 1;
				 launchColorChooser(mPaint.getColor());
				 return true;
			 }
		 });
		
		mLowColor.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			 public boolean onPreferenceClick(Preference preference) {
				 mPaint.setColor(mPrefs.getInt("lowColor", R.color.low_priority));
				 mCaller = 0;
				 launchColorChooser(mPaint.getColor());
				 return true;
			 }
		 });
		
		mDefaultColors.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Context context = getApplicationContext();
				Resources res = context.getResources();
				SharedPreferences.Editor editor = mPrefs.edit();
				editor.putInt("highColor", res.getColor(R.color.high_priority));
				editor.putInt("normColor", res.getColor(R.color.norm_priority));
				editor.putInt("lowColor", res.getColor(R.color.low_priority));
				editor.commit();
				Toast.makeText(context, "Default priority colors loaded.", Toast.LENGTH_SHORT).show();
				return true;
			}
		});
	}
	
	/**
	 * Launch the ColorPickerDialog
	 * 
	 * @author 	will
	 * 
	 * @param 	Color
	 * 			The color to start the ColorPickDialog
	 * 
	 */
	
	public void launchColorChooser(int Color) {
		new ColorPickerDialog(this, this, Color).show();
	}

	/**
	 * Called when a selection has been made in the ColorPickerDialog.
	 * We take that value and commit it to our SharedPreferences
	 * 
	 * @author 	will
	 * 
	 * @param 	color 
	 * 			The color chosen by the user
	 */
	
	public void colorChanged(int color) {
		SharedPreferences.Editor editor = mPrefs.edit();
		switch(mCaller) {
		case 2:
			editor.putInt("highColor", color);
			mCaller = -1;
			break;
		case 1:
			editor.putInt("normColor", color);
			mCaller = -1;
			break;
		case 0: 
			editor.putInt("lowColor", color);
			mCaller = -1;
			break;
		default:
			break;
		}
		editor.commit();
	}
}
