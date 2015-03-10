package de.peculator.nachmacherx;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by peculator
 */
public class Preferences {
    public String lastURLSource;
    public String lastURLResult;
    public int lastAlpha;
    public boolean frontCamera;

    public static String FOLDER_NAME_GALLERY = "NachmacherX";
    public static final String IMAGE_PATH = "de.peculator.nachmacherx.CameraActivity.IMAGE_PATH";


    public Context applicationContext;

    public String PREFNAME = "nachmacherx_prefs";
    public String pre = "nachmacherx_";
    public String urlResult = "urlResult";
    public String url = "url";
    public String alpha = "alpha";
    public String camera = "camera";


    public Preferences(Context context) {
        this.applicationContext = context;
    }

    public String getLastURLResult() {
        return lastURLResult;
    }

    public void setLastURLResult(String lastURLResult) {
        this.lastURLResult = lastURLResult;
    }

    public String getLastURLSource() {
        return lastURLSource;
    }

    public void setLastURLSource(String lastURLSource) {
        this.lastURLSource = lastURLSource;
    }

    public boolean isFrontCamera() {
        return frontCamera;
    }

    public void setFrontCamera(boolean frontCamera) {
        this.frontCamera = frontCamera;
    }

    public int getLastAlpha() {
        return lastAlpha;
    }

    public void setLastAlpha(int lastAlpha) {
        this.lastAlpha = lastAlpha;
    }


    public void loadLastPreferences() {
        SharedPreferences settings = applicationContext.getSharedPreferences(PREFNAME, 0);
        this.setLastAlpha(settings.getInt(pre+alpha,50));
        this.setFrontCamera(settings.getBoolean(pre + camera, false));
        this.setLastURLSource(settings.getString(pre + url, ""));
        this.setLastURLResult(settings.getString(pre + urlResult, ""));

    }

    public void storePreferences(){
        SharedPreferences settings = applicationContext.getSharedPreferences(PREFNAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(pre + alpha, getLastAlpha());
        editor.putBoolean(pre + camera, isFrontCamera());
        editor.putString(pre + url, getLastURLSource());
        editor.putString(pre + urlResult, getLastURLResult());

        editor.commit();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append(" Alpha:").append(getLastAlpha())
                .append("; Front-Camera:").append(isFrontCamera())
                .append("; LastURL:").append(getLastURLSource())
                .append("; LastURLResult:").append(getLastURLSource()).toString();
    }
}
