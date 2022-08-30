package me.weishu.epic.samples;

import android.app.Application;
import android.content.Context;

import uts.MinRef;

/**
 * Created by weishu on 17/10/31.
 */
public class MainApplication extends Application {

    private static Context sContext;

    public static Context getAppContext() {
        return sContext;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sContext = base;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MinRef.unseal(this);
//        new Case5().hook();
//        new Case19().hook();

    }

}
