## Android 插件化

动态加载技术 也叫 插件化技术。

三个基础性问题
 1. 资源访问 2. Activity 生命周期的管理 3. ClassLoader 的管理

**宿主：** 普通的apk。
**插件：** 经过处理的 dex 或者 apk。

**动态加载定义**

- 应用在运行的时候通过加载一些**本地不存在**的可执行文件实现一些特定的功能。
- 这些可执行文件是**可以替换的**的。
- 更换静态资源（比如换启动图、换主题、或者用服务器参数开关控制广告的隐藏显示等）**不属于**动态加载。
- Android 中动态加载的核心思想是动态调用外部的 **dex文件**，极端的情况下，Android APK 自身带有的 Dex 文件只是一个程序的入库（或者说空壳），所有的功能都通过从服务器下载最新的 Dex 文件完成。

### PathClassLoader 与 DexClassLoader 的区别
- PathClassLoader：只能加载已经安装到Android系统中的apk文件（/data/app目录），是Android默认使用的类加载器。
- DexClassLoader：可以加载任意目录下的dex/jar/apk/zip文件，比PathClassLoader更灵活，是实现热修复的重点。

### 1. 资源访问

```
/** Return an AssetManager instance for your application's package. */
public abstract AssetManager getAssets();
/** Return a Resources instance for your application's package. */
public abstract Resources getResources();
```
复写这两个方法，通过反射调用 AssetManager 的 addAssetPath 方法。

### 2. Activity 生命周期的管理

采用接口机制，将activity的大部分生命周期方法提取出来作为一个接口（DLPlugin），然后通过代理activity（DLProxyActivity）去调用插件activity实现的生命周期方法

### 3. ClassLoader 的管理





**参考**
