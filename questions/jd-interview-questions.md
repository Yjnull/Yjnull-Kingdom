# JD-interview-questions

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
16. 两个 ListView，一个竖滑，一个横向滑动，如何解决滑动冲突
17. 京东分类页面，两个滑动如何实现的
18. 数据库框架有用过吗，ormLite，greenDAO
19. 自定义 view
20. 微信上的下拉出现小程序列表是怎么做到的
21. LruCache 原理 通过LinkedHashMap实现
