## Surface 初探

Surface ——纵跨 Java / JNI 层的对象

### 1. 和 Surface 有关的流程

- ViewRoot 构造时，会创建一个 Surface，使用的无参构造函数。
- ViewRoot 在 setView 方法中，会调用 IWindowSession 的 add 方法，它最终会调用到 WMS 的 windowAddedLocked 函数，创建一个 SurfaceSession。
- ViewRoot 在 performTraversals 的处理过程中，会调用 IWindowSession 的 relayout 函数。
- ViewRoot 调用 Surface 的 lockCanvas 得到一块画布。
- ViewRoot 调用 Surface 的 unlockCanvasAndPost 释放这块画布。

### 2. relayout 函数

从这里开始分析，因为之前的都分析过了。

**ViewRoot.java**

```java
private int relayoutWindow(WindowManager.LayoutParams params, int viewVisibility,
            boolean insetsPending) throws RemoteException {
    ......
    // 注意传了 mSurface 参数
    int relayoutResult = sWindowSession.relayout(
            mWindow, params,
            (int) (mView.mMeasuredWidth * appScale + 0.5f),
            (int) (mView.mMeasuredHeight * appScale + 0.5f),
            viewVisibility, insetsPending, mWinFrame,
            mPendingContentInsets, mPendingVisibleInsets,
            mPendingConfiguration, mSurface);
    ......
    return relayoutResult;
}
```

这是一个 IPC 调用，由 WMS 中的 Session 实际处理。Session 会去调用 WMS 的 relayoutWindow 方法进一步处理。

```java
public int relayoutWindow(Session session, IWindow client,
            WindowManager.LayoutParams attrs, int requestedWidth,
            int requestedHeight, int viewVisibility, boolean insetsPending,
            Rect outFrame, Rect outContentInsets, Rect outVisibleInsets,
            Configuration outConfig, Surface outSurface) {
    ......
    synchronized(mWindowMap) {
        // 还记得前面的映射吗
        WindowState win = windowForClientLocked(session, client, false);
        ......

        if (viewVisibility == View.VISIBLE &&
                (win.mAppToken == null || !win.mAppToken.clientHidden)) {
            ......
            try {
                // 创建一个本地的 Surface
                Surface surface = win.createSurfaceLocked();
                if (surface != null) {
                    // 将本地 Surface 的内容拷贝到 ViewRoot 传递过来的 outSurface 中
                    outSurface.copyFrom(surface);
                    ......
        ......

    return (inTouchMode ? WindowManagerImpl.RELAYOUT_IN_TOUCH_MODE : 0)
            | (displayed ? WindowManagerImpl.RELAYOUT_FIRST_TIME : 0);
}
```

前面在 ViewRoot 的 setView 方法中，我们调用过 IWindowSession 的 add 方法，最终会调用 WMS 的 addWindow 函数，那里面我们创建了一个 WindowState 并且建立了和 IWindow 的映射。

这里我们将 WindowState 取出来，并调用它的 createSurfaceLocked 函数创建一个本地的 Surface。

**WMS.java::WindowState**

```java
Surface createSurfaceLocked() {
    if (mSurface == null) {
        ......
        try {
            mSurface = new Surface(
                    mSession.mSurfaceSession, mSession.mPid,
                    mAttrs.getTitle().toString(),
                    0, w, h, mAttrs.format, flags);
            ......
        } catch ......
        ......
        Surface.openTransaction();
        try {
            ......
        } finally {
            ......
            Surface.closeTransaction();
        }
        ......
    }
    return mSurface;
}
```

还记得这个 SurfaceSession 嘛，它是在 WindowState 被创建出来后，调用它的 attach 函数创建的 SurfaceSession。

#### 2.1 疑问

WindowState？客户端 Window State

SurfaceSession 是啥？

Surface？有两个构造函数，一个无参的，一个有参，有参的这个传入了 SurfaceSession。为啥要传 SurfaceSession？

#### 2.2 进入 JNI 层分析 Surface

还是围绕 relayoutWindow 展开的。

##### 2.2.1 SurfaceSession

根据 SDK 对 SurfaceSession 的描述，我们可以理解它的实例 代表了一个与 SurfaceFlinger 的连接 。首先来看 SurfaceSession 的构造，它的构造很简单调了 native 方法 init 函数。

**android_view_Surface.cpp**

```c++
static void SurfaceSession_init(JNIEnv* env, jobject clazz)
{
    sp<SurfaceComposerClient> client = new SurfaceComposerClient;
    client->incStrong(clazz);
    // 这里是将 SurfaceComposerClient 的指针保存在 SurfaceSession Java 对象中的 mClient 变量
    env->SetIntField(clazz, sso.client, (int)client.get());
}
```

##### 2.2.2 Surface 的有参构造

**android_view_Surface.cpp**

```java
public Surface(SurfaceSeesion s, int pid, String name, int display, int w, int h, int format, int flags) throws OutOfResourcesExeception {
    ......
    mCanvas = new CompatibleCanvas();
    // s 代表一个与 SurfaceFlinger 的连接；w，h 代表绘图区域的宽高
    init(s, pid, name, display, w, h, format, flags);
    mName = name;
} 
```

Surface 的 native init 函数的 JNI 实现，也在 android_view_Surface.cpp 中，来看看吧

**android_view_Surface.cpp**

```c++
static void Surface_init(
        JNIEnv* env, jobject clazz, 
        jobject session,
        jint pid, jstring jname, jint dpy, jint w, jint h, jint format, jint flags)
{
    ......
    // 从 SurfaceSession 中取出之前创建的 SurfaceComposerClient
    SurfaceComposerClient* client =
            (SurfaceComposerClient*)env->GetIntField(session, sso.client);
    // 注意这里是 SurfaceControl
    sp<SurfaceControl> surface;
    if (jname == NULL) {
        // 通过 SurfaceComposerClient 创建一个 SurfaceControl
        surface = client->createSurface(pid, dpy, w, h, format, flags);
    } else {
        ......
    }
    ......
    // 把这个 SurfaceControl 对象设置到 Java 层的 mSurfaceControl 中。
    setSurfaceControl(env, clazz, surface);
}
```

通过调用有参的 Surface 构造函数，我们可以通过 SurfaceSession 的 SurfaceComposerClient 拿到一个 SurfaceControl。现在暂时还不知道这个 SurfaceControl 是干啥的。

##### 2.2.3 copyFrom 分析

**android_view_Surface.cpp**

```c++
static void Surface_copyFrom(
        JNIEnv* env, jobject clazz, jobject other)
{
    ......

    /*
     * This is used by the WindowManagerService just after constructing
     * a Surface and is necessary for returning the Surface reference to
     * the caller. At this point, we should only have a SurfaceControl.
     */
    // clazz 代表 copyFrom 的调用对象，也就是 outSurface
    const sp<SurfaceControl>& surface = getSurfaceControl(env, clazz);
    // other 是我们通过有参构造函数创建的 Surface
    const sp<SurfaceControl>& rhs = getSurfaceControl(env, other);
    if (!SurfaceControl::isSameSurface(surface, rhs)) {
        // we reassign the surface only if it's a different one
        // otherwise we would loose our client-side state.
        setSurfaceControl(env, clazz, rhs);
    }
}
```

#### 2.3 小总结

- ViewRoot 在 setView 方法中，调用过 IWindowSession 的 add 方法，这个方法最终会调用到 WMS 层，然后创建一个 SurfaceSession，而 SurfaceSession 的构造函数会创建一个 SurfaceComposerClient。
- ViewRoot 的 relayout 把 mSurface 传给了 WMS
- WMS 则通过带参数的构造函数创建了一个新的 Surface，在这个构造函数中，会调用 SurfaceComposerClient 的 createSurface 得到一个 SurfaceControl 对象。
- 调用 SurfaceControl 的 writeToParcel 把一些信息写到 Parcel 中
- readFromParcel 根据 Parcel 包中的内容构造一个 Native 层的 Surface 对象，把这个 Surface 对象保存到 Java 层的 mSurface 对象中。
- 最终，ViewRoot 得到了一个  Native 的 Surface 对象。

### 3. lockCanvas

**android_view_Surface.cpp**

```java
static jobject Surface_lockCanvas(JNIEnv* env, jobject clazz, jobject dirtyRect)
{
    // 取出 Native 层的 Surface 对象
    const sp<Surface>& surface(getSurface(env, clazz));
    if (!Surface::isValid(surface))
        return 0;

    // 代表需要重绘的区域
    Region dirtyRegion;
    if (dirtyRect) {
        Rect dirty;
        dirty.left  = env->GetIntField(dirtyRect, ro.l);
        dirty.top   = env->GetIntField(dirtyRect, ro.t);
        dirty.right = env->GetIntField(dirtyRect, ro.r);
        dirty.bottom= env->GetIntField(dirtyRect, ro.b);
        if (!dirty.isEmpty()) {
            dirtyRegion.set(dirty);    
        }
    } else {
        dirtyRegion.set(Rect(0x3FFF,0x3FFF));
    }

    // 调用 Native Surface 的 lock 函数，拿到一块区域
    Surface::SurfaceInfo info;
    status_t err = surface->lock(&info, &dirtyRegion);
    ......

    // Java 的 Surface 对象在构造时，会创建一个 CompatibleCanvas，这里将它取出来
    jobject canvas = env->GetObjectField(clazz, so.canvas);
    env->SetIntField(canvas, co.surfaceFormat, info.format);
    // 从 CompatibleCanvas 中取出 SKCanvas
    SkCanvas* nativeCanvas = (SkCanvas*)env->GetIntField(canvas, no.native_canvas);

    SkBitmap bitmap;
    ssize_t bpr = info.s * bytesPerPixel(info.format);
    bitmap.setConfig(convertPixelFormat(info.format), info.w, info.h, bpr);
    if (info.format == PIXEL_FORMAT_RGBX_8888) {
        bitmap.setIsOpaque(true);
    }
    if (info.w > 0 && info.h > 0) {
        // info.bits 指向一块存储区域，由 Native Surface 分配的
        bitmap.setPixels(info.bits);
    } else {
        // be safe with an empty bitmap.
        bitmap.setPixels(NULL);
    }
    // 给 SKCanvas 设置一个 Bitmap，这样画布就出来了
    nativeCanvas->setBitmapDevice(bitmap);
    
    ......
    
    return canvas;
}
```

lockCanvas 主要就是找 Native Surface 拿到一块存储区域放到 SurfaceInfo.bits 中，然后将这块区域设置给 SkBitmap，再将整个 Bitmap 设置给 SkCanvas 就 OK 了。
