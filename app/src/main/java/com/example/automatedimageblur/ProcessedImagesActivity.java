package com.example.automatedimageblur;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.jackandphantom.blurimage.BlurImage;
import com.ramijemli.percentagechartview.PercentageChartView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import br.com.simplepass.loadingbutton.customViews.CircularProgressButton;

public class ProcessedImagesActivity extends AppCompatActivity {

	ArrayList<Bitmap> imageBitmaps;
	boolean toLoad = true;
	ProcessedImagesAdapter imagesAdapter;
	ImageView editImageView;
	CircularProgressButton exportButton;
	ImageButton undoButton, doneButton;
	RelativeLayout popUp;
	boolean isPopUpVisible = false;
	ArrayList<Uri> imagesPath = new ArrayList<>();

	ArrayList<Rect> editRectangles = new ArrayList<>();

	Paint rectPaint;
	int rectInitX, rectInitY, rectFinalX, rectFinalY;
	float scaleX, scaleY;
	Bitmap editedBitmap;
	DialogInterface.OnClickListener dialogClickListener;

	RelativeLayout progress;
	TextView progress_text;
	Button start_process;

	@SuppressLint("ClickableViewAccessibility")
	@RequiresApi(api = Build.VERSION_CODES.N)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_processed_images);
		exportButton = findViewById(R.id.exportButton);
		progress = findViewById(R.id.progress);
		start_process = findViewById(R.id.start_process);
		progress_text = findViewById(R.id.progress_text);
		rectPaint = new Paint();
		rectPaint.setColor(Color.RED);
		rectPaint.setStyle(Paint.Style.STROKE);
		rectPaint.setStrokeWidth(4);

		popUp = findViewById(R.id.editImagePopUp);
		editImageView = findViewById(R.id.editImageImageView);
		undoButton = findViewById(R.id.buttonPanelUndo);
		doneButton = findViewById(R.id.buttonPanelDone);

		editRectangles.add(new Rect(0, 0, 0, 0));

		imageBitmaps = new ArrayList<>();
		getPermission();

		start_process.setOnClickListener(view -> {
			loadBitmaps();
		});

		dialogClickListener = (dialog, which) -> {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					saveEditedImage(null);
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					//No button clicked
					break;
			}
		};

		imagesAdapter = new ProcessedImagesAdapter(this, imageBitmaps);
		GridView gridView = findViewById(R.id.processed_images);
		gridView.setAdapter(imagesAdapter);
		gridView.setOnItemLongClickListener((adapterView, view, i, l) -> {
			imageBitmaps.remove(i);
			toLoad = false;
			imagesAdapter.notifyDataSetChanged();
//        if (images.size() == 0) hideProcessButton();
			return true;
		});

		undoButton.setOnClickListener(this::undoEditedImage);

		editImageView.setOnTouchListener((view, motionEvent) -> {

			editImageView.getWidth();
			if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
				rectInitX = Math.round(motionEvent.getX() * scaleX);
				rectInitY = Math.round(motionEvent.getY() * scaleY);
			} else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
				if (Math.round((float) rectFinalX / 3) == Math.round(motionEvent.getX() * scaleX / 3) &&
				    Math.round((float) rectFinalY / 3) == Math.round(motionEvent.getY() * scaleY / 3))
					return true;
				rectFinalX = Math.round(motionEvent.getX() * scaleX);
				rectFinalY = Math.round(motionEvent.getY() * scaleY);
				editRectangles.remove(editRectangles.size() - 1);
				editRectangles.add(getRect());
				drawRectangle();

			} else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
				if (editRectangles.size() > 0) {
					undoButton.setAlpha(1f);
					undoButton.setEnabled(true);
				}
				editRectangles.add(new Rect(0, 0, 0, 0));
				drawRectangle();
				Log.d("TAG", "onTouch: " + editRectangles);
			}
			return true;
		});


		gridView.setOnItemClickListener((adapterView, view, i, l) -> {
			editImageView.setTag(i);
			editedBitmap = imageBitmaps.get(i).copy(Bitmap.Config.ARGB_8888, true);
			editImageView.setImageBitmap(editedBitmap);
			showEditImagePopUp();
			undoButton.setAlpha(0.5f);
			undoButton.setEnabled(false);
		});
		gridView.setEmptyView(findViewById(R.id.empty));
	}

	Rect getRect() {
		return new Rect(Math.min(rectInitX, rectFinalX),
				Math.min(rectInitY, rectFinalY),
				Math.max(rectInitX, rectFinalX),
				Math.max(rectInitY, rectFinalY)
		);
	}


	public void loadBitmaps() {
		ArrayList<Uri> images = getIntent().getParcelableArrayListExtra("images");
		for (Uri image : images) {
			imageBitmaps.add(processImage(image));
		}
		imagesAdapter.notifyDataSetChanged();
		progress.setVisibility(View.INVISIBLE);
	}

	public void hideEditImagePopUp(View v) {

		if (editRectangles.size() > 1) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("There are unsaved changes. Are you sure?")
			       .setPositiveButton("Yes", dialogClickListener)
			       .setNegativeButton("No", dialogClickListener)
			       .show();
			return;
		}
		toLoad = false;

		popUp.setAlpha(1);
		popUp.animate().alpha(0).setDuration(80);
		popUp.setVisibility(View.GONE);
		isPopUpVisible = false;
		editRectangles.clear();
		editRectangles.add(new Rect(0, 0, 0, 0));
	}

	public void showEditImagePopUp() {
		popUp.setVisibility(View.VISIBLE);
		popUp.setAlpha(0);
		popUp.animate().alpha(1).setDuration(80).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				int i = (int) editImageView.getTag();
				scaleX = ((float) imageBitmaps.get(i).getWidth()) / ((float) editImageView.getWidth());
				scaleY = ((float) imageBitmaps.get(i).getHeight()) / ((float) editImageView.getHeight());
			}
		});
		isPopUpVisible = true;
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

	public void drawRectangle() {
		Bitmap bitmap = editedBitmap.copy(Bitmap.Config.ARGB_8888, true);
		Canvas c = new Canvas(bitmap);
		for (Rect rect : editRectangles) {
			c.drawRect(rect, rectPaint);
		}
		editImageView.setImageBitmap(bitmap);
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
		Log.d("TAG", "exportImages: " + "button clicked");
		exportButton.startAnimation();
		ArrayList<Bitmap> tempBitmaps = new ArrayList<>(imageBitmaps);
		toLoad = false;
		for (Bitmap bitmap : tempBitmaps) {
			new android.os.Handler(Looper.getMainLooper()).postDelayed(
					() -> {
						String timeStamp = new SimpleDateFormat("yyMMdd_HHmmssSSS").format(new Date());
						String mImageName = "AIB_" + timeStamp + ".jpg";
						imagesPath.add(Uri.parse(CapturePhotoUtils.insertImage(getContentResolver(), bitmap, mImageName, mImageName)));
						imageBitmaps.remove(bitmap);
						imagesAdapter.notifyDataSetChanged();
						if (imageBitmaps.isEmpty())
							exportButton.doneLoadingAnimation(ContextCompat.getColor(ProcessedImagesActivity.this,
											R.color.teal_200
									),
									getBitmapFromVectorDrawable(ProcessedImagesActivity.this,
											R.drawable.ic_baseline_share_24
									)
							);

					},
					(long) ((new Random().nextFloat()) * 3000)
			);
		}
	}

	@Override
	public void onBackPressed() {
		if (isPopUpVisible) {
			hideEditImagePopUp(null);
		} else {
			super.onBackPressed();
		}
	}

	public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
		Drawable drawable = ContextCompat.getDrawable(context, drawableId);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			drawable = (DrawableCompat.wrap(drawable)).mutate();
		}

		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888
		);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}

	public void saveEditedImage(View view) {

		Bitmap imageBitmap = imageBitmaps.get((int) editImageView.getTag());
		imageBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true);

		try {
			Canvas canvas = new Canvas(imageBitmap);
			Paint p = new Paint();
			p.setColor(Color.argb(255, 243, 140, 73));
			p.setMaskFilter(new BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL));
			Log.d("TAG", "saveEditedImage: " + editRectangles.size());

			for (Rect rect : editRectangles) {
				if (rect.width() == 0 || rect.height() == 0)
					continue;
				Log.d("TAG", "saveEditedImage: " + rect);
				Bitmap tempImgBmp = imageBitmap.copy(Bitmap.Config.ARGB_8888, true);
				Bitmap blurredBitmap = BlurImage.with(getApplicationContext())
				                                .load(Bitmap.createBitmap(
						                                tempImgBmp,
						                                rect.left,
						                                rect.top,
						                                rect.width(),
						                                rect.height()
				                                ))
				                                .intensity(10)
				                                .Async(false)
				                                .getImageBlur();
				canvas.drawBitmap(Bitmap.createScaledBitmap(blurredBitmap,
								rect.width(),
								rect.height(),
								false
						), rect.left, rect.top, p
				);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		editImageView.setImageBitmap(imageBitmap);
		imageBitmaps.set((int) editImageView.getTag(), imageBitmap);
		imagesAdapter.notifyDataSetChanged();
		editRectangles.clear();
		editRectangles.add(new Rect(0, 0, 0, 0));
		hideEditImagePopUp(null);
	}

	public void undoEditedImage(View view) {
		editRectangles.remove(editRectangles.size() - 2);

		if (editRectangles.size() < 2) {
			view.setEnabled(false);
			view.setAlpha(0.5f);
		}
		drawRectangle();
	}

	public void shareImages(View view) {
		Log.d("TAG", "shareImages: "+imagesPath);
		Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
		i.setType("image/*");
		i.putParcelableArrayListExtra(Intent.EXTRA_STREAM,imagesPath);
		startActivity(Intent.createChooser(i,"Share"));
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
			progressBar.setVisibility(View.GONE);
			imageView.setVisibility(View.VISIBLE);

			imageView.setImageBitmap(bitmaps.get(i));
			return view;
		}
	}


}