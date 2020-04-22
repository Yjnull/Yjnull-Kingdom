## Activity 组件的启动过程

当我们在 Launcher 中点击一个 app 时，发生了什么？

### 1. 参与角色

- Activity
- Task：是一系列 Activity 的集合，这个集合是以堆栈的形式来组织的。
- Instrumentation
- ActivityThread：ActivityThread 用来描述一个应用程序进程，系统每当启动一个应用程序进程时，都会在该进程里加载一个 ActivityThread 实例，并且执行 main 方法，从而开启主线程 loop 循环。并且每一个在该进程中启动的 Activity 组件，都会保存这个 ActivityThread 实例在成员变量 mMainThread 中。
- ApplicationThread：看名字会很困惑以为也是一个线程，实则不然，它是一个 **Binder 本地对象**，可与 AMS 进行 IPC 通信。（继承 IApplicationThread.Stub）
- Launcher
- AMS
- ActivityStack：用来描述一个 Activity 组件堆栈。
- ActivityInfo：
- ResolveInfo：PMS.resolveIntent()，解析 intent 得到的一个信息
- ProcessRecord：在 AMS 中，每一个应用程序进程都用 ProcessRecord 来描述，并且保存在 AMS 内部、
- New Process
- TaskRecord
- ActivityRecord：AMS 中的一个 Binder 本地对象，每一个已经启动的 Activity 组件在 AMS 中都有一个对应的 ActivityRecord 对象，用来维护对应的 Activity 组件的运行状态和信息。通常 Activity 中的 **mToken** 成员变量会指向它，mToken 是一个 Binder 代理对象。

### 2. 启动流程

#### 2.1 Launcher 发生的事

当我们在 Launcher 中点击一个 app 图标时，会调用 `startActivitySafely` 来启动这个 app 的根 Activity。

```java
void startActivitySafely(Intent intent, Object tag) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            ......
        } catch (SecurityException e) {
            ......
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

**Step 1 frameworks/base/services/java/com/android/server/am/ActivityStack.java**

```java
/**
* caller: mMainThread.getApplicationThread(), Launcher 组件所在的应用程序进程ApplicationThread 的 Binder 本地对象。
* intent: 前面所构造的 Intent
* resolvedType: null
* grantedUriPermissions: null
* grantedMode: 0
* resultTo: Launcher Activity 中的 mToken，指向 AMS 中一个类型为 ActivityRecord 的 Binder 本地对象
* resultWho: null
* requestCode: -1
* onlyIfNeeded: false
* debug: false
* outResult: null
* config: null
*/
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
            int callingPid;
            int callingUid;
            if (caller == null) {
                ......
            } else {
                callingPid = callingUid = -1;
            }
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

**Step 2 frameworks/base/services/java/com/android/server/am/ActivityStack.java**

```java
/**
* ...... 同上
* aInfo: 通过 PMS 去解析参数 intent 的内容，得到 ResolveInfo.activityInfo
* resultTo: Launcher Activity 中的 mToken，指向 AMS 中一个类型为 ActivityRecord 的 Binder 本地对象
* resultWho: null
* requestCode: -1
* callingPid: -1
* callingUid: -1
* onlyIfNeeded: false
* componentSpecified: true
*/
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
            // 这里的 mService 指向 AMS，所获得的 ProcessRecord 对象实际上指向了 Launcher 组件所在的应用程序进程信息。
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
        // 这里的几个参数如下：AMS; ActivityStack; 描述 Launcher 的 ProcessRecord; Launcher 进程的 uid; intent; null; ActivityInfo 即将要启动的 Activity 相关信息; AMS.mConfiguration; null; null; -1; true
        ActivityRecord r = new ActivityRecord(mService, this, callerApp, callingUid,
                intent, resolvedType, aInfo, mService.mConfiguration,
                resultRecord, resultWho, requestCode, componentSpecified);

        ......
        return startActivityUncheckedLocked(r, sourceRecord,
                grantedUriPermissions, grantedMode, onlyIfNeeded, true);
    }
```

这一段首先是通过 resultTo 参数，在 Activity 堆栈中拿到 Launcher 这个 Activity 的相关信息并保存在 sourceRecod 中，然后创建一个新的 ActivityRecord 用来描述即将要启动的 Activity 的相关信息，并保存在变量 r 中。 接着调用 startActivityUncheckedLocked 函数进行下一步操作。

**Step 3 frameworks/base/services/java/com/android/server/am/ActivityStack.java**

```java
/**
* r: 上面新建的 ActivityRecord，用来描述即将要启动的 Activity 组件
* sourceRecord: 用来描述 Launcher 组件的一个 ActivityRecord
* grantedUriPermissions: null
* grantedMode: 0
* onlyIfNeeded: false
* doResume: true
*/
final int startActivityUncheckedLocked(ActivityRecord r,
            ActivityRecord sourceRecord, Uri[] grantedUriPermissions,
            int grantedMode, boolean onlyIfNeeded, boolean doResume) {
        final Intent intent = r.intent;
        final int callingUid = r.launchedFromUid;
        // 首先获得 intent 的标志位
        int launchFlags = intent.getFlags();
        
        // mUserLeaving = true
        mUserLeaving = (launchFlags&Intent.FLAG_ACTIVITY_NO_USER_ACTION) == 0;
        ......
        // notTop = null
        ActivityRecord notTop = (launchFlags&Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP)
                != 0 ? r : null;

        // 这里 onlyIfNeeded 为 false，所以不看里面的内容
        if (onlyIfNeeded) {
            ......
        }

        if (sourceRecord == null) {
            // 原文注释以及 Slog 打印的内容说的很清楚了，这正好是当我们以 ApplicationContext 启动一个 Activity 的情况，这种情况下，launchFlags 必须设置 FLAG_ACTIVITY_NEW_TASK
            // This activity is not being started from another...  in this
            // case we -always- start a new task.
            if ((launchFlags&Intent.FLAG_ACTIVITY_NEW_TASK) == 0) {
                Slog.w(TAG, "startActivity called from non-Activity context; forcing Intent.FLAG_ACTIVITY_NEW_TASK for: "
                      + intent);
                launchFlags |= Intent.FLAG_ACTIVITY_NEW_TASK;
            }
        } else if (sourceRecord.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
            // 如果 Launcher 的启动模式是 SINGLE_INSTANCE，那么我们要启动的 Activity 必须得在一个新的 task 中去启动。
            launchFlags |= Intent.FLAG_ACTIVITY_NEW_TASK;
        } else if (r.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE
                || r.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) {
            // The activity being started is a single instance...  it always
            // gets launched into its own task.
            launchFlags |= Intent.FLAG_ACTIVITY_NEW_TASK;
        }	

        if (r.resultTo != null && (launchFlags&Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
            ......
        }
  
        // 由于我们将要启动的 MainActivity 没有配置 launchMode 属性，所以这里的 r.launchMode == ActivityInfo.LAUNCH_MULTIPLE
        boolean addingToTask = false;
        if (((launchFlags&Intent.FLAG_ACTIVITY_NEW_TASK) != 0 &&
                (launchFlags&Intent.FLAG_ACTIVITY_MULTIPLE_TASK) == 0)
                || r.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK
                || r.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
            // r.resultTo 就是上面构造函数中的 resultRecord，可知是为 null 的，表示 Launcher 不需要等这个即将要启动的 MainActivity 的执行结果
            if (r.resultTo == null) {
                // 这里 r.launchMode 是不等于 SINGLE_INSTANCE 的，所以通过 findTaskLocked 来查找是否有 Task 可以用来执行这个将要启动的 Activity 组件。我们的场景是在 Launcher 中第一次启动一个 app，因此这里返回的 null，即 taskTop == null，因此需要创建一个新的 task 来启动 Activity
                ActivityRecord taskTop = r.launchMode != ActivityInfo.LAUNCH_SINGLE_INSTANCE
                        ? findTaskLocked(intent, r.info)
                        : findActivityLocked(intent, r.info);
                if (taskTop != null) {
                    ......
                }
            }
        }

        if (r.packageName != null) {
            // 当前在堆栈顶端的 Activity 是否就是即将要启动的 Activity，因为有些情况下，如果即将要启动的 Activity 就在堆栈的顶端，那么就不会重新启动这个 Activity 的另一个实例了。我们的场景下，当前处在堆栈顶端的 Activity 是 Launcher，因此不继续往下看
            ActivityRecord top = topRunningNonDelayedActivityLocked(notTop);	
            if (top != null && r.resultTo == null) {
                if (top.realActivity.equals(r.realActivity)) {
                    ......
                }
            }

        } else {
            ......
        }

        boolean newTask = false;

        // 首先 addingToTask 在我们的场景下，现在是 false
        // 执行到这里，其实就是要在一个新的 Task 里面来启动这个 Activity 了。
        if (r.resultTo == null && !addingToTask
                && (launchFlags&Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
            // todo: should do better management of integers.
            mService.mCurTask++;
            if (mService.mCurTask <= 0) {
                mService.mCurTask = 1;
            }
            // 新建一个 TaskRecord 
            r.task = new TaskRecord(mService.mCurTask, r.info, intent,
                    (r.info.flags&ActivityInfo.FLAG_CLEAR_TASK_ON_LAUNCH) != 0);
            ......
            newTask = true;
            if (mMainStack) {
                // 并且添加到 AMS 中
                mService.addRecentTaskLocked(r.task);
            }
        } else if (sourceRecord != null) {
            ......
        } else {
            ......
        }

        if (grantedUriPermissions != null && callingUid > 0) {
            ......
        }
        
        ......
        startActivityLocked(r, newTask, doResume);
        return START_SUCCESS;
    }
```

这一段主要是结合 Launcher 的 launchMode 以及将要启动的  MainActivity 的 launchMode 来判断是否需要在一个新的 Task 中启动这个 MainActivity，如果需要就 new 一个新的 TaskRecord，保存在 r.task 中，并添加到 AMS中，然后进入 `startActivityLocked(r, newTask, doResume)` 进一步处理。

总结这一段就是判断你的 LaunchMode，然后决定要不要新建 Task。



**Step 4 frameworks/base/services/java/com/android/server/am/ActivityStack.java**

```java
/**
* r: 上面的 ActivityRecord，现在 r.task 有值了
* newTask: true
* doResume: true
*/
private final void startActivityLocked(ActivityRecord r, boolean newTask,
            boolean doResume) {
        // mHistory 是一个 ArrayList ，存放着 ActivityRecord
        final int NH = mHistory.size();

        int addPos = -1;
        
        if (!newTask) {
            ......
        }

        // 这里 NH 肯定大于 0，因为 Launcher 已经跑起来了。
        if (addPos < 0) {
            addPos = NH;
        }
        
        if (addPos < NH) {
            ......
        }
        
        // Slot the activity into the history stack and proceed
        mHistory.add(addPos, r);
        r.inHistory = true;
        r.frontOfTask = newTask;
        r.task.numActivities++;
        if (NH > 0) {
            // We want to show the starting preview window if we are
            // switching to a new task, or the next activity's process is
            // not currently running.
            // 这一段是当切换新任务时，要做一些任务切换的界面操作，主要是操作 AMS 中的 WindowManager
            ......
        } else {
            // If this is the first activity, don't do any fancy animations,
            // because there is nothing for it to animate on top of.
            ......
        }
        ......
        if (doResume) {
            resumeTopActivityLocked(null);
        }
    }
```

这一段主要是将 ActivityRecord 添加到 mHistory 中，并做一些界面切换的操作，然后调用 `resumeTopActivityLocked` 进一步操作



**Step 5 frameworks/base/services/java/com/android/server/am/ActivityStack.java**

```java
/*
* prev: null
*/
final boolean resumeTopActivityLocked(ActivityRecord prev) {
        // Find the first activity that is not finishing.
        // 这边获取到的 next 就是要启动的 MainActivity 了
        ActivityRecord next = topRunningActivityLocked(null);

        // Remember how we'll process this pause/resume situation, and ensure
        // that the state is reset however we wind up proceeding.
        // 这里的 mUserLeaving 在上面的分析中得出是 true
        final boolean userLeaving = mUserLeaving;
        mUserLeaving = false;

        if (next == null) {
            ......
        }

        next.delayedResume = false;
        
        // mResumedActivity 这里是 Launcher
        // 这段主要是查看当前要启动的 Activity 是否就是当前处于 Resume 状态的 Activity，如果是的话就什么都不用做，直接返回了
        if (mResumedActivity == next && next.state == ActivityState.RESUMED) {
            // Make sure we have executed any pending transitions, since there
            // should be nothing left to do at this point.
            mService.mWindowManager.executeAppTransition();
            mNoAnimActivities.clear();
            return false;
        }

        // 这里是处理休眠状态时的情况，mLastPausedActivity 保存堆栈顶端的 Activity
        if ((mService.mSleeping || mService.mShuttingDown)
                && mLastPausedActivity == next && next.state == ActivityState.PAUSED) {
            // Make sure we have executed any pending transitions, since there
            // should be nothing left to do at this point.
            mService.mWindowManager.executeAppTransition();
            mNoAnimActivities.clear();
            return false;
        }
        ......
          
        // 在我们的情景中，上面两个情况肯定都不满足，因此执行到这里
        // We need to start pausing the current activity so the top one
        // can be resumed...
        if (mResumedActivity != null) {
            if (DEBUG_SWITCH) Slog.v(TAG, "Skip resume: need to start pausing");
            startPausingLocked(userLeaving, false);
            return true;
        }

        ......
        }

        return true;
    }
```

这一段主要做的事情是将当前处于 Resume 状态的 Activity 推入 Paused 状态去。这个过程也反映了当启动一个新的 Activity 时，旧 Activity 是先进入 Paused 状态，新 Activity 才 create 的。

到这里可以分个小阶段，因为我们要启动的 Activity 的信息保存下来了，Task 也建立起来了。接下来还是转场给 Launcher 这个应用程序进程了，得让它进入 Paused 状态。

------

#### 2.3 回到 Launcher 让它 Pause

**Step 6 frameworks/base/services/java/com/android/server/am/ActivityStack.java**

```java
/*
* userLeaving: true
* uiSleeping: false
*/
private final void startPausingLocked(boolean userLeaving, boolean uiSleeping) {
        if (mPausingActivity != null) {
            ......
        }
        // 这里的 prev 是 Launcher Activity
        ActivityRecord prev = mResumedActivity;
        if (prev == null) {
            ......
        }
        ......
        mResumedActivity = null;
        // 这个赋值在下面 2.4 小节有用
        mPausingActivity = prev;
        mLastPausedActivity = prev;
        prev.state = ActivityState.PAUSING;
        prev.task.touchActiveTime();

        mService.updateCpuStats();
        
        if (prev.app != null && prev.app.thread != null) {
            ......
            try {
                ......
                // 这里其实就是通过 Launcher 进程中的 ApplicationThread 来通知 Launcher 进入 Paused 状态。其中参数 prev.finishing 代表当前 Activity 是否正在等待结束的 Activity 列表中，由于 Launcher 正在运行，所以这里为 false
                prev.app.thread.schedulePauseActivity(prev, prev.finishing, userLeaving, prev.configChangeFlags);
                ......
            } catch (Exception e) {
                ......
            }
        } else {
            ......
        }
        ......
    }
```

这里其实就是通过 ApplicationThread 发起一个 IPC，通知 Launcher 进程进入 paused 状态。



**Step 7 frameworks/base/core/java/android/app/ApplicationThreadNative.java**

```java
public final void schedulePauseActivity(IBinder token, boolean finished,
            boolean userLeaving, int configChanges) throws RemoteException {
        Parcel data = Parcel.obtain();
        data.writeInterfaceToken(IApplicationThread.descriptor);
        data.writeStrongBinder(token);
        data.writeInt(finished ? 1 : 0);
        data.writeInt(userLeaving ? 1 :0);
        data.writeInt(configChanges);
        mRemote.transact(SCHEDULE_PAUSE_ACTIVITY_TRANSACTION, data, null,
                IBinder.FLAG_ONEWAY);
        data.recycle();
    }
```

没啥说的，通过 Binder 进入到 ApplicationThread.schedulePauseActivity 方法。

**Step 8 frameworks/base/core/java/android/app/ActivityThread.java**

```java
/*
* token: Launcher 的 ActivityRecord。
* finished: false
* userLeaving: true
* configChanges: 暂不关心
!!! 到这里是否可以猜测 mToken 是怎么赋值的了，前面我们知道给将要启动的 MainActivity 新建了一个 ActivityRecord 并且存在了 mHistory 中，想必接下来在给那个将要启动的 MainActivity 发送 Create 消息时，会把这个 ActivityRecord 带过来，这样 mToken 就给赋值了 !!!
*/
public final void schedulePauseActivity(IBinder token, boolean finished,
                boolean userLeaving, int configChanges) {
            queueOrSendMessage(
                    finished ? H.PAUSE_ACTIVITY_FINISHING : H.PAUSE_ACTIVITY,
                    token,
                    (userLeaving ? 1 : 0),
                    configChanges);
        }
```

这里无非就是通过 Handler 将消息发送出去了，最后是由 H.handleMessage 来处理这个消息。H 收到 Paused 消息后，会交给 handlePauseActivity 来处理。

**Step 9 frameworks/base/core/java/android/app/ActivityThread.java**

```java
private final void handlePauseActivity(IBinder token, boolean finished,
            boolean userLeaving, int configChanges) {
        ActivityClientRecord r = mActivities.get(token);
        if (r != null) {
            if (userLeaving) {
                // 我们前面知道 userLeaving 是为 true 的。
                // 执行这个方法会回调 Activity 的 onUserLeaveHint 通知 Activity，用户要离开它了
                performUserLeavingActivity(r);
            }

            r.activity.mConfigChangeFlags |= configChanges;
            // 回调 Activity 的生命周期，进入 onPause()
            Bundle state = performPauseActivity(token, finished, true);
            
            ......
            
            // 告诉 AMS 我们已经进入 Paused 了
            try {
                ActivityManagerNative.getDefault().activityPaused(token, state);
            } catch (RemoteException ex) {
            }
        }
    }
```

这里又可以告一段落了，Launcher 已经进入 onPause 了，并且去通知 AMS，AMS 接到这个通知就可以继续完成未完成的事情了，即启动 MainActivity。

------

#### 2.4 Launcher 通知 AMS 我暂停好了，你继续做你接下来要做的事吧

**Step 10 frameworks/base/services/java/com/android/server/am/ActivityManagerService.java**

```java
/*
* token: 是 Launcher 的 ActivityRecord
*/
public final void activityPaused(IBinder token, Bundle icicle) {
        // Refuse possible leaked file descriptors
        if (icicle != null && icicle.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Bundle");
        }

        final long origId = Binder.clearCallingIdentity();
        mMainStack.activityPaused(token, icicle, false);
        Binder.restoreCallingIdentity(origId);
    }
```

这里可以看到再次进入到 ActivityStack 类中，去执行 activityPaused 函数。

**Step 11 frameworks/base/services/java/com/android/server/am/ActivityStack.java**

```java
/*
* token: 是 Launcher 的 ActivityRecord
* icicle: 不关心
* timeout: false
*/
final void activityPaused(IBinder token, Bundle icicle, boolean timeout) {
        ......
        ActivityRecord r = null;

        synchronized (mService) {
            // 这里拿到的是 Launcher 在 mHistory 列表中的 index
            int index = indexOfTokenLocked(token);
            if (index >= 0) {
                // 拿到 Launcher 的 ActivityRecord
                r = (ActivityRecord)mHistory.get(index);
                if (!timeout) {
                    r.icicle = icicle;
                    r.haveState = true;
                }
                mHandler.removeMessages(PAUSE_TIMEOUT_MSG, r);
                // 在前面的 2.3 小节中，我们让 Launcher 进入 Paused 状态时，把 Launcher 赋值给了 mPausingActivity，因此下面这个判断是相等的
                if (mPausingActivity == r) {
                    r.state = ActivityState.PAUSED;
                    completePauseLocked();
                } else {
                    ......
                }
            }
        }
    }
```

这一段主要是判断 mPausingActivity 是否等于 token 代表的 Activity，如果是相等就代表完成了 Pause，进入  completePauseLocked 方法。

**Step 12 frameworks/base/services/java/com/android/server/am/ActivityStack.java**

```java
private final void completePauseLocked() {
        // 代表 Launcher
        ActivityRecord prev = mPausingActivity;
        ......
        
        if (prev != null) {
            ......
            // 这边把 mPausingActivity 置空，因为已经不需要了
            mPausingActivity = null;
        }

        if (!mService.mSleeping && !mService.mShuttingDown) {
            resumeTopActivityLocked(prev);
        } else {
            ......
        }
        
        ......
    }
```

这里 AMS 肯定还没有在睡眠也没有 shutdown，因此进入 resumeTopActivityLocked(prev)

**Step 13 frameworks/base/services/java/com/android/server/am/ActivityStack.java**

```java
/*
* prev: Launcher Activity
*/
final boolean resumeTopActivityLocked(ActivityRecord prev) {
        // 这边获取到的 next 就是要启动的 MainActivity 了
        ActivityRecord next = topRunningActivityLocked(null);

        // 这里的 mUserLeaving 在上面的分析中得出是 true
        final boolean userLeaving = mUserLeaving;
        mUserLeaving = false;
  
        ......
          
        next.delayedResume = false;
        
        // mResumedActivity 这里是 null，因为之前最后一个 Resumed 状态的 Activity 是 Launcher，现在它已经处于 Paused 状态了。
        if (mResumedActivity == next && next.state == ActivityState.RESUMED) {
            ......
            return false;
        }

        // 这里是处理休眠状态时的情况，这里 mLastPausedActivity 是 Launcher
        if ((mService.mSleeping || mService.mShuttingDown)
                && mLastPausedActivity == next && next.state == ActivityState.PAUSED) {
            ......
            return false;
        }
        ......
          
        // 在 Step 5 的时候，这里是满足情况的，会去执行 startPausingLocked，但是现在不满足了，mResumedActivity 已经为 null 了
        if (mResumedActivity != null) {
            ......
            startPausingLocked(userLeaving, false);
            return true;
        }
        ......
          
        // next 是将要启动的 MainActivity，前面我们只是为它创建了 ActivityRecord，然后就让 Launcher 去 Pause 了，因此这里的 app 域还是为 null 的。也很容易理解，我们还没启动起来呢，怎么可能不为 null
        if (next.app != null && next.app.thread != null) {
            ......
        } else {
            ......
            // 调用这个去启动 Activity
            startSpecificActivityLocked(next, true, true);
        }
  
        return true;
    }
```

这里在 Step 5 的时候分析过，那个时候 prev 是为 null 的，现在有值了，是 Launcher Activity，所以会走不同的逻辑了。

**Step 14 frameworks/base/services/java/com/android/server/am/ActivityStack.java**

```java
/*
* r: 要启动的 MainActivity 的 ActivityRecord
* andResume: true
* checkConfig: true
*/
private final void startSpecificActivityLocked(ActivityRecord r,
            boolean andResume, boolean checkConfig) {
        // 这里我们的是第一次启动应用程序的 Activity，所以取到的 app 为 null
        ProcessRecord app = mService.getProcessRecordLocked(r.processName,
                r.info.applicationInfo.uid);
        
        ......
        
        if (app != null && app.thread != null) {
            try {
                realStartActivityLocked(r, app, andResume, checkConfig);
                return;
            } catch (RemoteException e) {
                .......
            }
        }

        mService.startProcessLocked(r.processName, r.info.applicationInfo, true, 0,
                "activity", r.intent.getComponent(), false);
    }
```

**Step 15 frameworks/base/services/java/com/android/server/am/ActivityManagerService.java**

```java
/*
* processName: com.yjnull.demo
* info: null
* knownToBeDead: true
* intentFlags: 0
* hostingType: "activity"
* hostingName: ComponentName 对象
* allowWhileBooting: false
*/
final ProcessRecord startProcessLocked(String processName,
            ApplicationInfo info, boolean knownToBeDead, int intentFlags,
            String hostingType, ComponentName hostingName, boolean allowWhileBooting) {
        // 这里再次检查 process+uid 命名的进程是否存在，取到的 app 还是等于 null
        ProcessRecord app = getProcessRecordLocked(processName, info.uid);
        ......
          
        // 这里的 hostingNameStr = com.yjnull.demo/.activity.MainActivity
        String hostingNameStr = hostingName != null
                ? hostingName.flattenToShortString() : null;
        
        ......
        
        if (app == null) {
            // 创建一个 processRecord
            app = newProcessRecordLocked(null, info, processName);
            // mProcessNames 是一个 ProcessMap<ProcessRecord> 类型的变量
            mProcessNames.put(processName, info.uid, app);
        } else {
            // If this is a new package in the process, add the package to the list
            app.addPackage(info.packageName);
        }

        ......
        // 这里应该是去真正创建一个新的进程了
        startProcessLocked(app, hostingType, hostingNameStr);
        return (app.pid != 0) ? app : null;
}

// --- 接着看 startProcessLocked 方法 ---------------------------------------------

/*
* app: 上面新创建的 ProcessRecord
* hostingType: "activity"
* hostingNameStr: "com.yjnull.demo/.activity.MainActivity"
*/
private final void startProcessLocked(ProcessRecord app,
            String hostingType, String hostingNameStr) {
        ......
        
        try {
            int uid = app.info.uid;
            int[] gids = null;
            try {
                gids = mContext.getPackageManager().getPackageGids(
                        app.info.packageName);
            } catch (PackageManager.NameNotFoundException e) {
                Slog.w(TAG, "Unable to retrieve gids", e);
            }
            ......
            int debugFlags = 0;
            ......
            // 这里主要是通过 Process.start 来创建一个新的进程，新的进程会导入 android.app.ActivityThread 类，并执行它的 main 函数
            int pid = Process.start("android.app.ActivityThread",
                    mSimpleProcessManagement ? app.processName : null, uid, uid,
                    gids, debugFlags, null);
            ......
            if (pid == 0 || pid == MY_PID) {
                // Processes are being emulated with threads.
                app.pid = MY_PID;
                app.removed = false;
                mStartingProcesses.add(app);
            } else if (pid > 0) {
                app.pid = pid;
                app.removed = false;
                synchronized (mPidsSelfLocked) {
                    this.mPidsSelfLocked.put(pid, app);
                    Message msg = mHandler.obtainMessage(PROC_START_TIMEOUT_MSG);
                    msg.obj = app;
                    mHandler.sendMessageDelayed(msg, PROC_START_TIMEOUT);
                }
            } else {
                app.pid = 0;
                ......
            }
        } catch (RuntimeException e) {
            ......
        }
    }
```



#### 2.5 创建新进程去了

创建新进程的主要过程：

- 把一些参数拼接好，通过 socket 发出去。ZygoteInit 类在 runSelectLoopMode 函数会一直侦听是否有请求，当侦听到有请求来临时，会交给 ZygoteConnection 的 runOnce 函数去处理。这里面会通过 Zygote.forkAndSpecialize 真正创建进程。
- Zygote.forkAndSpecialize 创建一个进程后，会有两个返回值，一个是在当前进程中返回的，一个是在新创建的进程中返回的。在当前进程中返回的是新创建进程的 pid，而在新创建进程中返回的是 0。 当 pid 不等于 0 时，会调用 handleParentProc ，这里面会通过 `mSocketOutStream.writeInt(pid);` 将 pid 发送回去。这样上面所讲的 AMS 去启动一个新进程的流程就结束了，AMS 拿到了 pid，并赋给了 ProcessRecord。然鹅，新进程开始继续运行了呢。

- 创建好新进程后，肯定还需要一些处理，前面我们有拼接过一些参数，那些参数里有一个 `--runtime-init` ，因此新进程通过这个参数就知道要去初始化运行时库，于是继续执行 RuntimeInit.zygoteInit 进一步处理。这里面主要做了两件事，一个 zygoteInitNative()，一个 invokeStaticMain()。前者是执行 Binder 驱动程序初始化相关的工作。后者就是执行进程的入口函数，在这个场景下就是 `android.app.ActivityThread` 的 main 函数。

- 这样新进程就进入到 ActivityThread 的 main 函数了，在 main 里面，我们会创建一个 ActivityThread 实例，然后调用它的 attach 函数，接着就通过 Looper 进入消息循环了，直到最后进程退出。

在创建新进程这里我们好像断流程了。 AMS 在拿到 pid 后就结束了。

为什么会觉得断流程了，因为 MainActivity 还是没启动起来。但是仔细想想，我们运行 MainActivity 的进程已经启动起来了，并且调用了 attach 函数，那么我们新的流程就从 attach 开始分析。



#### 2.6 新进程调用 ActivityThread.attach()

函数 attach 最终会调用 AMS 的 attachApplication 函数，传入的参数是 mAppThread。

**Step 16 frameworks/base/services/java/com/android/server/am/ActivityManagerService.java**

```java
/*
* thread: 新进程的 ApplicationThread
* pid: 新进程的 Binder.getCallingPid()
*/
private final boolean attachApplicationLocked(IApplicationThread thread,
            int pid) {
        // Find the application record that is being attached...  either via
        // the pid if we are running in multiple processes, or just pull the
        // next app record if we are emulating process with anonymous threads.
        ProcessRecord app;
        if (pid != MY_PID && pid >= 0) {
            synchronized (mPidsSelfLocked) {
                // 前面在 Step15 创建一个新进程获得 pid 时，将 ProcessRecord put 进 mPidsSelfLocked 了。这里通过 pid 取出来
                app = mPidsSelfLocked.get(pid);
            }
        } else if (mStartingProcesses.size() > 0) {
            ......
        } else {
            ......
        }

        if (app == null) {
            ......
            return false;
        }

        ......

        String processName = app.processName;
        try {
            thread.asBinder().linkToDeath(new AppDeathRecipient(
                    app, pid, thread), 0);
        } catch (RemoteException e) {
            ......
            return false;
        }

        ......
        
        app.thread = thread;
        app.curAdj = app.setAdj = -100;
        app.curSchedGroup = Process.THREAD_GROUP_DEFAULT;
        app.setSchedGroup = Process.THREAD_GROUP_BG_NONINTERACTIVE;
        app.forcingToForeground = null;
        app.foregroundServices = false;
        app.debugging = false;

        mHandler.removeMessages(PROC_START_TIMEOUT_MSG, app);

        boolean normalMode = mProcessesReady || isAllowedWhileBooting(app.info);
        ......

        boolean badApp = false;
        boolean didSomething = false;

        // See if the top visible activity is waiting to run in this process...
        ActivityRecord hr = mMainStack.topRunningActivityLocked(null);
        if (hr != null && normalMode) {
            if (hr.app == null && app.info.uid == hr.info.applicationInfo.uid
                    && processName.equals(hr.processName)) {
                try {
                    if (mMainStack.realStartActivityLocked(hr, app, true, true)) {
                        didSomething = true;
                    }
                } catch (Exception e) {
                    ......
                    badApp = true;
                }
            } else {
                ......
            }
        }

        // Find any services that should be running in this process...
        if (!badApp && mPendingServices.size() > 0) {
            ServiceRecord sr = null;
            try {
                for (int i=0; i<mPendingServices.size(); i++) {
                    sr = mPendingServices.get(i);
                    if (app.info.uid != sr.appInfo.uid
                            || !processName.equals(sr.processName)) {
                        continue;
                    }

                    mPendingServices.remove(i);
                    i--;
                    realStartServiceLocked(sr, app);
                    didSomething = true;
                }
            } catch (Exception e) {
                Slog.w(TAG, "Exception in new application when starting service "
                      + sr.shortName, e);
                badApp = true;
            }
        }

        // Check if the next broadcast receiver is in this process...
        BroadcastRecord br = mPendingBroadcast;
        if (!badApp && br != null && br.curApp == app) {
            try {
                mPendingBroadcast = null;
                processCurBroadcastLocked(br, app);
                didSomething = true;
            } catch (Exception e) {
                Slog.w(TAG, "Exception in new application when starting receiver "
                      + br.curComponent.flattenToShortString(), e);
                badApp = true;
                logBroadcastReceiverDiscardLocked(br);
                finishReceiverLocked(br.receiver, br.resultCode, br.resultData,
                        br.resultExtras, br.resultAbort, true);
                scheduleBroadcastsLocked();
                // We need to reset the state if we fails to start the receiver.
                br.state = BroadcastRecord.IDLE;
            }
        }

        // Check whether the next backup agent is in this process...
        if (!badApp && mBackupTarget != null && mBackupTarget.appInfo.uid == app.info.uid) {
            if (DEBUG_BACKUP) Slog.v(TAG, "New app is backup target, launching agent for " + app);
            ensurePackageDexOpt(mBackupTarget.appInfo.packageName);
            try {
                thread.scheduleCreateBackupAgent(mBackupTarget.appInfo, mBackupTarget.backupMode);
            } catch (Exception e) {
                Slog.w(TAG, "Exception scheduling backup agent creation: ");
                e.printStackTrace();
            }
        }

        if (badApp) {
            // todo: Also need to kill application to deal with all
            // kinds of exceptions.
            handleAppDiedLocked(app, false);
            return false;
        }

        if (!didSomething) {
            updateOomAdjLocked();
        }

        return true;
    }
```



### 问题

1. mToken 是怎么初始化的。