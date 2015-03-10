package de.peculator.nachmacherx;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by sven on 02.03.15.
 */
public class Utils {
    public static boolean hasImageCaptureBug() {

        // list of known devices that have the bug
        ArrayList<String> devices = new ArrayList<String>();
        devices.add("android-devphone1/dream_devphone/dream");
        devices.add("generic/sdk/generic");
        devices.add("vodafone/vfpioneer/sapphire");
        devices.add("tmobile/kila/dream");
        devices.add("verizon/voles/sholes");
        devices.add("google_ion/google_ion/sapphire");

        return devices.contains(android.os.Build.BRAND + "/" + android.os.Build.PRODUCT + "/"
                + android.os.Build.DEVICE);

    }

    public static float getRatio(Bitmap bitmap, View view) {
        float resultA = 1f;
        float resultB = 1f;

        if(view.getHeight()>0) {

            if (bitmap.getWidth() > view.getWidth()) {
                resultA = bitmap.getWidth() / view.getWidth();
                Log.i(MainActivity.TAG, resultA + " (A) " + bitmap.getWidth() + " " + view.getWidth());
            }

            if (bitmap.getHeight() > view.getHeight()) {
                resultB = bitmap.getHeight() / view.getHeight();
                Log.i(MainActivity.TAG, resultB + " (B) " + bitmap.getHeight() + " " + view.getHeight());
            }

            return (resultA < resultB) ? resultA : resultB;
        }
        return 1;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        reqWidth = (reqWidth == 0) ? 800 : reqWidth;
        reqHeight = (reqHeight == 0) ? 800 : reqHeight;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
                if (inSampleSize > 32) break;
            }
        }

        Log.i(MainActivity.TAG, "Samplesize: " + inSampleSize + " -" + reqWidth + " _ " + reqHeight);
        return inSampleSize;
    }
}
