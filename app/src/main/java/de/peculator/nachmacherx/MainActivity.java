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
import android.os.AsyncTask;
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
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

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
    private static int inProcess = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myPrefs = new Preferences(getApplicationContext());
        myPrefs.loadLastPreferences();

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
        myPrefs.setLastURLSource("");
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
            openBrowser(0);
        }
        if (id == R.id.action_openBrowserWiki) {
            openBrowser(1);
        }


        return super.onOptionsItemSelected(item);
    }


    private void openBrowser(int mode) {

        String url = (mode == 0) ? "https://images.google.de" : "http://commons.wikimedia.org/wiki/Main_Page";

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

            if (id == R.id.action_openInstructions) {
                showInstructions();
            }


            return super.onOptionsItemSelected(item);
        }

        private void showInstructions() {

            String message = getString(R.string.stepOne) + getString(R.string.stepTwo) + getString(R.string.stepThree);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.settingsOpenInstructions)).setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).setMessage(message).show();


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
                        myBitmap = BitmapFactory.decodeFile(myLastImageURI.getPath(), options);
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
                        myBitmap = BitmapFactory.decodeFile(myLastImageURI.getPath(), options);
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
                        myBitmapResult = rotateImage(1, myBitmapResult, 0, true, myPrefs.getLastURLResult(), false);
                    } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        myBitmapResult = rotateImage(1, myBitmapResult, 270, true, myPrefs.getLastURLResult(), false);
                    }
                } else {
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        //myBitmapResult = rotateImage(myBitmapResult, 0, false, myPrefs.getLastURLResult(), false);
                    } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        myBitmapResult = rotateImage(1, myBitmapResult, 90, false, myPrefs.getLastURLResult(), false);
                    }
                }


                myImageViewResult.setImageBitmap(myBitmapResult);
            }

            changeLayout();
        }


        private void enableStart() {
            Button myStartButton = (Button) rootView.findViewById(R.id.buttonStart);
            myStartButton.setEnabled(true);
        }

        private void showModifyDialog(final int whichImage) {
            if (whichImage == 0 && myBitmap != null || whichImage == 1 && myBitmapResult != null) {
                final String[] items = new String[]{getResources().getString(R.string.rotateR),
                        getResources().getString(R.string.rotateL), getResources().getString(R.string.mirrorH),
                        getResources().getString(R.string.mirrorV),getResources().getString(R.string.openOtherApplication)};

                final Integer[] icons = new Integer[]{R.drawable.ic_action_rotate_right, R.drawable.ic_action_rotate_left,
                        R.drawable.android_flip, R.drawable.android_flip_v, R.drawable.abc_ic_menu_share_mtrl_alpha};
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
                                case 4:
                                    Intent editIntent = new Intent(Intent.ACTION_SEND);
                                    //editIntent.setDataAndType(Uri.parse(myPrefs.getLastURLSource()), "image/jpg");
                                    editIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    editIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(myPrefs.getLastURLSource()));
                                    editIntent.setType("image/jpg");
                                    startActivity(Intent.createChooser(editIntent, getResources().getString(R.string.sendImageTo)));
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
                                case 4:
                                    Intent editIntent = new Intent(Intent.ACTION_EDIT);
                                    editIntent.setDataAndType(Uri.parse(myPrefs.getLastURLResult()), "image/*");
                                    editIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    startActivity(Intent.createChooser(editIntent, null));
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
            } else

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

            myBitmap = rotateImage(0, myBitmap, 90, false, myPrefs.getLastURLSource(), special);

            myImageView.setImageBitmap(myBitmap);
            MainActivity.overlay = myBitmap;
        }

        public void rotateImageResult(boolean special) {
            myBitmapResult = rotateImage(1, myBitmapResult, 90, false, myPrefs.getLastURLResult(), special);

            myImageViewResult.setImageBitmap(myBitmapResult);
        }

        public void mirrorImageSource(boolean special) {
            myBitmap = rotateImage(0, myBitmap, 0, true, myPrefs.getLastURLSource(), special);

            myImageView.setImageBitmap(myBitmap);
            MainActivity.overlay = myBitmap;
        }

        public void mirrorImageResult(boolean special) {
            myBitmapResult = rotateImage(1, myBitmapResult, 0, true, myPrefs.getLastURLResult(), special);

            myImageViewResult.setImageBitmap(myBitmapResult);
        }

        public Bitmap rotateImage(int num, Bitmap bitmap, float value, boolean isMirrored, String path, boolean special) {
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
                    StoreBitmapTask async = new StoreBitmapTask(matrix, path, num);
                    MainActivity.inProcess++;
                    disableButtons();
                    async.execute();
                }
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.noImageFound), Toast.LENGTH_SHORT).show();
            }

            return bitmap;
        }

        private void disableButtons() {
            Button myFirstImagePlayButton = (Button) rootView.findViewById(R.id.playOne);
            Button myFirstImagePrefsButton = (Button) rootView.findViewById(R.id.prefOne);

            Button mySecondImagePlayButton = (Button) rootView.findViewById(R.id.playTwo);
            Button mySecondImagePrefsButton = (Button) rootView.findViewById(R.id.prefTwo);

            myFirstImagePlayButton.setEnabled(false);
            myFirstImagePrefsButton.setEnabled(false);

            mySecondImagePlayButton.setEnabled(false);
            mySecondImagePrefsButton.setEnabled(false);

            myFirstImagePlayButton.setAlpha(.4f);
            myFirstImagePrefsButton.setAlpha(.4f);

            mySecondImagePlayButton.setAlpha(.4f);
            mySecondImagePrefsButton.setAlpha(.4f);

        }

        private void enableButtons() {
            Button myFirstImagePlayButton = (Button) rootView.findViewById(R.id.playOne);
            Button myFirstImagePrefsButton = (Button) rootView.findViewById(R.id.prefOne);

            Button mySecondImagePlayButton = (Button) rootView.findViewById(R.id.playTwo);
            Button mySecondImagePrefsButton = (Button) rootView.findViewById(R.id.prefTwo);

            myFirstImagePlayButton.setEnabled(true);
            myFirstImagePrefsButton.setEnabled(true);

            mySecondImagePlayButton.setEnabled(true);
            mySecondImagePrefsButton.setEnabled(true);

            myFirstImagePlayButton.setAlpha(1f);
            myFirstImagePrefsButton.setAlpha(1f);

            mySecondImagePlayButton.setAlpha(1f);
            mySecondImagePrefsButton.setAlpha(1f);
        }

        private class StoreBitmapTask extends AsyncTask<Void, Integer, Void> {
            private final ProgressBar pb;
            Matrix matrix;
            String path;
            int num;

            private StoreBitmapTask(Matrix matrix, String path, int num) {
                this.matrix = matrix;
                this.path = path;
                this.num = num;

                int which = (num == 0) ? R.id.progressBarOne : R.id.progressBarTwo;
                pb = (ProgressBar) rootView.findViewById(which);
                pb.setProgress(1);
                pb.setVisibility(View.VISIBLE);
            }


            @Override
            protected Void doInBackground(Void... params) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                Bitmap tmp = BitmapFactory.decodeFile(path, options);

                // return new bitmap rotated using matrix
                tmp = Bitmap.createBitmap(tmp, 0, 0, tmp.getWidth(), tmp.getHeight(),
                        matrix, true);

                File pictureFile = new File(path);
                if (!pictureFile.exists()) {
                    Log.d(MainActivity.TAG, "Error creating media file ");
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
                return null;
            }


            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                MainActivity.inProcess--;
                Log.d(MainActivity.TAG, "Finished storing " + MainActivity.inProcess);
                Toast.makeText(getActivity(), getResources().getString(R.string.imageStored), Toast.LENGTH_SHORT).show();
                pb.setProgress(0);
                pb.setVisibility(View.INVISIBLE);
                enableButtons();

            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            setHasOptionsMenu(true);
            rootView = inflater.inflate(R.layout.fragment_main, container, false);

            // check orientation and change it
            changeLayout();
            myImageView = (ImageView) rootView.findViewById(R.id.imageView);
            myImageViewResult = (ImageView) rootView.findViewById(R.id.newImageView);

            Button myFirstImagePlayButton = (Button) rootView.findViewById(R.id.playOne);
            Button myFirstImagePrefsButton = (Button) rootView.findViewById(R.id.prefOne);

            myFirstImagePlayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (myBitmap != null) {
                        if (MainActivity.inProcess == 0) {
                            startViewer(1);

                        } else {
                            Toast.makeText(getActivity(), getString(R.string.inprocess) + ":" + MainActivity.inProcess, Toast.LENGTH_LONG).show();
                        }
                    }
                }


            });

            myFirstImagePlayButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(getActivity(), getString(R.string.play), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

            myImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (myBitmap != null) {
                        if (MainActivity.inProcess == 0) {
                            startViewer(1);

                        } else {
                            Toast.makeText(getActivity(), getString(R.string.inprocess) + ":" + MainActivity.inProcess, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });

            myImageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (myBitmap != null) {
                        if (MainActivity.inProcess == 0) {
                            showModifyDialog(0);
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.inprocess) + ":" + MainActivity.inProcess, Toast.LENGTH_LONG).show();
                        }
                    }
                    return true;
                }
            });

            myFirstImagePrefsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (myBitmap != null) {
                        if (MainActivity.inProcess == 0) {
                            showModifyDialog(0);
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.inprocess) + ":" + MainActivity.inProcess, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });

            myFirstImagePrefsButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(getActivity(), getString(R.string.modifyImage), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

            Button mySecondImagePlayButton = (Button) rootView.findViewById(R.id.playTwo);
            Button mySecondImagePrefsButton = (Button) rootView.findViewById(R.id.prefTwo);

            mySecondImagePlayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!myPrefs.getLastURLResult().equals("") && myBitmapResult != null) {
                        if (MainActivity.inProcess == 0) {
                            startViewer(2);
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.inprocess) + ":" + MainActivity.inProcess, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
            mySecondImagePlayButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(getActivity(), getString(R.string.play), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

            myImageViewResult.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (myBitmapResult != null && !myPrefs.getLastURLResult().equals("") ) {
                        if (MainActivity.inProcess == 0) {
                            startViewer(2);

                        } else {
                            Toast.makeText(getActivity(), getString(R.string.inprocess) + ":" + MainActivity.inProcess, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });

            myImageViewResult.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (myBitmapResult != null) {
                        if (MainActivity.inProcess == 0) {
                            showModifyDialog(1);
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.inprocess) + ":" + MainActivity.inProcess, Toast.LENGTH_LONG).show();
                        }
                    }
                    return true;
                }
            });

            mySecondImagePrefsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (myBitmapResult != null) {
                        if (MainActivity.inProcess == 0) {
                            showModifyDialog(1);
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.inprocess) + ":" + MainActivity.inProcess, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });

            mySecondImagePrefsButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(getActivity(), getString(R.string.modifyImage), Toast.LENGTH_SHORT).show();
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

                if (myBitmapResult != null && myBitmap != null)
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
                        if (new File(myPrefs.getLastURLResult()).exists() && new File(myPrefs.getLastURLSource()).exists()) {
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

        private void startViewer(int k) {
            Intent i = new Intent(getActivity(), ImageViewer.class);
            i.putExtra("path", k);
            startActivity(i);
        }
    }
}
