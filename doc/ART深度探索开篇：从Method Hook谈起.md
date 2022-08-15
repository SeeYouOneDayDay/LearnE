# ART 深度探索开篇：从 Method Hook 谈起 | Weishu's Notes

Android 上的热修复框架 AndFix 想必已经是耳熟能详，它的原理实际上很简单：方法替换——Java 层的每一个方法在虚拟机实现里面都对应着一个 ArtMethod 的结构体，只要把原方法的结构体内容替换成新的结构体的内容，在调用原方法的时候，真正执行的指令会是新方法的指令；这样就能实现热修复，详细代码见 AndFix。



Android 上的热修复框架 AndFix 想必已经是耳熟能详，它的原理实际上很简单：方法替换——Java 层的每一个方法在虚拟机实现里面都对应着一个 ArtMethod 的结构体，只要把原方法的结构体内容替换成新的结构体的内容，在调用原方法的时候，真正执行的指令会是新方法的指令；这样就能实现热修复，详细代码见 [AndFix](https://github.com/alibaba/AndFix)。



为什么可以这么做呢？那得从 Android 虚拟机的方法调用过程说起。作为一个系列的开篇，本文不打算展开讲虚拟机原理等内容，首先给大家一道开胃菜；后续我们再深入探索 ART。



众所周知，AndFix 是一种 native 的 hotfix 方案，它的替换过程是用 c 在 native 层完成的，但其实，我们也可以用纯 Java 实现它！而且，代码还非常精简，且看——





## 方法替换原理

既然我们知道 AndFix 的原理是方法替换，那么为什么直接替换 Java 里面的 `java.lang.reflect.Method` 有什么问题吗？直接这样貌似很难下结论，那我们换个思路。我们实现方法替换的结果，就是调用原方法的时候最终是调用被替换的方法。因此，我们可以看看 `java.lang.reflect.Method`类的 `invoke` 方法。（这里有个疑问，Foo.bar() 这种直接调用与反射调用 Foo.class.getDeclaredMethod(“bar”).invoke(null) 有什么区别吗？这个问题后续再谈）



```
private native Object invoke(Object receiver, Object[] args, boolean accessible)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;
```



这个 invoke 是一个 native 方法，它的 native 实现在 `art/runtime/native/java_lang_reflect_Method.cc` 里面，这个 jni 方法最终调用了 `art/runtime/reflection.cc` 的 `InvokeMethod`方法：



```c++
object InvokeMethod(const ScopedObjectAccessAlreadyRunnable& soa, jobject javaMethod,
                     jobject javaReceiver, jobject javaArgs, bool accessible) {
  mirror::ArtMethod* m = mirror::ArtMethod::FromReflectedMethod(soa, javaMethod);
  mirror::Class* declaring_class = m->GetDeclaringClass();
  mirror::Object* receiver = nullptr;
  if (!m->IsStatic()) {
    receiver = soa.Decode<mirror::Object*>(javaReceiver);
    if (!VerifyObjectIsClass(receiver, declaring_class)) {
      return NULL;
    }

    
    m = receiver->GetClass()->FindVirtualMethodForVirtualOrInterface(m);
  }
  InvokeWithArgArray(soa, m, &arg_array, &result, shorty);
  return soa.AddLocalReference<jobject>(BoxPrimitive(mh.GetReturnType()->GetPrimitiveType(),
                                                     result));
}
```



上面函数 InvokeMethod 的第二个参数 `javaMethod` 就是 Java 层我们进行反射调用的那个 Method 对象，在 jni 层反映为一个 jobject；InvokeMethod 这个 native 方法首先通过 `mirror::ArtMethod::FromReflectedMethod` 获取了 Java 对象的在 native 层的 ArtMethod 指针，我们跟进去看看是怎么实现的：



```c++
ArtMethod* ArtMethod::FromReflectedMethod(const ScopedObjectAccessAlreadyRunnable& soa,
                                          jobject jlr_method) {
  mirror::ArtField* f =
      soa.DecodeField(WellKnownClasses::java_lang_reflect_AbstractMethod_artMethod);
  mirror::ArtMethod* method = f->GetObject(soa.Decode<mirror::Object*>(jlr_method))->AsArtMethod();
  DCHECK(method != nullptr);
  return method;
}
```



我们在这里看到了一点端倪，获取到了 Java 层那个 Method 对象的一个叫做 `artMethod`的字段，然后强转成了 ArtMethod 指针（这里的说法不是很准确，但是要搞明白这里面的细节一两篇文章讲不清楚 ~_~，我们暂且这么认为吧。）



AndFix 的实现里面，也正是使用这个 `FromReflectedMethod` 方法拿到 Java 层 Method 对应 native 层的 ArtMethod 指针，然后执行替换的。



上面我们也看到了，我们在 native 层替换的那个 ArtMethod 不是在 Java 层也有对应的东西么？我们直接替换掉 Java 层的这个 artMethod 字段不就 OK 了？但是我们要注意的是，在 Java 里面除了基本类型，其他东西都是引用。要实现类似 C++ 里面那种替换引用所指向内容的机智，需要一些黑科技。



## Unsafe 和 Memory

要在 Java 层操作内容，也不是没有办法做到；JDK 给我们留了一个后门：`sun.misc.Unsafe` 类；在 OpenJDK 里面这个类灰常强大，从内存操作到 CAS 到锁机制，无所不能（可惜的是据说 JDK8 要去掉？）但是在 Android 平台还有一点点不一样，在 Android N 之前，Android 的 JDK 实现是 Apache Harmony，这个实现里面的 Unsafe 就有点鸡肋了，没法写内存；好在 Android 又开了一个后门：`Memory` 类。



有了这两个类，我们就能在 Java 层进行简单的内存操作了！！由于这两个类是隐藏类，我写了一个 wrapper，如下：



```java
private static class Memory {

    
    static byte peekByte(long address) {
        return (Byte) Reflection.call(null, "libcore.io.Memory", "peekByte", null, new Class[]{long.class}, new Object[]{address});
    }

    static void pokeByte(long address, byte value) {
        Reflection.call(null, "libcore.io.Memory", "pokeByte", null, new Class[]{long.class, byte.class}, new Object[]{address, value});
    }

    public static void memcpy(long dst, long src, long length) {
        for (long i = 0; i < length; i++) {
            pokeByte(dst, peekByte(src));
            dst++;
            src++;
        }
    }
}

static class Unsafe {

    static final String UNSAFE_CLASS = "sun.misc.Unsafe";
    static Object THE_UNSAFE;

    private static boolean is64Bit;

    static {
        THE_UNSAFE = Reflection.get(null, UNSAFE_CLASS, "THE_ONE", null);
        Object runtime = Reflection.call(null, "dalvik.system.VMRuntime", "getRuntime", null, null, null);
        is64Bit = (Boolean) Reflection.call(null, "dalvik.system.VMRuntime", "is64Bit", runtime, null, null);
    }

    public static long getObjectAddress(Object o) {
        Object[] objects = {o};
        Integer baseOffset = (Integer) Reflection.call(null, UNSAFE_CLASS,
                "arrayBaseOffset", THE_UNSAFE, new Class[]{Class.class}, new Object[]{Object[].class});
        return ((Number) Reflection.call(null, UNSAFE_CLASS, is64Bit ? "getLong" : "getInt", THE_UNSAFE,
                new Class[]{Object.class, long.class}, new Object[]{objects, baseOffset.longValue()})).longValue();
    }
}
```



## 具体实现

接下来思路就很简单了呀，用伪代码表示就是：

```
memcopy(originArtMethod, replaceArtMethod);
```



但是还有一个问题，我们要整个把 originMethod 的 artMethod 所在的内存直接替换为 replaceMethod 的 artMethod 所在的内存（上面我们已经知道，Java 层 Method 类的 artMethod 实际上就是 native 层的指针表示，在 Android N 上更明显，这玩意儿直接就是一个 long），现在我们已经知道这两个地址是什么，那么我们把 replaceArtMethod 代表的内存复制到 originArtMethod 的区域，应该还需要知道一个 artMethod 有多大。



但是事情没有一个 sizeof 那么简单。你看 AndFix 的实现是在每个 Android 版本把 ArtMethod 这个结构体复制一份的；要想用 sizeof 还得把这个类所有的引用复制过来，及其麻烦。更何况在 Java 里面 sizeof 都没有。不过也不是没有办法，既然我们已经能在 Java 层拿到对象的地址，只需要创建一个数组，丢两个 ArtMethod，把两个数组元素的起始地址相减不就得到一个 artMethod 的大小了吗？(此方法来自 [Android 热修复升级探索——追寻极致的代码热替换](https://yq.aliyun.com/articles/74598?t=t1))



不过，既然我们实现了方法替换；还有最后一个问题，如果我们需要在替换后的方法里面调用原函数呢？这个也很简单，我们只需要把原函数 copy 一份保存起来，需要调用原函数的时候调用那个 copy 的函数不就行了？不过在具体实现的时候，会遇到一个问题，就是 Java 的非 static 非 private 的方法默认是虚方法，在调用这个方法的时候会有一个类似查找虚函数表的过程，这个在上面的代码 `InvokeMethod` 里面可以看到：



```
mirror::Object* receiver = nullptr;
if (!m->IsStatic()) {
  
  receiver = soa.Decode<mirror::Object*>(javaReceiver);
  if (!VerifyObjectIsClass(receiver, declaring_class)) {
    return NULL;
  }

  
  m = receiver->GetClass()->FindVirtualMethodForVirtualOrInterface(m);
}
```



在调用的时候，如果不是 static 的方法，会去查找这个方法的真正实现；我们直接把原方法做了备份之后，去调用备份的那个方法，如果此方法是 public 的，则会查找到原来的那个函数，于是就无限循环了；我们只需要阻止这个过程，查看 FindVirtualMethodForVirtualOrInterface 这个方法的实现就知道，只要方法是 invoke-direct 进行调用的，就会直接返回原方法，这些方法包括：构造函数，private 的方法 ( 见 https://source.android.com/devices/tech/dalvik/dalvik-bytecode.html) 因此，我们手动把这个备份的方法属性修改为 private 即可解决这个问题。



详细代码见：[github/epic](https://github.com/tiann/epic)



至此，我们就用纯 Java 实现了一个 AndFix，代码只有 200 行不到！！是不是很神奇？当然，这里面包含了很多黑科技，接下来我们将以这个为引子，深入探索 Android ART 的方方面面，揭开虚拟机底层的神秘面纱，敬请期待～～