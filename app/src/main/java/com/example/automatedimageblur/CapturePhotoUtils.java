package com.example.automatedimageblur;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.OutputStream;

public class CapturePhotoUtils {
	public static String insertImage(ContentResolver cr,
	                                 Bitmap source,
	                                 String title,
	                                 String description) {

		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, title);
		values.put(MediaStore.Images.Media.DISPLAY_NAME, title);
		values.put(MediaStore.Images.Media.DESCRIPTION, description);
		values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
		// Add the date meta data to ensure the image is added at the front of the gallery
		values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
		values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

		Uri url = null;
		String stringUrl = null;    /* value to be returned */

		try {
			url = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

			if (source != null) {
				try (OutputStream imageOut = cr.openOutputStream(url)) {
					source.compress(Bitmap.CompressFormat.JPEG, 100, imageOut);
				}

				long id = ContentUris.parseId(url);
				Bitmap
						miniThumb =
						MediaStore.Images.Thumbnails.getThumbnail(cr,
								id,
								MediaStore.Images.Thumbnails.MINI_KIND,
								null
						);
			} else {
				cr.delete(url, null, null);
				url = null;
			}
		} catch (Exception e) {
			if (url != null) {
				cr.delete(url, null, null);
				url = null;
			}
		}

		if (url != null) {
			stringUrl = url.toString();
		}

		return stringUrl;
	}

}