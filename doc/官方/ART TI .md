# ART TI

 





在 Android 8.0 及更高版本中，ART 工具接口 (ART TI) 可提供某些运行时的内部架构信息，并允许分析器和调试程序影响应用的运行时行为。这可用于实现最先进的性能工具，以便在其他平台上实现原生代理。

运行时内部架构信息会提供给已加载到运行时进程中的代理。它们通过直接调用和回调与 ART 通信。运行时支持多个代理，以便能够区分不同的正交分析问题。代理可以在运行时启动时提供（调用 `dalvikvm` 或 `app_process` 时），也可以连接到已在运行的进程。

由于插桩和修改应用及运行时行为的功能非常强大，因此 ART TI 中集成了两项安全措施：

- 首先，提供代理接口 JVMTI 的代码作为运行时插件（而不是运行时的核心组件）来实现。插件加载可能会受到限制，这样可阻止代理找到任何接口点。
- 其次，`ActivityManager` 类和运行时进程只允许代理连接到可调试的应用。可调试应用由其开发者签核，以供分析和插桩，而不会分发给最终用户。Google Play 商店不允许发布可调试应用。这可确保普通应用（包括核心组件）无法遭到检测或操纵。

## 设计理念

已插桩应用中的一般流程和互连如**图 1** 所示。



![已插桩应用中的流程和互连](https://raw.githubusercontent.com/hhhaiai/Picture/main/img/202208151712259.png)**图 1.** 已插桩应用的流程和互连



ART 插件 `libopenjdkjvmti` 提供了 ART TI，旨在满足平台的需求和限制：

- 类重定义基于 `Dex` 文件，其中仅包含一个类定义，而不是多个类文件。
- 没有提供用于插桩和重新定义的 Java 版 API。

ART TI 还支持 Android Studio 分析器。

### 加载或连接代理

如需在运行时启动时连接代理，请使用以下命令加载 JVMTI 插件和指定的代理：

```
dalvikvm -Xplugin:libopenjdkjvmti.so -agentpath:/path/to/agent/libagent.so …
```

如果在运行时启动时加载代理，系统不会采取任何安全措施，因此请注意，手动启动的运行时允许在不采取安全措施的情况下执行全面修改（这样可以进行 ART 测试）。

注意：这不适用于设备上的普通应用（包括系统服务器）。应用是从已在运行的 zygote 中派生的，而 zygote 进程无法加载代理。

如需将代理连接到已在运行的应用，请使用以下命令：

```
adb shell cmd activity attach-agent [process]
/path/to/agent/libagent.so[=agent-options]
```

如果尚未加载 JVMTI 插件，连接代理会同时加载该插件和相应代理库。

代理只能连接到正在运行且标记为“可调试”（应用清单的一部分，应用节点上所含的属性 `android:debuggable` 设置为 `true`）的应用。`ActivityManager` 类和 ART 都应执行检查，然后才允许连接代理。[ActivityManager](https://developer.android.com/reference/android/app/ActivityManager) 类会检查当前应用信息（派生自 [PackageManager](https://developer.android.com/reference/android/content/pm/PackageManager) 类数据）的可调试状态，而运行时会检查其当前状态（这是在应用启动时设置的）。

### 代理位置 

运行时需要将代理加载到当前进程中，以便代理直接与之绑定并进行通信。ART 本身与代理原本所处的具体位置无关。该字符串用于进行 `dlopen` 调用。文件系统权限和 SELinux 政策会限制实际的加载行为。

如需提供能由可调试应用运行的代理，请执行以下操作：

- 将代理嵌入应用 APK 的库目录中。
- 使用 `run-as` 将代理复制到应用的数据目录中。

#### API

以下方法已添加到 `android.os.Debug` 中。

```
/**
     * Attach a library as a jvmti agent to the current runtime, with the given classloader
     * determining the library search path.
     * Note: agents may only be attached to debuggable apps. Otherwise, this function will
     * throw a SecurityException.
     *
     * @param library the library containing the agent.
     * @param options the options passed to the agent.
     * @param classLoader the classloader determining the library search path.
     *
     * @throws IOException if the agent could not be attached.
     * @throws a SecurityException if the app is not debuggable.
     */
    public static void attachJvmtiAgent(@NonNull String library, @Nullable String options,
            @Nullable ClassLoader classLoader) throws IOException {
```

### 其他 Android API

attach-agent 命令是公开显示的。以下命令会将 JVMTI 代理连接到正在运行的进程：

```
adb shell 'am attach-agent com.example.android.displayingbitmaps
\'/data/data/com.example.android.displayingbitmaps/code_cache/libfieldnulls.so=Ljava/lang/Class;.name:Ljava/lang/String;\''
```

`am start -P` 和 `am start-profiler/stop-profiler` 命令类似于 attach-agent 命令。

### JVMTI

此功能可向代理提供 JVMTI API（原生代码）。一些重要的功能包括：

- 重新定义类。
- 跟踪对象分配和垃圾回收过程。
- 遵循对象的引用树，遍历堆中的所有对象。
- 检查 Java 调用堆栈。
- 暂停（和恢复）所有线程。

不同版本的 Android 可能会提供不同的功能。

### 兼容性

此功能需要仅针对 Android 8.0 及更高版本提供的核心运行时支持。设备制造商无需进行任何更改即可实现此功能。它是 AOSP 的一部分。

### 验证

在 Android 8 及更高版本上，CTS 会测试以下内容：

- 测试代理是否可以连接到可调试应用，并且无法连接到不可调试的应用。
- 测试所有已实现的 JVMTI API
- 测试代理的二进制接口是否稳定

Android 9 及更高版本中添加了其他测试，这些测试包含在这些版本的 CTS 测试中。