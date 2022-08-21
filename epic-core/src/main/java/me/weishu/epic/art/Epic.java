/*
 * Copyright (c) 2017, weishu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.weishu.epic.art;

import android.os.Build;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.weishu.epic.art.arch.Arm64;
import me.weishu.epic.art.arch.ShellCode;
import me.weishu.epic.art.arch.Thumb2;
import me.weishu.epic.art.method.ArtMethod;
import utils.Debug;
import utils.Logger;
import utils.Runtime;

/**
 * Hook Center.
 */
public final class Epic {

    private static final String TAG = "Epic";

    // 方法地址--->对应方法
    private static final Map<String, ArtMethod> backupMethodsMapping = new ConcurrentHashMap<>();

    //方法地址--->缓存信息(是否静态、参数个数、参数类型、返回类型、artMethod方法)
    private static final Map<Long, MethodInfo> originSigs = new ConcurrentHashMap<>();

    private static final Map<Long, Trampoline> scripts = new HashMap<>();
    private static ShellCode ShellCode;

    static {
        boolean isArm = true; // TODO: 17/11/21 TODO
        int apiLevel = Build.VERSION.SDK_INT;
        boolean thumb2 = true;
        if (isArm) {
            if (Runtime.is64Bit()) {
                ShellCode = new Arm64();
            } else if (Runtime.isThumb2()) {
                ShellCode = new Thumb2();
            } else {
                thumb2 = false;
                ShellCode = new Thumb2();
                Logger.w(TAG, "ARM32, not support now.");
            }
        }
        if (ShellCode == null) {
            throw new RuntimeException("Do not support this ARCH now!! API LEVEL:" + apiLevel + " thumb2 ? : " + thumb2);
        }
        Logger.i(TAG, "Using: " + ShellCode.getName());
    }

    public static boolean hookMethod(Constructor origin) {
        return hookMethod(ArtMethod.of(origin));
    }

    public static boolean hookMethod(Method origin) {
        ArtMethod artOrigin = ArtMethod.of(origin);
        return hookMethod(artOrigin);
    }

    private static boolean hookMethod(ArtMethod artOrigin) {

        MethodInfo methodInfo = new MethodInfo();
        methodInfo.isStatic = Modifier.isStatic(artOrigin.getModifiers());
        final Class<?>[] parameterTypes = artOrigin.getParameterTypes();
        if (parameterTypes != null) {
            methodInfo.paramNumber = parameterTypes.length;
            methodInfo.paramTypes = parameterTypes;
        } else {
            methodInfo.paramNumber = 0;
            methodInfo.paramTypes = new Class<?>[0];
        }
        methodInfo.returnType = artOrigin.getReturnType();
        methodInfo.method = artOrigin;

        originSigs.put(artOrigin.getAddress(), methodInfo);
        Logger.d("Epic hookMethod() setto MEMORY addr[" + artOrigin.getAddress() + "] originSigs:" + originSigs);
        if (!artOrigin.isAccessible()) {
            artOrigin.setAccessible(true);
        }

        artOrigin.ensureResolved();

        long originEntry = artOrigin.getEntryPointFromQuickCompiledCode();
        if (originEntry == ArtMethod.getQuickToInterpreterBridge()) {
            Logger.i(TAG, "this method is not compiled, compile it now. current entry: 0x" + Long.toHexString(originEntry));
            boolean ret = artOrigin.compile();
            if (ret) {
                originEntry = artOrigin.getEntryPointFromQuickCompiledCode();
                Logger.i(TAG, "compile method success, new entry: 0x" + Long.toHexString(originEntry));
            } else {
                Logger.e(TAG, "compile method failed...");
                return false;
                // return hookInterpreterBridge(artOrigin);
            }
        }

        ArtMethod backupMethod = artOrigin.backup();

//        Logger.i(TAG, "backup method address:" + Debug.addrHex(backupMethod.getAddress()));
//        Logger.i(TAG, "backup method entry :" + Debug.addrHex(backupMethod.getEntryPointFromQuickCompiledCode()));

        Logger.i(TAG, "backup method \r\n\t"
                + "address:" +backupMethod.getAddress()
                + "method getEntryPointFromQuickCompiledCode:" +backupMethod.getEntryPointFromQuickCompiledCode()
                + "method EntryPointFromJni:" +backupMethod.getEntryPointFromJni()
        );
        Logger.i(TAG, "artOrigin method \r\n\t"
                + "address:" +artOrigin.getAddress()
                + "method getEntryPointFromQuickCompiledCode:" +artOrigin.getEntryPointFromQuickCompiledCode()
                + "method EntryPointFromJni:" +artOrigin.getEntryPointFromJni()
        );

        ArtMethod backupList = getBackMethod(artOrigin);

//        Logger.e("artOrigin.address:" + artOrigin.getAddress() + "-----backupMethod:" + backupMethod.getAddress());
        Logger.i(TAG, "backupList method \r\n\t"
                + "address:" +backupList.getAddress()
                + "method getEntryPointFromQuickCompiledCode:" +backupList.getEntryPointFromQuickCompiledCode()
                + "method EntryPointFromJni:" +backupList.getEntryPointFromJni()
        );

        if (backupList == null) {
            setBackMethod(artOrigin, backupMethod);
        }
        Logger.e(TAG,"hookMethod()  backupMethodsMapping:" + backupMethodsMapping.toString());
        final long key = originEntry;
        // 这部分是创建跳板
        final EntryLock lock = EntryLock.obtain(originEntry);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (lock) {
            if (!scripts.containsKey(key)) {
                scripts.put(key, new Trampoline(ShellCode, originEntry));
            }
            Logger.i("key:" + key + "-----scripts:" + scripts);
            Trampoline trampoline = scripts.get(key);
            boolean ret = trampoline.install(artOrigin);
            Logger.i(TAG, "hook Method result:" + ret);
            return ret;
        }
    }

    /*
    private static boolean hookInterpreterBridge(ArtMethod artOrigin) {

        String identifier = artOrigin.getIdentifier();
        ArtMethod backupMethod = artOrigin.backup();

        uts.Logger.d(TAG, "backup method address:" + Debug.addrHex(backupMethod.getAddress()));
        uts.Logger.d(TAG, "backup method entry :" + Debug.addrHex(backupMethod.getEntryPointFromQuickCompiledCode()));

        List<ArtMethod> backupList = backupMethodsMapping.get(identifier);
        if (backupList == null) {
            backupList = new LinkedList<ArtMethod>();
            backupMethodsMapping.put(identifier, backupList);
        }
        backupList.add(backupMethod);

        long originalEntryPoint = ShellCode.toMem(artOrigin.getEntryPointFromQuickCompiledCode());
        uts.Logger.d(TAG, "originEntry Point(bridge):" + Debug.addrHex(originalEntryPoint));

        originalEntryPoint += 16;
        uts.Logger.d(TAG, "originEntry Point(offset8):" + Debug.addrHex(originalEntryPoint));

        if (!scripts.containsKey(originalEntryPoint)) {
            scripts.put(originalEntryPoint, new Trampoline(ShellCode, artOrigin));
        }
        Trampoline trampoline = scripts.get(originalEntryPoint);

        boolean ret = trampoline.install();
        uts.Logger.i(TAG, "hook Method result:" + ret);
        return ret;

    }*/
    public synchronized static ArtMethod getBackMethod(ArtMethod origin) {
        String identifier = origin.getIdentifier();
        return backupMethodsMapping.get(identifier);
    }

    public static synchronized void setBackMethod(ArtMethod origin, ArtMethod backup) {
        String identifier = origin.getIdentifier();
        backupMethodsMapping.put(identifier, backup);
    }

    public static MethodInfo getMethodInfo(long address) {
        return originSigs.get(address);
    }

    public static int getQuickCompiledCodeSize(ArtMethod method) {

        long entryPoint = ShellCode.toMem(method.getEntryPointFromQuickCompiledCode());
        long sizeInfo1 = entryPoint - 4;
        byte[] bytes = EpicNative.get(sizeInfo1, 4);
        int size = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
        Logger.d(TAG, "getQuickCompiledCodeSize: " + size);
        return size;
    }


    // 缓存原方案状态及细节
    public static class MethodInfo {
        public boolean isStatic;
        public int paramNumber;
        public Class<?>[] paramTypes;
        public Class<?> returnType;
        public ArtMethod method;

        @Override
        public String toString() {
            return method.toGenericString();
        }
    }

    private static class EntryLock {
        static Map<Long, EntryLock> sLockPool = new HashMap<>();

        static synchronized EntryLock obtain(long entry) {
            if (sLockPool.containsKey(entry)) {
                return sLockPool.get(entry);
            } else {
                EntryLock entryLock = new EntryLock();
                sLockPool.put(entry, entryLock);
                return entryLock;
            }
        }
    }
}
