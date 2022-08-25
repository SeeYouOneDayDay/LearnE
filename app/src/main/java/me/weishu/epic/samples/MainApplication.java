package me.weishu.epic.samples;

import android.app.Application;
import android.content.Context;

import utils.gs.demo.OffHeapArray;
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
        demo();
    }

    private void demo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 5; i++) {
                    OffHeapArray.cases();
                    System.gc();
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
