[![Join the chat at https://gitter.im/android-hacker/epic](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/android-hacker/epic?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)   

[中文文档入口](README.md "中文")

What is it?
-----------

Epic is the continuation of [Dexposed](https://github.com/alibaba/dexposed) on ART (Supports 5.0 ~ 11).

> Dexposed is a powerful yet non-invasive runtime [AOP (Aspect-oriented Programming)](http://en.wikipedia.org/wiki/Aspect-oriented_programming) framework
for Android app development, based on the work of open-source [Xposed](https://github.com/rovo89/Xposed) [framework](https://github.com/rovo89/XposedBridge) project.
>
> The AOP of Dexposed is implemented purely non-invasive, without any annotation processor,
weaver or bytecode rewriter. The integration is as simple as loading a small JNI library
in just one line of code at the initialization phase of your app.
>
> Not only the code of your app, but also the code of Android framework that running in your
app process can be hooked.

Epic keeps the same API and all capability of Dexposed, you can do anything which is supported by Dexposed.

Typical use-cases
-----------------

* Classic AOP programming
* Instrumentation (for testing, performance monitoring and etc.)
* Security audit (sensitive api check,Smash shell)
* Just for fun :)


Integration
-----------

Directly add epic aar to your project as compile libraries, Gradle dependency like following(jitpack):

```groovy
dependencies {
    compile 'com.github.tiann:epic:0.11.2'
}
```

Everything is ready.

Basic usage
-----------

There are three injection points for a given method: *before*, *after*, *origin*.

Example 1: monitor the creation and destroy of java thread

```java
class ThreadMethodHook extends XC_MethodHook{
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        super.beforeHookedMethod(param);
        Thread t = (Thread) param.thisObject;
        Log.i(TAG, "thread:" + t + ", started..");
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        super.afterHookedMethod(param);
        Thread t = (Thread) param.thisObject;
        Log.i(TAG, "thread:" + t + ", exit..");
    }
}

DexposedBridge.hookAllConstructors(Thread.class, new XC_MethodHook() {
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        super.afterHookedMethod(param);
        Thread thread = (Thread) param.thisObject;
        Class<?> clazz = thread.getClass();
        if (clazz != Thread.class) {
            Log.d(TAG, "found class extend Thread:" + clazz);
            DexposedBridge.findAndHookMethod(clazz, "run", new ThreadMethodHook());
        }
        Log.d(TAG, "Thread: " + thread.getName() + " class:" + thread.getClass() +  " is created.");
    }
});
DexposedBridge.findAndHookMethod(Thread.class, "run", new ThreadMethodHook());
```

Example 2: Intercept the dex loading behavior

```java
DexposedBridge.findAndHookMethod(DexFile.class, "loadDex", String.class, String.class, int.class, new XC_MethodHook() {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        super.beforeHookedMethod(param);
        String dex = (String) param.args[0];
        String odex = (String) param.args[1];
        Log.i(TAG, "load dex, input:" + dex + ", output:" + odex);
    }
});
```

Checkout the `sample` project to find out more.

Support
----------

Epic supports ART thumb2 and arm64 architecture from Android 5.0 ~ 11. arm32, x86, x86_64 and mips are not supported now (Thus it cannot work on android emulator).


Known Issues
-------------

1. Short method (instruction less 8 bytes on thumb2 or less 16bytes in ARM64) are not supported.
2. Fully inline methods are not supported.

Contribute
----------

We are open to constructive contributions from the community, especially pull request
and quality bug report. **Currently, the implementation for ART is not proved in large scale, we value your help to test or improve the implementation.**

You can clone this project, build and install the sample app, just make some click  in your device, if some bugs/crash occurs, please file an issue or a pull request, I would appreciate it :)

Thanks
-------

1. [Dexposed](https://github.com/alibaba/dexposed)
2. [Xposed](http://repo.xposed.info/module/de.robv.android.xposed.installer)
3. [mar-v-in/ArtHook](https://github.com/mar-v-in/ArtHook)
4. [Nougat_dlfunctions](https://github.com/avs333/Nougat_dlfunctions.git)

Contact me
----------

twsxtd@gmail.com

[Join discussion](https://gitter.im/android-hacker/epic?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) 
