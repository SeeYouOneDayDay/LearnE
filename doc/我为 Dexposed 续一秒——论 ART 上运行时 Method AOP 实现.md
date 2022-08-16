我为 Dexposed 续一秒——论 ART 上运行时 Method AOP 实现 | Weishu's Notes

两年前阿里开源了 Dexposed 项目，它能够在 Dalvik 上无侵入地实现运行时方法拦截，正如其介绍「enable ‘god’ mode for single android
application」所言，能在非 root 情况下掌控自己进程空间内的任意 Java 方法调用，给我们带来了很大的想象空间。

两年前阿里开源了 [Dexposed](https://github.com/alibaba/dexposed) 项目，它能够在 Dalvik 上无侵入地实现运行时方法拦截，正如其介绍「enable
‘god’ mode for single android application」所言，能在非 root 情况下掌控自己进程空间内的任意 Java
方法调用，给我们带来了很大的想象空间。比如能实现运行时 AOP，在线热修复，做性能分析工具（拦截线程、IO 等资源的创建和销毁）等等。然而，随着 ART 取代 Dalvik 成为 Android
的运行时，一切都似乎戛然而止。

今天，我在 ART 上重新实现了 Dexposed，在它能支持的平台（Android 5.0 ~ 7.1 Thumb2/ARM64) 上，有着与 Dexposed 完全相同的能力和
API；项目地址在这里 [epic](https://github.com/tiann/epic)，感兴趣的可以先试用下:) 然后我们聊一聊 ART 上运行时 Method AOP 的故事。

## ART 有什么特别的？

为什么 Dexposed 能够在 Dalvik 上为所欲为，到 ART 时代就不行了呢？排除其他非技术因素来讲，ART 确实比 Dalvik 复杂太多；更要命的是，从 Android L 到
Android O，每一个 Android 版本中的 ART 变化都是天翻地覆的，大致列举一下：

- Android L(5.0/5.1) 上的 ART 是在 Dalvik 上的 JIT 编译器魔改过来的，名为 quick（虽然有个 portable
  编译器，但是从未启用过）；这个编译器会做一定程度的方法内联，因此很多基于入口替换的 Hook 方式一上来就跪了。
- Android M(6.0) 上的 ART 编译器完全重新实现了：Optimizing。且不说之前在 Android L 上的 Hook 实现要在 M 上重新做一遍，这个编译器的寄存器分配比
  quick 好太多，结果就是 hook 实现的时候你要是乱在栈或者寄存器上放东西，代码很容易就跑飞。
- Android N(7.0/7.1) N 开始采用了混合编译的方式，既有 AOT 也有 JIT，还伴随着解释执行；混合模式对 Hook
  影响是巨大的，以至于 [Xposed 直到今年才正式支持 Android N](https://www.xda-developers.com/official-xposed-framework-android-nougat/)。首先
  JIT 的出现导致方法入口不固定，跑着跑着入口就变了，更麻烦的是还会有 OSR（栈上替换），不仅入口变了，正在运行时方法的汇编代码都可能发生变化；其次，JIT
  的引入带来了更深度的运行时方法内联，这些都使得虚拟机层面的 Hook 更为复杂。
- Android O(8.0) Android O 的 Runtime 做了很多优化，传统 Java VM 有的一些优化手段都已经实现，比如类层次分析，循环优化，向量化等；除此之外，DexCache
  被删除，跨 dex 方法内联以及 Concurrent compacting GC 的引入，使得 Hook 技术变的扑朔迷离。

可以看出，ART 不仅复杂，而且还爱折腾；一言不合就魔改，甚至重写。再加上 Android 的碎片化，这使得实现一个稳定的虚拟机层面上运行时 Java Method AOP 几无可能。

说到这里也许你会问，那 substrate，frida 等 hook 机制不是挺成熟了吗？跟这里说的 ART Hook 有什么联系与区别？事实上，substrate/frida 主要处理 native
层面的 Hook，可以实现任意 C/C++ 函数甚至地址处的调用拦截；而 ART Java Method Hook/AOP 更多地是在虚拟机层面，用来 Hook 和拦截 Java 方法，虚拟机层面的
Hook 底层会使用于 substrate 等类似的 Hook 技术，但是还要处理虚拟机独有的特点，如 GC/JNI/JIT 等。

## 已有的一些方案

虽然 ART 上的运行时 Java Method AOP 实现较为困难，但还是有很多先驱者和探索者。最有名的莫过于 AndFix（虽然它不能实现 AOP）；在学术界，还有两篇研究 ART Hook
的论文，一篇实现了 Callee side dynamic rewrite，另一篇基于虚函数调用原理实现了 vtable hook。另外，除了在讲 epic 之前，我们先看看这些已有的方案。

首先简单介绍下 ART 上的方法调用原理（本文不讨论解释模式，所有 entrypoint 均指 compiled_code_entry_point)。在 ART 中，每一个 Java
方法在虚拟机（注：ART 与虚拟机虽有细微差别，但本文不作区分，两者含义相同，下同）内部都由一个 ArtMethod 对象表示（native 层，实际上是一个 C++ 对象），这个 native 的
ArtMethod 对象包含了此 Java 方法的所有信息，比如名字，参数类型，方法本身代码的入口地址（entrypoint) 等；暂时放下 trampoline 以及 interpreter 和
jit 不谈，一个 Java 方法的执行非常简单：

1. 想办法拿到这个 Java 方法所代表的 ArtMethod 对象
2. 取出其 entrypoint，然后跳转到此处开始执行

[![img](http://blog.dimenspace.com/201605/1511369316918.png)](http://blog.dimenspace.com/201605/1511369316918.png)

### entrypoint replacement

从上面讲述的 ART 方法调用原理可以得到一种很自然的 Hook 办法————直接替换 entrypoint。通过把原方法对应的 ArtMethod 对象的 entrypoint 替换为目标方法的
entrypoint，可以使得原方法被调用过程中取 entrypoint 的时候拿到的是目标方法的 entry，进而直接跳转到目标方法的 code 段；从而达到 Hook 的目的。

AndFix 就是基于这个原理来做热修复的，[Sophix](https://yq.aliyun.com/articles/74598?t=t1#)
对这个方案做了一些改进，也即整体替换，不过原理上都一样。二者在替换方法之后把原方法直接丢弃，因此无法实现
AOP。[AndroidMethodHook](https://github.com/panhongwei/AndroidMethodHook) 基于 Sophix 的原理，用 dexmaker
动态生成类，将原方法保存下来，从而实现了 AOP。

不过这种方案能够生效有一个前提：方法调用必须是先拿到 ArtMethod，再去取 entrypoint 然后跳转实现调用。但是很多情况下，第一步是不必要的；系统知道你要调用的这个方法的
entrypoint 是什么，直接写死在汇编代码里，这样方法调用的时候就不会有取 ArtMethod 这个动作，从而不会去拿被替换的 entrypoint，导致 Hook
失效。这种调用很典型的例子就是系统函数，我们看一下 Android 5.0 上 调用`TextView.setText(Charsequence)` 这个函数的汇编代码：

```
private void callSetText(TextView textView) {
    textView.setText("hehe");
}
```

OAT 文件中的汇编代码：

```
0x00037e10: e92d40e0	push    {r5, r6, r7, lr}
0x00037e14: b088    	sub     sp, sp, #32
0x00037e16: 1c07    	mov     r7, r0
0x00037e18: 9000    	str     r0, [sp, #0]
0x00037e1a: 910d    	str     r1, [sp, #52]
0x00037e1c: 1c16    	mov     r6, r2
0x00037e1e: 6978    	ldr     r0, [r7, #20]
0x00037e20: f8d00ef0	ldr.w   r0, [r0, #3824]
0x00037e24: b198    	cbz     r0, +38 (0x00037e4e)
0x00037e26: 1c05    	mov     r5, r0
0x00037e28: f24a6e29	movw    lr, #42537
0x00037e2c: f2c73e87	movt    lr, #29575
0x00037e30: f24560b0	movw    r0, #22192
0x00037e34: f6c670b4	movt    r0, #28596
0x00037e38: 1c31    	mov     r1, r6
0x00037e3a: 1c2a    	mov     r2, r5
0x00037e3c: f8d1c000	ldr.w   r12, [r1, #0]
suspend point dex PC: 0x0002
GC map objects:  v0 (r5), v1 ([sp + #52]), v2 (r6)
0x00037e40: 47f0    	blx     lr
```

看这两句代码：

```
0x00037e28: f24a6e29	movw    lr, #42537
0x00037e2c: f2c73e87	movt    lr, #29575
```

什么意思呢？lr = 0x7387a629，然后接着就 blx 跳转过去了。事实上，这个地址 `0x7387a629` 就是 TextView.setText(Charsequence)` 这个方法
entrypoint 的绝对地址；我们可以把系统编译好的 oat 代码弄出来看一看：

adb shell oatdump –oat-file=/data/dalvik-cache/arm/system@framework@boot.oat

```
364: void android.widget.TextView.setText(java.lang.CharSequence) (dex_method_idx=28117)
  
  QuickMethodFrameInfo
    frame_size_in_bytes: 48
    core_spill_mask: 0x000081e0 (r5, r6, r7, r8, r15)
    fp_spill_mask: 0x00000000
  CODE: (code_offset=0x037d8629 size_offset=0x037d8624 size=64).
```

其中这个方法的code_offset = 0x037d8629; boot.oat的EXECUTABLE OFFSET 为0x02776000, boot.oat 在proc//maps
中的基址如下：

```
rw-p 00000000 103:1f 32773

700a1000-72818000 r--p 00000000 103:1f 32772     /data/dalvik-cache/arm/system@framework@boot.oat
72818000-74689000 r-xp 02777000 103:1f 32772     /data/dalvik-cache/arm/system@framework@boot.oat
74689000-7468a000 rw-p 045e8000 103:1f 32772     /data/dalvik-cache/arm/system@framework@boot.oat

```

其中 可执行段的地址为 0x72818000，因此算出来的 TextView.setText(CharSequence)
这个方法的地址为 `0x037d8629 - 0x02776000 + 0x72818000 = 0x7387a629`；丝毫不差。

为什么会这么干呢？因为 boot.oat 这个文件在内存中的加载地址是固定的（如果发生变化，所有 APP 的 oat
文件会重新生成，于是又重新固定），因此里面的每一个函数的绝对地址也是固定的，如果你调用系统函数，ART
编译器知道系统每一个函数入口的绝对地址，根本没有必要再去查找方法，因此生成的代码中没有任何查找过程。

所以，从原理上讲，如果要支持系统方法的 Hook，这种方案在很多情况下是行不通的。当然如果你 Hook 自己 App 的代码，并且调用方和被调用方在不同的 dex，在 Android O
之前是没什么问题的（在 Android O 之前跨 dex 一定会走方法查找）。

从上面的分析可以看出，就算不查找 ArtMethod，这个 ArtMethod 的 enntrypoint 所指向代码是一定要用到的（废话，不然 CPU
执行什么，解释执行在暂不讨论）。既然替换入口的方式无法达到 Hook 所有类型方法的目的，那么如果不替换入口，而是直接修改入口里面指向的代码呢？（这种方式有个高大上的学名：callee side
dynamic rewriting)

### dynamic callee-side rewriting

第一次学到这个词是在 Wißfeld, Marvin
的论文 [ArtHook: Callee-side Method Hook Injection on the New Android Runtime ART](http://publications.cispa.saarland/143/)
上。这篇文章很精彩，讲述了各种 Hook 的原理，并且他还在 ART 上实现了 dynamic callee-side rewriting 的 Hook 技术，代码在 github
上：[ArtHook](https://github.com/mar-v-in/ArtHook)

通俗地讲，dynamic callee-side rewriting 其实就是修改 entrypoint 所指向的代码。但是有个基本问题：Hook 函数和原函数的代码长度基本上是不一样的，而且为了实现
AOP，Hook 函数通常比原函数长很多。如果直接把 Hook 函数的代码段 copy 到原函数 entrypoint 所指向的代码段，很可能没地儿放。因此，通常的做法是写一段
trampoline。也就是把原函数 entrypoint 所指向代码的开始几个字节修改为一小段固定的代码，这段代码的唯一作用就是跳转到新的位置开始执行，如果这个「新的位置」就是 Hook
函数，那么基本上就实现了 Hook；这种跳板代码我们一般称之为 trampoline/stub，比如 Android 源码中的
art_quick_invoke_stub/art_quick_resolution_trampoline 等。

这篇论文基本上指明了 ART 上 Method Hook 的方向，而且 Wißfeld 本人的项目 ArtHook 也差不多达到了这个目的。不过他的 Hook
实现中，被用来替换的方法必须写死在代码中，因此无法达到某种程度上的动态 Hook。比如，我想知道所有线程的创建和销毁，因此选择拦截 Thread.class 的 run 方法；但是 Thread
子类实现的 run 方法不一定会调用 Thread 的 run，所以可能会漏掉一些线程。比如：

```
class MyThread extends Thread {
    @Override
    public void run() {
        
        Log.i(TAG, "dang dang dang..");
    }
}

new Thread(new Runnable() {
    @Override
    public void run() {
        Log.i(TAG, "I am started..");
    }
}).start(); 

new Thread() {
    @Override
    public void run() {
        
        
    }
}.start(); 

new MyThread().start();
```

上述例子中，如果仅仅 Hook Thread.class 的 run 方法，只有 Thread1 能被发现，其他两个都是漏网之鱼。既然如此，我们可以 Hook
线程的构造函数（子类必定调用父类），从而知道有哪些自定义的线程类被创建，然后直接 Hook 这些在运行时才发现的类，就能知道所有 Java 线程的创建和销毁。

要解决「不写死 Hook 方法」这个问题有两种思路：其一，直接在运行时凭空创建出一个 Method；其二，把 Hook 收拢到一个统一的方法，在这个方法中做分发处理。

第一种方式：凭空创建 Method，并非 new 一个 Method 对象就行了，这个方法必须要有你想执行的代码，以及必要的 declaring_class, dex_method_index
等成员；要达到这个目的，可以使用运行时字节码生成技术，比如 [dexmaker](https://github.com/linkedin/dexmaker)。另外，Java
本身的动态代理机制也可以也会动态生成代理类，在代理类中有全新创建的方法，如果合适处理，也能达到目的；不过这种方案貌似还无人实现，反倒是 entrypoint replcement 中有人这么做 :(

第二种方式：用一个函数来处理 hook 的分发逻辑，这是典型的 xposed/dexposed 实现方式。不过 Xposed 支持 Android N 过程中直接修改了
libart.so，这种方式对进程内 Hook 是行不通的。dexposed 的 [dev_art](https://github.com/alibaba/dexposed/tree/dev_art)
分支有尝试过实现，但是几乎不可用。

有趣地是，还有另外一个项目 [YAHFA](https://github.com/rk700/YAHFA) 也提出了一种 Hook 方案；不过他这种方案看起来是 entrypoint
replacement 和 dynamic callee-side rewriting 的结合体：把 entrypoint 替换为自己的 trampoline 函数的地址，然后在 trampoline
继续处理跳转逻辑。作者的[博客](http://rk700.github.io/2017/06/30/hook-on-android-n/)值得一看。

### vtable replacement

除了传统的类 inline hook 的 dynamic callee-side rewriting 的 Hook 方式，也有基于虚拟机特定实现的 Hook 技术，比如 vtable hook。ART
中的这种 Hook
方式首先是在论文 [ARTDroid: A Virtual-Method Hooking Framework on Android ART Runtime](http://ceur-ws.org/Vol-1575/paper_10.pdf)
中提出的，作者的实现代码也在 github 上 [art-hooking-vtable](https://github.com/tdr130/art-hooking-vtable)。

[![img](https://raw.githubusercontent.com/hhhaiai/Picture/main/img/202208151705690.png)](http://blog.dimenspace.com/201601/1511342723016.png)

这种 Hook 方式是基于 invoke-virtual 调用原理的；简单来讲，ART 中调用一个 virtual method 的时候，会查相应 Class
类里面的一张表，如果修改这张表对应项的指向，就能达到 Hook
的目的。更详细的实现原理，作者的论文以及他的[博客](http://roptors.me/art/art-part-iii-arthook-framework/)讲的很详细，感兴趣的可以自行围观。

这种方式最大的缺点是只能 Hook virtual 方法，虽然根据作者提供的数据：

59.2% of these methods are declared as virtual 1.0% are non-virtual 39.8% methods not found

高达 99% 的方法都能被 hook 住，不管你信不信，反正我是不信。所以，这种 Hook 方式无法 Hook 所有的调用过程，只能作为一种补充手段使用。

## epic 的实现

### 基本原理

了解到已有项目的一些实现原理以及当前的现状，我们可以知道，要实现一个较为通用的 Hook 技术，几乎只有一条路———基于 dynamic dispatch 的 dynamic callee-side
rewriting。epic 正是使用这种方式实现的，它的基本原理如下图：

[![img](http://blog.dimenspace.com/201601/1511354138004.png)](http://blog.dimenspace.com/201601/1511354138004.png)

在讲解这张图之前，有必要说明一下 ART 中的函数的调用约定。以 Thumb2 为例，子函数调用的参数传递是通过寄存器 r0~r3 以及 sp 寄存器完成的。r0 ~ r3 依次传递第一个至第 4
个参数，同时 *sp,* (sp + 4), *(sp + 8),* (sp + 12) 也存放着 r0~r3 上对应的值；多余的参数通过 sp 传递，比如 *(sp + 16)
放第四个参数，以此类推。同时，函数的返回值放在 r0 寄存器。如果一个参数不能在一个寄存器中放下，那么会占用 2 个或多个寄存器。

在 ART 中，r0 寄存器固定存放被调用方法的 ArtMethod 指针，如果是 non-static 方法，r1 寄存器存放方法的 this 对象；另外，只有 long/double 占用
8bytes，其余所有基本类型和对象类型都占用 4bytes。不过这只是基本情形，不同的 ART 版本对这个调用约定有不同的处理，甚至不完全遵循。

好了我们回到 epic。如上图所述，如果我们要 Hook `android.util.Log.i` 这个方法，那么首先需要找到这个方法的 entrypoint，可以通过这个方法的 ArtMethod
对象得到；然后我们直接修改内存，把这个函数的前 8 个字节：

```
e92d40e0  ; push    {r5, r6, r7, lr} 
b088      ; sub     sp, sp, #32 
1c07      ; mov     r7, r0
```

修改为一段跳转指令：

```
dff800f0  
7f132450
```

这样，在执行`Log.i` 这个函数的时候，会通过这第一段跳板直接跳转到 0x7f132450
这个地址开始执行。这个地址是我们预先分配好的一段内存，也是一段跳转函数，我们姑且称之为二段跳板。在接下来的二段跳板中，我们开始准备分发逻辑：

```
ldr ip, 3f  
cmp r0, ip  
bne.w 5f
```

这段代码是用来判断是否需要执行 Hook 的，如果不需要，跳转到原函数的控制流，进而达到调用原函数的目的。接下来就是一些参数准备：

```
str sp, [ip, #0]
str r2, [ip, #4]
str r3, [ip, #8]
mov r3, ip
ldr r2, 3f
str r2, [ip, #12]
mov r2, r9
ldr pc, 2f
```

在参数准备好之后，直接跳转到另外一个 Java 方法的入口开始执行，这个方法称之为 bridge 方法。bridge 方法接管控制流之后我们就回到了 Java 世界，自此之后我们就可以开始处理 AOP
逻辑。

### 一些问题

基本原理比较简单，但是在实现过程中会有很多问题，这里简单交代一下。

#### bridge 函数分发以及堆栈平衡

从上面的基本介绍我们可以知道，方法的 AOP 逻辑是交给一个 Java 的 bridge 函数统一处理的，那么这个统一的函数如何区分每一个被 Hook 的方法，进而调用对应的回调函数呢？

最直接的办法是把被 Hook 的方法通过额外参数直接传递给 bridge 函数，而传递参数可以通过寄存器和堆栈实现。用来传递参数的寄存器（如 r0~r3) 最好是不要**直接**
改的，不然我们的处理函数可能就收到不到原函数对应的参数，进而无法完成调用原函数的逻辑。如果用堆栈传递参数的话，我们是直接在堆栈上分配内存吗？

事实证明这样做是不行的，如果我们在二段跳板代码里面开辟堆栈，进而修改了 sp 寄存器；那么在我们修改 sp 到调用 bridge 函数的这段时间里，堆栈结构与不 Hook 的时候是不一样的（虽然
bridge 函数执行完毕之后我们可以恢复正常）；在这段时间里如果虚拟机需要进行栈回溯，sp 被修改的那一帧会由于回溯不到对应的函数引发致命错误，导致 Runtime 直接
Abort。什么时候会回溯堆栈？发生异常或者 GC 的时候。最直观的感受就是，如果 bridge 函数里面有任何异常抛出（即使被 try..catch 住）就会使虚拟机直接崩溃。dexposed 的
dev_art 分支中的 AOP 实现就有这个问题。

既然无法分配新的堆栈，那么能否找到空闲的空间使用呢？上面我们在介绍 Thumb2 调用约定的时候提到，r0~r3 传递第一至第四个参数，sp ~ sp + 12
也传递第一至第四个参数，看起来好像是重复了；我们能否把 sp ~ sp + 12 这段空间利用起来呢？

但是实际实现的过程中又发现，此路不通。你以为就你会耍这点小聪明吗？虚拟机本身也是知道 sp + 12
这段空间相当于是浪费的，因此他直接把这段空间当做类似寄存器使用了；如果你把额外的参数丢在这里，那么根本就收不到参数，因为函数调用一旦发生，ART 很可能直接把这段内存直接使用了。

既然如此，我们只能把要传递的一个或者多个额外参数打包在一起（比如放在结构体），通过指针一块传递了。再此观察我们上面的二段跳板代码：

```
ldr ip, 4f
str sp, [ip, #0]
str r2, [ip, #4]
str r3, [ip, #8]
mov r3, ip
ldr r2, 3f
str r2, [ip, #12]
```

其中，`4f` 处是我们预先分配好的一段 16 字节的内存 (假设起始地址为 base)；我们把 sp 放到 *(base) 上，把 r2 寄存器（原第三个参数）放到* (base + 4)，把
r3（原第四个参数）放到 *(base + 8)，把 `3f`（被 Hook 函数的地址）放到* (base + 12)；然后把这个 base 的地址放在 r3 寄存器里面，这样根据调用约定，我们的
bridge 函数就可以在第四个参数上收到四个打包好的数据，然后通过相同的访问方式就可以把原始数据取出来。这些数据中就包括了被 Hook 的原函数地址，通过这个地址，我们可以区分不同的被 Hook
函数，进而触发各自对应的处理逻辑。

#### 入口重合的问题

在二段跳板函数的开始处，有这么一段代码：

```
ldr ip, 3f  
cmp r0, ip  
bne.w 5f
```

也许你会问，这个比较逻辑是有必要的吗？除了达到调用原函数的目的之外，这个逻辑还有一个更重要的用途：区分入口相同，但是实际上 Java 方法完全不同的处理逻辑。

什么时候不同的 Java 函数的入口会一样呢？至少有下面几种情况：

1. 所有 ART 版本上未被 resolve 的 static 函数
2. Android N 以上的未被编译的所有函数
3. 代码逻辑一模一样的函数
4. JNI 函数

static 函数是 lazy resolve 的，在方法没有被调用之前，static 函数的入口地址是一个跳板函数，名为
art_quick_resolution_trampoline，这个跳转函数做的事情就是去 resvole 原始函数，然后进行真正的调用逻辑；因此没有被调用的 static 函数的
entrypoint 都是一样的。

Android N 以上，APK 安装的时候，默认是不会触发 AOT 编译的；因此如果刚安装完你去看 apk 生成的 OAT 文件，会发现里面的 code 都是空。在这些方法被 resolve
的时候，如果 ART 发现 code 是空，会把 entrypoint 设置为解释执行的入口；接下来如果此方法被执行会直接进入到解释器。所以，Android N 上未被编译的所有方法入口地址都相同。

如果代码逻辑完全一样，那么 AOT 编译器会发现这完全可以用一个函数来代替，于是这些函数都有了同一个入口地址；而 JNI 函数由于函数体都是空（也即所有代码相同），理所当然会共享同一个入口。

如果没有这段处理逻辑，你会发现你 Hook 一个函数的时候，很可能莫名其妙滴 Hook 了一堆你压根都不知道是什么的函数。

#### 指针与对象转换

在基本的 bridge 函数调用（从汇编进入 Java 世界）的问题搞定之后，我们会碰到一个新问题：在 bridge
函数中接受到的参数都是一些地址，但是原函数的参数明明是一些对象，怎么把地址还原成原始的参数呢？

如果传递的是基本类型，那么接受到的地址其实就是基本类型值的表示；但是如果传递的是对象，那接受到的 int/long 是个什么东西？

这个问题一言难尽，它的背后是 ART 的对象模型；这里我简单说明一下。一个最直观的问题就是：JNI 中的 jobject，Java 中的 Object，ART 中的 art::mirror::
Object 到底是个什么关系？

实际上，art::mirror::Object 是 Java 的 Object 在 Runtime 中的表示，java.lang.Object 的地址就是 art::mirror::Object
的地址；但是 jobject 略有不同，它并非地址，而是一个句柄（或者说透明引用）。为何如此？

因为 JNI 对于 ART 来说是外部环境，如果直接把 ART 中的对象地址交给 JNI 层（也就是 jobject 直接就是 Object 的地址），其一不是很安全，其二直接暴露内部实现不妥。就拿
GC 来说，虚拟机在 GC 过程中很可能移动对象，这样对象的地址就会发生变化，如果 JNI 直接使用地址，那么对 GC 的实现提出了很高要求。因此，典型的 Java 虚拟机对 JNI
的支持中，jobject 都是句柄（或者称之为透明引用）；ART 虚拟机内部可以在 joject 与 art::mirror::Object 中自由转换，但是 JNI 层只能拿这个句柄去标志某个对象。

那么 jobject 与 java.lang.Object 如何转换呢？这个 so easy，直接通过一次 JNI 调用，ART 就自动完成了转换。

因此归根结底，我们需要找到一个函数，它能实现把 art::mirror::Object 转换为 jobject 对象，这样我们可以通过 JNI 进而转化为 Java 对象。这样的函数确实有，那就是：

```
art::JavaVMExt::AddWeakGlobalReference(art::Thread*, art::mirror::Object*)
```

此函数在 libart.so 中，我们可以通过 `dlsym`拿到函数指针，然后直接调用。不过这个函数有一个 art::Thread *的参数，如何拿到这个参数呢？查阅 art::Thread
的源码发现，这个 art::Thread 与 java.lang.Thread 也有某种对应关系，它们是通过 peer 结合在一起的（JNI 文档中有讲）。也就是说，java.lang.Thread
类中的 nativePeer 成员代表的就是当前线程的 art::Thread* 对象。这个问题迎刃而解。

#### Android N 无法 dlsym

上文提到，为了实现对象和指针的转换，我们需要 `dlsym` 一个 libart.so 中的导出函数；但不幸地是，在 Android N 中，Google 禁止了这种行为，如果你用 `dlsym`
去取符号，返回的结果是 nullptr。怎么办呢？

libart.so 不过是一个加载在内存中的 elf 文件而已。我们通过读取 `/proc/self/maps` 拿到这个文件的加载基地址，然后直接解析 ELF 文件格式，查出这个符号在 ELF
文件中的偏移，再加上内存基址，就能得到这个符号真正的地址。不过这过程已经有人实现了，而且放在了 github
上：[Nougat_dlfunctions](https://github.com/avs333/Nougat_dlfunctions) 可以直接使用 :)

#### Android N 解释执行

Android N 采用了混合编译的模式，既有解释执行，也有 AOT 和 JIT；APK 刚安装完毕是解释执行的，运行时 JIT
会收集方法调用信息，必要的时候直接编译此方法，甚至栈上替换；在设备空闲时，系统会根据收集到的信息执行 AOT 操作。

那么在 APK 刚装完然后使用的那么几次，方法都是解释执行的，我们要 Hook 掉解释执行的入口吗？这当然可以，但是如果解释执行到一半方法入口被替换为 JIT 编译好的机器码的入口，那么本次 Hook
就会失效；我们还需要把 JIT 编译的机器码入口也拦截住。但是问题是，我们何时知道 JIT 执行完成？

所以这种方式实行起来比较麻烦，**还不如一开始就全部是机器码** 这样我们只用 Hook 机器码的 entrypoint 就可以了。事实上，Android N 可以手动触发 AOT
全量编译，如 [官方文档](https://source.android.com/devices/tech/dalvik/jit-compiler) 所述，可以通过如下命令手动执行 AOT 编译：

adb shell cmd package compile -m speed -f

这样一来，我们一般情况下就不用管解释器的事了。

虽然多这么一个步骤，勉强能解决问题，但还是有点小瑕疵；(毕竟要多这么一步嘛！何况如果这个投入线上使用，你指望用户给你主动编译？）在研究了一段时间的 JIT 代码之后，我发现**可以主动调用 JIT
编译某个方法**。这样，在 Hook 之前我们可以先请求 JIT 编译此方法，得到机器码的 entrypoint，然后按照正常的流程 Hook 即可。具体如何调用 JIT 可以参阅 epic
的[源码](https://github.com/tiann/epic)。

#### Android N JIT 编译

上文提到 Android N 上开启了 JIT 编译器，即使我们手动触发全量 AOT 编译，在运行时这种机制依然存在；JIT 的一个潜在隐患就是，他有可能动态修改代码，这使得在 Android N
上的 Hook 可能随机出现 crash。

记得我在刚实现完 Android N 上的 Hook 之后，发现我的测试 case 偶尔会崩溃，崩溃过程完全没有规律，而且崩溃的错误几乎都是 SIG 11。当时追查了一段时间，觉得这种随机崩溃可能跟
2 个原因有关：GC 或者 JIT；不过一直没有找到证据。

某天半夜我发现一个有趣的现象，如果我把测试 case 中的 Logcat 日志输出关掉，崩溃的概率会小很多——如果输出 Logcat
可能测试八九次就闪退了，但如果关掉日志，要数十次或者几乎不会闪退。当时我就怀疑是不是碰上了薛定谔猫。

理性分析了一番之后我觉得这种尺度不可能触发量子效应，于是我只能把锅摔倒 Log 头上。我在想是不是 Log 有 IO 操作导致 hook 过程太慢了使得这段时间别的线程有机会修改代码？于是我在
Hook 过程中 Sleep 5s 发现一点问题没有。实在没辙，我就一条条删 Log，结果发现一个神奇的现象：Log 越多越容易崩。然后我就写个循环输出日志 100 次，结果几乎是毕现闪退。

事情到这里我就瞬间明白了：调用 Log 的过程中很有可能由于 Log 函数调用次数过多进而达到 JIT 编译的阈值从而触发了 JIT，这时候 JIT 线程修改了被执行函数的代码，而 Hook
的过程也会修改代码，这导致内存中的值不可预期，从而引发随机 crash。

按照这种情况推测的话，JIT 的存在导致 Android N 上的 Hook 几乎是毕现闪退的。因为我的测试 demo 代码量很少，一个稍微有点规模的 App 很容易触发 JIT 编译，一旦在 JIT
过程中执行 Hook，那么必崩无疑。

因此比较好的做法是，在 Hook 的过程中暂停所有其他线程，不让它们有机会修改代码；在 Hook 完毕之后在恢复执行。那么问题来了，如何暂停 / 恢复所有线程？Google 了一番发现有人通过
ptrace 实现：开一个 linux task 然后挨个 ptrace 本进程内的所有子线程，这样就是实现了暂停。这种方式很重而且不是特别稳定，于是我就放弃了。ART
虚拟机内部一定也有暂停线程的需求（比如 GC），因此我可以选择直接调用 ART 的内部函数。

在源码里面捞了一番之后果然在 thread_list.cc 中找到了这样的函数 resumeAll/suspendAll；不过遗憾的是这两个函数是 ThreadList
类的成员函数，要调用他们必须拿到 ThreadList 的指针；一般情况下是没有比较稳定的方式拿到这个对象的。不过好在 Android 源码通过 RAII 机制对
suspendAll/resumeAll 做了一个封装，名为 `ScopedSuspendAll`
这类的构造函数里面执行暂停操作，析构函数执行恢复操作，在栈上分配变量此类型的变量之后，在这个变量的作用域内可以自动实现暂停和恢复。因此我只需要用 `dlsym`
拿到构造函数和析构函数的符号之后，直接调用就能实现暂停恢复功能。详细实现见 epic [源码](https://github.com/tiann/epic)

写了这么多，实际上还有很多想写的没有写完；比如 Android M Optimizing 编译器上的寄存器分配问题，long/double 参数的处理细节，不同 ART 版本的调用约定 与
ATPCS/AAPCS 之间不同等；不过来日方长，这些问题以后在慢慢道来吧 :)

## 使用

扯了这么久的实现原理，我们来看看这玩意儿具体怎么用吧。只需要在你的项目中加入 epic 的依赖即可 (jcenter 仓库)：

```
dependencies {
    compile 'me.weishu:epic:0.1.2@aar'
}
```

然后就可以在你的项目中做 AOP Hook，比如说要拦截所有 Java 线程的创建，我们可以用如下代码：

```
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

这里有 2 个 AOP 点，其一是 Thread.class 的 run 方法，拦截这个方法，我们可以知道所有通过 Thread 类本身创建的线程；其二是 Thread 的构造函数，这个 Hook
点我们可以知道运行时具体有哪些类继承了 Thread.class 类，在找到这样的子类之后，直接 hook 掉这个类的 run 方法，从而达到了拦截所有线程创建的目的。

当然，还有很多有趣的 AOP 点等待你去挖掘，这一切取决于您的想象力 :)

## 局限

上文提到，「要在 ART 上实现一个完善而稳定的 Hook 机制，几无可能」，epic 也不例外：它也有它自己的缺点，有些是先天的，有些是后天的，还有一些我没有发现的 ~_~；比如说：

1. 受限于 dynamic callee-side rewrite 机制，如果被 Hook 函数的 code 段太短以至于一个简单的 trampoline 跳转都放不下，那么 epic 无能为力。
2. 如果 ART 中有深度内联，直接把本函数的代码内联到调用者，那么 epic 也搞不定。
3. Android O(8.0) 还没有去研究和实现。
4. 当前仅支持 thumb2/arm64 指令集，arm32/x86/mips 还没有支持。
5. 在支持硬浮点的 cpu 架构，比如 (armeabi-v7a, arm64-v8a) 上，带有 double/float 参数的函数 Hook 可能有问题，没有充分测试。
6. 还有一些其他机型上的，或者我没有发现的闪退。

我本人只在 Android 5.0, 5.1, 6.0, 7.0, 7.1 的个别机型，以及这些机型的 thumb2 指令集，和 6.0/7.1 的 arm64
指令集做过测试；其他的机型均未测试，因此这么长的文章还读到最后的你，不妨拿出你手头的手机帮我测试一下，在下感激不尽 :)