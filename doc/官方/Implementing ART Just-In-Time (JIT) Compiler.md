# Implementing ART Just-In-Time (JIT) Compiler

* https://source.android.com/devices/tech/dalvik/jit-compiler

Android runtime (ART) includes a just-in-time (JIT) compiler with code profiling that continually improves the performance of Android applications as they run. The JIT compiler complements ART's current ahead-of-time (AOT) compiler and improves runtime performance, saves storage space, and speeds application and system updates. It also improves upon the AOT compiler by avoiding system slowdown during automatic application updates or recompilation of applications during over-the-air (OTA) updates.

Although JIT and AOT use the same compiler with a similar set of optimizations, the generated code might not be identical. JIT makes use of runtime type information, can do better inlining, and makes on stack replacement (OSR) compilation possible, all of which generates slightly different code.

## JIT architecture

![JIT architecture](https://raw.githubusercontent.com/hhhaiai/Picture/main/img/202208151709083.png)

**Figure 1.** JIT architecture.

## JIT compilation

JIT compilation involves the following activities:

![Profile-guided comp](https://source.android.com/devices/tech/dalvik/images/jit-profile-comp.png)

**Figure 2.** Profile-guided compilation.

1. The user runs the app, which then triggers ART to load the

     

    ```
    .dex
    ```

     

    file.

    - If the `.oat` file (the AOT binary for the `.dex` file) is available, ART uses it directly. Although `.oat` files are generated regularly, they don't always contain compiled code (AOT binary).
    - If the `.oat` file does not contain compiled code, ART runs through JIT and the interpreter to execute the `.dex` file.

2. JIT is enabled for any application that is not compiled according to the `speed` compilation filter (which says "compile as much as you can from the app").

3. The JIT profile data is dumped to a file in a system directory that only the application can access.

4. The AOT compilation (`dex2oat`) daemon parses that file to drive its compilation.

    ![JIT daemon](https://raw.githubusercontent.com/hhhaiai/Picture/main/img/202208151709874.png)**Figure 3.** JIT daemon activities.

The Google Play service is an example used by other applications that behave similar to shared libraries.

## JIT workflow

![JIT architecture](https://raw.githubusercontent.com/hhhaiai/Picture/main/img/202208151709299.png)

**Figure 4.** JIT data flow.

- Profiling information is stored in the code cache and subjected to garbage collection under memory pressure.

    - There is no guarantee a snapshot taken when the application was in the background will contain complete data (i.e., everything that was JITed).
    - There is no attempt to ensure everything is recorded (as this can impact runtime performance).

- Methods can be in three different states:

    - interpreted (dex code)
    - JIT compiled
    - AOT compiled

    If both JIT and AOT code exists (e.g. due to repeated de-optimizations), the JITed code is preferred.

- The memory requirement to run JIT without impacting foreground app performance depends upon the app in question. Large apps require more memory than small apps. In general, large apps stabilize around 4 MB.

## Turning on JIT logging

To turn on JIT logging, run the following commands:

```
adb root
adb shell stop
adb shell setprop dalvik.vm.extra-opts -verbose:jit
adb shell start
```

## Disabling JIT

To disable JIT, run the following commands:

```
adb root
adb shell stop
adb shell setprop dalvik.vm.usejit false
adb shell start
```

## Forcing compilation

To force compilation, run the following:

```
adb shell cmd package compile
```

Common use cases for force compiling a specific package:

- Profile-based:

    ```
    adb shell cmd package compile -m speed-profile -f my-package
    ```

- Full:

    ```
    adb shell cmd package compile -m speed -f my-package
    ```

Common use cases for force compiling all packages:

- Profile-based:

    ```
    adb shell cmd package compile -m speed-profile -f -a
    ```

- Full:

    ```
    adb shell cmd package compile -m speed -f -a
    ```

## Clearing profile data

To clear profile data and remove compiled code, run the following:

- For one package:

    ```
    adb shell cmd package compile --reset my-package
    ```

- For all packages:

    ```
    adb shell cmd package compile --reset -a
    ```
