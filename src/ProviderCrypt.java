package org.wzq.android.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.wzq.android.crypt.CryptAES;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;

/**
 * description: 自定义的ContentProvider，用于处理WebView的资源加载请求,并根据url进行鉴别处理。
 *
 * @date: 2014年5月8日
 * @author: wzq
 */
public class ProviderCrypt extends ContentProvider {
	public static final String AUTH = "org.wzq.android.provider.ProviderCrypt";
	public static final String SCHEME_FILE = "/file://";
	public static final String PATH_ASSET = "/android_asset/";
	public static final String PATH_SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();

	private static final String TAG = ProviderCrypt.class.getSimpleName();
	private static final boolean IS_ENCRYPT = false;

	public static String getCombineURL(String url) {
		return "content://" + AUTH + "/" + url;
	}

	/** uri = content://AUTH/xxxx */
	@SuppressWarnings("unused")
	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) {
		Log.v(TAG, "provider receive uri=" + uri);
		InputStream ins = getStreamFromUri(uri);

		// need decrypt
		if (ins != null && IS_ENCRYPT) {
			try {
				ins = CryptAES.getInstance().decryptStream(ins);
			} catch (Exception e) {
				Log.e(TAG, "decrypt stream fail, uri=" + uri);
				e.printStackTrace();
				return null;
			}
		}

		if (ins == null) {
			return null;
		}

		// stream => FileDescriptor
		try {
			ParcelFileDescriptor p = ParcelFileDescriptorUtil.pipeFrom(ins);
			Log.v(TAG, "provider fetch uri ok, uri=" + uri);
			return p;
		} catch (Exception e) {
			Log.e(TAG, "stream =>  fail, uri=" + uri);
			e.printStackTrace();
		}

		return null;
	}

	public InputStream getStreamFromUri(Uri uri) {
		String firstPath = uri.getPath();

		if (!firstPath.startsWith(SCHEME_FILE)) {
			Log.e(TAG, "undefined uri=" + uri);
			return null;
		}
		String secPath = firstPath.substring(SCHEME_FILE.length());

		// asset
		if (secPath.startsWith(PATH_ASSET)) {
			String assetPath = secPath.substring(PATH_ASSET.length());
			Log.v(TAG, "read asset:" + assetPath);
			AssetManager assetManager = getContext().getAssets();
			try {
				return assetManager.open(assetPath);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		// sd card
		if (secPath.startsWith(PATH_SDCARD)) {
			File file = new File(PATH_SDCARD, secPath);
			Log.v(TAG, "read sdcard :" + file.getAbsolutePath());
			try {
				return new FileInputStream(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return null;
			}

		}

		Log.e(TAG, "undefined uri=" + uri);
		return null;
	}

	static class ParcelFileDescriptorUtil {
		public static ParcelFileDescriptor pipeFrom(InputStream inputStream) throws Exception {
			ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
			ParcelFileDescriptor readSide = pipe[0];
			ParcelFileDescriptor writeSide = pipe[1];
			new TransferThread(inputStream, new ParcelFileDescriptor.AutoCloseOutputStream(writeSide)).start();
			return readSide;
		}

		static class TransferThread extends Thread {
			final InputStream mIn;
			final OutputStream mOut;

			TransferThread(InputStream in, OutputStream out) {
				super("ParcelFileDescriptor Transfer Thread");
				mIn = in;
				mOut = out;
				setDaemon(true);
			}

			@Override
			public void run() {
				byte[] buf = new byte[1024];
				try {
					int len;
					while ((len = mIn.read(buf)) > 0) {
						mOut.write(buf, 0, len);
					}
					mOut.flush(); // just to be safe
				} catch (IOException e) {
					Log.e(TAG, "TransferThread " + e.toString());
				} finally {
					try {
						mIn.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						mOut.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("Not supported by this provider");
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		throw new UnsupportedOperationException("Not supported by this provider:query");
	}

	@Override
	public String getType(Uri uri) {
		throw new UnsupportedOperationException("Not supported by this provider");
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException("Not supported by this provider");
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("Not supported by this provider");
	}
}
