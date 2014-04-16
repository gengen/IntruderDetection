package org.g_oku.intruderdetection;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

/**
 * {@link GridView} の {@link ArrayAdapter}.
 */
public class ImageAdapter extends ArrayAdapter<ImageItem> {
    Context mContext;

    /**
     * コンストラクタ.
     * @param context {@link Context}
     */
    public ImageAdapter(Context context) {
        super(context, 0);

        mContext = context;
    }
    
    public ImageAdapter(Context context, int layout){
    	super(context, layout);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ImageItem item = getItem(position);

        ImageView view = new ImageView(getContext());
        view.setLayoutParams(new AbsListView.LayoutParams(100, 100));
        view.setScaleType(ScaleType.FIT_CENTER);
        view.setImageBitmap(item.bitmap);
        view.setTag(item);
        
        CheckableLayout l = new CheckableLayout(mContext);
        l.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.WRAP_CONTENT, GridView.LayoutParams.WRAP_CONTENT));      
        l.addView(view);

        return l;
    }
    
    public class CheckableLayout extends FrameLayout implements Checkable {
        private boolean mChecked;

        public CheckableLayout(Context context) {
            super(context);
        }

        public void setChecked(boolean checked) {
            mChecked = checked;
            setBackgroundDrawable(checked ? getResources().getDrawable(R.drawable.cyan) : null);
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void toggle() {
            setChecked(!mChecked);
        }
    }
}
