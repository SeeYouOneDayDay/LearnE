package me.weishu.epic.samples.tests.custom;

import utils.Logger;

/**
 * Created by weishu on 17/11/6.
 */

public class Case02 implements Case {

    private static final String TAG = "Case02";

    @Override
    public void hook() {
        Object test = new Object();

//        /*********************************/
//        Method m = Refunsafe.getMethod(Case02.class, "sayHello", String.class);
//        ArtMethod artme = ArtMethod.of(m);
//        Logger.i("方法比较："
//                +"\r\nAPI:"+ Build.VERSION.SDK_INT
//                +"\r\nart:"+ UnsafeHelper.isArt()
//                +"\r\n64Bit:"+ UnsafeHelper.is64Bit()
//                +"\r\nART_QUICK_CODE_OFFSET:"+ Offset.ART_QUICK_CODE_OFFSET
//                +"\r\nART_ACCESS_FLAG_OFFSET:"+ Offset.ART_ACCESS_FLAG_OFFSET
//                +"\r\nART_JNI_ENTRY_OFFSET:"+ Offset.ART_JNI_ENTRY_OFFSET
//                +"\r\ngetAddress:"+artme.getAddress()
//                +"\r\ngetFieldOffset:"+artme.getFieldOffset()
//                +"\r\ngetEntryPointFromQuickCompiledCode:"+artme.getEntryPointFromQuickCompiledCode()
//                +"\r\ngetArtMethodSize:"+ArtMethod.getArtMethodSize()
//                + "\r\n[ArtHelper]getMethodAddress:"+ ArtHelper.getMethodAddress(m)
//                + "\r\n[ArtHelper]sizeOfArtMethod:"+ ArtHelper.sizeOfArtMethod()
//                + "\r\n[ArtHelper]getMethodIndexOffset:"+ ArtHelper.getMethodIndexOffset()
//        );

//        ArtHelper.getMethodIndexOffset();
    }

    @Override
    public boolean validate(Object... args) {
        return true;
    }

    public static void sayHello(String he) {
        Logger.i(TAG, "sayHello.  hi, " + he);
    }

    public static void run(String he) {
        Logger.i(TAG, "run. " + he + " 拍起来快的像条狗！");
    }

}
