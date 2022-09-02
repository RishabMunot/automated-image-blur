package com.example.automatedimageblur;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    int PICK_IMAGE_MULTIPLE = 1;
    String imageEncoded;
    List<String> imagesEncodedList;
    LoadedImagesAdapter adapterLoading;
    ArrayList<Uri> images;
    boolean toLoad = true;
    Button processImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermission();
        images = new ArrayList<>();
        adapterLoading = new LoadedImagesAdapter(this, images);
        processImageButton = findViewById(R.id.processButton);
        GridView gridView = findViewById(R.id.loaded_images);
        gridView.setAdapter(adapterLoading);
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                images.remove(i);
                toLoad = false;
                adapterLoading.notifyDataSetChanged();
                if (images.size() == 0) hideProcessButton();
                return true;
            }
        });

    }

    public void getPermission()
    {
        ArrayList<String> arrPerm = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            arrPerm.add(Manifest.permission.READ_PHONE_STATE);
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            arrPerm.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(!arrPerm.isEmpty()) {
            String[] permissions = new String[arrPerm.size()];
            permissions = arrPerm.toArray(permissions);
            ActivityCompat.requestPermissions(this, permissions, 33);
        }
    }

    public void handleAddImages(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        toLoad = true;
        startActivityForResult(Intent.createChooser(intent, "Select Screenshots"), PICK_IMAGE_MULTIPLE);
    }

    public void handleClearImages(View view) {
        images.clear();
        adapterLoading.notifyDataSetChanged();
        hideProcessButton();
    }

    public void showProcessButton() {
        // Prepare the View for the animation
        processImageButton.setVisibility(View.VISIBLE);
        processImageButton.setAlpha(0.0f);
        processImageButton.animate()
                          .translationY(-234)
                          .alpha(1.0f)
                          .setListener(null);
    }

    public void hideProcessButton() {
        processImageButton.animate()
                          .translationY(0)
                          .alpha(0.0f)
                          .setListener(new AnimatorListenerAdapter() {
                              @Override
                              public void onAnimationEnd(Animator animation) {
                                  super.onAnimationEnd(animation);
                                  processImageButton.setVisibility(View.GONE);
                              }
                          });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            // When an Image is picked
            if (requestCode == PICK_IMAGE_MULTIPLE && resultCode == RESULT_OK
                    && null != data) {
                // Get the Image from data

                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                imagesEncodedList = new ArrayList<String>();
                if (data.getData() != null) {

                    Uri mImageUri = data.getData();
                    images.add(mImageUri);
                    adapterLoading.notifyDataSetChanged();

                    // Get the cursor
                    Cursor cursor = getContentResolver().query(mImageUri,
                            filePathColumn, null, null, null);
                    // Move to first row
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    imageEncoded = cursor.getString(columnIndex);
                    cursor.close();

                } else {
                    if (data.getClipData() != null) {
                        ClipData mClipData = data.getClipData();
                        for (int i = 0; i < mClipData.getItemCount(); i++) {

                            ClipData.Item item = mClipData.getItemAt(i);
                            Uri uri = item.getUri();
                            images.add(uri);
                            Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
                            cursor.moveToFirst();

                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            imageEncoded = cursor.getString(columnIndex);
                            imagesEncodedList.add(imageEncoded);
                            cursor.close();

                        }
                        adapterLoading.notifyDataSetChanged();

                    }
                }
                showProcessButton();

            } else {
                Toast.makeText(this, "You haven't picked Image",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void handleProcess(View view) {
        Intent i = new Intent(MainActivity.this,ProcessedImagesActivity.class);
        i.putParcelableArrayListExtra("images",images);
        startActivity(i);
    }

    class LoadedImagesAdapter extends BaseAdapter {

        ArrayList<Uri> images;
        Context context;

        LoadedImagesAdapter(Context context, ArrayList<Uri> images) {
            this.context = context;
            this.images = images;
        }

        @Override
        public int getCount() {
            return images.size();
        }

        @Override
        public Object getItem(int i) {
            return images.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
                        (long) ((new Random().nextFloat()) * 500));
            } else {
                progressBar.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
            }
//            if (i == images.size()) toLoad = !toLoad;
            imageView.setImageURI(images.get(i));
            return view;
        }
    }

}