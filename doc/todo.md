# TODO

## 1. EPIC功能
 发现结构的内存对象，内存捞出来。 ----【xposed-api封装】----  对应地址修改对应值

## 2. 几个库的功能及依赖关系

### 几个库的功能

* `epic-core`: 
  - 获取对象内存地址
  - 修改内存地址
  - 从内存获取对象
  - 向内存设置对象
  - xposed API(DexposedBridge--应该是改良淘系的)
* `exposed-core`: xposed 模块管理？ 没太细看
* `exposed-xposedapi`: xposed API, 含两部分: xposed api、部分系统类(mirror类)
* `FreeReflection-core`: 维术的反射库，提供如下几个方案
  - 方式一`VMRuntime.setHiddenApiExemptions`,安卓10+后方法丢失
  - 方式二`new DexFile(File file)`方式加载本地dex,这样被加载的DEX中父加载器是null,这样就等于系统加载，高版本应该会失效
  - 方式三修改对应的Runtime的隐私策略(`partialRuntime->hidden_api_policy_ = EnforcementPolicy::kNoChecks;`)。
    - `enum class EnforcementPolicy` 地址:https://android.googlesource.com/platform/art/+/master/runtime/hidden_api.h
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

### 三: `relinker-core`是否支持热更新加载SO?

* `0-1加载`:
* `1-2更新`:
### 四: Thread.nativePeer解析
* Java`private volatile long nativePeer;`
  - 官方注释: 对本机线程对象的引用。 如果本机线程尚未创建/启动或已被销毁，则为 0。