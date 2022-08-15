# 无需 Root 也能使用 Xposed！ | Weishu's Notes

Xposed 是 Android 系统上久负盛名的一个框架，它给了普通用户任意 DIY 系统的能力；比如典型的微信防撤回、自动抢红包、修改主题字体，以及模拟位置等等等等。





Xposed 是 Android 系统上久负盛名的一个框架，它给了普通用户任意 DIY 系统的能力；比如典型的微信防撤回、自动抢红包、修改主题字体，以及模拟位置等等等等。不过，使用 Xposed 的前提条件之一就是需要 Root。随着 Android 系统的演进，这一条件达成越来越难了；那么，**能不能不用 Root 就可以享用 Xposed 的功能呢？**

我们想一下，Xposed 为什么需要 Root？从现在的实现来看，因为 Xposed 需要修改系统文件，而这些文件只有 root 权限才能修改；但是这只是当前实现的特性（修改系统分区文件），而非根本原因。Xposed 要实现的最终目的是在任意 App 进程启动之前能任意加载 **特定 Xposed 模块** 的代码；这些特定的 Xposed 模块中能在 App 进程启动之前有机会执行特定代码，从而控制任意进程的行为。归根结底，Xposed 需要控制别的进程，而没有高级权限（Root），越俎代庖是不行的。

有没有别的实现方式？



虽然没有办法控制别的进程，但是在本进程内，几乎是可以为所欲为的；如果换个方式，**把别的 App 放在自己的进程里面运行，然后 Hook 自己** 不就打到目的了嘛？「把别的 App 放在自己的进程里面运行」这种机制是容器，或者通俗点叫双开；「Hook 自己」这是典型的 Dexposed 的思路，不过 Dexposed 不支持 ART——但前不久 epic 的出现完成了这最后一块拼图。（关于 epic 在 ART 上实现 AOP Hook 可以参考 [我为 Dexposed 续一秒——论 ART 上运行时 Method AOP 实现](http://weishu.me/2017/11/23/dexposed-on-art/)

双开的典型实现是 lody 的 [VirtualApp](https://github.com/asLody/VirtualApp)，那么我们来一看 `VirtualApp` 与 `epic` 结合会产生什么样奇妙的化学反应。

我们的思路很清晰：用 VirtualApp 去启动别的 App，在启动过程中通过 epic Hook 本进程，从而控制被启动的 App。同时，由于 Xposed 模块已经比较成熟，而且有成千上万的插件生态，最好能够直接复用 Xposed 的模块，使得在双开环境下，Xposed 模块就跟运行在 Root 手机中的 Xposed 环境中一样。为此，我写了一个 双开环境下的 Xposed 兼容层：[Exposed](https://github.com/android-hacker/exposed)；同时，修改了 VirtualApp 的部分实现，使得它能够在进程的启动的时候加载 Exposed 这个兼容层，代码在这：[VAExposed](https://github.com/android-hacker/VAExposed)。这样，在双开环境中，可以直接加载已有的 Xposed 模块进而实现非 Root 模式下的 Xposed 的功能。更有趣的是，你还可以直接使用 XposedInstaller 安装和管理任意的 Xposed 模块，就跟你使用真正的 Xposed 一样！

具体的代码就不详细讲了，可以直接去看源码 [Exposed](https://github.com/android-hacker/exposed)，[VAExposed](https://github.com/android-hacker/VAExposed) 我们以微信防撤回为例，看看具体的效果：

首先安装 VAExposed 这个修改版的双开 APK，你可以 clone 源码直接 build，也可以使用我编译好的版本 [Github 下载](https://raw.githubusercontent.com/android-hacker/VAExposed/master/VirtualApp/VAExposed_0.1.5.apk) 百度网盘: https://pan.baidu.com/s/1qXB9qtY 密码: i45e

然后安装微信防撤回模块：微信巫师，发布的主页在这：[WeChat Magician（微信巫师）](http://xposed.appkg.com/2558.html)；直接下载 [链接](http://dl-xda.xposed.info/modules/com.gh0u1l5.wechatmagician_v30_1387ce.apk)

接下来需要确保你手机上的微信是微信巫师所支持的，目前支持微信的版本为 6.5.8~6.5.16；如果不是的话需要去下载一个支持的版本，比如 [微信_6.5.8.apk](https://down.shouji.com.cn/wap/wdown/softversion?id=188561&package=com.tencent.mm)。

最后，你需要打开 VAExposed 这个双开软件，添加微信和微信巫师为双开模块，如下图：



<img src="https://raw.githubusercontent.com/hhhaiai/Picture/main/img/202208151734853.png" alt="img" style="zoom:33%;" />

这样，使用双开中的微信，就能享受 Xposed 模块的防撤回功能了！



<img src="https://raw.githubusercontent.com/hhhaiai/Picture/main/img/202208151733511.png" alt="img" style="zoom:35%;" />

另外，你还可以直接在双开中使用 XposedInstaller，然后就可以方便滴下载和管理 Xposed 模块了：

<img src="https://raw.githubusercontent.com/hhhaiai/Picture/main/img/202208151734963.png" alt="img" style="zoom:33%;" />


<img src="https://raw.githubusercontent.com/hhhaiai/Picture/main/img/202208151734950.png" alt="img" style="zoom:33%;" />
<img src="https://raw.githubusercontent.com/hhhaiai/Picture/main/img/202208151734056.png" alt="img" style="zoom:33%;" />



就这样，我们在非 Root 手机下，就能享用 Xposed 模块的功能，Have Fun ：）

不过，在实现完这个功能之后，我不寒而栗：千万不要在 Root 环境或者双开环境下运行关键 App，不然你的微信登录密码，支付宝支付密码，银行卡账号，很有可能被尽收眼底。

PS：目前 Exposed 层的实现处于初级阶段，个人精力非常有限（一般都是凌晨写代码）；如果你对 **实现非 Root 模式下的 Xposed** 感兴趣，非常欢迎跟我一起组队 :) 项目地址在这：https://github.com/android-hacker/exposed。