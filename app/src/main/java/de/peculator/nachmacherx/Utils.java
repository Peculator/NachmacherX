package de.peculator.nachmacherx;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

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
    public static float getRatio(Bitmap b) {
        float ratio;

        if (b.getWidth() > b.getHeight()) {
            ratio = (float) b.getWidth() / 2048;
        } else {
            ratio = (float) b.getHeight() / 2048;
        }
        return ratio;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        reqWidth = (reqWidth==0)?800:reqWidth;
        reqHeight = (reqHeight==0)?800:reqHeight;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
                if(inSampleSize>32)break;
            }
        }

        Log.i(MainActivity.TAG,"Samplesize: "+inSampleSize+ " -"+reqWidth+" _ " + reqHeight);
        return inSampleSize;
    }
}
