/*
 * Copyright (c) 2017, weishu twsxtd@gmail.com
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

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import me.weishu.epic.art.arch.ShellCode;
import me.weishu.epic.art.entry.Entry;
import me.weishu.epic.art.entry.Entry64;
import me.weishu.epic.art.method.ArtMethod;
import utils.Logger;
import utils.Runtime;

class Trampoline {
    private static final String TAG = "Trampoline";

    private final ShellCode shellCode;
    // JIT编译后的地址，同entryPoint
    public final long jumpToAddress;
    // 原始值
    public final byte[] originalCode;
    // 中间值的大小。蹦床大小
    public int trampolineSize;
    //  中间值的地址。蹦床地址
    public long trampolineAddress;
    // 是否应用
    public boolean active;

    // 防止重复方法
    // private ArtMethod artOrigin;
    private Set<ArtMethod> segments = new HashSet<>();

    Trampoline(ShellCode shellCode, long entryPoint) {
        this.shellCode = shellCode;
        this.jumpToAddress = shellCode.toMem(entryPoint);
        this.originalCode = EpicNative.get(jumpToAddress, shellCode.sizeOfDirectJump());
    }

    public boolean install(ArtMethod originMethod) {
        Logger.d(TAG, "inside install");
        boolean modified = segments.add(originMethod);
        if (!modified) {
            // Already hooked, ignore
            Logger.d(TAG, "install() " + originMethod.toString() + " is already hooked, return.");
            return true;
        }

        // 创建跳转 + 原来的信息 组成 byte[]
        byte[] page = create();
        // 申请对应大小的内存(mmap)，通过地址将byte[] 放进到对应地址(memput)
        EpicNative.put(page, getTrampolineAddress());

        // 获取原方法的偏移后的字符，并解析其大小
        int quickCompiledCodeSize = Epic.getQuickCompiledCodeSize(originMethod);
        // 获取跳转的大小
        int sizeOfDirectJump = shellCode.sizeOfDirectJump();
        Logger.d(TAG, "install() " + originMethod.toString()
                + "\r\n\tquickCompiledCodeSize: " + quickCompiledCodeSize
                + "\r\n\tsizeOfDirectJump: " + sizeOfDirectJump
        );

        // 如大小不对
        if (quickCompiledCodeSize < sizeOfDirectJump) {
            Logger.d(TAG, "install() 跳转汇编大小<直跳汇编大小,即将重新设置EntryPointFromQuickCompiledCode。 size:" + getTrampolinePc());
            originMethod.setEntryPointFromQuickCompiledCode(getTrampolinePc());
            return true;
        }

        // 这里是绝对不能改EntryPoint的，碰到GC就挂(GC暂停线程的时候，遍历所有线程堆栈，如果被hook的方法在堆栈上，那就GG)
        // source.setEntryPointFromQuickCompiledCode(script.getTrampolinePc());
        //绑定让其执行
        return activate();
    }

    private long getTrampolineAddress() {
        if (getSize() != trampolineSize) {
            alloc();
        }
        return trampolineAddress;
    }

    private long getTrampolinePc() {
        return shellCode.toPC(getTrampolineAddress());
    }

    private void alloc() {
        if (trampolineAddress != 0) {
            free();
        }
        trampolineSize = getSize();
        trampolineAddress = EpicNative.map(trampolineSize);
        Logger.d(TAG, "Trampoline alloc:" + trampolineSize + ", addr: 0x" + Long.toHexString(trampolineAddress));
    }

    private void free() {
        if (trampolineAddress != 0) {
            EpicNative.unmap(trampolineAddress, trampolineSize);
            trampolineAddress = 0;
            trampolineSize = 0;
        }

        if (active) {
            EpicNative.put(originalCode, jumpToAddress);
        }
    }

    private int getSize() {
        int count = 0;
        count += shellCode.sizeOfBridgeJump() * segments.size();
        count += shellCode.sizeOfCallOrigin();
        return count;
    }

    //组成数据：
    // 1. 大小： BridgeJump桥接大小*拦截个数  + DirectJump直跳大小*2
    // 2. 组成：
    //      方法跳转段汇编拼接。（若多个，则此处多个）
    //      原方法的跳转汇编地址
    private byte[] create() {
        Logger.d(TAG, "create trampoline." + segments);
        byte[] mainPage = new byte[getSize()];

        int offset = 0;
        for (ArtMethod method : segments) {
            byte[] bridgeJump = createTrampoline(method);
            int length = bridgeJump.length;
            //        arraycopy(Object src,  int  srcPos,  Object dest, int destPos, int length);
            System.arraycopy(bridgeJump, 0, mainPage, offset, length);
            offset += length;
        }

        byte[] callOriginal = shellCode.createCallOrigin(jumpToAddress, originalCode);
//        arraycopy(Object src,  int  srcPos,  Object dest, int destPos, int length);
        System.arraycopy(callOriginal, 0, mainPage, offset, callOriginal.length);

        return mainPage;
    }

    private boolean activate() {
        long pc = getTrampolinePc();
//        Logger.d(TAG, "Writing direct jump entry " + Debug.addrHex(pc) + " to origin entry: 0x" + Debug.addrHex(jumpToAddress));
        Logger.d(TAG, "Trampoline  activate()---即将EpicNative.activateNative "
                + "\r\ngetTrampolinePc(pc):" + pc
                + "\r\njumpToAddress:" + jumpToAddress
                + "\r\nsizeOfDirectJump:" + shellCode.sizeOfDirectJump()
                + "\r\nsizeOfBridgeJump:" + shellCode.sizeOfBridgeJump()
                + "\r\ncreateDirectJump:" + shellCode.createDirectJump(pc)
        );

        // native逻辑，暂停JIT编译，分配一个可读写的空间，将跳板信息拷贝进去，重新开始JIT编译
        //stop_jit
        synchronized (Trampoline.class) {
            return EpicNative.activateNative(jumpToAddress, pc, shellCode.sizeOfDirectJump(),
                    shellCode.sizeOfBridgeJump(), shellCode.createDirectJump(pc));
        }
    }

    @Override
    protected void finalize() throws Throwable {
        free();
        super.finalize();
    }

    // 根据方法 绑定对应回调的函数
    //  malloc 4个指针大小
    // 根据 目标地址/目标EntryPointFromQuickCompiledCode/原地址/分配的四个指针地址(malloc(length))
    private byte[] createTrampoline(ArtMethod source) {
        Logger.d("inside Trampoline.createTrampoline. addr(source):" + source.getAddress());
        final Epic.MethodInfo methodInfo = Epic.getMethodInfo(source.getAddress());
        final Class<?> returnType = methodInfo.returnType;

//        Method bridgeMethod = Runtime.is64Bit() ? (Build.VERSION.SDK_INT == 23 ? Entry64_2.getBridgeMethod(methodInfo) : Entry64.getBridgeMethod(returnType))
//                : Entry.getBridgeMethod(returnType);
        Method bridgeMethod = Runtime.is64Bit() ? Entry64.getBridgeMethod(returnType)
                : Entry.getBridgeMethod(returnType);
// 获取对应类型。然后将跳转地点绑定
        final ArtMethod target = ArtMethod.of(bridgeMethod);
        long targetAddress = target.getAddress();
        long targetEntry = target.getEntryPointFromQuickCompiledCode();
        long sourceAddress = source.getAddress();
        long structAddress = EpicNative.malloc(4);
        Logger.d("Trampoline.createTrampoline \r\n\ttarget address ：" + targetAddress
                + "\r\n\ttargetEntry: " + targetEntry
                + "\r\n\tsourceAddress: " + sourceAddress
                + "\r\n\tstructAddress: " + structAddress
        );

//        Logger.d(TAG, "targetAddress:" + Debug.longHex(targetAddress));
//        Logger.d(TAG, "sourceAddress:" + Debug.longHex(sourceAddress));
//        Logger.d(TAG, "targetEntry:" + Debug.longHex(targetEntry));
//        Logger.d(TAG, "structAddress:" + Debug.longHex(structAddress));

        return shellCode.createBridgeJump(targetAddress, targetEntry, sourceAddress, structAddress);
    }
}
