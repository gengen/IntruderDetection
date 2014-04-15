package org.g_oku.intruderdetection;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

public class FileDataUtil {
	public static Bitmap loadBitmap(String fileName) throws IOException {
		Bitmap result = null;

		FileInputStream fileInput = null;
		BufferedInputStream bufInput = null;
		try {
			fileInput = new FileInputStream(fileName);
			bufInput = new BufferedInputStream(fileInput);

			//読み込むサイズを決定
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			//サイズのみを読み込むためにデコード
			BitmapFactory.decodeFile(fileName, options);
			int sample = calculateInSampleSize(options, 100, 100);
			//Log.d("cachesample", "sample = " + sample);
		    options.inSampleSize = sample;
			options.inJustDecodeBounds = false;
			//決定したサンプリングで読み込む
			result = BitmapFactory.decodeStream(bufInput, null, options);
	         
		} finally {
			if (fileInput != null) {
				try {
					fileInput.close();
				} catch (IOException e) {
				}
			}
			if (bufInput != null) {
				try {
					bufInput.close();
				} catch (IOException e) {
				}
			}
		}
		return result;
	}
	
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

	    //Log.d("cachesample", "size = " + height + "," + width);

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	/**
	 * アプリケーション保存ディレクトリに存在する画像ファイルを取得する。
	 * @return 画像ファイルリスト (存在しない場合は空のリスト)
	 */
	public static List<File> getApplicationBitmapFileList(Context context) throws IOException {
		List<File> result = new ArrayList<File>();;

		File sdcardRoot = getSdCardRootDirectory();
		File loadDir = new File(sdcardRoot, "Intruder");
		//Log.d("GallerySample", loadDir.getPath());
		if (!loadDir.exists()) {
			loadDir.mkdir();
			//throw new IOException(String.format("アプリケーションの保存先が存在しません(%s)", loadDir));
		}
		if (!loadDir.canRead()) {
			throw new IOException(String.format("ファイルを読み出しできません(%s)", loadDir));
		}

		// アプリケーションディレクトリの画像ファイルを取得
		File[] files = loadDir.listFiles(new FilenameFilter() {

			// 画像ファイルのみを対象とする(*.PNG(*.png) or *.JPEG(*.jpeg))
			public boolean accept(File loadDir, String fileName) {
				// CompressFormat
	            //   JPEG = new CompressFormat("JPEG", 0);
	            //   PNG = new CompressFormat("PNG", 1);
				return fileName.matches(".+\\.(PNG|png|JPEG|jpeg|JPG|jpg)$");
			}
		});
		// メイン側で扱いやすいようにリストに変換しています
		if (files != null && files.length > 0) {
			for (File item : files) {
				result.add(item);
			}
		}
		return result;
	}

	/**
	 * 外部メディアディレクトリを取得する。
	 * @return 外部メディアディレクトリ (/sdcard/)
	 * @throws IOException メディアエラー
	 */
	public static File getSdCardRootDirectory() throws IOException {
		// SDカードがマウントされているか
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			throw new IOException(String.format("メディアがセットされていません(%s)", Environment.getExternalStorageState()));
		}
		File root = Environment.getExternalStorageDirectory();
		if (root == null) {
			throw new IOException("メディアが利用できません");
		}
		return root;
	}
}
