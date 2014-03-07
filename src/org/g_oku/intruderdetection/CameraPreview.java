package org.g_oku.intruderdetection;

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;

import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class CameraPreview implements SurfaceHolder.Callback {
    Camera mCamera = null;
    Context mContext = null;

    AutoFocusCallback mFocus = null;

    private Size mSize = null;
    private List<Size> mSupportList = null;
    //サポートリストに対する端末の下限値のインデックス
    private int mOffset = 0;
    PreviewCallback mPreviewCallback = null;
	
	//初期設定
	private int mPicIdx = 0;
	private String mSizeStr = null;
	
	//画面サイズ
	int mWidth = 0;
	int mHeight = 0;
		
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	CameraPreview(Context context){
        mContext = context;
        
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
			mWidth = display.getWidth();
			mHeight = display.getHeight();
		}
		else{
			Point size = new Point();
			display.getSize(size);

			mWidth = size.x;
			mHeight = size.y;
		}
	}
    
    public void surfaceCreated(SurfaceHolder holder) {
    	//Log.d(TAG, "enter CameraPreview#surfaceCreated");

    	if(mCamera == null){
    	    try{
                mCamera = Camera.open(getFrontCameraNo());
    	        
    	    }catch(RuntimeException e){
    	        try {
    	            this.finalize();
    	        } catch (Throwable t) {
    	            ((WatchService)mContext).stop();        
    	        }
    	        return;
    	    }
    	    
    	    if(mCamera == null){
                try {
                    this.finalize();
                } catch (Throwable t) {
    	            ((WatchService)mContext).stop();        
                }
                return;
    	    }
    	    
            mCamera.setDisplayOrientation(90);
    	}
    	
    	if(mSupportList == null){
    	    createSupportList();
    	}

    	try {
            mCamera.setPreviewDisplay(holder);               
        } catch (IOException e) {
            Log.e(IntruderDetectionActivity.TAG, "IOException in surfaceCreated");
            mCamera.release();
            mCamera = null;
            ((WatchService)mContext).stop();
        }
    }
    
	private int getFrontCameraNo(){
		int num = Camera.getNumberOfCameras();
		for(int i=0; i<num; i++){
			CameraInfo caminfo = new CameraInfo();
			Camera.getCameraInfo(i, caminfo);
		
			if(caminfo.facing == CameraInfo.CAMERA_FACING_FRONT){
				return i;
			}
		}
		
		return 0;
	}
    
    private void createSupportList(){
        if(mCamera == null){
            return;
        }
        
        Camera.Parameters params = mCamera.getParameters();
        mSupportList = Reflect.getSupportedPreviewSizes(params);
           
        if (mSupportList != null && mSupportList.size() > 0) {
            //降順にソート
            Collections.sort(mSupportList, new PreviewComparator());
            
            for(int i = 0; i < mSupportList.size(); i++){
                if(mSupportList.get(i).width > mWidth){
                    continue;
                }
                
                if(mSupportList.get(i).height > mHeight){
                    continue;
                }
                
                mSize = mSupportList.get(i);
                mOffset = i;
                break;
            }
            
            if(mSize == null){
                mSize = mSupportList.get(0);
                mOffset = 0;
            }
        }
    }
    
    public void surfaceDestroyed(SurfaceHolder holder) {
        //Log.d(TAG, "enter CameraPreview#surfaceDestroyed");
    	release();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        //Log.d(TAG, "enter CameraPreview#surfaceChanged");
        
        //Cameraがopen()できなかったとき用
        if(mCamera == null){
            ((WatchService)mContext).stop();
            return;
        }

        //止めないでsetParameters()するとエラーとなる場合があるため止める
        mCamera.stopPreview();
        
        List<String> list = getSizeList();
        for(int i = 0; i<list.size(); i++){
            if(list.get(i).equals(mSizeStr)){
                mPicIdx = i;
            }
            //mSizeStrが"0"のときはmPicIdxに値が設定されずに抜ける(=0になる)
        }
        
        setAllParameters();

        mCamera.startPreview();
        //focus
        mFocus = new AutoFocusCallback(){
            public void onAutoFocus(boolean success, Camera camera) {
                if(mPreviewCallback == null){
                    mPreviewCallback = new PreviewCallback(CameraPreview.this);
        			mCamera.startPreview();
        			mCamera.setPreviewCallback(mPreviewCallback);
                }
            }
        };

        try{
            mCamera.autoFocus(mFocus);
        }catch(Exception e){
            if(mPreviewCallback == null){
                mPreviewCallback = new PreviewCallback(CameraPreview.this);
            }
        }

        /*
    	if(mPreviewCallback != null){
    		if(mCamera != null){
    			mCamera.startPreview();
    			mCamera.setPreviewCallback(mPreviewCallback);
    		}
    	}
    	*/
    }
    
    private void setAllParameters(){
        Camera.Parameters param = mCamera.getParameters();

        try{
            mSize = mSupportList.get(mOffset + mPicIdx);        
            param.setPreviewSize(mSize.width, mSize.height);
            mCamera.setParameters(param);
        }catch(Exception e){
            //nothing to do
        }
    }
    
    List<String> getSizeList(){
    	List<String> list = new ArrayList<String>();
    	for(int i = mOffset; i<mSupportList.size(); i++){
    		String size = mSupportList.get(i).width + "x" + mSupportList.get(i).height;
    		list.add(size);
    	}
    	return list;
    }
    
    void release(){
        if(mCamera != null){
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    class PreviewComparator implements java.util.Comparator {
    	public int compare(Object s, Object t) {
    		//降順
    		return ((Size) t).width - ((Size) s).width;
    	}
    }

    
    public class PreviewCallback implements Camera.PreviewCallback {
        private CameraPreview mPreview = null;

        PreviewCallback(CameraPreview preview){
            mPreview = preview;
        }

        public void onPreviewFrame(byte[] data, Camera camera) {
        	//Log.d("IntruderDetection", "onPreviewFrame");
            //一旦コールバックを止める
        	camera.setPreviewCallback(null);

            //convert to "real" preview size. not size setting before.
            Size size = convertPreviewSize(data);

            final int width = size.width;
            final int height = size.height;            

            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            
            ImageAsyncTask task = new ImageAsyncTask(mContext, CameraPreview.this, data, size);
            task.execute(bmp);
       }
        
        private Size convertPreviewSize(byte[] data){
            double displaysize = data.length / 1.5;
            Size size;
            int x, y;
            
            for(int i=0; i<mSupportList.size(); i++){
                size = mSupportList.get(i);
                x = size.width;
                y = size.height;
                if((x*y) == displaysize){
                    return size;
                }
            }
            return null;
        }
    }
}