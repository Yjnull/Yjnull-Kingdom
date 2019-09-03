# interview questions

## Java相关
1. Java核心思想
2. Java高级知识，注解、反射、泛型的理解与作用		**x3**
3. Java线程，场景实现，多个线程如何同时请求，返回的结果如何等待所有线程数据完成后合成一个数据
4. 线程种类  **x2**
5. 什么是hash，作用是什么，hashMap的源码是什么，根据什么原理实现的
6. LinkedList与ArrayList，HashTable与HashMap的区别与存储过程与遍历方式
7. Java语言特点，OOP思想
8. Java中线程创建方式，线程池的工作原理 **x2**

## Android相关
1. framework方面的理解		**x2**
2. Activity的生命周期与启动模式(两个Activity调整的生命周期，如果一个Act跳转另一个Act再按下HOME键在回到Act的生命周期)	**x3**
3. 性能优化
4. 内存优化		**x3**
5. Bitmap如何优化，三级缓存的大致思想与逻辑		**x3**
6. 说出一个项目中用到的自定义View的实现，不要细节到代码，主要的思想与核心方法输出即可
7. ListView如何优化，复用的原理，为什么图片会错位，如何解决，分页的思想
8. Android子线程与主线程交互方式，原理以及各自的优缺点
9. 项目介绍，开源框架的认识以及如何封装，项目中的难点
10. 有哪几种创建线程的方式，优缺点
11. 热修复的实现原理与区别
12. 实际开发中的内存泄漏产生原因，如何查看，以及内存泄漏检测工具
13. handler的使用与原理，为什么会出现内存泄漏，为什么基础Handler就不会出现内存泄漏？   **?**
14. 线程切换与线程池的种类与作用，什么情况下使用多线程，有什么好处
15. 简单介绍四大组件(从Framework层说出大致流程) **x2**
16. AndroidManifest.xml有什么作用
17. 什么是多进程，进程和线程的区别，如何给四大组件指定多进程
18. 多进程之间的通信方式，如何使用AIDL。使用场景 **x3**
19. View的加载流程
20. 如何实现一个自定义View	**x3**
21. 如何选择第三方库，从哪些方面考虑
22. 设计一个音乐播放界面，你会如何实现，用到哪些类，如何设计，如何定义接口，如何与后台交互，如何缓存与下载，如何优化
23. 布局优化的理解
24. 你所知道的热更新与插件化，实现核心原理，异同
25. 从哪些角度可以减少APK体积 **x2**
26. gradle命令与打包，接触过哪些平台的上架
27. 接入支付的流程
28. Android的数据存储方式有哪些，SQLite中要继承哪个类来创建与更新数据库。SQL语句掌握怎么样，如何得到操作数据库的类等
29. Activity的横竖屏切换的生命周期，用哪个方法保存数据，两者的区别。触发在什么时候 在哪个方法里可以获取数据等
30. SurfaceView，它是什么？它的继承方式是什么？它与View的区别(源码角度，如加载、绘制等)
31. 如何实现进程保活
32. 冷启动与热启动是什么，区别，如何优化，使用场景等
33. Android中的线程有哪些(HandlerThrea、AsyncTask等，又问了他们的原理与各自特点)
34. 说下产生OOM，ANR的原因，三级缓存，如何优化ListView
35. 对Collection这个类的理解
36. 对广播的理解
37. 对服务的理解，如何杀死一个服务。服务的生命周期(start与bind)
38. 是否接触过蓝牙开发
39. 设计一个ListView左右分页排版的功能自定义View
40. binder序列化与反序列化过程，使用过程
41. JNI/NDK，java如何调用C语言的方法
42. 如何查看模拟器中的SP与SQLite文件。如何可视化查看布局嵌套层数与加载时间
43. 用过哪些注解框架，原理
44. java虚拟机的理解，JVM回收机制
45. 如何与后台交互，post请求的数据格式在哪里定义，手写
46. Android中特有的数据结构与常见的java数据结构，存储过程，源码与底层实现。
47. MVP架构理解

## 网络
1. http与https的理解，4层
2. http的结构(请求头、请求行)
3. session与cookie的区别
4. 如何在后台没有给接口的情况下开发
5. 如何封装网络请求，数据缓存，优化，文件下载，线程池，OKHttp3源码的理解，如何提交post请求，如何处理返回数据，异常处理，实体定义等实际开发中与后台协作相关知识

## 数据结构与算法
1. 手写冒泡排序
2. hash原理
3. 手写代码，如何找出一段字符串中，出现最多的字符是哪个
```
大致思路
int[] x = new int[26];
char[] c = str.toCharArray();
for(int i = 0; i < c.length; i++){
	x[c[i] - 'a']++; //只对小写的26个英文字母有效
}
```

## 设计模式
1. 手写三种单例模式
2. 熟知的设计模式与使用场景

## git相关









* * *

# interview-questions

## Java 相关
1. Java 线程有哪几种实现方式
2. Java 反射
3. Java 泛型
4. Java ClassLoader 加载机制
5. GC 回收算法
6. Java 的四种引用，强软弱虚，软引用和弱引用的区别
7. StringBuilder 和 StringBuffer 的区别

## 数据结构算法
1. 8种排序算法

	- 冒泡排序、选择排序、插入排序、归并排序、快速排序、堆排序、希尔排序、基数排序
	- > 选择排序、快速排序、希尔排序、堆排序**不是稳定**的排序算法。
	- > 冒泡排序、插入排序、基数排序、归并排序**是稳定**的排序算法。
2. 有几种查找算法

	- 顺序查找、二分查找、哈希表查找和二叉排序树查找。
	- 在有序数组 (或者部分有序的数组) 中查找一个数字或者统计某个数字出现的次数，可以尝试用**二分查找**。
	- **哈希表**最主要的优点能在 O(1) 时间查找某一个元素，缺点是 需要额外的空间实现哈希表。
	- **二叉排序树查找算法**对应的数据结构是 **二叉搜索树**。


## Android 相关
1. android 插件化
2. 热修复
3. 列表滑动问题，如何实现第一个 item 不滑动
4. NavigationView 侧滑菜单原理
5. 事件分发
6. View 绘制流程
7. 换肤功能如何实现，不知道的话， 你有什么思路实现
	**答：** 可参考 Android 插件化的技术来实现。

8. 布局优化
9. 缓存机制
10. Glide 图片框架原理，结合缓存说说
11. MVP,MVC,MVVM
12. handler 原理
13. 为什么要有 handler 机制
	**答：Handler 的主要作用是 将一个任务切换到 Handler所在的线程中去执行。**
    1. Android 规定访问 UI 只能在主线程中进行，如果在子线程中访问 UI，程序会抛出异常。ViewRootImpl 对 UI 操作做了验证。
```
void checkThread() {
        if (mThread != Thread.currentThread()) {
            throw new CalledFromWrongThreadException(
                    "Only the original thread that created a view hierarchy can touch its views.");
        }
    }
```
    2. 多个线程并发更新 UI 的同时，保证线程安全。

14. AIDL
15. android8.0 可以用文件进行进程通信嘛
	**答:**
    [Android 7.0 行为变更 Hongyang](https://blog.csdn.net/lmj623565791/article/details/72859156)
    [Android 7.0 行为变更 官方](https://developer.android.com/about/versions/nougat/android-7.0-changes)
    [官方已经在 API23 后废除了 SharedPreference 的多进程模式](https://www.jianshu.com/p/cc40ab07c14e)

16. 两个 ListView，一个竖滑，一个横向滑动，如何解决滑动冲突
17. 京东分类页面，两个滑动如何实现的
18. 数据库框架有用过吗，ormLite，greenDAO
19. 自定义 view
20. 微信上的下拉出现小程序列表是怎么做到的
21. LruCache 原理 通过 LinkedHashMap 实现




* * *

# A - Q

1. 内存映射原理
2. hashmap自己实现
3. hashmap  hashtable区别
4. 热修复更具体点
5. 下载文件 断点下载 , RandomAccessFile   Range
6. Android 新特性  画中画模式
7. mvp 模式
8. 事件分发时，viewgroup是怎样找到被点击的view的
9. 如果要你在应用最开始打一行日志，你会在哪打印。在application的构造函数中。

10. 标记整理算法，如果一个对象正在使用，怎样把它移到一端。(先暂停整个程序的全部运行线程，以回收线程进行扫描标记。)

# B - Q
1. 内部类 内存泄漏问题
2. looper阻塞的话是在主线程阻塞的吗，其中的ThreadLocal没讲清楚

3. 项目中的难点

# C - Q
1. 滑动冲突
2. 抽象工厂模式，简单工厂模式。
3. 手写代码，两个字符串，abcdfg   bf是包含在里面的fb不是。 (给定两个字符串，第二个是否包含在第一个里面)

# D - Q
1. oom解决思路
2. i am a boy 倒叙，不使用额外的空间。两次翻转
3. 复制算法为什么比标记整理清除 快？ 复制也需要把对象移动到另一块内存


# E - Q
1. onDraw 里频繁创建对象，效率低下的原因是 频繁GC
2. MessageQueue 中的 IDLE去了解下，阻塞原因
3. MeasureSpec
4. 性能优化 merge  用在什么情况下
5. 链表反转
6. setcontentview 怎么设置进去的
7. 调用PhoneWindow的setContentview
8. hashmap 具体   为什么扩容是2倍 indexFor
9. settextview 会重绘多少次  invalidate方法
10. TCP UDP 区别

# F - Q
1. http  https的区别。
2. 跨进程的方式，Binder优点，原理
3. 平衡二叉树判定，快排手写

# G - Q
1. jvm 内存区域、GC算法
2. 一套试卷，四个算法题，一个网络，一个sql
2.1 HTTP 请求格式， 状态行、请求头、消息主体

