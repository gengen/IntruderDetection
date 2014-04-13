package org.g_oku.intruderdetection;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * GridView を表示する {@link Activity}.
 */
public class MyGalleryActivity extends FragmentActivity {

    /** ログ出力用のタグ. */
    private static final String TAG = MyGalleryActivity.class.getSimpleName();

    /** メモリキャッシュクラス. */
    private LruCache<String, Bitmap> mLruCache;
    /** {@link GridView}. */
    private GridView mGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.gallery);
        mGridView = (GridView)findViewById(R.id.gridview);
        mGridView.setNumColumns(2);
        mGridView.setVerticalSpacing(10);
        
        setDisplay();
    }
    
    private void setDisplay(){
        //複数選択設定
        mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        mGridView.setMultiChoiceModeListener(new MultiChoiceModeListener(){
			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				Log.d(TAG, "onActionItemClicked");
		    	switch (item.getItemId()) {
		    	case R.id.action_delete:
		    		Log.d(TAG, "delete");
		    		delete();
		    	}
				return true;
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				Log.d(TAG, "onCreateActionMode");
		        getMenuInflater().inflate(R.menu.gallery_action, menu);
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode arg0) {
				Log.d(TAG, "onDestroyActionMode");
			}

			@Override
			public boolean onPrepareActionMode(ActionMode arg0, Menu arg1) {
				Log.d(TAG, "onPrepareActionMode");
				return true;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int pos, long id, boolean checked) {
				Log.d(TAG, "onItemCheckedStateChanged");
				int count = mGridView.getCheckedItemCount();
				Log.d(TAG, "check count = " + count);
			}
        });

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            	//return value.getByteCount();
            }
        };

        // Adapter の作成とアイテムの追加
		List<File> imageFileList = null;
		try {
			imageFileList = FileDataUtil.getApplicationBitmapFileList(this);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ImageAdapter adapter = new ImageAdapter(this);
        mGridView.setAdapter(adapter);
        //Log.d("cachesample", "size = " + imageFileList.size());
        for (int i = 0; i < imageFileList.size(); i++) {
            ImageItem item = new ImageItem();
            item.id = i;
            item.path = imageFileList.get(i).getPath();
            
            adapter.add(item);
        }
		
        mGridView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    // スクロールが止まったときに読み込む
                    loadBitmap();
                }
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
        
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            //選択でImageViewに拡大表示
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                ImageAdapter adapter = (ImageAdapter)mGridView.getAdapter();
                ImageItem item = adapter.getItem(position);
                
                //Log.d(TAG, "path = " + item.path);
                
        		FileInputStream fileInput = null;
        		BufferedInputStream bufInput = null;
				try {
					fileInput = new FileInputStream(item.path);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
                bufInput = new BufferedInputStream(fileInput);
    			Bitmap bmp = BitmapFactory.decodeStream(bufInput);

    			ImageView view = (ImageView)findViewById(R.id.image);
                view.setImageBitmap(bmp);
            }
        });

        loadBitmap();
    }	
    
    private void delete(){
    	ImageAdapter adapter = (ImageAdapter)mGridView.getAdapter();

    	//選択されているインデックス取得
    	SparseBooleanArray positions = mGridView.getCheckedItemPositions();
    	Log.d(TAG, "size = " + positions.size());
    	for(int i=0; i<positions.size(); i++){
        	int key = positions.keyAt(i);
        	Log.d(TAG, "key = " + key);
        	
        	ImageItem item = adapter.getItem(key);
        	Log.d(TAG, "path = " + item.path);
        	
        	File file = new File(item.path);
        	file.delete();
    	}
    	
    	//TODO ギャラリーからの削除
    	
    	setDisplay();
    }
    
    /**
     * 画像を読み込む.
     */
    private void loadBitmap() {
        // 現在の表示されているアイテムのみリクエストする
        ImageAdapter adapter = (ImageAdapter) mGridView.getAdapter();
        int first = mGridView.getFirstVisiblePosition();
        int count = mGridView.getChildCount();
        
        //起動時countが0になってしまうため、対処
        int realCount = mGridView.getCount();
        if(count == 0 && realCount == 0){
        	count = 0;
        }
        else if(count == 0 && realCount != 0){
        	//20件以上ある場合は初回は20件表示
        	if(realCount < 20){
        		count = realCount;
        	}
        	else{
        		count = 20;
        	}
        }
        
        for (int i = 0; i < count; i++) {
            ImageItem item = adapter.getItem(i + first);
            // キャッシュの存在確認
            Bitmap bitmap = mLruCache.get("" + item.id);
            if (bitmap != null) {
                // キャッシュに存在
                //Log.i(TAG, "キャッシュあり=" + item.id);
                setBitmap(item);
                mGridView.invalidateViews();
            } else {
                // キャッシュになし
                //Log.i(TAG, "キャッシュなし=" + item.id);
                Bundle bundle = new Bundle();
                bundle.putSerializable("item", item);
                getSupportLoaderManager().initLoader(i, bundle, callbacks);
            }
        }
    }

    /**
     * アイテムの View に Bitmap をセットする.
     * @param item
     */
    private void setBitmap(ImageItem item) {
        ImageView view = (ImageView) mGridView.findViewWithTag(item);
        if (view != null) {
            view.setImageBitmap(item.bitmap);
            mGridView.invalidateViews();
        }
    }
    
    /**
     * ImageLoader のコールバック.
     */
    private LoaderCallbacks<Bitmap> callbacks = new LoaderCallbacks<Bitmap>() {
        @Override
        public Loader<Bitmap> onCreateLoader(int i, Bundle bundle) {
            ImageItem item = (ImageItem) bundle.getSerializable("item");
            ImageLoader loader = new ImageLoader(getApplicationContext(), item);
            loader.forceLoad();
            return loader;
        }
        @Override
        public void onLoadFinished(Loader<Bitmap> loader, Bitmap bitmap) {
            int id = loader.getId();
            getSupportLoaderManager().destroyLoader(id);
            // メモリキャッシュに登録する
            ImageItem item = ((ImageLoader) loader).item;
            //Log.i(TAG, "キャッシュに登録=" + item.id);
            item.bitmap = bitmap;
            mLruCache.put("" + item.id, bitmap);
            setBitmap(item);
        }
        @Override
        public void onLoaderReset(Loader<Bitmap> loader) {
        }
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gallery_setting, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG, "onOptionsItemSelected");
    	// Handle presses on the action bar items
    	switch (item.getItemId()) {
    	case R.id.action_deleteAll:
    		deleteAll();
    		return true;

    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    private void deleteAll(){
    	Log.d(TAG, "deleteALL");
    }

}
