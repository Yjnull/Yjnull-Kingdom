## Activity 组件的启动过程

当我们在 Launcher 中点击一个 app 时，发生了什么？

### 1. 参与角色

- Activity
- Instrumentation
- ActivityThread：ActivityThread 用来描述一个应用程序进程，系统每当启动一个应用程序进程时，都会在该进程里加载一个 ActivityThread 实例，并且执行 main 方法，从而开启主线程 loop 循环。并且每一个在该进程中启动的 Activity 组件，都会保存这个 ActivityThread 实例在成员变量 mMainThread 中。
- ApplicationThread：看名字会很困惑以为也是一个线程，实则不然，它是一个 **Binder 本地对象**，可与 AMS 进行 IPC 通信。（继承 IApplicationThread.Stub）
- Launcher
- AMS
- ActivityStack：用来描述一个 Activity 组件堆栈。
- ActivityInfo：
- ResolveInfo：
- ProcessRecord：在 AMS 中，每一个应用程序进程都用 ProcessRecord 来描述，并且保存在 AMS 内部、
- New Process
- TaskRecord
- ActivityRecord：AMS 中的一个 Binder 本地对象，每一个已经启动的 Activity 组件在 AMS 中都有一个对应的 ActivityRecord 对象，用来维护对应的 Activity 组件的运行状态和信息。通常 Activity 中的 mToken 成员变量会指向它，mToken 是一个 Binder 代理对象。

### 2. 启动流程

#### 2.1 Launcher 发生的事

当我们在 Launcher 中点击一个 app 图标时，会调用 `startActivitySafely` 来启动这个 app 的根 Activity。

```java
void startActivitySafely(Intent intent, Object tag) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to launch. tag=" + tag + " intent=" + intent, e);
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity. "
                    + "tag="+ tag + " intent=" + intent, e);
        }
    }
```

因此参数 intent 所包含的信息有

```java
action = "android.intent.action.MAIN"
category = "android.intent.category.LAUNCHER"
cmp = "com.yjnull.demo.activity.MainActivity"
```

Launcher 是怎么获得这些信息的呢？Launcher 在最开始启动的过程中会向 PMS 查询所有 Activity 名称等于 "android.intent.action.MAIN"，并且 Category 等于 "android.intent.category.LAUNCHER" 的 Activity 组件。这样当用户点击一个快捷图标时，就可以拿到相应 Activity 组件的信息并启动起来。

接着上述代码讲，调用父类的 `startActivity(intent)`，即 `Activity.startActivity(intent)`，这里经过层层调用会走到 `Activity.startActivityForResult(Intent intent, int requestCode)` 这里去。

```
public void startActivityForResult(Intent intent, int requestCode) {
        if (mParent == null) {
            Instrumentation.ActivityResult ar =
                mInstrumentation.execStartActivity(
                    this, mMainThread.getApplicationThread(), mToken, this,
                    intent, requestCode);
            if (ar != null) {
                mMainThread.sendActivityResult(
                    mToken, mEmbeddedID, requestCode, ar.getResultCode(),
                    ar.getResultData());
            }
            if (requestCode >= 0) {
                ......
            }
        } else {
            ......
        }
    }
```

这里通过方法名其实就可以知道，会把启动 Activity 的操作委托给 `Instrumentation` 去做。所以 `Instrumentation` 是什么鬼。。这里有段代码上的原文注释：

> Base class for implementing application instrumentation code.  When running with instrumentation turned on, this class will be instantiated for you before any of the application code, allowing you to monitor all of the interaction the system has with the application.  An Instrumentation  implementation is described to the system through an AndroidManifest.xml's &lt;instrumentation&gt; tag.

大意就是它用来监控应用程序和系统之间的交互操作。因为 Activity 的启动最后需要通过 AMS 启动，而 AMS 又是运行在系统进程 (system 进程) 的，所以算是和系统的交互操作，因此需要交给 `Instrumentation` 来执行。

`execStartActivity` 方法里有几个参数需要注意：

- mMainThread.getApplicationThread()：Launcher 组件所在的应用程序进程的 ApplicationThread 的 Binder 本地对象。为了将它传递给 AMS，这样 AMS 接下来就可以通过它来通知 Launcher 组件进入 Paused 状态。
- mToken：类型为 IBinder，是一个 Binder 代理对象，指向 AMS 中一个类型为 ActivityRecord 的 Binder 本地对象。将它传递给 AMS 的话，AMS 就可以通过它获得 Launcher 组件的详细信息了。

接下来我们走进 `Instrumentation` 的 `execStartActivity` 方法

```java
public ActivityResult execStartActivity(
        Context who, IBinder contextThread, IBinder token, Activity target,
        Intent intent, int requestCode) {
        IApplicationThread whoThread = (IApplicationThread) contextThread;
        ......
        try {
            int result = ActivityManagerNative.getDefault()
                .startActivity(whoThread, intent,
                        intent.resolveTypeIfNeeded(who.getContentResolver()),
                        null, 0, token, target != null ? target.mEmbeddedID : null,
                        requestCode, false, false);
            checkStartActivityResult(result, intent);
        } catch (RemoteException e) {
        }
        return null;
    }
```

这里可以看到会通知 AMS 来将一个 Activity 组件启动起来。当然中间还有一些过程，无非就是 ActivityManagerProxy 将一些参数进行封装写入到 Parcel 对象中，然后通过 mRemote 向 AMS 发送一个类型为 START_ACTIVITY_TRANSACTION 的进程间通信请求。

#### 2.2 AMS 中做的事

接着上述来，Launcher 通过 Instrumentation 发起了一个 START_ACTIVITY_TRANSACTION 的进程间通信，因此会回调到 AMS 中的 startActivity 方法中去，如下所示：

```java
public final int startActivity(IApplicationThread caller,
            Intent intent, String resolvedType, Uri[] grantedUriPermissions,
            int grantedMode, IBinder resultTo,
            String resultWho, int requestCode, boolean onlyIfNeeded,
            boolean debug) {
        return mMainStack.startActivityMayWait(caller, intent, resolvedType,
                grantedUriPermissions, grantedMode, resultTo, resultWho,
                requestCode, onlyIfNeeded, debug, null, null);
    }
```

这里可以看到将启动操作委托给 mMainStack 了，mMainStack 是什么呢？它是一个类型为 ActivityStack 的成员变量，用来描述一个 Activity 组件堆栈。OK，那我们进到 ActivityStack 中的 startActivityMayWait 方法看看，这个方法挺长，所以分析就在注释里了：

**frameworks/base/services/java/com/android/server/am/ActivityStack.java**

```java
final int startActivityMayWait(IApplicationThread caller,
            Intent intent, String resolvedType, Uri[] grantedUriPermissions,
            int grantedMode, IBinder resultTo,
            String resultWho, int requestCode, boolean onlyIfNeeded,
            boolean debug, WaitResult outResult, Configuration config) {
        ......
        // 省略部分代码
        boolean componentSpecified = intent.getComponent() != null;
        
        // Don't modify the client's object!
        intent = new Intent(intent);

        // 这里定义了一个 ActivityInfo 对象，目测是用来整合前面传进来的 Intent 中所描述的信息
        ActivityInfo aInfo;
        try {
            // 这里通过 PMS 去解析参数 intent 的内容，以便可以获得即将启动的 Activity 组件的更多信息
            ResolveInfo rInfo =
                AppGlobals.getPackageManager().resolveIntent(
                        intent, resolvedType,
                        PackageManager.MATCH_DEFAULT_ONLY
                        | ActivityManagerService.STOCK_PM_FLAGS);
            // 将上面解析出来的信息保存在 ActivityInfo 中
            aInfo = rInfo != null ? rInfo.activityInfo : null;
        } catch (RemoteException e) {
            aInfo = null;
        }

        ......

        synchronized (mService) {
            ......
            
            int res = startActivityLocked(caller, intent, resolvedType,
                    grantedUriPermissions, grantedMode, aInfo,
                    resultTo, resultWho, requestCode, callingPid, callingUid,
                    onlyIfNeeded, componentSpecified);
            
            ......
            
            return res;
        }
    }
```

其实总结下来无非就是通过 PMS 将 Intent 中的参数解析出来，并获取到即将启动的 Activity 组件的更多信息，也就是 `com.yjnull.demo.activity.MainActivity` 的更多信息。

**frameworks/base/services/java/com/android/server/am/ActivityStack.java**

```java
final int startActivityLocked(IApplicationThread caller,
            Intent intent, String resolvedType,
            Uri[] grantedUriPermissions,
            int grantedMode, ActivityInfo aInfo, IBinder resultTo,
            String resultWho, int requestCode,
            int callingPid, int callingUid, boolean onlyIfNeeded,
            boolean componentSpecified) {
        int err = START_SUCCESS;

        ProcessRecord callerApp = null;
        if (caller != null) {
            // 这里的 mService 指向 AMS，所获得的 ProcessRecord 对象实际上指向了 Launcher 组件所在的应用程序进程。
            callerApp = mService.getRecordForAppLocked(caller);
            if (callerApp != null) {
                callingPid = callerApp.pid;
                callingUid = callerApp.info.uid;
            } else {
                ......
            }
        }
        
        ......
        // sourceRecord 用来描述 Launcher 组件的一个 ActivityRecord
        ActivityRecord sourceRecord = null;
        ......
        if (resultTo != null) {
            // 通过 resultTo 找到 Launcher 在 Activity 组件堆栈中的 index，这里的 resultTo 就是前面在 Launcher 进程中 Instrumentation 传递的 mToken 参数。
            int index = indexOfTokenLocked(resultTo);
            ......
            if (index >= 0) {
                sourceRecord = (ActivityRecord)mHistory.get(index);
                ......
            }
        }

        ......
        // 这里创建一个 ActivityRecord 用来描述即将启动的 Activity 组件，即 MainActivity 组件，可以注意，前面我们说过 mToken 就是指向 ActivityRecord 的，所以可以关注这个 ActivityRecord 对象，看看它是怎么传递给 Activity 类的成员变量
        ActivityRecord r = new ActivityRecord(mService, this, callerApp, callingUid,
                intent, resolvedType, aInfo, mService.mConfiguration,
                resultRecord, resultWho, requestCode, componentSpecified);

        ......
        return startActivityUncheckedLocked(r, sourceRecord,
                grantedUriPermissions, grantedMode, onlyIfNeeded, true);
    }
```



### 问题

1. mToken 是怎么初始化的。