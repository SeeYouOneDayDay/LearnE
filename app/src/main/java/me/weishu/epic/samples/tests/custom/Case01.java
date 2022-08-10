package me.weishu.epic.samples.tests.custom;

import me.weishu.epic.art.method.ArtMethod;
import me.weishu.epic.samples.tests.fsx.ArtHelper;
import me.weishu.epic.samples.tests.fsx.MemoryHelper;
import utils.Logger;

/**
 * @Copyright © 2022 sanbo Inc. All rights reserved.
 * @Description: TODO
 * @Version: 1.0
 * @Create: 2022-08-10 17:17:20
 * @author: sanbo
 */
public class Case01 implements Case {

    private static final String TAG = "Case01";

    @Override
    public void hook() {
        Object test = new Object();
//        Logger.i(TAG, "test object:" + test);
//
//        long testAddr = Unsafe.getObjectAddress(test);
//        Logger.i(TAG, "Unsafe获取 object地址:" + testAddr);
//        Logger.i(TAG, "UnsafeHelper【getObjectAddress】获取object地址:" + UnsafeHelper.getObjectAddress(test));
//        Logger.i(TAG, "UnsafeHelper【toAddress】获取object地址:" + UnsafeHelper.toAddress(test));
//        Logger.i(TAG, "isUseUnsafe :" + EpicNative.isUseUnsafe());
//        Logger.i(TAG, "EpicNative 获取对象:" + EpicNative.getObject(testAddr));
//        Logger.i(TAG, "UnsafeHelper[fromAddress] 获取对象:" + UnsafeHelper.fromAddress(testAddr));

        Logger.i("===============方法地址及偏移测试=========================");
        ArtMethod am = ArtMethod.of(ArtHelper.getM1());
        ArtMethod am2 = ArtMethod.of(ArtHelper.getM2());
        long ad1 = am.getAddress();
        long ad2 = am2.getAddress();
        Logger.i(TAG, "ArtMethod address  ad1:" + ad1 + ", ad2:" + ad2 + "---->" + (ad2 - ad1));

        long m1 = MemoryHelper.getMethodAddress(ArtHelper.getM1());
        long m2 = MemoryHelper.getMethodAddress(ArtHelper.getM2());
        Logger.i(TAG, "toAddress  am:" + m1 + ", am2:" + m2 + "---->" + (m2 - m1));

        Logger.d(TAG, "地址offset:" + (am2.getAddress() - am.getAddress())
                + "----sizeOfArtMethod：" + ArtHelper.sizeOfArtMethod());

    }

    @Override
    public boolean validate(Object... args) {
        return true;
    }
}
