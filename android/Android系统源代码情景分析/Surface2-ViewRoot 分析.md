## ViewRoot 分析

这里是衔接 深入理解 Surface 系统 那篇文章中的 ViewRoot 分析。

在那篇文章中，我们简略的列出了 ViewRoot 的三个成员变量，如下:

```java
public final class ViewRoot extends Handler implements ViewParent,
        View.AttachInfo.Callbacks 
{
    private final Surface mSurface = new Surface();
    final W mWindow;
    View mView; // 这个 mView 就是 DecorView，上面分析的 root.setView 这个方法会给 mView 赋值
}
```

另外我们知道 ViewRoot 是中间桥梁，连接着 View 和 Surface。我们会从 Surface 中拿出一块区域给 View 去绘制。也就是 View 的绘制是在 画布 Surface 上展开的。

从构造函数开始吧

### 1. ViewRoot 的创建

```java
public ViewRoot(Context context) {
        super();
        // WindowSession 是啥？
        getWindowSession(context.getMainLooper());
        
        mThread = Thread.currentThread();
        ......
        // 这里的 mWindow 是 W 类型，注意并不是我们认识的 Window
        // W 继承至 IWindow.Stub
        mWindow = new W(this, context);
        ......
    }
```

看看 getWindowSession 做了啥

```java
public static IWindowSession getWindowSession(Looper mainLooper) {
    synchronized (mStaticInit) {
        if (!mInitialized) {
            try {
                InputMethodManager imm = InputMethodManager.getInstance(mainLooper);
                sWindowSession = IWindowManager.Stub.asInterface(
                        ServiceManager.getService("window"))
                        .openSession(imm.getClient(), imm.getInputContext());
                mInitialized = true;
            } catch (RemoteException e) {
            }
        }
        return sWindowSession;
    }
}
```
根据代码解读，我们首先拿到 WMS 的 Binder 代理，然后调用它的 openSession 方法，拿到 WindowSession。好的，代码都看的懂，可是 WindowSession 是什么？WindowManagerService 又是啥？还是先看 setView 的处理吧。


### 2. setView 的分析

深入理解 Surface 系统中分析到在 WindowManagerImpl 去 addView 的时候会创建 ViewRoot，同时创建完后会调用 setView 方法，我们说过 setView 里会赋值 mView，也就是 mView = DecorView。现在来看看其余的细节吧。

```java
// 这里的 view 是 DecorView
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
    synchronized (this) {
        if (mView == null) {
            mView = view;
            ......

            // 请求布局？
            requestLayout();

            mInputChannel = new InputChannel();
            try {
                // mWindow 我们知道是 W 类型的。
                res = sWindowSession.add(mWindow, mWindowAttributes,
                        getHostVisibility(), mAttachInfo.mContentInsets,
                        mInputChannel);
            } catch (RemoteException e) {
                ......

            ......
        }
    }
}
```

setView 做了三件事

- 给 mView 赋值，这里的  mView 就指向 PhoneWindow 中的 DecorView
- 调用 requestLayout
- 调用 IWindowSession 的 add 函数，这是一个跨进程的 Binder 通信

看下 requestLayout 做了啥

```java
public void requestLayout() {
    checkThread();
    mLayoutRequested = true;
    scheduleTraversals();
}
public void scheduleTraversals() {
    if (!mTraversalScheduled) {
        mTraversalScheduled = true;
        sendEmptyMessage(DO_TRAVERSAL);
    }
}
```

这两段很简单，无非就是往 handler 中发送一个 DO_TRAVERSAL 的消息。我们知道 ViewRoot 继承了 Handler，所以它自己会处理这个消息，会去调用 performTraversals 方法，这个方法就控制着我们经典的 View 绘制流程了，measure、layout、draw。先暂时粗略的看下 performTraversals

**ViewRoot.java**

```java
private void performTraversals() {
    // 这里的 mView 还记得嘛，是 DecorView
    final View host = mView;
    ......
    if (mFirst || windowShouldResize || insetsChanged
            || viewVisibilityChanged || params != null) {
        ......
        try {
            ......
            // 关键函数 relayoutWindow
            relayoutResult = relayoutWindow(params, viewVisibility, insetsPending);
            ......
        } catch (RemoteException e) {
        }
        ......
        if (focusChangedDueToTouchMode || mWidth != host.mMeasuredWidth
                || mHeight != host.mMeasuredHeight || contentInsetsChanged) {
            childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
            childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);
            ......

             // 测量
            host.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            ......
        }
    }
    ......
    if (didLayout) {
        ......
        // 布局
        host.layout(0, 0, host.mMeasuredWidth, host.mMeasuredHeight);
        ......
    }
    ......
    if (!cancelDraw && !newSurface) {
        mFullRedrawNeeded = false;
        // 绘制
        draw(fullRedrawNeeded);
        ......
    } else {
        ......
    }
}
```

可以看到 performTraversals 里面包含了 测量、布局、绘制三大过程。另外 relayoutWindow 中会调用 IWindowSession 的 relayout 函数，同时会把 mSurface 传进去，这是个关键点，先记住这个调用。

### 3. ViewRoot 和 WMS 的关系

从上面的分析我们知道，ViewRoot 和 WMS 是有交互的。

- ViewRoot 调用 WMS 的 openSession 函数，得到一个 IWindowSession 对象。
- 调用 IWindowSession 的 add 函数，把一个 IWindow 类型的 mWindow 作为参数传入。

嗨，一个一个来吧，首先 openSession 是一个跨进程调用，所以我们来看看 WMS 的 openSession 函数。

**WindowManagerService.java**

```java
public IWindowSession openSession(IInputMethodClient client,IInputContext inputContext) {
    ......
    Session session = new Session(client, inputContext);
    return session;
}
```

Session 是 WMS 的内部类，定义如下，可以看到它支持 Binder 通信，属于 Bn 端，即响应请求的服务端。

 `private final class Session extends IWindowSession.Stub implements IBinder.DeathRecipient`

然后看下 Session 的 add 函数，代码就不放了，它的 add 函数会去调用 WMS 的 addWindow 函数，我们直接看看 addWindow

**WindowManagerService.java**

```java
// client 就是 ViewRoot 的 W 
public int addWindow(Session session, IWindow client,
            WindowManager.LayoutParams attrs, int viewVisibility,
            Rect outContentInsets, InputChannel outInputChannel) {
    ......
    synchronized(mWindowMap) {
        ......
        // 不能重复添加
        if (mWindowMap.containsKey(client.asBinder())) {
            Slog.w(TAG, "Window " + client + " is already added");
            return WindowManagerImpl.ADD_DUPLICATE_ADD;
        }
        ......
        // 创建了一个 WindowState
        win = new WindowState(session, client, token,
                attachedWindow, attrs, viewVisibility);
        ......
        // 调用 attach，并建立 client -> win 的映射
        win.attach();
        mWindowMap.put(client.asBinder(), win);
        ......
    }
    ......
    return res;
}
```

WindowState 也是 WMS 的内部类，它的 attach() 函数会去调用 Session 的 windowAddedLocked() 函数，

**WMS.java::Session**

```java
void windowAddedLocked() {
    if (mSurfaceSession == null) {
        ......
        // 创建了一个 SurfaceSession
        mSurfaceSession = new SurfaceSession();
        // ......
        mSessions.add(this);
    }
    mNumWindow++;
}
```

这里出现了一个跟 Surface 相关的 SurfaceSession 对象，看起来它很重要了。我们先停一停，看看我们现在做了些啥。

- 我们在 ViewRoot 构造函数中通过 getWindowSession 拿到了一个 IWindowSession，而它在 WMS 中的表现是 Session 这个内部类。
- 然后在 setView 方法中，我们调用了 IWindowSession 的 add 方法，并且把 W 类型的 mWindow 传了进去，W 也是有 Binder 通信能力的，它继承至 IWindow.Stub，是 IWindow 的 Bn 端，用于响应请求。
- 现在在 ViewRoot 即 Activity 所在进程中有 W 用来响应请求，IWindowSession 用来发送请求给 WMS。在 WMS 中有 Session 用来响应请求，同时可以通过 W 来把一些事件回调给 Activity 所在进程

-> 图四



### 4. 总结

至此，ViewRoot 粗略的分析完了，总结一下：

- ViewRoot 实现了 ViewParent 接口，它有三个重要的成员变量，一个是 mView，它指向 Activity 顶层 UI 单元的 DecorView。一个是 mSurface，这个 Surface 包含了一个 Canvas（画布）。另外一个是 mWindow，它是 W 类型的，具有 IPC 能力。
- ViewRoot 能处理 Handler 消息，View 的显示就是由 ViewRoot 在它的 performTraversals 函数中完成的。
- 在构造函数中，ViewRoot 初始化了 IWindowSession、mWindow。
- setView 中做了三步，第一步是给 mView 赋值 DecorView
- 第二步是 requestLayout，requestLayout 会发送一个 DO_TRAVERSAL 消息，由 ViewRoot 自己的 performTraversals 函数处理，这个函数会去调用一个重要的 IPC 调用 relayoutWindow（会把 mSurface 传进去）  以及测量、布局、绘制等过程。
- 第三步则是调用 IWindowSession 的 add 函数，会把 mWindow 传进去。add 函数会去调用 WMS 的 addWindow 函数，然后生成一个 WindowState 对象（把 mWindow 与 WindowState 建立映射），并调用 WindowState 的 attach 函数，而 attach 又会去调用 Session 的 windowAddedLocked，从而创建出 SurfaceSession 对象。
- IWindow 可以理解为 回调函数，用来处理事件分发的。

那么现在有两个没分析，一个 relayoutWindow 函数。一个 SurfaceSession 对象。




