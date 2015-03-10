package de.peculator.nachmacherx;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static de.peculator.nachmacherx.Utils.getRatio;

/**
 * Created by sven on 25.02.15.
 */
public class ImageViewer extends Activity {
    int currentCommand;
    private ImageSwitcher view;
    private Bitmap splitImageA;
    private Bitmap splitImageB;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        view = (ImageSwitcher) findViewById(R.id.myImageView);
        view.setFactory(new ViewSwitcher.ViewFactory() {

            @Override
            public View makeView() {
                ImageView myView = new ImageView(getApplicationContext());
                myView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                myView.setLayoutParams(new ImageSwitcher.LayoutParams(ActionBar.LayoutParams.
                        FILL_PARENT, ActionBar.LayoutParams.FILL_PARENT));
                return myView;
            }

        });

        currentCommand = getIntent().getIntExtra("path", 1);


        if (hasTwoImages()) {
            generateSplitBitmap();

            view.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
                @Override
                public void onSwipeLeft() {
                    nextImage();
                }

                @Override
                public void onSwipeRight() {
                    prevImage();
                }

                @Override
                public void onClick() {
                    if (currentCommand > 2) showStoringDialog();
                }

                @Override
                public void onSwipeUp() {
                    Log.i(MainActivity.TAG, "up");
                }

                @Override
                public void onSwipeDown() {
                    Log.i(MainActivity.TAG, "down");
                }

            });
        }

        currentCommand--;
        nextImage();
    }

    private void showStoringDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ImageViewer.this);
        builder.setTitle(getString(R.string.saveImage))
                .setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        storeImage();
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

    private void storeImage() {
        File pictureFile = CameraActivity.getOutputMediaFile(true);
        if (pictureFile == null) {
            Log.d(MainActivity.TAG, "Error creating media file, check storage permissions: ");
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            if (currentCommand == 3)
                splitImageA.compress(Bitmap.CompressFormat.PNG, 100, fos);
            else if (currentCommand == 4)
                splitImageB.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(MainActivity.TAG, "File not found: " + e.getMessage());
            return;
        } catch (IOException e) {
            Log.d(MainActivity.TAG, "Error accessing file: " + e.getMessage());
            return;
        }
        Toast.makeText(getApplicationContext(), getString(R.string.imageStored), Toast.LENGTH_SHORT).show();

        CameraActivity.addImageToGallery(pictureFile.getPath(), getApplicationContext());
        Toast.makeText(getApplicationContext(), getString(R.string.imageAdded), Toast.LENGTH_SHORT).show();
    }

    private void generateSplitBitmap() {

        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inSampleSize = 1;
        options.inJustDecodeBounds=false;

        Bitmap bitmapA = BitmapFactory.decodeFile(getImagePath(1), options);
        Bitmap bitmapB = BitmapFactory.decodeFile(getImagePath(2), options);

        splitImageA = combineImages(bitmapA, bitmapB);
        splitImageB = combineImages(bitmapB, bitmapA);
        Log.i(MainActivity.TAG,"combined");
    }

    public Bitmap combineImages(Bitmap c, Bitmap s) { // can add a 3rd parameter 'String loc' if you want to save the new image - left some code to do that at the bottom

        int width = c.getWidth() / 2 + s.getWidth() / 2;
        int height = (c.getHeight()>=s.getHeight())?c.getHeight():s.getHeight();


        Bitmap cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas comboImage = new Canvas(cs);

        //First image is smaller
        if(c.getHeight()<=s.getHeight()){
            int yOffset = (s.getHeight()-c.getHeight())/2;

            Rect r=new Rect(0,0,c.getWidth()/2,c.getHeight());
            Rect drawR=new Rect(0,yOffset,c.getWidth()/2,c.getHeight()+yOffset);

            comboImage.drawBitmap(c, r, drawR, null);

            Rect r_right=new Rect(s.getWidth()/2,0,s.getWidth(),s.getHeight());
            Rect drawR_right=new Rect(c.getWidth()/2, 0, s.getWidth()/2+c.getWidth()/2, s.getHeight());

            comboImage.drawBitmap(s, r_right,drawR_right, null);
        }
        //Second image is smaller
        else{
            int yOffset = (c.getHeight()-s.getHeight())/2;

            Rect r=new Rect(0,0,c.getWidth()/2,c.getHeight());
            Rect drawR=new Rect(0,0,c.getWidth()/2,c.getHeight());

            comboImage.drawBitmap(c, r, drawR, null);

            Rect r_right=new Rect(s.getWidth()/2,0,s.getWidth(),s.getHeight());
            Rect drawR_right=new Rect(c.getWidth()/2,yOffset,s.getWidth()/2+c.getWidth()/2,s.getHeight()+yOffset);

            comboImage.drawBitmap(s, r_right,drawR_right, null);
        }

        return cs;
    }

    private boolean hasTwoImages() {
        if (MainActivity.myPrefs.getLastURLResult() != "" && new File(MainActivity.myPrefs.getLastURLResult()).exists()
                && MainActivity.myPrefs.getLastURLSource() != "" && new File(MainActivity.myPrefs.getLastURLSource()).exists())
            return true;
        return false;
    }


    private Bitmap getBitmap() {


        Bitmap ops = null;
        Log.i(MainActivity.TAG,splitImageA.getWidth() + " " + (float)splitImageA.getHeight());

        if (currentCommand == 3 && splitImageA.getWidth()>0) return Bitmap.createScaledBitmap(splitImageA,1000,(int)(1000f/((float)splitImageA.getWidth()/(float)splitImageA.getHeight())),false);
        if (currentCommand == 4 && splitImageB.getWidth()>0) return Bitmap.createScaledBitmap(splitImageB,1000,(int)(1000f/((float)splitImageB.getWidth()/(float)splitImageB.getHeight())),false);

        String path = getImagePath(currentCommand);
        if (path != "") {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds=true;

            BitmapFactory.decodeFile(path, options);

            options.inSampleSize=Utils.calculateInSampleSize(options,view.getWidth(),view.getHeight());
            options.inJustDecodeBounds=false;

            ops = BitmapFactory.decodeFile(path, options);

        }
        return ops;
    }

    private String getImagePath(int command) {
        String path = "";
        switch (command) {
            case 1:
                path = MainActivity.myPrefs.getLastURLSource();
                break;
            case 2:
                path = MainActivity.myPrefs.getLastURLResult();
                break;
        }
        return path;
    }

    private void nextImage() {
        currentCommand = (currentCommand == 4) ? 1 : currentCommand + 1;
        Log.i(MainActivity.TAG, " " + currentCommand);

        doAnimation();
    }

    private void doAnimation() {
        Animation in = AnimationUtils.loadAnimation(this,
                android.R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(this,
                android.R.anim.fade_out);
        view.setInAnimation(in);
        view.setOutAnimation(out);

        Drawable drawable = new BitmapDrawable(getBitmap());
        view.setImageDrawable(drawable);
    }

    private void prevImage() {
        currentCommand = (currentCommand == 1) ? 4 : currentCommand - 1;
        Log.i(MainActivity.TAG, " " + currentCommand);

        doAnimation();
    }


}
