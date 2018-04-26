# JVM

## 垃圾收集器参数总结

表 1-1 垃圾收集相关的常用参数

| 参数                   |  描述   |
|---------------        |:------:|
| UseSerialGC           | 虚拟机运行在Client模式下的默认值，打开此开关后，使用 Serial+Serial Old 的收集器组合进行内存回收 |
| UseParNewGC           | 打开此开关后，使用 ParNew + Serial Old 的收集器组合进行内存回收 |
| UseConcMarkSweepGC    | 打开此开关后，使用 ParNew + CMS + Serial Old 的收集器组合进行内存回收。Serial Old 收集器将作为 CMS 收集器出现 Concurrent Mode Failure 失败后的后备收集器使用 |
| UseParallelGC         | 虚拟机运行在 Server 模式下的默认值，打开此开关后，使用 Parallel Scavenge + Serial Old(PS MarkSweep) 的收集器组合进行内存回收 |
| UseParallelOldGC      | 打开此开关后，使用 Parallel Scavenge + Parallel Old 的收集器组合进行内存回收 |
| SurvivorRatio        | 新生代中 Eden 区域与 Survivor 区域的容量比值，默认为8，代表 Eden : Survivor = 8 : 1 |
| PretenureSizeThreshold   | 直接晋升到老年代的对象大小，设置这个参数后，大于这个参数的对象将直接在老年代分配 |
| MaxTenuringThreshold     | 晋升到老年代的对象年龄，每个对象在坚持过一次 Minor GC 之后，年龄就增加1，当超过这个参数值时就进入老年代 |
| UseAdaptiveSizePolicy    | 动态调整 Java 堆中各个区域的大小以及进入老年代的年龄 |
| HandlePromotionFailure   | 是否允许分配担保失败，即老年代的剩余空间不足以应付新生代的整个 Eden 和 Survivor 区的所有对象都存活的极端情况 |
| ParallelGCThreads       | 设置并行GC时进行内存回收的线程数 |


(续)

| 参数                             |  描述   |
|--------------- ---------------  |:------:|
| GCTimeRatio                     | GC 时间占总时间的比率，默认值为99，即允许 1% 的GC时间，仅在使用 Parallel Scavenge 收集器生效 |
| MaxGCPauseMillis                | 设置 GC 的最大停顿时间，仅在使用 Parallel Scavenge 收集器时生效 |
| CMSInitiatingOccupancyFraction  | 设置 CMS 收集器在老年代空间被使用多少后触发垃圾收集，默认值为 68%，仅在使用 CMS 收集器时生效 |
| UseCMSCompactAtFullCollection   | 设置 CMS 收集器在完成垃圾收集后是否要进行一次内存碎片整理，仅在使用 CMS 收集器时生效 |
| CMSFullGCsBeforeCompaction      | 设置 CMS 收集器在进行若干次垃圾收集后再启动一次内存碎片整理，仅在使用 CMS 收集器时生效 |

