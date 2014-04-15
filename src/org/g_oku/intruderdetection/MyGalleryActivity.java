package org.g_oku.intruderdetection;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * GridView を表示する {@link Activity}.
 */

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MyGalleryActivity extends FragmentActivity {

    /** ログ出力用のタグ. */
    private static final String TAG = MyGalleryActivity.class.getSimpleName();

    /** メモリキャッシュクラス. */
    private LruCache<String, Bitmap> mLruCache;
    /** {@link GridView}. */
    private GridView mGridView;
    
    String mCurImgPath = "";
    
	boolean mDisplayFlag = false;
	//100枚以上ならアラートを出す
	private static final int ARERT_IMAGE_NUM = 100; 

    ProgressDialog mProgressDialog = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.gallery);
        mGridView = (GridView)findViewById(R.id.gridview);
        mGridView.setNumColumns(2);
        mGridView.setVerticalSpacing(10);
        
        initProgressDialog();
        
        setDisplay();
    }
    
    void initProgressDialog(){
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.dialog_progress_delete));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
    }
    
    Runnable runnable = new Runnable() {
        public void run() {
        	handler.sendMessage(new Message());
        }
    };
    
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg)
        {
            mProgressDialog.dismiss();
        };
    };
    
    private void setDisplay(){
        //複数選択設定
        mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        mGridView.setMultiChoiceModeListener(new MultiChoiceModeListener(){
			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				//Log.d(TAG, "onActionItemClicked");
		    	switch (item.getItemId()) {
		    	case R.id.action_delete:
		    		delete();
		    		break;

		    	case R.id.action_share:
		    		share();
		    		break;
		    	}

		    	return true;
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				//Log.d(TAG, "onCreateActionMode");
		        getMenuInflater().inflate(R.menu.gallery_action, menu);
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode arg0) {
				//Log.d(TAG, "onDestroyActionMode");
			}

			@Override
			public boolean onPrepareActionMode(ActionMode arg0, Menu arg1) {
				//Log.d(TAG, "onPrepareActionMode");
				return true;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int pos, long id, boolean checked) {
				//Log.d(TAG, "onItemCheckedStateChanged");
				//int count = mGridView.getCheckedItemCount();
				//Log.d(TAG, "check count = " + count);
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
			Toast.makeText(this, R.string.toast_read_error_message, Toast.LENGTH_LONG).show();
			return;
		}
		
		if(imageFileList == null){
			return;
		}
		
		//枚数が多い場合は削除するようアラートを入れる
		alertNotifyDialog(imageFileList.size());

		ImageAdapter adapter = new ImageAdapter(this);
        mGridView.setAdapter(adapter);
        //Log.d("cachesample", "size = " + imageFileList.size());
        for (int i = 0; i < imageFileList.size(); i++) {
            ImageItem item = new ImageItem();
            item.id = i;
            item.path = imageFileList.get(i).getPath();
            
            adapter.add(item);
        }
        //最新順にソート
        adapter.sort(new AdapterComparator());
		
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
    			ImageView view = (ImageView)findViewById(R.id.image);
                view.setImageBitmap(null);

                ImageAdapter adapter = (ImageAdapter)mGridView.getAdapter();
                ImageItem item = adapter.getItem(position);
                
                //選択されている画像のパスを覚えておく(delete時のクリア用)
                mCurImgPath = item.path;
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

                view.setImageBitmap(bmp);
            }
        });

        loadBitmap();
    }
    
    private void alertNotifyDialog(int size){
		SharedPreferences sharePref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean flag = sharePref.getBoolean("displayFlag", true);
		if(size > ARERT_IMAGE_NUM && flag){
			LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
			View view = inflater.inflate(R.layout.notify_dialog, null);
			
			AlertDialog dialog = new AlertDialog.Builder(this)
			.setTitle(R.string.dialog_notify_title)
			.setMessage(getString(R.string.dialog_notify_message))
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					//Checkboxの状態を保存
					SharedPreferences sharePref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
					Editor editor = sharePref.edit();
					editor.putBoolean("displayFlag", !mDisplayFlag);
					editor.commit();
				}
			}).create();
			dialog.setView(view, 0, 0, 0, 0);
			dialog.show();

			CheckBox checkbox = (CheckBox)view.findViewById(R.id.checkBox1);
			checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
					mDisplayFlag = isChecked;
				}
			});
		}
    }
    
    private void share(){
    	ArrayList<Uri> list = new ArrayList<Uri>();
    	
		ImageAdapter adapter = (ImageAdapter)mGridView.getAdapter();
    	//選択されているインデックス取得
    	SparseBooleanArray positions = mGridView.getCheckedItemPositions();
    	for(int i=0; i<positions.size(); i++){
        	int key = positions.keyAt(i);
        	
        	ImageItem item = adapter.getItem(key);
        	String url = "file://" + item.path;
        	list.add(Uri.parse(url));
    	}
    	
		Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		intent.setType("image/jpg");
		intent.putExtra(Intent.EXTRA_STREAM, list);
		startActivity(intent);
    }
    
    private void delete(){
    	new AlertDialog.Builder(this)
    	.setTitle(R.string.dialog_confirm_title)
    	.setMessage(getString(R.string.dialog_delete_confirm))
    	.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
		    	//プログレスダイアログ表示
		    	mProgressDialog.show();
		    	
				AsyncTask task = new AsyncTask() {
					@Override
					protected Object doInBackground(Object... params) {
				    	ImageAdapter adapter = (ImageAdapter)mGridView.getAdapter();

				    	//選択されているインデックス取得
				    	SparseBooleanArray positions = mGridView.getCheckedItemPositions();
				    	for(int i=0; i<positions.size(); i++){
				        	int key = positions.keyAt(i);
				        	
				        	ImageItem item = adapter.getItem(key);
				        	//Log.d(TAG, "path = " + item.path);
				        	
				        	deleteImageFile(item.path);
				    	}

						return null;
					}

					@Override
					protected void onPostExecute(Object obj) {
						setDisplay();
						mProgressDialog.dismiss();
					}
				};
				task.execute();
			}
		})
		.setNegativeButton(R.string.ng, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				//何もしない
			}
		})
		.show();    	
    }
    
    private void deleteImageFile(String path){
    	//ギャラリーからの削除
    	getContentResolver().delete(Media.EXTERNAL_CONTENT_URI, Images.Media.DATA + " = ?", new String[]{path});

    	//削除対象の画像を表示中ならクリア
    	if(mCurImgPath.equals(path)){
    		ImageView view = (ImageView)findViewById(R.id.image);
    		view.setImageBitmap(null);
    		mCurImgPath = "";
    	}
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
    	//ここはただ数を取りたいだけ
        List<File> fileList = null;
		try {
			fileList = FileDataUtil.getApplicationBitmapFileList(this);
		} catch (IOException e) {
			Toast.makeText(this, R.string.toast_delete_error_message, Toast.LENGTH_LONG).show();
			return;
		}
		//ファイルが0の場合は何もしない
		if(fileList.size() == 0){
			return;
		}
    	
    	new AlertDialog.Builder(this)
    	.setTitle(R.string.dialog_confirm_title)
    	.setMessage(getString(R.string.dialog_delete_all_confirm))
    	.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				//プログレスダイアログ表示
		    	mProgressDialog.show();

				AsyncTask task = new AsyncTask() {
					@Override
					protected Object doInBackground(Object... params) {
				    	List<File> files = null;
						try {
							files = FileDataUtil.getApplicationBitmapFileList(getApplicationContext());
						} catch (IOException e) {
						}

						for (int i = 0; i < files.size(); i++) {
				            deleteImageFile(files.get(i).getPath());
				        }

						return null;
					}

					@Override
					protected void onPostExecute(Object obj) {
						setDisplay();
						mProgressDialog.dismiss();
					}
				};
				task.execute();
			}
		})
		.setNegativeButton(R.string.ng, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				//何もしない
			}
		})
		.show();
    }
    
    class AdapterComparator implements java.util.Comparator {
    	public int compare(Object s, Object t) {
    		return ((ImageItem)t).id - ((ImageItem)s).id;
    	}
    }
}
