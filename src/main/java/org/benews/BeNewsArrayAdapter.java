package org.benews;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by zad on 28/10/14.
 */
public class BeNewsArrayAdapter extends ArrayAdapter<HashMap<String,String> >{

	public static final String TAG = "BeNewsArrayAdapter";
	private static final int LEFT_ALIGNED_VIEW = 0;
	private static final int RIGHT_ALIGNED_VIEW = 1;
	private final ArrayList<HashMap<String,String> > list;
	private final Context context;



	public BeNewsArrayAdapter(Context context, ArrayList<HashMap<String,String>>  objects) {
		super(context,R.layout.item_layout_right, objects);
		list=objects;
		this.context=context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewHolderItem viewElements;
		if (position % 2 == 0) {
			viewElements = getCachedView((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE), parent, BeNewsArrayAdapter.RIGHT_ALIGNED_VIEW);
		} else {
			viewElements = getCachedView((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE), parent, BeNewsArrayAdapter.LEFT_ALIGNED_VIEW);
		}
		if ( list != null ) {
			HashMap<String, String> item = list.get(position);
			String path = item.get(BackgroundSocket.HASH_FIELD_PATH);
			String type = item.get(BackgroundSocket.HASH_FIELD_TYPE);
			if (path != null && type != null) {
				if (type.equals(BackgroundSocket.TYPE_IMG_DIR)) {
					try {
						File imgFile = new File(path);
						if (imgFile.exists()) {
							Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
							if (myBitmap != null) {
								int it = (BitmapHelper.img_preview_limit_high == 0) ? 100 : BitmapHelper.img_preview_limit_high;
								if (myBitmap.getHeight() > it)
									myBitmap = BitmapHelper.scaleToFitHeight(myBitmap, (int) ((BitmapHelper.dp2dpi_factor == 0) ? 48 : 48 * BitmapHelper.dp2dpi_factor));

								viewElements.imageView.setImageBitmap(myBitmap);
							}
						} else {
							//removing corrupted image
							if (list.contains(item)) {
								list.remove(item);
								this.notifyDataSetChanged();
							}
							return viewElements.view;
						}
					} catch (Exception e) {
						//removing corrupted image
						if (list.contains(item)) {
							list.remove(item);
							this.notifyDataSetChanged();
						}
						Log.d(TAG, " (getView):" + e);
						return viewElements.view;
					}
				}
				if (item.containsKey(BackgroundSocket.HASH_FIELD_TITLE)) {
					viewElements.title.setText(item.get(BackgroundSocket.HASH_FIELD_TITLE));
				}
				if (item.containsKey(BackgroundSocket.HASH_FIELD_HEADLINE)) {
					viewElements.secondLine.setText(item.get(BackgroundSocket.HASH_FIELD_HEADLINE));
				}
				if (item.containsKey(BackgroundSocket.HASH_FIELD_DATE)) {
					try {

						Date date = new Date();
						long epoch = Long.parseLong(item.get(BackgroundSocket.HASH_FIELD_DATE));
						date.setTime(epoch * 1000L);
						//Log.d(TAG,"date "+date +" long=" + epoch);
						viewElements.date.setText(BackgroundSocket.dateFormatter.format(date));
					} catch (Exception e) {
						Log.d(TAG, "Invalid date " + item.get(BackgroundSocket.HASH_FIELD_DATE));
						viewElements.date.setText("--/--/----");
					}
				}

			}
		}
		return viewElements.view;
	}

	private ViewHolderItem getCachedView(LayoutInflater inflater, ViewGroup parent, int viewTipe) {
		ViewHolderItem viewElements=null;
		switch (viewTipe){
			default:
			case RIGHT_ALIGNED_VIEW:
					viewElements = new ViewHolderItem(inflater.inflate(R.layout.item_layout_right, parent, false));
				break;
			case LEFT_ALIGNED_VIEW:
					viewElements = new ViewHolderItem(inflater.inflate(R.layout.item_layout_left, parent, false));
				break;
		}
		return viewElements;
	}

	private class ViewHolderItem {
		View view;
		TextView title;
		TextView secondLine;
		TextView date;
		ImageView imageView;

		public ViewHolderItem(View inflated) {
			view = inflated;
			title = (TextView) view.findViewById(R.id.title);
			secondLine = (TextView) view.findViewById(R.id.secondLine);
			imageView = (ImageView) view.findViewById(R.id.icon);
			date = (TextView) view.findViewById(R.id.date);

		}

	}
}
