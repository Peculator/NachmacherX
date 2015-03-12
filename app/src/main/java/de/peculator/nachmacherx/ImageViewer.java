package de.peculator.nachmacherx;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
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

/**
 * Created by sven on 25.02.15.
 */
public class ImageViewer extends Activity {
    int currentCommand;
    private ImageSwitcher view;
    private Bitmap splitImageA;
    private Bitmap splitImageB;
    private Bitmap bitmapA;
    private Bitmap bitmapB;

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

        String path = getImagePath(1);
        if (path != "" && path != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(path, options);

            options.inSampleSize = Utils.calculateInSampleSize(options, view.getWidth(), view.getHeight());
            options.inJustDecodeBounds = false;
            options.inMutable = true;

            bitmapA = BitmapFactory.decodeFile(path, options);
        }

        path = getImagePath(2);
        if (path != "" && path != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(path, options);

            options.inSampleSize = Utils.calculateInSampleSize(options, view.getWidth(), view.getHeight());
            options.inJustDecodeBounds = false;
            options.inMutable = true;

            bitmapB = BitmapFactory.decodeFile(path, options);
        }


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
                    Log.d(MainActivity.TAG, "up");
                }

                @Override
                public void onSwipeDown() {
                    Log.d(MainActivity.TAG, "down");
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


        splitImageA = combineImages(bitmapA, bitmapB);
        splitImageB = combineImages(bitmapB, bitmapA);
    }

    public Bitmap combineImages(Bitmap c, Bitmap s) { // can add a 3rd parameter 'String loc' if you want to save the new image - left some code to do that at the bottom

        int yOffset = 0;


        //First image is smaller
        if (c.getHeight() <= s.getHeight()) {
            c = Bitmap.createScaledBitmap(c, (int) (c.getWidth() * (float) s.getHeight() / (float) c.getHeight()), s.getHeight(), false);
            yOffset = (s.getHeight() - c.getHeight()) / 2;
        }
        //Second image is smaller
        if (c.getHeight() > s.getHeight()) {
            s = Bitmap.createScaledBitmap(s, (int) (s.getWidth() * (float) c.getHeight() / (float) s.getHeight()), c.getHeight(), false);
            yOffset = (c.getHeight() - s.getHeight()) / 2;
        }

        int width = c.getWidth() / 2 + s.getWidth() / 2;
        int height = (c.getHeight() >= s.getHeight()) ? c.getHeight() : s.getHeight();
        int blendZone = width / 10;


        Bitmap cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas comboImage = new Canvas(cs);


        //First image is smaller
        if (c.getHeight() <= s.getHeight()) {

            Rect r = new Rect(0, 0, c.getWidth() / 2 - blendZone / 2, c.getHeight());
            Rect drawR = new Rect(0, yOffset, c.getWidth() / 2 - blendZone / 2, c.getHeight() + yOffset);

            comboImage.drawBitmap(c, r, drawR, null);

            Rect r_right = new Rect(s.getWidth() / 2 + blendZone / 2, 0, s.getWidth(), s.getHeight());
            Rect drawR_right = new Rect(c.getWidth() / 2 + blendZone / 2, 0, s.getWidth() / 2 + c.getWidth() / 2, s.getHeight());

            comboImage.drawBitmap(s, r_right, drawR_right, null);

        }
        //Second image is smaller
        else {

            Rect r = new Rect(0, 0, c.getWidth() / 2 - blendZone / 2, c.getHeight());
            Rect drawR = new Rect(0, 0, c.getWidth() / 2 - blendZone / 2, c.getHeight());

            comboImage.drawBitmap(c, r, drawR, null);

            Rect r_right = new Rect(s.getWidth() / 2 + blendZone / 2, 0, s.getWidth(), s.getHeight());
            Rect drawR_right = new Rect(c.getWidth() / 2 + blendZone / 2, yOffset, s.getWidth() / 2 + c.getWidth() / 2, s.getHeight() + yOffset);

            comboImage.drawBitmap(s, r_right, drawR_right, null);
        }

        // Blendzone
        Bitmap blend = Bitmap.createBitmap(blendZone, height, Bitmap.Config.ARGB_4444);

        for (int i = 0; i < blendZone; i++) {
            float ratio = 1-((float)i / (float)blendZone);

            for (int j = 0; j < height; j++) {
                int colorA = c.getPixel(i + c.getWidth() / 2 - blendZone / 2, j);
                int colorB = s.getPixel(i + s.getWidth() / 2 - blendZone / 2, j);

                int red = (int)(Color.red(colorA)*ratio + Color.red(colorB)*(1-ratio));
                int green = (int)(Color.green(colorA)*ratio + Color.green(colorB)*(1-ratio));
                int blue = (int)(Color.blue(colorA)*ratio + Color.blue(colorB)*(1-ratio));
                int alpha = (int)(Color.alpha(colorA)*ratio + Color.alpha(colorB)*(1-ratio));

                blend.setPixel(i, j, Color.argb(alpha, red, green, blue));
            }
        }

        comboImage.drawBitmap(blend, c.getWidth() / 2 - blendZone / 2, 0, null);

        Bitmap b = BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher);
        // Logo
        comboImage.drawBitmap(b, width-b.getWidth(), height-b.getHeight(), null);

        return cs;
    }

    private boolean hasTwoImages() {
        if (MainActivity.myPrefs.getLastURLResult() != "" && new File(MainActivity.myPrefs.getLastURLResult()).exists()
                && MainActivity.myPrefs.getLastURLSource() != "" && new File(MainActivity.myPrefs.getLastURLSource()).exists())
            return true;
        return false;
    }


    private Bitmap getBitmap() {


        if (currentCommand == 3 && splitImageA != null)
            return Bitmap.createScaledBitmap(splitImageA, 1000, (int) (1000f / ((float) splitImageA.getWidth() / (float) splitImageA.getHeight())), false);
        if (currentCommand == 4 && splitImageB != null)
            return Bitmap.createScaledBitmap(splitImageB, 1000, (int) (1000f / ((float) splitImageB.getWidth() / (float) splitImageB.getHeight())), false);

        if (currentCommand == 1 && bitmapA != null)
            return Bitmap.createScaledBitmap(bitmapA, 1000, (int) (1000f / ((float) bitmapA.getWidth() / (float) bitmapA.getHeight())), false);
        if (currentCommand == 2 && bitmapB != null)
            return Bitmap.createScaledBitmap(bitmapB, 1000, (int) (1000f / ((float) bitmapB.getWidth() / (float) bitmapB.getHeight())), false);

        return null;
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
        if (new File(path).exists())
            return path;
        else
            return null;
    }

    private void nextImage() {
        currentCommand = (currentCommand == 4) ? 1 : currentCommand + 1;

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

        doAnimation();
    }


}
