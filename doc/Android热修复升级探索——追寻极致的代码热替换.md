# Android 热修复升级探索——追寻极致的代码热替换 - 阿里云开发者社区

阿里云移动热修复 Sophix 技术实现 。手机淘宝开发团队对代码的 native 替换原理重新进行了深入思考，从克服其限制和兼容性入手，以一种更加优雅的替换思路，实现了即时生效的代码热修复。



## 前言



前段时间，Android 平台上涌现了一系列热修复方案，如阿里的 Andfix、微信的 Tinker、QQ 空间的 Nuva、手 Q 的 QFix 等等。



其中，Andfix 的**即时生效**令人印象深刻，它稍显另类，并不需要重新启动，而是在加载补丁后直接对方法进行替换就可以完成修复，然而它的使用限制也遭遇到更多的质疑。



我们也对代码的 native 替换原理重新进行了深入思考，从克服其限制和兼容性入手，以一种更加优雅的替换思路，实现了即时生效的代码热修复。



## Andfix 回顾



我们先来看一下，为何唯独 Andfix 能够做到即时生效呢？



原因是这样的，在 app 运行到一半的时候，所有需要发生变更的 Class 已经被加载过了，在 Android 上是无法对一个 Class 进行卸载的。而腾讯系的方案，都是让 Classloader 去加载新的类。如果不重启，原来的类还在虚拟机中，就无法加载新类。因此，只有在下次重启的时候，在还没走到业务逻辑之前抢先加载补丁中的新类，这样后续访问这个类时，就会 Resolve 为新的类。从而达到热修复的目的。



Andfix 采用的方法是，在已经加载了的类中直接在 native 层替换掉原有方法，是在原来类的基础上进行修改的。我们这就来看一下 Andfix 的具体实现。



其核心在于 replaceMethod 函数



```
@AndFix/src/com/alipay/euler/andfix/AndFix.java

private static native void replaceMethod(Method src, Method dest);
```



这是一个 native 方法，它的参数是在 Java 层通过反射机制得到的 Method 对象所对应的 jobject。src 对应的是需要被替换的原有方法。而 dest 对应的就是新方法，新方法存在于补丁包的新类中，也就是补丁方法。



```
@AndFix/jni/andfix.cpp

static void replaceMethod(JNIEnv* env, jclass clazz, jobject src,
        jobject dest) {
    if (isArt) {
        art_replaceMethod(env, src, dest);
    } else {
        dalvik_replaceMethod(env, src, dest);
    }
}
```



Android 的 java 运行环境，在 4.4 以下用的是 dalvik 虚拟机，而在 4.4 以上用的是 art 虚拟机。



```
@AndFix/jni/art/art_method_replace.cpp

extern void __attribute__ ((visibility ("hidden"))) art_replaceMethod(
        JNIEnv* env, jobject src, jobject dest) {
    if (apilevel > 23) {
        replace_7_0(env, src, dest);
    } else if (apilevel > 22) {
        replace_6_0(env, src, dest);
    } else if (apilevel > 21) {
        replace_5_1(env, src, dest);
    } else if (apilevel > 19) {
        replace_5_0(env, src, dest);
    }else{
        replace_4_4(env, src, dest);
    }
}
```



我们以 art 为例，对于不同 Android 版本的 art，底层 Java 对象的数据结构是不同的，因而会进一步区分不同的替换函数，这里我们以 Android 6.0 为例，对应的就是`replace_6_0`。



```
@AndFix/jni/art/art_method_replace_6_0.cpp

void replace_6_0(JNIEnv* env, jobject src, jobject dest) {

    
    art::mirror::ArtMethod* smeth =
            (art::mirror::ArtMethod*) env->FromReflectedMethod(src);

    art::mirror::ArtMethod* dmeth =
            (art::mirror::ArtMethod*) env->FromReflectedMethod(dest);

    ... ...
    
    
    smeth->declaring_class_ = dmeth->declaring_class_;
    smeth->dex_cache_resolved_methods_ = dmeth->dex_cache_resolved_methods_;
    smeth->dex_cache_resolved_types_ = dmeth->dex_cache_resolved_types_;
    smeth->access_flags_ = dmeth->access_flags_;
    smeth->dex_code_item_offset_ = dmeth->dex_code_item_offset_;
    smeth->dex_method_index_ = dmeth->dex_method_index_;
    smeth->method_index_ = dmeth->method_index_;

    smeth->ptr_sized_fields_.entry_point_from_interpreter_ =
    dmeth->ptr_sized_fields_.entry_point_from_interpreter_;

    smeth->ptr_sized_fields_.entry_point_from_jni_ =
    dmeth->ptr_sized_fields_.entry_point_from_jni_;
    smeth->ptr_sized_fields_.entry_point_from_quick_compiled_code_ =
    dmeth->ptr_sized_fields_.entry_point_from_quick_compiled_code_;

    LOGD("replace_6_0: %d , %d",
         smeth->ptr_sized_fields_.entry_point_from_quick_compiled_code_,
         dmeth->ptr_sized_fields_.entry_point_from_quick_compiled_code_);
}
```



每一个 Java 方法在 art 中都对应着一个 ArtMethod，ArtMethod 记录了这个 Java 方法的所有信息，包括所属类、访问权限、代码执行地址等等。



通过`env->FromReflectedMethod`，可以由 Method 对象得到这个方法对应的 ArtMethod 的真正起始地址。然后就可以把它强转为 ArtMethod 指针，从而对其所有成员进行修改。



这样全部替换完之后就完成了热修复逻辑。以后调用这个方法时就会直接走到新方法的实现中了。



## 虚拟机调用方法的原理



为什么这样替换完就可以实现热修复呢？这需要从虚拟机调用方法的原理说起。



在 Android 6.0，art 虚拟机中 ArtMethod 的结构是这个样子的：



```
@art/runtime/art_method.h

class ArtMethod FINAL {
 ... ...

 protected:
  
  
  GcRoot<mirror::Class> declaring_class_;

  
  GcRoot<mirror::PointerArray> dex_cache_resolved_methods_;

  
  GcRoot<mirror::ObjectArray<mirror::Class>> dex_cache_resolved_types_;

  
  uint32_t access_flags_;

  

  
  uint32_t dex_code_item_offset_;

  
  uint32_t dex_method_index_;

  

  
  
  
  uint32_t method_index_;

  

  
  
  
  struct PACKED(4) PtrSizedFields {
    
    
    void* entry_point_from_interpreter_;

    
    void* entry_point_from_jni_;

    
    
    void* entry_point_from_quick_compiled_code_;
  } ptr_sized_fields_;

... ...
}
```



这其中最重要的字段就是 entry_point_from_interprete_和 entry_point_from_quick_compiled_code_了，从名字可以看出来，他们就是方法的执行入口。我们知道，Java 代码在 Android 中会被编译为 Dex Code。



art 中可以采用解释模式或者 AOT 机器码模式执行。



解释模式，就是取出 Dex Code，逐条解释执行就行了。如果方法的调用者是以解释模式运行的，在调用这个方法时，就会取得这个方法的 entry_point_from_interpreter_，然后跳转过去执行。



而如果是 AOT 的方式，就会先预编译好 Dex Code 对应的机器码，然后运行期直接执行机器码就行了，不需要一条条地解释执行 Dex Code。如果方法的调用者是以 AOT 机器码方式执行的，在调用这个方法时，就是跳转到 entry_point_from_quick_compiled_code_执行。



那我们是不是只需要替换这几个 entry_point_* 入口地址就能够实现方法替换了呢？



并没有这么简单。因为不论是解释模式或是 AOT 机器码模式，在运行期间还会需要用到 ArtMethod 里面的其他成员字段。



就以 AOT 机器码模式为例，虽然 Dex Code 被编译成了机器码。但是机器码并不是可以脱离虚拟机而单独运行的，以这段简单的代码为例：



```
public class MainActivity extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

... ...
```



编译为 AOT 机器码后，是这样的：



```
  7: void com.patch.demo.MainActivity.onCreate(android.os.Bundle) (dex_method_idx=20639)
    DEX CODE:
      0x0000: 6f20 4600 1000            | invoke-super {v0, v1}, void android.app.Activity.onCreate(android.os.Bundle) 
      0x0003: 0e00                      | return-void


    CODE: (code_offset=0x006fdbac size_offset=0x006fdba8 size=96)
      ... ...
      0x006fdbe0: f94003e0  ldr x0, [sp]        ;x0 = MainActivity.onCreate对应的ArtMethod指针
      0x006fdbe4: b9400400  ldr w0, [x0, #4]    ;w0 = [x0 + 4] = dex_cache_resolved_methods_字段
      0x006fdbe8: f9412000  ldr x0, [x0, #576]  ;x0 = [x0 + 576] = dex_cache_resolved_methods_数组的第72(=576/8)个元素，即对应Activity.onCreate的ArtMethod指针
      0x006fdbec: f940181e  ldr lr, [x0, #48]   ;lr = [x0 + 48] = Activity.onCreate的ArtMethod成员的entry_point_from_quick_compiled_code_执行入口点
      0x006fdbf0: d63f03c0  blr lr              ;调用Activity.onCreate
      ... ...
```



这里面我去掉了一些校验之类的无关代码，可以很清楚看到，在调用一个方法时，取得了 ArtMethod 中的 dex_cache_resolved_methods_，这是一个存放 ArtMethod * 的指针数组，通过它就可以访问到这个 Method 所在 Dex 中所有的 Method 所对应的 ArtMethod*。



Activity.onCreate 的方法索引是 70，由于是 64 位系统，因此每个指针的大小为 8 字节，又由于 ArtMethod * 元素是从这个数组的第 0x2 个位置开始存放的，因此偏移 (70 + 2) * 8 = 576 的位置正是 Activity.onCreate 的 ArtMethod 指针。



这是一个比较简单的例子，而在实际代码中，有许多更为复杂的调用情况。很多情况下还需要用到 dex_code_item_offset_等字段。由此可以看出，AOT 机器码的执行过程，还是会有对于虚拟机以及 ArtMethod 其他成员字段的依赖。



因此，当把一个旧方法的所有成员字段换成都新方法后，执行时所有数据就可以保持和新方法的一致。这样在所有执行到旧方法的地方，会取得新方法的执行入口、所属 class、方法索引号以及所属 dex 信息，然后像调用旧方法一样顺滑地执行到新方法的逻辑。



## 兼容性问题的根源



然而，目前市面上几乎所有的 native 替换方案，比如 Andfix 和另一种 Hook 框架 Legend，都是写死了 ArtMethod 结构体，这会带来巨大的兼容性问题。



从刚才的分析可以看到，虽然 Andfix 是把底层结构强转为了 art::mirror::ArtMethod，但这里的 art::mirror::ArtMethod 并非等同于 app 运行时所在设备虚拟机底层的 art::mirror::ArtMethod，而是 Andfix 自己构造的 art::mirror::ArtMethod。



```
@AndFix/jni/art/art_6_0.h

class ArtMethod {
public:

    
    
    uint32_t declaring_class_;
    
    uint32_t dex_cache_resolved_methods_;
    
    uint32_t dex_cache_resolved_types_;
    
    uint32_t access_flags_;
    
    
    uint32_t dex_code_item_offset_;
    
    uint32_t dex_method_index_;
    
    
    
    
    uint32_t method_index_;
    
    
    
    
    struct PtrSizedFields {
        
        
        void* entry_point_from_interpreter_;
        
        void* entry_point_from_jni_;
        
        
        void* entry_point_from_quick_compiled_code_;
    } ptr_sized_fields_;
};
```



我们再来回顾一下 Android 开源代码里面 art 虚拟机里的 ArtMethod:



```
@art/runtime/art_method.h

class ArtMethod FINAL {
 ... ...

 protected:
  
  
  GcRoot<mirror::Class> declaring_class_;

  
  GcRoot<mirror::PointerArray> dex_cache_resolved_methods_;

  
  GcRoot<mirror::ObjectArray<mirror::Class>> dex_cache_resolved_types_;

  
  uint32_t access_flags_;

  

  
  uint32_t dex_code_item_offset_;

  
  uint32_t dex_method_index_;

  

  
  
  
  uint32_t method_index_;

  

  
  
  
  struct PACKED(4) PtrSizedFields {
    
    
    void* entry_point_from_interpreter_;

    
    void* entry_point_from_jni_;

    
    
    void* entry_point_from_quick_compiled_code_;
  } ptr_sized_fields_;

... ...
}
```



可以看到，ArtMethod 结构里的各个成员的大小是和 AOSP 开源代码里完全一致的。这是由于 Android 源码是公开的，Andfix 里面的这个 ArtMethod 自然是遵照 android 虚拟机 art 源码里面的 ArtMethod 构建的。



但是，由于 Android 是开源的，各个手机厂商都可以对代码进行改造，而 Andfix 里 ArtMethod 的结构是根据公开的 Android 源码中的结构写死的。如果某个厂商对这个 ArtMethod 结构体进行了修改，就和原先开源代码里的结构不一致，那么在这个修改过了的设备上，替换机制就会出问题。



比如，在 Andfix 替换`declaring_class_`的地方，



```
    smeth->declaring_class_ = dmeth->declaring_class_;
```



由于`declaring_class_`是 andfix 里 ArtMethod 的第一个成员，因此它和以下这行代码等价：



```
    *(uint32_t*) (smeth + 0) = *(uint32_t*) (dmeth + 0)
```



如果手机厂商在 ArtMethod 结构体的`declaring_class_`前面添加了一个字段`additional_`，那么，additional_就成为了 ArtMethod 的第一个成员，所以 smeth + 0 这个位置在这台设备上实际就变成了`additional_`，而不再是`declaring_class_`字段。所以这行代码的真正含义就变成了：



```
    smeth->additional_ = dmeth->additional_;
```



这样就和原先替换`declaring_class_`的逻辑不一致，从而无法正常执行热修复逻辑。



这也正是 Andfix 不支持很多机型的原因，很大的可能，就是因为这些机型修改了底层的虚拟机结构。



## 突破底层结构差异



知道了 native 替换方式兼容性问题的原因，我们是否有办法寻求一种新的方式，不依赖于 ROM 底层方法结构的实现而达到替换效果呢？



我们发现，这样 native 层面替换思路，其实就是替换 ArtMethod 的所有成员。那么，我们并不需要构造出 ArtMethod 具体的各个成员字段，只要把 ArtMethod 的作为整体进行替换，这样不就可以了吗？



也就是把原先这样的逐一替换

![img](https://raw.githubusercontent.com/hhhaiai/Picture/main/img/202208151737679.png)





变成了这样的整体替换

![img](https://raw.githubusercontent.com/hhhaiai/Picture/main/img/202208151737748.png)





因此 Andfix 这一系列繁琐的替换:



```
    smeth->declaring_class_ = dmeth->declaring_class_;
    smeth->dex_cache_resolved_methods_ = dmeth->dex_cache_resolved_methods_;
    smeth->dex_cache_resolved_types_ = dmeth->dex_cache_resolved_types_;
    smeth->access_flags_ = dmeth->access_flags_;
    smeth->dex_code_item_offset_ = dmeth->dex_code_item_offset_;
    smeth->dex_method_index_ = dmeth->dex_method_index_;
    smeth->method_index_ = dmeth->method_index_;
    ... ...
```



其实可以浓缩为：



```
    memcpy(smeth, dmeth, sizeof(ArtMethod));
```



就是这样，一句话就能取代上面一堆代码，这正是我们深入理解替换机制的本质之后研发出的新替换方案。



刚才提到过，不同的手机厂商都可以对底层的 ArtMethod 进行任意修改，但即使他们把 ArtMethod 改得六亲不认，只要我像这样把整个 ArtMethod 结构体完整替换了，就能够把所有旧方法成员自动对应地换成新方法的成员。



**但这其中最关键的地方，在于 sizeof(ArtMethod)。如果 size 计算有偏差，导致部分成员没有被替换，或者替换区域超出了边界，都会导致严重的问题。**



对于 ROM 开发者而言，是在 art 源代码里面，所以一个简单的`sizeof(ArtMethod)`就行了，因为这是在编译期就可以决定的。



但我们是上层开发者，app 会被下发给各式各样的 Android 设备，所以我们是需要在运行时动态地得到 app 所运行设备上面的底层 ArtMethod 大小的，这就没那么简单了。



想要忽略 ArtMethod 的具体结构成员直接取得其 size 的精确值，我们还是需要从虚拟机的源码入手，**从底层的数据结构及排列特点探寻答案**。



在 art 里面，初始化一个类的时候会给这个类的所有方法分配空间，我们可以看到这个分配空间的地方：



```
@android-6.0.1_r62/art/runtime/class_linker.cc

void ClassLinker::LoadClassMembers(Thread* self, const DexFile& dex_file,
                                   const uint8_t* class_data,
                                   Handle<mirror::Class> klass,
                                   const OatFile::OatClass* oat_class) {
    ... ...
    
    ArtMethod* const direct_methods = (it.NumDirectMethods() != 0)
        ? AllocArtMethodArray(self, it.NumDirectMethods())
        : nullptr;
    ArtMethod* const virtual_methods = (it.NumVirtualMethods() != 0)
        ? AllocArtMethodArray(self, it.NumVirtualMethods())
        : nullptr;                                   
   
    ... ...                                
```



类的方法有 direct 方法和 virtual 方法。direct 方法包含 static 方法和所有不可继承的对象方法。而 virtual 方法就是所有可以继承的对象方法了。



AllocArtMethodArray 函数分配了他们的方法所在区域。



```
@android-6.0.1_r62/art/runtime/class_linker.cc

ArtMethod* ClassLinker::AllocArtMethodArray(Thread* self, size_t length) {
  const size_t method_size = ArtMethod::ObjectSize(image_pointer_size_);
  uintptr_t ptr = reinterpret_cast<uintptr_t>(
      Runtime::Current()->GetLinearAlloc()->Alloc(self, method_size * length));
  CHECK_NE(ptr, 0u);
  for (size_t i = 0; i < length; ++i) {
    new(reinterpret_cast<void*>(ptr + i * method_size)) ArtMethod;
  }
  return reinterpret_cast<ArtMethod*>(ptr);
}
```



可以看到，ptr 是这个方法数组的指针，而方法是一个接一个紧密地 new 出来排列在这个方法数组中的。这时只是分配出空间，还没填入真正的 ArtMethod 的各个成员值，不过这并不影响我们观察 ArtMethod 的空间结构。





![img](https://raw.githubusercontent.com/hhhaiai/Picture/main/img/202208151737835.png)





正是这里给了我们启示，ArtMethod 们是紧密排列的，所以一个 ArtMethod 的大小，不就是相邻两个方法所对应的 ArtMethod 的起始地址的差值吗？



正是如此。我们就从这个排列特点入手，自己构造一个类，以一种巧妙的方式获取到这个差值。



```
public class NativeStructsModel {
    final public static void f1() {}
    final public static void f2() {}
}
```



由于 f1 和 f2 都是 static 方法，所以都属于 direct ArtMethod Array。由于 NativeStructsModel 类中只存在这两个方法，因此它们肯定是相邻的。



那么我们就可以在 JNI 层取得它们地址的差值：



```
    size_t firMid = (size_t) env->GetStaticMethodID(nativeStructsModelClazz, "f1", "()V");
    size_t secMid = (size_t) env->GetStaticMethodID(nativeStructsModelClazz, "f2", "()V");
    size_t methSize = secMid - firMid;
```



然后，就以这个`methSize`作为`sizeof(ArtMethod)`，代入之前的代码。



```
    memcpy(smeth, dmeth, methSize);
```



问题就迎刃而解了。



**值得一提的是，由于忽略了底层 ArtMethod 结构的差异，对于所有的 Android 版本都不再需要区分，而统一以`memcpy`实现即可，代码量大大减少。即使以后的 Android 版本不断修改 ArtMethod 的成员，只要保证 ArtMethod 数组仍是以线性结构排列，就能直接适用于将来的 Android 8.0、9.0 等新版本，无需再针对新的系统版本进行适配了。事实也证明确实如此，当我们拿到 Google 刚发不久的 Android O(8.0) 开发者预览版的系统时，hotfix demo 直接就能顺利地加载补丁跑起来了，我们并没有做任何适配工作，鲁棒性极好。**



## 访问权限的问题



### 方法调用时的权限检查



看到这里，你可能会有疑惑：我们只是替换了 ArtMethod 的内容，但新替换的方法的所属类，和原先方法的所属类，是不同的类，被替换的方法有权限访问这个类的其他 private 方法吗？



以这段简单的代码为例



```
public class Demo {
    Demo() {
        func();
    }

    private void func() {
    }
}
```



Demo 构造函数调用私有函数`func`所对应的 Dex Code 和 Native Code 为



```
   void com.patch.demo.Demo.<init>() (dex_method_idx=20628)
    DEX CODE:
      ... ...
      0x0003: 7010 9550 0000            | invoke-direct {v0}, void com.patch.demo.Demo.func() 
      ... ...
    
    CODE: (code_offset=0x006fd86c size_offset=0x006fd868 size=140)...
      ... ...
      0x006fd8c4: f94003e0  ldr x0, [sp]             ; x0 = <init>的ArtMethod*
      0x006fd8c8: b9400400  ldr w0, [x0, #4]         ; w0 = dex_cache_resolved_methods_
      0x006fd8cc: d2909710  mov x16, #0x84b8         ; x16 = 0x84b8
      0x006fd8d0: f2a00050  movk x16, #0x2, lsl #16  ; x16 = 0x84b8 + 0x20000 = 0x284b8 = (20629 + 2) * 8, 
                                                     ; 也就是Demo.func的ArtMethod*相对于表头dex_cache_resolved_methods_的偏移。
      0x006fd8d4: f8706800  ldr x0, [x0, x16]        ; 得到Demo.func的ArtMethod*
      0x006fd8d8: f940181e  ldr lr, [x0, #48]        ; 取得其entry_point_from_quick_compiled_code_
      0x006fd8dc: d63f03c0  blr lr                   ; 跳转执行
      ... ...
```



这个调用逻辑和之前 Activity 的例子大同小异，需要注意的地方是，在构造函数调用同一个类下的私有方法`func`时，没有做任何权限检查。也就是说，这时即使我把`func`方法的偷梁换柱，也能直接跳过去正常执行而不会报错。



可以推测，在 dex2oat 生成 AOT 机器码时是有做一些检查和优化的，由于在 dex2oat 编译机器码时确认了两个方法同属一个类，所以机器码中就不存在权限检查的相关代码。



### 同包名下的权限问题



但是，并非所有方法都可以这么顺利地进行访问的。我们发现补丁中的类在访问同包名下的类时，会报出访问权限异常：



```
Caused by: java.lang.IllegalAccessError:
Method 'void com.patch.demo.BaseBug.test()' is inaccessible to class 'com.patch.demo.MyClass' (declaration of 'com.patch.demo.MyClass' 
appears in /data/user/0/com.patch.demo/files/baichuan.fix/patch/patch.jar)
```



虽然`com.patch.demo.BaseBug`和`com.patch.demo.MyClass`是同一个包`com.patch.demo`下面的，但是由于我们替换了`com.patch.demo.BaseBug.test`，而这个替换了的`BaseBug.test`是从补丁包的 Classloader 加载的，与原先的 base 包就不是同一个 Classloader 了，这样就导致两个类无法被判别为同包名。具体的校验逻辑是在虚拟机代码的`Class::IsInSamePackage`中：



```
android-6.0.1_r62/art/runtime/mirror/class.cc

bool Class::IsInSamePackage(Class* that) {
  Class* klass1 = this;
  Class* klass2 = that;
  if (klass1 == klass2) {
    return true;
  }
  
  if (klass1->GetClassLoader() != klass2->GetClassLoader()) {
    return false;
  }
  
  while (klass1->IsArrayClass()) {
    klass1 = klass1->GetComponentType();
  }
  while (klass2->IsArrayClass()) {
    klass2 = klass2->GetComponentType();
  }
  
  if (klass1 == klass2) {
    return true;
  }
  
  std::string temp1, temp2;
  return IsInSamePackage(klass1->GetDescriptor(&temp1), klass2->GetDescriptor(&temp2));
}
```



关键点在于，Class loaders must match 这行注释。



知道了原因就好解决了，我们只要设置新类的 Classloader 为原来类就可以了。而这一步同样不需要在 JNI 层构造底层的结构，只需要通过反射进行设置。这样仍旧能够保证良好的兼容性。



实现代码如下：



```
    Field classLoaderField = Class.class.getDeclaredField("classLoader");
    classLoaderField.setAccessible(true);
    classLoaderField.set(newClass, oldClass.getClassLoader());
```



这样就解决了同包名下的访问权限问题。



### 反射调用非静态方法产生的问题



当一个非静态方法被热替换后，在反射调用这个方法时，会抛出异常。



比如下面这个例子：



```
    ... ...
    
    BaseBug bb = new BaseBug();
    Method testMeth = BaseBug.class.getDeclaredMethod("test");
    testMeth.invoke(bb);
```



invoke 的时候就会报：



```
Caused by: java.lang.IllegalArgumentException:
  Expected receiver of type com.patch.demo.BaseBug,
  but got com.patch.demo.BaseBug
```



这里面，expected receiver 的 BaseBug，和 got 到的 BaseBug，虽然都叫 com.patch.demo.BaseBug，但却是不同的类。



前者是被热替换的方法所属的类，由于我们把它的 ArtMethod 的 declaring_class_替换了，因此就是新的补丁类。而后者作为被调用的实例对象 bb 的所属类，是原有的 BaseBug。两者是不同的。



在反射 invoke 这个方法时，在底层会调用到 InvokeMethod：



```
jobject InvokeMethod(const ScopedObjectAccessAlreadyRunnable& soa, jobject javaMethod,
                     jobject javaReceiver, jobject javaArgs, size_t num_frames) {
      ... ...
      
      if (!VerifyObjectIsClass(receiver, declaring_class)) {
        return nullptr;
      }
      
      ... ...
```



这里面会调用 VerifyObjectIsClass 函数做验证。



```
inline bool VerifyObjectIsClass(mirror::Object* o, mirror::Class* c) {
  if (UNLIKELY(o == nullptr)) {
    ThrowNullPointerException("null receiver");
    return false;
  } else if (UNLIKELY(!o->InstanceOf(c))) {
    InvalidReceiverError(o, c);
    return false;
  }
  return true;
}
```



o 表示 Method.invoke 传入的第一个参数，也就是作用的对象。
c 表示 ArtMethod 所属的 Class。



因此，只有 o 是 c 的一个实例才能够通过验证，才能继续执行后面的反射调用流程。



由此可知，这种热替换方式所替换的非静态方法，在进行反射调用时，由于 VerifyObjectIsClass 时旧类和新类不匹配，就会导致校验不通过，从而抛出上面那个异常。



那为什么方法是非静态才有这个问题呢？因为如果是静态方法，是在类的级别直接进行调用的，就不需要接收对象实例作为参数。所以就没有这方面的检查了。



对于这种反射调用非静态方法的问题，我们会采用另一种冷启动机制对付，本文在最后会说明如何解决。



## 即时生效所带来的限制



除了反射的问题，像本方案以及 Andfix 这样直接在运行期修改底层结构的热修复，都存在着一个限制，那就是只能支持方法的替换。而对于补丁类里面存在方法增加和减少，以及成员字段的增加和减少的情况，都是不适用的。



原因是这样的，一旦补丁类中出现了方法的增加和减少，就会导致这个类以及整个 Dex 的方法数的变化。方法数的变化伴随着方法索引的变化，这样在访问方法时就无法正常地索引到正确的方法了。



而如果字段发生了增加和减少，和方法变化的情况一样，所有字段的索引都会发生变化。并且更严重的问题是，如果在程序运行中间某个类突然增加了一个字段，那么对于原先已经产生的这个类的实例，它们还是原来的结构，这是无法改变的。而新方法使用到这些老的实例对象时，访问新增字段就会产生不可预期的结果。



不过新增一个完整的、原先包里面不存在的新类是可以的，这个不受限制。



总之，只有两种情况是不适用的：1). 引起原有了类中发生结构变化的修改，2). 修复了的非静态方法会被反射调用，而对于其他情况，这种方式的热修复都可以任意使用。



## 总结



虽然有着一些使用限制，但一旦满足使用条件，这种热修复方式是十分出众的，它补丁小，加载迅速，能够实时生效无需重新启动 app，并且具有着完美的设备兼容性。对于较小程度的修复再适合不过了。



本修复方案将最先在**阿里 Hotfix 最新版本 (Sophix)** 上应用，由手机淘宝技术团队与阿里云联合发布。



Sophix 提供了一套更加完美的客户端服务端一体的热更新方案。针对小修改可以采用本文这种即时生效的热修复，并且可以结合资源修复，做到资源和代码的即时生效。



而如果触及了本文提到的热替换使用限制，**对于比较大的代码改动以及被修复方法反射调用情况**，Sophix 也提供了另一种完整代码修复机制，不过是需要 app 重新冷启动，来发挥其更加完善的修复及更新功能。从而可以做到无感知的应用更新。



并且 Sophix 做到了图形界面一键打包、加密传输、签名校验和服务端控制发布与灰度功能，让你用最少的时间实现最强大可靠的全方位热更新。



一张表格来说明一下各个版本热修复的差别：



| 方案对比     | Andfix 开源版本      | 阿里 Hotfix 1.X    | 阿里 Hotfix 最新版 (Sophix)  |
| :----------- | :------------------- | :----------------- | :--------------------------- |
| 方法替换     | 支持，除部分情况 [0] | 支持，除部分情况   | 全部支持                     |
| 方法增加减少 | 不支持               | 不支持             | 以冷启动方式支持 [1]         |
| 方法反射调用 | 只支持静态方法       | 只支持静态方法     | 以冷启动方式支持             |
| 即时生效     | 支持                 | 支持               | 视情况支持 [2]               |
| 多 DEX       | 不支持               | 支持               | 支持                         |
| 资源更新     | 不支持               | 不支持             | 支持                         |
| so 库更新    | 不支持               | 不支持             | 支持                         |
| Android 版本 | 支持 2.3~7.0         | 支持 2.3~6.0       | 全部支持包含 7.0 以上        |
| 已有机型     | 大部分支持 [3]       | 大部分支持         | 全部支持                     |
| 安全机制     | 无                   | 加密传输及签名校验 | 加密传输及签名校验           |
| 性能损耗     | 低，几乎无损耗       | 低，几乎无损耗     | 低，仅冷启动情况下有些损耗   |
| 生成补丁     | 繁琐，命令行操作     | 繁琐，命令行操作   | 便捷，图形化界面             |
| 补丁大小     | 不大，仅变动的类     | 小，仅变动的方法   | 不大，仅变动的资源和代码 [4] |
| 服务端支持   | 无                   | 支持服务端控制 [5] | 支持服务端控制               |




说明：
[0] 部分情况指的是构造方法、参数数目大于 8 或者参数包括 long,double,float 基本类型的方法。
[1] 冷启动方式，指的是需要重启 app 在下次启动时才能生效。
[2] 对于 Andfix 及 Hotfix 1.X 能够支持的代码变动情况，都能做到即时生效。而对于 Andfix 及 Hotfix 1.X 不支持的代码变动情况，会走冷启动方式，此时就无法做到即时生效。
[3] Hotfix 1.X 已经支持绝大部分主流手机，只是在 X86 设备以及修改了虚拟机底层结构的 ROM 上不支持。
[4] 由于支持了资源和库，如果有这些方面的更新，就会导致的补丁变大一些，这个是很正常的。并且由于只包含差异的部分，所以补丁已经是最大程度的小了。
[5] 提供服务端的补丁发布和停发、版本控制和灰度功能，存储开发者上传的补丁包。



从现在起，让你的 APP 实现随心所欲的热更新吧！[请猛戳这里 >_<](https://www.aliyun.com/product/hotfix)



最后，感谢团队 @悟二和 @查郁冷启动修复及 so 库更新方面的支持，以及 @所为在开发过程中的问题讨论与文章校稿。



原创文章，转载请注明出处。手淘公众号文章链接：http://mp.weixin.qq.com/s/Uv0BS67-wgvCor6Fss6ChQ