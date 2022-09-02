package com.example.automatedimageblur;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.jackandphantom.blurimage.BlurImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

public class ProcessedImagesActivity extends AppCompatActivity {

	ArrayList<Bitmap> imageBitmaps;
	boolean toLoad = true;
	ProcessedImagesAdapter imagesAdapter;
	ImageView clickedImage;
	Button exportButton;
	RelativeLayout popUp;
	float finalX;
	float finalY;
	int finalWidth;
	int finalHeight;

	@RequiresApi(api = Build.VERSION_CODES.N)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_processed_images);
		exportButton = findViewById(R.id.exportButton);
		imageBitmaps = new ArrayList<>();
		loadBitmaps();
		getPermission();

		imagesAdapter = new ProcessedImagesAdapter(this, imageBitmaps);
		GridView gridView = findViewById(R.id.processed_images);
		gridView.setAdapter(imagesAdapter);
		gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
				imageBitmaps.remove(i);
				toLoad = false;
				imagesAdapter.notifyDataSetChanged();
//        if (images.size() == 0) hideProcessButton();
				return true;
			}
		});
//		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//			@Override
//			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//				clickedImage.setX(view.getX());
//				clickedImage.setY(view.getY());
//				clickedImage.setLayoutParams(new RelativeLayout.LayoutParams(view.getWidth(),
//						view.getHeight()
//				));
//				clickedImage.setImageBitmap(imageBitmaps.get(i));
//				Log.d("joij", "onItemClick: "+finalX+finalY+finalWidth+finalHeight);
//				clickedImage.animate()
//				            .translationX(finalX)
//				            .translationY(finalY)
//				            .scaleXBy(finalWidth / (float) view.getWidth())
//				            .scaleYBy(finalHeight / (float) view.getHeight())
//				            .setDuration(1000)
//				            .start();
//
//			}
//		});
	}

	@RequiresApi(api = Build.VERSION_CODES.N)
	public void loadBitmaps() {
		ArrayList<Uri> images = getIntent().getParcelableArrayListExtra("images");
		images.forEach((image) -> {
			imageBitmaps.add(processImage(image));
		});
	}

	public void hidePopUp() {
	}

	public void showPopUp() {
	}

	private File getOutputMediaFile() {

		File dir = new File(String.valueOf(Environment.getExternalStorageDirectory())+File.separator+"AutoScreenshots");
		if (!dir.exists()) {
			dir.mkdir();
			if (dir.isDirectory())
				Toast.makeText(ProcessedImagesActivity.this,
						"Folder Created Successfully",
						Toast.LENGTH_SHORT
				).show();
		}

		String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmSS").format(new Date());
		File mediaFile;
		String mImageName = "AIB_" + timeStamp + ".jpg";
		mediaFile = new File(dir, mImageName);
		return mediaFile;
	}

	public void getPermission() {
		ArrayList<String> arrPerm = new ArrayList<>();
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) !=
		    PackageManager.PERMISSION_GRANTED) {
			arrPerm.add(Manifest.permission.READ_PHONE_STATE);
		}
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
		    PackageManager.PERMISSION_GRANTED) {
			arrPerm.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}
		if (!arrPerm.isEmpty()) {
			String[] permissions = new String[arrPerm.size()];
			permissions = arrPerm.toArray(permissions);
			ActivityCompat.requestPermissions(this, permissions, 33);
		}
	}

	public Bitmap processImage(Uri imageUri) {
		Bitmap imageBitmap = null;
		try {
			imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
			imageBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true);
			float scaleY = (float) imageBitmap.getHeight() / 2340;
			float scaleX = (float) imageBitmap.getWidth() / 1080;
			int left = (int) (scaleX * 350);
			int top = (int) (scaleY * 120);
			int width = (int) (scaleX * 450);
			int height = (int) (scaleY * 80);


			Bitmap blurredBitmap = BlurImage.with(getApplicationContext())
			                                .load(Bitmap.createBitmap(
					                                imageBitmap, left, top, width, height
			                                ))
			                                .intensity(10)
			                                .Async(false)
			                                .getImageBlur();

			Canvas canvas = new Canvas(imageBitmap);
			Paint p = new Paint();
			p.setColor(Color.argb(255, 243, 140, 73));
			p.setMaskFilter(new BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL));
			canvas.drawBitmap(Bitmap.createScaledBitmap(blurredBitmap, width, height, false),
					left,
					top,
					p
			);
			return imageBitmap;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void exportImages(View view) {

		for(Bitmap bitmap : imageBitmaps) {
			String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmSS").format(new Date());
			String mImageName = "AIB_" + timeStamp + ".jpg";
			CapturePhotoUtils.insertImage(getContentResolver(), bitmap, mImageName, mImageName);
		}
	}

	class ProcessedImagesAdapter extends BaseAdapter {
		ArrayList<Bitmap> bitmaps;
		Context context;

		ProcessedImagesAdapter(Context context, ArrayList<Bitmap> bitmaps) {
			this.context = context;
			this.bitmaps = bitmaps;
		}

		@Override
		public int getCount() {
			return bitmaps.size();
		}

		@Override
		public Object getItem(int i) {
			return bitmaps.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			if (view == null) {
				LayoutInflater
						inflater =
						(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.images_list_item, null);
			}
			ImageView imageView = view.findViewById(R.id.image);
			imageView.setVisibility(View.INVISIBLE);
			ProgressBar progressBar = view.findViewById(R.id.image_progress_bar);

			if (toLoad) {
				new android.os.Handler(Looper.getMainLooper()).postDelayed(
						new Runnable() {
							public void run() {
								progressBar.setVisibility(View.GONE);
								imageView.setVisibility(View.VISIBLE);
							}
						},
						(long) ((new Random().nextFloat()) * 1500)
				);
			} else {
				progressBar.setVisibility(View.GONE);
				imageView.setVisibility(View.VISIBLE);
			}
			imageView.setImageBitmap(bitmaps.get(i));
			return view;
		}
	}


}