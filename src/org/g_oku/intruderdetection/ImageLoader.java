package org.g_oku.intruderdetection;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.AsyncTaskLoader;

/**
 * {@link Bitmap} を非同期で読み込む {@link AsyncTaskLoader}.
 */
public class ImageLoader extends AsyncTaskLoader<Bitmap> {

    /** 対象のアイテム. */
    public ImageItem item;
    Context mContext;
    List<File> imageFileList;

    /**
     * コンストラクタ.
     * @param context {@link Context}
     * @param item {@link ImageItem}
     */
    public ImageLoader(Context context, ImageItem item) {
        super(context);
        this.mContext = context;
        this.item = item;
        
        init();
    }
    
    private void init(){    	
		try {
			imageFileList = FileDataUtil.getApplicationBitmapFileList(mContext);
		}
		catch (IOException e) {
		}
    }

    @Override
    public Bitmap loadInBackground() {
    	return getBitmap(item.id);
    }
    
	private Bitmap getBitmap(int id) {
		File item = imageFileList.get(id);
        Bitmap bitmap = null;
		try {
			bitmap = FileDataUtil.loadBitmap(item.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return bitmap;
	}
}
