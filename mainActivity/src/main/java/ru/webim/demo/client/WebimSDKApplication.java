package ru.webim.demo.client;

import android.app.Application;

public class WebimSDKApplication extends Application {
    private static boolean mIsInBackground = true;

    public static void setIsInBackground(boolean b) {
        mIsInBackground = b;
    }

    public static boolean isInBackground() {
        return mIsInBackground;
    }
}
