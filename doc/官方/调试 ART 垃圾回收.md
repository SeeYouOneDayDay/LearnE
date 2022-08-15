# 调试 ART 垃圾回收

本页介绍了如何调试 Android 运行时 (ART) 垃圾回收 (GC) 的正确性和性能问题。此外，还说明了如何使用 GC 验证选项、确定应对 GC 验证失败的解决方案，以及衡量并解决 GC 性能问题。

如需使用 ART，请参阅此 [ART 和 Dalvik](https://source.android.com/devices/tech/dalvik) 部分中介绍的内容，以及 [Dalvik 可执行文件格式](https://source.android.com/devices/tech/dalvik/dex-format)。如需获得验证应用行为方面的其他帮助，请参阅[在 Android Runtime (ART) 上验证应用行为](http://developer.android.com/guide/practices/verifying-apps-art)。

## ART GC 概览

ART 有多个不同的 GC 方案，涉及运行不同的垃圾回收器。从 Android 8 (Oreo) 开始，默认方案是并发复制 (CC)。另一个 GC 方案是并发标记清除 (CMS)。

并发复制 GC 的一些主要特性包括：

- CC 支持使用名为“RegionTLAB”的触碰指针分配器。此分配器可以向每个应用线程分配一个线程本地分配缓冲区 (TLAB)，这样，应用线程只需触碰“栈顶”指针，而无需任何同步操作，即可从其 TLAB 中将对象分配出去。
- CC 通过在不暂停应用线程的情况下并发复制对象来执行堆碎片整理。这是在读取屏障的帮助下实现的，读取屏障会拦截来自堆的引用读取，无需应用开发者进行任何干预。
- GC 只有一次很短的暂停，对于堆大小而言，该次暂停在时间上是一个常量。
- 在 Android 10 及更高版本中，CC 会扩展为分代 GC。它支持轻松回收存留期较短的对象，这类对象通常很快便会无法访问。这有助于提高 GC 吞吐量，并显著延迟执行全堆 GC 的需要。

ART 仍然支持的另一个 GC 方案是 CMS。此 GC 方案还支持压缩，但不是以并发方式。在应用进入后台之前，它会避免执行压缩，应用进入后台后，它会暂停应用线程以执行压缩。如果对象分配因碎片而失败，也必须执行压缩操作。在这种情况下，应用可能会在一段时间内没有响应。

由于 CMS 很少进行压缩，因此空闲对象可能会不连续。CMS 使用一个名为 RosAlloc 的基于空闲列表的分配器。与 RegionTLAB 相比，该分配器的分配成本较高。最后，由于内部碎片，Java 堆的 CMS 内存用量可能会高于 CC 内存用量。

## GC 验证和性能选项

### 更改 GC 类型

原始设备制造商 (OEM) 可以更改 GC 类型。如需进行更改，需要在构建时设置 `ART_USE_READ_BARRIER` 环境变量。默认值为 true，这会启用 CC 回收器，因为该回收器使用读取屏障。对于 CMS，此变量应明确设置为 false。

默认情况下，在 Android 10 及更高版本中，CC 回收器在分代模式下运行。如需停用分代模式，可以使用 `-Xgc:nogenerational_cc` 命令行参数。或者，也可以按如下方式设置系统属性：

```
adb shell setprop dalvik.vm.gctype nogenerational_cc
```

CMS 回收器始终在分代模式下运行。



### 验证堆

堆验证可能是调试 GC 相关错误或堆损坏的最有用的 GC 选项。启用堆验证会使 GC 在垃圾回收过程中在几个点检查堆的正确性。堆验证的选项与更改 GC 类型的相同。启用后，堆验证流程会验证根，并确保可访问对象仅引用了其他可访问对象。您可以通过传入以下 `-Xgc` 值来启用 GC 验证：

- 启用后，`[no]preverify` 将在启动 GC 之前执行堆验证。
- 启用后，`[no]presweepingverify` 将在开始垃圾回收器清除过程之前执行堆验证。
- 启用后，`[no]postverify` 将在 GC 完成清除之后执行堆验证。
- `[no]preverify_rosalloc`、`[no]postsweepingverify_rosalloc` 和 `[no]postverify_rosalloc` 是附加 GC 选项，仅验证 RosAlloc 内部记录的状态。因此，它们仅适用于使用 RosAlloc 分配器的 CMS 回收器。验证的主要内容是，魔法值是否与预期常量匹配，以及可用内存块是否已全部在 `free_page_runs_` 映射中注册。

## 性能

衡量 GC 性能的工具主要有两个：GC 时序转储和 Systrace。Systrace 还有一个高级版本，称为 Perfetto。如需衡量 GC 性能问题，直观的方法是使用 Systrace 和 Perfetto 确定哪些 GC 会导致长时间暂停或抢占应用线程。尽管 ART GC 经过多年发展已得到显著改进，但不良更改器行为（例如过度分配）仍会导致性能问题

### 回收策略

CC GC 通过运行新生代 GC 或全堆 GC 来回收垃圾。理想情况下，新生代 GC 的运行频率更高。GC 会一直执行新生代 CC 回收，直到刚结束的回收周期的吞吐量（计算公式是：释放的字节数除以 GC 持续秒数）小于全堆 CC 回收的平均吞吐量。发生这种情况时，将为下一次并发 GC 选择全堆 CC（而不是新生代 CC）。全堆回收完成后，下一次 GC 将切换回新生代 CC。新生代 CC 在完成后不会调整堆占用空间限制，这是此策略发挥作用的一个关键因素。这使得新生代 CC 运行得越来越频繁，直到吞吐量低于全堆 CC，最终导致堆增大。

### 使用 SIGQUIT 获取 GC 性能信息

如需获得应用的 GC 性能时序，请将 `SIGQUIT` 发送到已在运行的应用，或者在启动命令行程序时将 `-XX:DumpGCPerformanceOnShutdown` 传递给 `dalvikvm`。当应用获得 ANR 请求信号 (`SIGQUIT`) 时，会转储与其锁定、线程堆栈和 GC 性能相关的信息。

如需获得 GC 时序转储，请使用以下命令：

```
adb shell kill -s QUIT PID
```

这会在 `/data/anr/` 中创建一个文件（名称中会包含日期和时间，例如 anr_2020-07-13-19-23-39-817）。此文件包含一些 ANR 转储信息以及 GC 时序。您可以通过搜索“Dumping cumulative Gc timings”（转储累计 GC 时序）来确定 GC 时序。这些时序会显示一些需要关注的内容，包括每个 GC 类型的阶段和暂停时间的直方图信息。暂停信息通常比较重要。例如：

```
young concurrent copying paused:	Sum: 5.491ms 99% C.I. 1.464ms-2.133ms Avg: 1.830ms Max: 2.133ms
```

本示例中显示平均暂停时间为 1.83 毫秒，该值应该足够低，在大多数应用中不会导致丢帧，因此您不必担心。

需要关注的另一个方面是挂起时间，挂起时间测量在 GC 要求某个线程挂起后，该线程到达挂起点所需的时间。此时间包含在 GC 暂停时间中，所以对于确定长时间暂停是由 GC 缓慢还是线程挂起缓慢造成的很有用。以下是 Nexus 5 上的正常挂起时间示例：

```
suspend all histogram:	Sum: 1.513ms 99% C.I. 3us-546.560us Avg: 47.281us Max: 601us
```

还有其他一些需要关注的方面，包括总耗时和 GC 吞吐量。示例：

```
Total time spent in GC: 502.251ms
Mean GC size throughput: 92MB/s
Mean GC object throughput: 1.54702e+06 objects/s
```

以下示例说明了如何转储已在运行的应用的 GC 时序：

```
adb shell kill -s QUIT PID
adb pull /data/anr/anr_2020-07-13-19-23-39-817
```

此时，GC 时序在 `anr_2020-07-13-19-23-39-817` 中。以下是 Google 地图的输出示例：

```
Start Dumping histograms for 2195 iterations for concurrent copying
MarkingPhase:   Sum: 258.127s 99% C.I. 58.854ms-352.575ms Avg: 117.651ms Max: 641.940ms
ScanCardsForSpace:      Sum: 85.966s 99% C.I. 15.121ms-112.080ms Avg: 39.164ms Max: 662.555ms
ScanImmuneSpaces:       Sum: 79.066s 99% C.I. 7.614ms-57.658ms Avg: 18.014ms Max: 546.276ms
ProcessMarkStack:       Sum: 49.308s 99% C.I. 6.439ms-81.640ms Avg: 22.464ms Max: 638.448ms
ClearFromSpace: Sum: 35.068s 99% C.I. 6.522ms-40.040ms Avg: 15.976ms Max: 633.665ms
SweepSystemWeaks:       Sum: 14.209s 99% C.I. 3.224ms-15.210ms Avg: 6.473ms Max: 201.738ms
CaptureThreadRootsForMarking:   Sum: 11.067s 99% C.I. 0.835ms-13.902ms Avg: 5.044ms Max: 25.565ms
VisitConcurrentRoots:   Sum: 8.588s 99% C.I. 1.260ms-8.547ms Avg: 1.956ms Max: 231.593ms
ProcessReferences:      Sum: 7.868s 99% C.I. 0.002ms-8.336ms Avg: 1.792ms Max: 17.376ms
EnqueueFinalizerReferences:     Sum: 3.976s 99% C.I. 0.691ms-8.005ms Avg: 1.811ms Max: 16.540ms
GrayAllDirtyImmuneObjects:      Sum: 3.721s 99% C.I. 0.622ms-6.702ms Avg: 1.695ms Max: 14.893ms
SweepLargeObjects:      Sum: 3.202s 99% C.I. 0.032ms-6.388ms Avg: 1.458ms Max: 549.851ms
FlipOtherThreads:       Sum: 2.265s 99% C.I. 0.487ms-3.702ms Avg: 1.031ms Max: 6.327ms
VisitNonThreadRoots:    Sum: 1.883s 99% C.I. 45us-3207.333us Avg: 429.210us Max: 27524us
InitializePhase:        Sum: 1.624s 99% C.I. 231.171us-2751.250us Avg: 740.220us Max: 6961us
ForwardSoftReferences:  Sum: 1.071s 99% C.I. 215.113us-2175.625us Avg: 488.362us Max: 7441us
ReclaimPhase:   Sum: 490.854ms 99% C.I. 32.029us-6373.807us Avg: 223.623us Max: 362851us
EmptyRBMarkBitStack:    Sum: 479.736ms 99% C.I. 11us-3202.500us Avg: 218.558us Max: 13652us
CopyingPhase:   Sum: 399.163ms 99% C.I. 24us-4602.500us Avg: 181.851us Max: 22865us
ThreadListFlip: Sum: 295.609ms 99% C.I. 15us-2134.999us Avg: 134.673us Max: 13578us
ResumeRunnableThreads:  Sum: 238.329ms 99% C.I. 5us-2351.250us Avg: 108.578us Max: 10539us
ResumeOtherThreads:     Sum: 207.915ms 99% C.I. 1.072us-3602.499us Avg: 94.722us Max: 14179us
RecordFree:     Sum: 188.009ms 99% C.I. 64us-312.812us Avg: 85.653us Max: 2709us
MarkZygoteLargeObjects: Sum: 133.301ms 99% C.I. 12us-734.999us Avg: 60.729us Max: 10169us
MarkStackAsLive:        Sum: 127.554ms 99% C.I. 13us-417.083us Avg: 58.111us Max: 1728us
FlipThreadRoots:        Sum: 126.119ms 99% C.I. 1.028us-3202.499us Avg: 57.457us Max: 11412us
SweepAllocSpace:        Sum: 117.761ms 99% C.I. 24us-400.624us Avg: 53.649us Max: 1541us
SwapBitmaps:    Sum: 56.301ms 99% C.I. 10us-125.312us Avg: 25.649us Max: 1475us
(Paused)GrayAllNewlyDirtyImmuneObjects: Sum: 33.047ms 99% C.I. 9us-49.931us Avg: 15.055us Max: 72us
(Paused)SetFromSpace:   Sum: 11.651ms 99% C.I. 2us-49.772us Avg: 5.307us Max: 71us
(Paused)FlipCallback:   Sum: 7.693ms 99% C.I. 2us-32us Avg: 3.504us Max: 32us
(Paused)ClearCards:     Sum: 6.371ms 99% C.I. 250ns-49753ns Avg: 207ns Max: 188000ns
Sweep:  Sum: 5.793ms 99% C.I. 1us-49.818us Avg: 2.639us Max: 93us
UnBindBitmaps:  Sum: 5.255ms 99% C.I. 1us-31us Avg: 2.394us Max: 31us
Done Dumping histograms
concurrent copying paused:      Sum: 315.249ms 99% C.I. 49us-1378.125us Avg: 143.621us Max: 7722us
concurrent copying freed-bytes: Avg: 34MB Max: 54MB Min: 2062KB
Freed-bytes histogram: 0:4,5120:5,10240:19,15360:69,20480:167,25600:364,30720:529,35840:405,40960:284,46080:311,51200:38
concurrent copying total time: 569.947s mean time: 259.657ms
concurrent copying freed: 1453160493 objects with total size 74GB
concurrent copying throughput: 2.54964e+06/s / 134MB/s  per cpu-time: 157655668/s / 150MB/s
Average major GC reclaim bytes ratio 0.486928 over 2195 GC cycles
Average major GC copied live bytes ratio 0.0894662 over 2199 major GCs
Cumulative bytes moved 6586367960
Cumulative objects moved 127490240
Peak regions allocated 376 (94MB) / 2048 (512MB)
Start Dumping histograms for 685 iterations for young concurrent copying
ScanCardsForSpace:      Sum: 26.288s 99% C.I. 8.617ms-77.759ms Avg: 38.377ms Max: 432.991ms
ProcessMarkStack:       Sum: 21.829s 99% C.I. 2.116ms-71.119ms Avg: 31.868ms Max: 98.679ms
ClearFromSpace: Sum: 19.420s 99% C.I. 5.480ms-50.293ms Avg: 28.351ms Max: 507.330ms
ScanImmuneSpaces:       Sum: 9.968s 99% C.I. 8.155ms-30.639ms Avg: 14.552ms Max: 46.676ms
SweepSystemWeaks:       Sum: 6.741s 99% C.I. 3.655ms-14.715ms Avg: 9.841ms Max: 22.142ms
GrayAllDirtyImmuneObjects:      Sum: 4.466s 99% C.I. 0.584ms-14.315ms Avg: 6.519ms Max: 24.355ms
FlipOtherThreads:       Sum: 3.672s 99% C.I. 0.631ms-16.630ms Avg: 5.361ms Max: 18.513ms
ProcessReferences:      Sum: 2.806s 99% C.I. 0.001ms-9.459ms Avg: 2.048ms Max: 11.951ms
EnqueueFinalizerReferences:     Sum: 1.857s 99% C.I. 0.424ms-8.609ms Avg: 2.711ms Max: 24.063ms
VisitConcurrentRoots:   Sum: 1.094s 99% C.I. 1.306ms-5.357ms Avg: 1.598ms Max: 6.831ms
SweepArray:     Sum: 711.032ms 99% C.I. 0.022ms-3.502ms Avg: 1.038ms Max: 7.307ms
InitializePhase:        Sum: 667.346ms 99% C.I. 303us-2643.749us Avg: 974.227us Max: 3199us
VisitNonThreadRoots:    Sum: 388.145ms 99% C.I. 103.911us-1385.833us Avg: 566.635us Max: 5374us
ThreadListFlip: Sum: 202.730ms 99% C.I. 18us-2414.999us Avg: 295.956us Max: 6780us
EmptyRBMarkBitStack:    Sum: 132.934ms 99% C.I. 8us-1757.499us Avg: 194.064us Max: 8495us
ResumeRunnableThreads:  Sum: 109.593ms 99% C.I. 6us-4719.999us Avg: 159.989us Max: 11106us
ResumeOtherThreads:     Sum: 86.733ms 99% C.I. 3us-4114.999us Avg: 126.617us Max: 19332us
ForwardSoftReferences:  Sum: 69.686ms 99% C.I. 14us-2014.999us Avg: 101.731us Max: 4723us
RecordFree:     Sum: 58.889ms 99% C.I. 0.500us-185.833us Avg: 42.984us Max: 769us
FlipThreadRoots:        Sum: 58.540ms 99% C.I. 1.034us-4314.999us Avg: 85.459us Max: 10224us
CopyingPhase:   Sum: 52.227ms 99% C.I. 26us-728.749us Avg: 76.243us Max: 2060us
ReclaimPhase:   Sum: 37.207ms 99% C.I. 7us-2322.499us Avg: 54.316us Max: 3826us
(Paused)GrayAllNewlyDirtyImmuneObjects: Sum: 23.859ms 99% C.I. 11us-98.917us Avg: 34.830us Max: 128us
FreeList:       Sum: 20.376ms 99% C.I. 2us-188.875us Avg: 29.573us Max: 998us
MarkZygoteLargeObjects: Sum: 18.970ms 99% C.I. 4us-115.749us Avg: 27.693us Max: 122us
(Paused)SetFromSpace:   Sum: 12.331ms 99% C.I. 3us-94.226us Avg: 18.001us Max: 109us
SwapBitmaps:    Sum: 11.761ms 99% C.I. 5us-49.968us Avg: 17.169us Max: 67us
ResetStack:     Sum: 4.317ms 99% C.I. 1us-64.374us Avg: 6.302us Max: 190us
UnBindBitmaps:  Sum: 3.803ms 99% C.I. 4us-49.822us Avg: 5.551us Max: 70us
(Paused)ClearCards:     Sum: 3.336ms 99% C.I. 250ns-7000ns Avg: 347ns Max: 7000ns
(Paused)FlipCallback:   Sum: 3.082ms 99% C.I. 1us-30us Avg: 4.499us Max: 30us
Done Dumping histograms
young concurrent copying paused:        Sum: 229.314ms 99% C.I. 37us-2287.499us Avg: 334.764us Max: 6850us
young concurrent copying freed-bytes: Avg: 44MB Max: 50MB Min: 9132KB
Freed-bytes histogram: 5120:1,15360:1,20480:6,25600:1,30720:1,35840:9,40960:235,46080:427,51200:4
young concurrent copying total time: 100.823s mean time: 147.187ms
young concurrent copying freed: 519927309 objects with total size 30GB
young concurrent copying throughput: 5.15683e+06/s / 304MB/s  per cpu-time: 333152554/s / 317MB/s
Average minor GC reclaim bytes ratio 0.52381 over 685 GC cycles
Average minor GC copied live bytes ratio 0.0512109 over 685 minor GCs
Cumulative bytes moved 1542000944
Cumulative objects moved 28393168
Peak regions allocated 376 (94MB) / 2048 (512MB)
Total time spent in GC: 670.771s
Mean GC size throughput: 159MB/s per cpu-time: 177MB/s
Mean GC object throughput: 2.94152e+06 objects/s
Total number of allocations 1974199562
Total bytes allocated 104GB
Total bytes freed 104GB
Free memory 10MB
Free memory until GC 10MB
Free memory until OOME 442MB
Total memory 80MB
Max memory 512MB
Zygote space size 2780KB
Total mutator paused time: 544.563ms
Total time waiting for GC to complete: 117.494ms
Total GC count: 2880
Total GC time: 670.771s
Total blocking GC count: 1
Total blocking GC time: 86.373ms
Histogram of GC count per 10000 ms: 0:259879,1:2828,2:24,3:1
Histogram of blocking GC count per 10000 ms: 0:262731,1:1
Native bytes total: 30599192 registered: 8947416
Total native bytes at last GC: 30344912
```

## 分析 GC 正确性问题的工具

造成 ART 内部崩溃的原因多种多样。读取或写入对象字段时发生崩溃可能表明堆损坏。如果 GC 在运行时崩溃，也可能是由堆损坏造成的。造成堆损坏的最常见原因是应用代码不正确。好在有一些工具可用来调试与 GC 和堆相关的崩溃问题，这些工具包括上面指定的堆验证选项和 CheckJNI。

### CheckJNI

CheckJNI 是一种添加 JNI 检查来验证应用行为的模式；出于性能方面的原因，默认情况下不启用此类检查。此类检查将捕获一些可能会导致堆损坏的错误，如使用无效/过时的局部和全局引用。如需启用 CheckJNI，请使用以下命令：

```
adb shell setprop dalvik.vm.checkjni true
```

CheckJNI 的 forcecopy 模式对于检测超出数组区域末端的写入很有用。启用后，forcecopy 会促使数组访问 JNI 函数返回带有红色区域的副本。红色区域是返回的指针末端/始端的一个区域，该区域具有一个特殊值，该值在数组释放时得到验证。如果红色区域中的值与预期值不匹配，表明发生了缓冲区溢出或欠载。这会导致 CheckJNI 中止。如需启用 forcecopy 模式，请使用以下命令：

```
adb shell setprop dalvik.vm.jniopts forcecopy
```

举例来说，当写入超出从 `GetPrimitiveArrayCritical` 获取的数组的末端时，这就是 CheckJNI 应捕获的一个错误。此操作可能会损坏 Java 堆。如果写入发生在 CheckJNI 红色区域内，则在调用相应的 `ReleasePrimitiveArrayCritical` 时，CheckJNI 会捕获该问题。否则，写入会损坏 Java 堆中的某个随机对象，并且可能会导致将来发生 GC 崩溃。如果损坏的内存是引用字段，则 GC 可能会捕获错误并输出错误消息“Tried to mark <ptr> not contained by any spaces”。

当 GC 尝试标记一个对象但无法找到其空间时，就会发生此错误。此检查失败后，GC 会遍历根，并尝试查看无效的对象是否为根。结果共有两个选项：对象为根或非根。

### 无效根示例

如果对象为无效根，则会输出一些有用的信息：`art E 5955 5955 art/runtime/gc/collector/mark_sweep.cc:383] Tried to mark 0x2 not contained by any spaces`

```
art E  5955  5955 art/runtime/gc/collector/mark_sweep.cc:384] Attempting see if
it's a bad root
art E  5955  5955 art/runtime/gc/collector/mark_sweep.cc:485] Found invalid
root: 0x2
art E  5955  5955 art/runtime/gc/collector/mark_sweep.cc:486]
Type=RootJavaFrame thread_id=1 location=Visiting method 'java.lang.Object
com.google.gwt.corp.collections.JavaReadableJsArray.get(int)' at dex PC 0x0002
(native PC 0xf19609d9) vreg=1
```

在本示例中，`com.google.gwt.corp.collections.JavaReadableJsArray.get` 内的 `vreg=1` 应该包含一个堆引用，但却包含了一个地址为 `0x2` 的无效指针。这是一个无效根。如需调试此问题，请在 oat 文件上使用 `oatdump`，并查看具有无效根的方法。在本示例中，结果证明错误在于 x86 后端的编译器错误。修复该错误的变更列表如下：https://android-review.googlesource.com/#/c/133932/

### 损坏的对象示例

如果对象不是根，则输出类似于以下内容：

```
01-15 12:38:00.196  1217  1238 E art     : Attempting see if it's a bad root
01-15 12:38:00.196  1217  1238 F art     :
art/runtime/gc/collector/mark_sweep.cc:381] Can't mark invalid object
```

如果堆损坏不是无效根，将很难调试。此错误消息表明堆中至少有一个对象指向无效对象。