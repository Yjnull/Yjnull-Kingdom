## Android 热修复

### 0. 如何动态修复 bug

- 1、下发补丁（内含修复号的 class）到用户手机，即让 app 从服务器上下载。（网络传输）
- 2、app 通过**某种方式**，使补丁中的 class 被 app 调用（本地更新）

这里的**某种方式**，特指 Android 的类加载器，通过类加载器加载这些修复好的 class，覆盖有问题的 class，理论上就能修复 bug 了。

### 1. PathClassLoader 与 DexClassLoader 的区别

由上述内容可知，Android 的类加载器是关键。
我们知道 jvm 有 ClassLoader，但是 Android 对 jvm 优化过，使用的是 dalvik/ART，且 class 文件会被打包进一个 dex 文件中，底层虚拟机有所不同，因此它们的类加载器肯定也会有所区别。
在 Android 中，要加载 dex 文件中的 class 文件就需要用到 **PathClassLoader** 或 **DexClassLoader** 这两个 Android 专用的类加载器。

那么这两个类加载器有何区别呢。

首先源码查看
[PathClassLoader](http://androidxref.com/5.0.0_r2/xref/libcore/dalvik/src/main/java/dalvik/system/PathClassLoader.java)
[DexClassLoader](http://androidxref.com/5.0.0_r2/xref/libcore/dalvik/src/main/java/dalvik/system/DexClassLoader.java)

#### 1.1 使用场景
- PathClassLoader：在应用启动时创建，从 data/app/... 安装目录下加载 apk 文件。是 Android 默认使用的类加载器。
- DexClassLoader：可以加载任意目录下的dex/jar/apk/zip文件，比 PathClassLoader 更灵活，是实现热修复的基础。

#### 1.2 代码差异

**PathClassLoader：**源码中就 2 个构造函数，如下所示
```
public PathClassLoader(String dexPath, ClassLoader parent) {
    super(dexPath, null, null, parent);
}

public PathClassLoader(String dexPath, String libraryPath,
        ClassLoader parent) {
    super(dexPath, null, libraryPath, parent);
}
```
- **dexPath：** 包含 dex 的 jar 文件或 apk 文件的路径集，多个以文件分隔符分隔，默认是 " : "
- **libraryPath：** 包含 C/C++ 库的路径集，多个同样以文件分隔符分隔，可以为空


- - -

**DexClassLoader：**源码中就 1 个构造函数，如下所示
```
public DexClassLoader(String dexPath, String optimizedDirectory,
        String libraryPath, ClassLoader parent) {
    super(dexPath, new File(optimizedDirectory), libraryPath, parent);
}
```
- **dexPath：** 包含 class.dex 的 apk、jar 文件路径 ，多个用文件分隔符(默认是 ：)分隔
- **optimizedDirectory：** 用来缓存优化的 dex 文件的路径，即从 apk 或 jar文件中提取出来的 dex 文件。该路径不能为空，且应该是应用私有的，有读写权限的路径（实际上也可以使用外部存储空间，但是这样的话就存在代码注入的风险）
- **libraryPath：** 存储 C/C++ 库文件的路径集
- **parent：** 父类加载器，遵从双亲委托模型

**结论：**
- **DexClassLoader：** 可以假装任意目录下的 dex/jar/apk/zip 文件，需要指定一个 optimizedDirectory。
- **PathClassLoader：** 只能加载已经安装到 Android 系统中的 apk 文件。
- 同时，它们都继承自 BaseDexClassLoader，所以真正的实现都在 BaseDexClassLoader 内。


#### 1.3 BaseDexClassLoader

1、当传入一个完整的类名，调用 BaseDexClassLoader 的 findClass(String name) 方法。
2、BaseDexClassLoader 的 findClass(String name) 方法 会交给 DexPathList 的 findClass(String name, List< Throwable > suppressed) 方法处理。
3、在 DexPathList 方法的内部，会遍历 dexElements 数组，得到具体的 Element，再通过 Element.dexFile，得到具体的 DexFile，通过 DexFile.loadClassBinaryName(name, definingContext, suppressed) 来完成类的加载


### 2. 热修复实现原理
通过上述分析，我们知道，安卓的类加载器在加载一个类时会先从 BaseDexClassLoader 的 DexPathList 对象中的 Element 数组中获取到对应的 DexFile，然后通过 DexFile 的 loadClassBinaryName 将类加载出来。
这里是通过 数组遍历，遍历出一个个 dex 文件。

所以，我们只要将修复好的 class 打包成一个 dex 文件，然后将它放在 Element 数组的第一个元素。这样当类加载时，就能保证获取到的 class 是最新修复好的 class 了。（当然，有bug的class也是存在的，不过是放在了Element数组的最后一个元素中，所以没有机会被拿到而已）。






**参考**
https://juejin.im/post/5a0ad2b551882531ba1077a2#heading-13
https://jaeger.itscoder.com/android/2016/09/20/nuva-source-code-analysis.html
