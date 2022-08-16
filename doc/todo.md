# TODO

## 1. EPIC功能

发现结构的内存对象，内存捞出来。 ----【xposed-api封装】---- 对应地址修改对应值

## 2. 几个库的功能及依赖关系

### 几个库的功能

* `epic-core`:
    - 获取对象内存地址 y
    - 修改内存地址 y(替换方法)
    - 从内存获取对象 y
    - 向内存设置对象 y(替换方法)
    - xposed API(DexposedBridge--淘系API+优化)
* `exposed-core`: xposedAPI wrapper,用于适配epic和xposed
* `exposed-xposedapi`: xposed API, 含两部分: xposed api、部分系统类(mirror类)，mirror已经抽取出
* `FreeReflection-core`: 维术的反射库，提供如下几个方案
    - 方式一`VMRuntime.setHiddenApiExemptions`,安卓10+后方法。反射失效
    - 方式二`new DexFile(File file)`方式加载本地dex,这样被加载的DEX中父加载器是null,这样就等于系统加载，高版本应该会失效
    - 方式三修改对应的Runtime的隐私策略(`partialRuntime->hidden_api_policy_ = EnforcementPolicy::kNoChecks;`)。
        - `enum class EnforcementPolicy`
          地址:https://android.googlesource.com/platform/art/+/master/runtime/hidden_api.h
        - 根据targetSdkVersion和设备sdk组合起来，借助JavaVM(`JNIEnv *env->GetJavaVM`)计算出对应的Runtime指针
* `relinker-core`: 加载SO库,解决部分版本加载失败的问题

### 依赖关系

* epic-core

```
epic-core
   |--FreeReflection-core
   |--exposed-xposedapi (暂时合并，未来拆分)
      |--hiddenapistubs
```

* exposed-core

```
exposed-core
  |--exposed-xposedapi
      |--hiddenapistubs
  |--epic-core(不包含exposed-xposedapi)
     |--FreeReflection-core
     |--exposed-xposedapi
        |--hiddenapistubs
  |--relinker-core
```

* exposed-xposedapi

```
exposed-xposedapi
  |--hiddenapistubs
```

## 思考

### 一:是否可调整`ApplicationInfo.ApiEnforcementPolicy`达到越权效果

反射、越权访问是否可以考虑修改`ApplicationInfo.ApiEnforcementPolicy` 。
地址: https://android.googlesource.com/platform/art/+/master/runtime/hidden_api.h

``` c++
// Hidden API enforcement policy
// This must be kept in sync with ApplicationInfo.ApiEnforcementPolicy in
// frameworks/base/core/java/android/content/pm/ApplicationInfo.java
enum class EnforcementPolicy {
kDisabled             = 0,
kJustWarn             = 1,  // keep checks enabled, but allow everything (enables logging)
kEnabled              = 2,  // ban conditionally blocked & blocklist
kMax = kEnabled,
};
```

### 二: 内存捞取、修改是否可以迁移至Java层？

可以通过Memory.peekByte(src) 取具体地址的值 可以通过Memory.pokeByte(dst, byte) 往具体地址设置值

### 三: `relinker-core`是否支持热更新加载SO?

* `0-1加载`:
* `1-2更新`:

### 四: Thread.nativePeer解析

* Java`private volatile long nativePeer;`
    - 官方注释: 对本机线程对象的引用。 如果本机线程尚未创建/启动或已被销毁，则为 0。
    - 找到来源: 我为 Dexposed 续一秒——论 ART 上运行时 Method AOP 实现
* 指针与对象转换

  在基本的 bridge 函数调用（从汇编进入 Java 世界）的问题搞定之后，我们会碰到一个新问题：在
      bridge函数中接受到的参数都是一些地址，但是原函数的参数明明是一些对象，怎么把地址还原成原始的参数呢？

  如果传递的是基本类型，那么接受到的地址其实就是基本类型值的表示；但是如果传递的是对象，那接受到的 int/long 是个什么东西？

  这个问题一言难尽，它的背后是 ART 的对象模型；这里我简单说明一下。一个最直观的问题就是：JNI 中的 jobject，Java 中的 Object，ART 中的 
      art::mirror:: Object 到底是个什么关系？

  实际上，art::mirror::Object 是 Java 的 Object 在 Runtime 中的表示，java.lang.Object 的地址就是 art::mirror::Object
      的地址；但是 jobject 略有不同，它并非地址，而是一个句柄（或者说透明引用）。为何如此？

  因为 JNI 对于 ART 来说是外部环境，如果直接把 ART 中的对象地址交给 JNI 层（也就是 jobject 直接就是 Object
      的地址），其一不是很安全，其二直接暴露内部实现不妥。就拿 GC 来说，虚拟机在 GC 过程中很可能移动对象，这样对象的地址就会发生变化，如果 JNI 直接使用地址，那么对 GC
      的实现提出了很高要求。因此，典型的 Java 虚拟机对 JNI 的支持中，jobject 都是句柄（或者称之为透明引用）；ART 虚拟机内部可以在 joject 与 art::mirror::
      Object 中自由转换，但是 JNI 层只能拿这个句柄去标志某个对象。

  那么 jobject 与 java.lang.Object 如何转换呢？这个 so easy，直接通过一次 JNI 调用，ART 就自动完成了转换。

  因此归根结底，我们需要找到一个函数，它能实现把 art::mirror::Object 转换为 jobject 对象，这样我们可以通过 JNI 进而转化为 Java
     对象。这样的函数确实有，那就是：

    ``` c++
    art::JavaVMExt::AddWeakGlobalReference(art::Thread*, art::mirror::Object*)
    ```

  此函数在 libart.so 中，我们可以通过 `dlsym`拿到函数指针，然后直接调用。不过这个函数有一个 art::Thread *的参数，如何拿到这个参数呢？查阅 art::Thread
      的源码发现，这个 art::Thread 与 java.lang.Thread 也有某种对应关系，它们是通过 peer 结合在一起的（JNI
      文档中有讲）。也就是说，java.lang.Thread 类中的 nativePeer 成员代表的就是当前线程的 art::Thread* 对象。这个问题迎刃而解。