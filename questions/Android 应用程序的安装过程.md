# Android 应用程序的安装过程

- PackageManagerService 在安装一个应用程序时，如果发现它没有与其他应用程序共享同一个 Linux 用户 ID，那么就会为它分配一个唯一的 Linux 用户 ID，以便它可以在系统中获得合适的运行权限。



### Step 1: PackageManagerService.main

```java
public static final IPackageManager main(Context context, boolean factoryTest) {
    PackageManagerService m = new PackageManagerService(context, factoryTest);
    ServiceManager.addService("package", m);
    return m;
}
```
调用 PMS 的 main 函数将 PMS 启动起来，并注册到 ServiceManager 中。



/system/framework：保存的应用程序是资源型。资源型的应用程序是用来打包资源文件的，不包含执行代码。

/system/app：系统自带的应用程序。

/vendor/app：设备厂商提供的应用程序。

/data/app：用户自己安装的应用程序。

/data/app-private：受 DRM 保护的私有应用程序。



`mSettings.readLP()` 恢复上一次的应用程序安装信息。

`scanDirLI()` 安装保存在 dir 里的应用程序。

`updatePermissionsLP()` 为申请了特定的资源访问权限的应用程序分配响应的 Linux 用户组 ID。

`mSettings.writeLP()` 将前面所获得的应用程序安装信息保存在本地的一个配置文件中，以便下一次再安装这些应用程序时，可以将需要保持一致的应用程序信息恢复过来（比如：应用程序的 Linux 用户 ID）。



