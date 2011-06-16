package com.willtate.willdeux;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class GalleryAdapter extends BaseAdapter {
	public static final String TAG = "WillDeux";
	
    int mGalleryItemBackground;
    private ItemDbAdapter mDbHelper;
    private Context mContext;
    private Long mRowId;
    private String mImagePathArray[];
    

    public GalleryAdapter(Context c, Long rowId) {
        mContext = c;
        mRowId = rowId;
        String images = getImageString();
        TypedArray a = c.obtainStyledAttributes(R.styleable.Gallery);
        mGalleryItemBackground = a.getResourceId(
                R.styleable.Gallery_android_galleryItemBackground, 0);
        a.recycle();
        if(images == null || images.length() == 0) {
        	mImagePathArray = null;
        } else {
        	mImagePathArray = images.split(",");
        }
    }
    
    public String getImageString() {
    	String imgPaths = "";
    	if(mRowId != null) {
    		mDbHelper = new ItemDbAdapter(mContext);
    		mDbHelper.open();
    		Cursor item = mDbHelper.fetchItem(mRowId);
    		mDbHelper.close();
    		imgPaths = item.getString(item.getColumnIndexOrThrow(ItemDbAdapter.KEY_IMG));
    		item.close();
    	}
    	return imgPaths;
    }

    public int getCount() {
    	if(mImagePathArray == null) {
    		return 0;
    	}
        return mImagePathArray.length;
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView i = new ImageView(mContext);
    	BitmapFactory.Options options = new BitmapFactory.Options();
    	options.inSampleSize = 16;
    	Bitmap actualBitmap = BitmapFactory.decodeFile(mImagePathArray[position], options);
    	Bitmap scaledBitmap = Bitmap.createScaledBitmap(actualBitmap, 72, 72, false);
    	actualBitmap.recycle();
    	i.setImageBitmap(scaledBitmap);
        //i.setLayoutParams(new Gallery.LayoutParams(100, 100));
        i.setScaleType(ImageView.ScaleType.FIT_XY);
        i.setBackgroundResource(mGalleryItemBackground);

        return i;
    }
}
