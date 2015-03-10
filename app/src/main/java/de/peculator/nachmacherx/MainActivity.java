package de.peculator.nachmacherx;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by peculator
 */

public class MainActivity extends FragmentActivity {

    public static Preferences myPrefs;
    private static int RESULT_LOAD_IMAGE = 666;
    private static int REQUEST_CAMERA_IMAGE = 999;
    private static int REQUEST_IMAGE_CAPTURE = 333;
    public static String TAG = "NACHMACHER_X";
    public static Bitmap overlay;
    public static Uri myLastImageURI = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myPrefs = new Preferences(getApplicationContext());
        myPrefs.loadLastPreferences();
        Log.i(TAG, myPrefs.toString());

        //set default Settings
        //setDefaultSettings();


        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    private void setDefaultSettings() {
        myPrefs.setLastURLResult("");
        //myPrefs.setLastURLSource("");
        //myPrefs.setLastAlpha(0);
        myPrefs.storePreferences();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        if (id == R.id.action_open) {
            openGallery();
        }
        if (id == R.id.action_openBrowser) {
            openBrowser();
        }

        return super.onOptionsItemSelected(item);
    }

    private void openBrowser() {

        String url = "https://images.google.de";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setType("image/*");
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);

        Uri uri = Uri.parse(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath());


        intent.setData(uri);
        intent.setType("image/*");
        startActivity(intent);

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        View rootView;
        private Bitmap myBitmap;
        private Bitmap myBitmapResult;
        private ImageView myImageView;
        private ImageView myImageViewResult;

        public PlaceholderFragment() {
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();

            if (id == R.id.action_rotate) {
                rotateImageSource(false);
            }
            if (id == R.id.action_rotate_result) {
                rotateImageResult(false);
            }
            if (id == R.id.action_mirror) {
                mirrorImageSource(false);
            }
            if (id == R.id.action_mirror_result) {
                mirrorImageResult(false);
            }


            return super.onOptionsItemSelected(item);
        }


        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            LinearLayout ll = (LinearLayout) getActivity().findViewById(R.id.myFotoBox);
            // Checks the orientation of the screen
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                ll.setOrientation(LinearLayout.HORIZONTAL);
                ll.setGravity(Gravity.CENTER);
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                ll.setOrientation(LinearLayout.VERTICAL);
            }

        }

        public void changeLayout() {
            LinearLayout ll = (LinearLayout) rootView.findViewById(R.id.myFotoBox);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                ll.setOrientation(LinearLayout.HORIZONTAL);
                ll.setGravity(Gravity.CENTER);
            } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                ll.setOrientation(LinearLayout.VERTICAL);
            }

        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putParcelable("myBit", myBitmap);
            outState.putParcelable("myBitRes", myBitmapResult);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            Log.d(MainActivity.TAG, "On Activity Result");
            changeLayout();

            if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getActivity().getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                myImageView = (ImageView) rootView.findViewById(R.id.imageView);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                myBitmap = BitmapFactory.decodeFile(picturePath, options);

                options.inSampleSize = Utils.calculateInSampleSize(options, myImageView.getWidth(), myImageView.getHeight());
                options.inJustDecodeBounds = false;
                myBitmap = BitmapFactory.decodeFile(picturePath, options);

                myImageView.setImageBitmap(myBitmap);


                myPrefs.setLastURLSource(picturePath);
                myPrefs.setLastURLResult("");
                switchStartTextBack();

                myBitmapResult = null;

                myImageViewResult.setImageBitmap(null);
                myPrefs.storePreferences();

                MainActivity.overlay = myBitmap;
                enableStart();

            } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

                myImageView = (ImageView) rootView.findViewById(R.id.imageView);


                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;


                try {
                    if (Utils.hasImageCaptureBug()) {
                        myBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), Uri.fromFile(new File("/sdcard/tmp")));
                    } else {
                        myBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), myLastImageURI);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                options.inSampleSize = Utils.calculateInSampleSize(options, myImageView.getWidth(), myImageView.getHeight());
                options.inJustDecodeBounds = false;
                try {
                    if (Utils.hasImageCaptureBug()) {
                        myBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), Uri.fromFile(new File("/sdcard/tmp")));
                    } else {
                        myBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), myLastImageURI);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                myImageView.setImageBitmap(myBitmap);


                CameraActivity.addImageToGallery(myLastImageURI.getPath(), this.getActivity());

                myPrefs.setLastURLSource(myLastImageURI.getPath());
                myPrefs.setLastURLResult("");
                switchStartTextBack();

                myBitmapResult = null;

                myImageViewResult.setImageBitmap(null);
                myPrefs.storePreferences();

                MainActivity.overlay = myBitmap;
                enableStart();

            } else if (requestCode == REQUEST_CAMERA_IMAGE && resultCode == RESULT_OK && null != data) {
                String imgPath = data.getStringExtra(Preferences.IMAGE_PATH);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;


                myPrefs.setLastURLResult(imgPath);
                switchStartText();
                myPrefs.storePreferences();

                myBitmapResult = BitmapFactory.decodeFile(imgPath, options);

                options.inSampleSize = Utils.calculateInSampleSize(options, myImageView.getWidth(), myImageView.getHeight());
                options.inJustDecodeBounds = false;

                myBitmapResult = BitmapFactory.decodeFile(imgPath, options);

                if (myPrefs.isFrontCamera()) {
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        myBitmapResult = rotateImage(myBitmapResult, 0, true, myPrefs.getLastURLResult(), false);
                    } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        myBitmapResult = rotateImage(myBitmapResult, 270, true, myPrefs.getLastURLResult(), false);
                    }
                } else {
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        myBitmapResult = rotateImage(myBitmapResult, 0, false, myPrefs.getLastURLResult(), false);
                    } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        myBitmapResult = rotateImage(myBitmapResult, 90, false, myPrefs.getLastURLResult(), false);
                    }
                }


                myImageViewResult.setImageBitmap(myBitmapResult);
            }

            Log.d(MainActivity.TAG, "On Activity Result finished");
            changeLayout();
        }


        private void enableStart() {
            Button myStartButton = (Button) rootView.findViewById(R.id.buttonStart);
            myStartButton.setEnabled(true);
        }

        private void showStoringDialog(final int whichImage) {
            if (whichImage == 0 && myBitmap != null || whichImage == 1 && myBitmapResult != null) {
                final String[] items = new String[]{getResources().getString(R.string.rotateR),
                        getResources().getString(R.string.rotateL), getResources(). getString(R.string.mirrorH),
                        getResources().getString(R.string.mirrorV)};

                final Integer[] icons = new Integer[]{R.drawable.ic_launcher, R.drawable.ic_launcher,R.drawable.ic_launcher,R.drawable.ic_launcher};
                ListAdapter adapter = new ArrayAdapterWithIcon(getActivity(), items, icons);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getString(R.string.modifyImage)).setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (whichImage == 0)
                            switch (which) {
                                case 0:
                                    rotateImageSource(false);
                                    break;
                                case 1:
                                    rotateImageSource(true);
                                    break;
                                case 2:
                                    mirrorImageSource(false);
                                    break;
                                case 3:
                                    mirrorImageSource(true);
                                    break;
                            }
                        else {
                            switch (which) {
                                case 0:
                                    rotateImageResult(false);
                                    break;
                                case 1:
                                    rotateImageResult(true);
                                    break;
                                case 2:
                                    mirrorImageResult(false);
                                    break;
                                case 3:
                                    mirrorImageResult(true);
                                    break;
                            }
                        }
                        dialog.dismiss();
                    }
                })

            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            builder.create();
            builder.show();
        }

        else

        {
            Toast.makeText(getActivity(), getResources().getString(R.string.noImageFound), Toast.LENGTH_SHORT).show();
        }
    }

    private void switchStartText() {
        Button myStartButton = (Button) rootView.findViewById(R.id.buttonStart);
        myStartButton.setText(getResources().getString(R.string.startAgainText));
    }

    private void switchStartTextBack() {
        Button myStartButton = (Button) rootView.findViewById(R.id.buttonStart);
        myStartButton.setText(getResources().getString(R.string.startText));
    }


    public void rotateImageSource(boolean special) {

        myBitmap = rotateImage(myBitmap, 90, false, myPrefs.getLastURLSource(), special);

        myImageView.setImageBitmap(myBitmap);
        MainActivity.overlay = myBitmap;
    }

    public void rotateImageResult(boolean special) {
        myBitmapResult = rotateImage(myBitmapResult, 90, false, myPrefs.getLastURLResult(), special);

        myImageViewResult.setImageBitmap(myBitmapResult);
    }

    public void mirrorImageSource(boolean special) {
        myBitmap = rotateImage(myBitmap, 0, true, myPrefs.getLastURLSource(), special);

        myImageView.setImageBitmap(myBitmap);
        MainActivity.overlay = myBitmap;
    }

    public void mirrorImageResult(boolean special) {
        myBitmapResult = rotateImage(myBitmapResult, 0, true, myPrefs.getLastURLResult(), special);

        myImageViewResult.setImageBitmap(myBitmapResult);
    }

    public Bitmap rotateImage(Bitmap bitmap, float value, boolean isMirrored, String path, boolean special) {
        if (bitmap != null) {
            // create new matrix object
            Matrix matrix = new Matrix();

            value = (special) ? (360 - value) : value;

            matrix.postRotate(value);

            if (isMirrored) {
                matrix.postScale(-1, 1);
                if (special) matrix.postScale(1, -1);
            }

            // return new bitmap rotated using matrix
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    matrix, true);

            if (path != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                Bitmap tmp = BitmapFactory.decodeFile(path, options);

                // return new bitmap rotated using matrix
                tmp = Bitmap.createBitmap(tmp, 0, 0, tmp.getWidth(), tmp.getHeight(),
                        matrix, true);

                File pictureFile = new File(path);
                if (pictureFile == null) {
                    Log.d(MainActivity.TAG, "Error creating media file, check storage permissions: ");
                    return null;
                }

                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    tmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();
                } catch (FileNotFoundException e) {
                    Log.d(MainActivity.TAG, "File not found: " + e.getMessage());
                    return null;
                } catch (IOException e) {
                    Log.d(MainActivity.TAG, "Error accessing file: " + e.getMessage());
                    return null;
                }
                CameraActivity.addImageToGallery(path, getActivity());
            }
        } else {
            Toast.makeText(getActivity(), getResources().getString(R.string.noImageFound), Toast.LENGTH_SHORT).show();
        }

        return bitmap;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // check orientation and change it
        changeLayout();
        myImageView = (ImageView) rootView.findViewById(R.id.imageView);
        Log.i(TAG, myImageView.getWidth() + "");
        myImageViewResult = (ImageView) rootView.findViewById(R.id.newImageView);

        myImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myBitmap != null) {
                    Intent i = new Intent(getActivity(), ImageViewer.class);
                    i.putExtra("path", 1);
                    startActivity(i);
                }
            }
        });

        myImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showStoringDialog(0);
                return true;
            }
        });

        myImageViewResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myPrefs.getLastURLResult() != "") {
                    Intent i = new Intent(getActivity(), ImageViewer.class);
                    i.putExtra("path", 2);
                    startActivity(i);
                }
            }
        });

        myImageViewResult.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showStoringDialog(1);
                return true;
            }
        });


        if (savedInstanceState != null) {
            // Restore last state
            myBitmap = savedInstanceState.getParcelable("myBit");
            myBitmapResult = savedInstanceState.getParcelable("myBitRes");
            enableStart();

            if (myBitmap != null) {
                myImageView.setImageBitmap(myBitmap);
                MainActivity.overlay = myBitmap;
                enableStart();
            }

            if (myBitmapResult != null)
                myImageViewResult.setImageBitmap(myBitmapResult);
        } else {
            //Load last image
            if (myBitmap == null) {
                try {

                    if (new File(myPrefs.getLastURLSource()).exists()) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;

                        myBitmap = BitmapFactory.decodeFile(myPrefs.getLastURLSource(), options);

                        options.inJustDecodeBounds = false;
                        options.inSampleSize = Utils.calculateInSampleSize(options, myImageView.getWidth(), myImageView.getHeight());

                        myBitmap = BitmapFactory.decodeFile(myPrefs.getLastURLSource(), options);

                        myImageView.setImageBitmap(myBitmap);
                        MainActivity.overlay = myBitmap;
                        enableStart();
                    } else {
                        Log.e(TAG, "File does not exist anymore");
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                    myImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
                }
            }

            if (myBitmapResult == null) {
                try {
                    if (new File(myPrefs.getLastURLResult()).exists()) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;

                        myBitmapResult = BitmapFactory.decodeFile(myPrefs.getLastURLResult(), options);

                        options.inJustDecodeBounds = false;
                        options.inSampleSize = Utils.calculateInSampleSize(options, myImageViewResult.getWidth(), myImageViewResult.getHeight());
                        myBitmapResult = BitmapFactory.decodeFile(myPrefs.getLastURLResult(), options);

                        myImageViewResult.setImageBitmap(myBitmapResult);
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                    myImageViewResult.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
                }
            }
        }

        Button galleryButton = (Button) rootView.findViewById(R.id.buttonGallery);
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        Button takeImageButton = (Button) rootView.findViewById(R.id.buttonPhoto);
        takeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File photoFile = CameraActivity.getOutputMediaFile(false);

                myLastImageURI = Uri.fromFile(photoFile);
                if (Utils.hasImageCaptureBug()) {
                    takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File("/sdcard/tmp")));
                } else {
                    takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                }
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        });

        Button startButton = (Button) rootView.findViewById(R.id.buttonStart);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CameraActivity.class);
                startActivityForResult(intent, REQUEST_CAMERA_IMAGE);
            }
        });

        return rootView;
    }


}
}
