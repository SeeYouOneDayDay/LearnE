# 在 Android Runtime (ART) 中验证应用行为

 







Android Runtime (ART) 是运行 Android 5.0（API 级别 21）及更高版本的设备的默认运行时。此运行时提供大量功能，可提升 Android 平台和应用的性能和流畅度。如需了解关于 ART 新功能的更多信息，请参阅 [ART 简介](https://source.android.com/devices/tech/dalvik/art.html)。

不过，部分适合 Dalvik 的技术并不适用于 ART。本文档可帮助您了解在迁移现有应用，使其与 ART 兼容时需要注意的事项。大多数应用在通过 ART 运行时都能正常工作。

## 解决垃圾回收 (GC) 问题

在 Dalvik 中，应用时常发现显式调用 `System.gc()` 有助于促进垃圾回收 (GC)。而在 ART 中，这种做法显得没那么必要，尤其是当您需通过垃圾回收来预防出现 [`GC_FOR_ALLOC`](https://developer.android.com/tools/debugging/debugging-memory#LogMessages) 类型或减少碎片时。您可以通过调用 `System.getProperty("java.vm.version")` 来验证正在使用哪种运行时。如果使用的是 ART，则该属性值应为 `"2.0.0"` 或更高。

而且，[Android 开源项目 (AOSP)](https://source.android.com/) 中正在开发一种紧凑型垃圾回收器，旨在改善内存管理。因此，您应避免使用与紧凑型 GC 不兼容的方法（例如保存对象实例数据的指针）。对于使用 Java 原生接口 (JNI) 的应用而言，这一点尤其重要。如需了解详细信息，请参阅[预防 JNI 问题](https://developer.android.com/guide/practices/verifying-apps-art.html#JNI_Issues)。

## 预防 JNI 问题

ART 的 JNI 比 Dalvik 的 JNI 更为严格一些。使用 CheckJNI 模式来捕获常见问题是一种特别实用的方法。如果您的应用使用 C/C++ 代码，则您应阅读以下文章：

[使用 CheckJNI 调试 Android JNI](http://android-developers.blogspot.com/2011/07/debugging-android-jni-with-checkjni.html)

### 检查 JNI 代码中的垃圾回收问题

在 Android 开源项目 (AOSP) 中，ART 有一个正在开发中的紧凑型垃圾回收器。一旦该紧凑型垃圾回收器投入使用，您便可在内存中移动对象。如果您使用 C/C++ 代码，请勿执行与紧凑型 GC 不兼容的操作。我们对 CheckJNI 进行了增强，以识别一些潜在问题（如 [ICS 中的 JNI 局部引用更改](http://android-developers.blogspot.com/2011/11/jni-local-reference-changes-in-ics.html)中所述）。

有一点您需特别注意，即 `Get...ArrayElements()` 和 `Release...ArrayElements()` 函数的使用。在包含非紧凑型 GC 的运行时中，`Get...ArrayElements()` 函数通常会返回支持数组对象的实际内存的引用。如果更改返回的其中一个数组元素，则数组对象本身也会产生变化（并且系统往往会忽略 `Release...ArrayElements()` 的参数）。但是，如果正在使用的是紧凑型 GC，则 `Get...ArrayElements()` 函数可能会返回内存的副本。如果您在使用紧凑型 GC 的情况下误用引用，则可能会导致内存损坏或其他问题。例如：

- 如果您已更改返回的数组元素，则在完成更改后必须调用相应的 `Release...ArrayElements()` 函数，以确保您所做的更改已正确复制回基础数组对象。
- 在您释放内存数组元素时，必须根据所做更改使用相应的模式：
    - 如果您未更改任何数组元素，请使用 `JNI_ABORT` 模式，该模式会释放内存，而非将更改复制回基础数组元素。
    - 如果您已更改数组，并且不再需要该引用，请使用代码 `0`（它将更新数组对象并释放内存副本）。
    - 如果您已更改想要提交的数组，并且希望保留该数组的副本，请使用 `JNI_COMMIT`（它将更新基础数组对象并保留该副本）。
- 调用 `Release...ArrayElements()` 时会返回最初由 `Get...ArrayElements()` 返回的同一指针。例如，增加原始指针（以扫描所有返回的数组元素），然后将增加后的指针传递至 `Release...ArrayElements()` 是不安全的做法。传递此修改后的指针可能会导致释放错误的内存，进而导致内存损坏。

### 错误处理

ART 的 JNI 会在多种情况下抛出错误，而 Dalvik 则不然。（同样，您可以通过使用 CheckJNI 执行测试，以捕获大量此种情况）。

例如，如果使用不存在的方法（可能 **ProGuard** 等工具已移除该方法）调用 `RegisterNatives`，ART 现可正确抛出 `NoSuchMethodError`：

```
08-12 17:09:41.082 13823 13823 E AndroidRuntime: FATAL EXCEPTION: main
08-12 17:09:41.082 13823 13823 E AndroidRuntime: java.lang.NoSuchMethodError:
    no static or non-static method
    "Lcom/foo/Bar;.native_frob(Ljava/lang/String;)I"
08-12 17:09:41.082 13823 13823 E AndroidRuntime:
    at java.lang.Runtime.nativeLoad(Native Method)
08-12 17:09:41.082 13823 13823 E AndroidRuntime:
    at java.lang.Runtime.doLoad(Runtime.java:421)
08-12 17:09:41.082 13823 13823 E AndroidRuntime:
    at java.lang.Runtime.loadLibrary(Runtime.java:362)
08-12 17:09:41.082 13823 13823 E AndroidRuntime:
    at java.lang.System.loadLibrary(System.java:526)
```

如果不使用任何方法调用 `RegisterNatives`，ART 也会记录错误（在 logcat 中可见）：

```
W/art     ( 1234): JNI RegisterNativeMethods: attempt to register 0 native
methods for <classname>
```

此外，JNI 函数 `GetFieldID()` 和 `GetStaticFieldID()` 现可正确抛出 `NoSuchFieldError`，而不是仅返回 null。同样，`GetMethodID()` 和 `GetStaticMethodID()` 现可正确抛出 `NoSuchMethodError`。如此一来，CheckJNI 可能会因未处理的异常或抛给 Java 原生代码调用函数的异常而失败。因此，使用 CheckJN 模式测试 ART 兼容型应用便显得格外重要。

ART 希望 JNI `CallNonvirtual...Method()` 方法（例如 `CallNonvirtualVoidMethod()`）的用户按照 JNI 规范的要求，使用该方法的声明类（而非子类）。

## 预防堆栈大小问题

Dalvik 拥有单独的原生代码堆栈和 Java 代码堆栈，并且默认的 Java 堆栈大小为 32KB，默认的原生堆栈大小为 1MB。ART 拥有统一的堆栈，用于改善局部位置。通常情况下，ART `Thread` 堆栈大小应与 Dalvik 堆栈大小近乎相同。但是，如果您显式设置堆栈大小，则可能需针对 ART 中运行的应用重新访问这些值。

- 在 Java 中，检查对指定显式堆栈大小的 `Thread` 构造函数的调用。例如，如果发生 `StackOverflowError`，您将需要增大堆栈。

- 在 C/C++ 中，检查如何将

     

    ```
    pthread_attr_setstack()
    ```

     

    和

     

    ```
    pthread_attr_setstacksize()
    ```

     

    用于同时通过 JNI 运行 Java 代码的线程。以下是某个应用在 pthread 过小的情况下尝试调用 JNI

     

    ```
    AttachCurrentThread()
    ```

     

    时记录的错误示例：

    ```
    F/art: art/runtime/thread.cc:435]
        Attempt to attach a thread with a too-small stack (16384 bytes)
    ```

## 对象模型更改

Dalvik 不应允许子类重写软件包私有的方法。在这类情况下，ART 会发出警告：

```
Before Android 4.1, method void com.foo.Bar.quux()
would have incorrectly overridden the package-private method in
com.quux.Quux
```

如果您希望在其他软件包中重写某个类的方法，请将该方法声明为 `public` 或 `protected`。

`Object` 现包含私有字段。如果某个应用会反射其类层次中的字段，则其应小心避免尝试查看 `Object` 的字段。例如，如果您正在向上迭代某个作为序列化框架一部分的类层次，请在以下情况下停止迭代操作

```
Class.getSuperclass() == java.lang.Object.class
```

而不是继续操作，直至该方法返回 `null`。

如果没有任何参数，代理 `InvocationHandler.invoke()` 现将收到 `null`，而非空数组。此行为先前已有记录，但未在 Dalvik 中得到正确处理。先前版本的 [Mockito](https://code.google.com/p/mockito/) 难以处理这一问题，因此在使用 ART 测试时，请使用更新的 Mockito 版本。

## 修复 AOT 编译问题

ART 的运行前 (AOT) Java 编译应适用于所有标准 Java 代码。编译由 ART 的 `dex2oat` 工具执行；如果您在安装时遇到任何与 `dex2oat` 有关的问题，请联系我们（请参阅[报告问题](https://developer.android.com/guide/practices/verifying-apps-art.html#Reporting_Problems)），以便我们能尽快将其修复。需要注意的几个问题：

- ART 会在安装时执行比 Dalvik 更严格的字节代码验证。Android 构建工具生成的代码应该没有问题。但是，一些后期处理工具（尤其是执行模糊处理的工具）可能会生成无效文件，这类文件勉强可适用于 Dalvik，但无法适用于 ART。我们一直在与工具供应商合作，以查找并修复此类问题。在许多情况下，获取最新版本的工具并重新生成 DEX 文件即可修复这些问题。

- ART 验证器标记了一些典型问题，其中包括：

    - 无效的控制流
    - 失衡的 `moniterenter`/`moniterexit`
    - 0 长度参数类型列表大小

- 某些应用拥有与已安装的

     

    ```
    .odex
    ```

     

    文件格式相关的依赖，这些格式或安装在

     

    ```
    /system/framework
    ```

    、

    ```
    /data/dalvik-cache
    ```

     

    中，或安装在

     

    ```
    DexClassLoader
    ```

     

    经优化的输出目录中。现在，这些文件是 ELF 文件，而非 DEX 文件的扩展形式。尽管 ART 努力遵循与 Dalvik 相同的命名和锁定规则，但应用不能依赖于文件格式，因为该格式可能未经通知便发生更改。

    **注意：**在 Android 8.0（API 级别 26）和更高版本中，`DexClassLoader` 优化了已弃用的输出目录。如需了解详细信息，请参阅 `DexClassLoader()` 构造函数文档。

## 报告问题

如果您遇到的任何问题均非应用 JNI 问题导致，请访问 https://code.google.com/p/android/issues/list，通过 Android 开源项目问题追踪器报告这些问题。请加入 `"adb bugreport"` 和 Google Play 商店中的应用链接（如可用）。否则，如果可能，请附加用于重现该问题的 APK。请注意，这些问题（包括附件）是公开可见的。