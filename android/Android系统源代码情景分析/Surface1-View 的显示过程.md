## 一个 View 的显示过程

### 带着问题去学习：

1. **一个 View 究竟是怎么显示在屏幕上的？** 计算机内一切都是二进制的数据，想把一个东西在屏幕上展示出来，少不了跟显卡打交道，那么无非就是通过驱动往显卡内写一段数据不就能显示出来一些图像了。那么对于应用层来说，不可能去操作底层的显卡驱动啥的，而且肯定也不是塞数据就行了，肯定得按照一定格式啥的塞吧，这些我们都不关心，猜测是通过 OpenGL 啥的去操作。那么我们关心什么，我们关心图像，也就是数据，数据的生产者是我们，所以我们怎样把数据送进去是关心的。根据一些已有的知识，我们肯定是把数据给 SurfaceFlinger，由它帮我们把数据送给 OpenGL，再由 OpenGL 帮我们送入显卡。所以我们只需要关心数据怎样送给 SurfaceFlinger 就行了，ok，Surface 代表着什么呢？还有我们自定义 View 时，onDraw 里面的参数 Canvas 又代表什么？
2. **View 的绘制流程** View 的 onDraw、onMeasure、onLayout 是由谁调用的，都说是 ViewGroup，那么 ViewGroup 的这几个方法又是谁调用的呢，而且我们前面说过数据要发给 SurfaceFlinger 才能显示出图像来，很明显光靠这几个方法不可能显示出来，这几个方法可以理解为我们生产数据的一个过程，onDraw 里面生产着我们想要画个什么样的图像，而 onMeasure、onLayout 代表着我们生产的这个图像有多大以及想把这个图像放在哪个位置。另外 ViewGroup 一级一级往上，总得有个头吧，肯定得有个根 View 来控制自己的所有子 View，并且它帮我们把数据发给 SurfaceFlinger 才行。

Surface 像是 UI 的画布，我们就像是在这个画布上作画，所以 app 和 Surface 的关系是需要探究的。

Surface 向 SurfaceFlinger 提供数据，而 SurfaceFlinger 则混合数据，再交给 OpenGL

以上是我们分析之前的一些思考，可能有些想法并不对，但是这个过程得有。

### 1. Activity 与 View

第一个问题的思考，让我们从源头走起。首先我们的 View 是在 Activity 的承载下展示出来的，通过 `setContentView` 可以显示出我们 xml 文件中所写的内容，或者自定义的 View。然后当一个 Activity 的 onResume 被回调的时候，View 就显示出来了，那我们先看看 resume 的过程中做了些啥。

**ActivityThread.java**

```java
final void handleResumeActivity(IBinder token, boolean clearHide, boolean isForward) {
        ......
            boolean willBeVisible = !a.mStartedActivity;
            ......
            if (r.window == null && !a.mFinished && willBeVisible) {
                r.window = r.activity.getWindow();
                // 从 Window 中获得一个 Decor View
                View decor = r.window.getDecorView();
                decor.setVisibility(View.INVISIBLE);
                // 从 Activity 中获得 ViewManager
                ViewManager wm = a.getWindowManager();
                WindowManager.LayoutParams layoutParams = r.window.getAttributes();
                ......
                if (a.mVisibleFromClient) {
                    a.mWindowAdded = true;
                    // 把刚刚的 Decor View 添加到 ViewManager 中
                    wm.addView(decor, layoutParams);
                }

            ......
            }
            ......
    }
```

上面有三行代码注释，这些看起来都和 UI 有联系，什么 DecorView、Window、ViewManager 等。但是我们注意到 这些对象好像都是直接 get 的，说明在之前就已经创建好了，这里只是把 view 添加进 ViewManager 而已。那么这些东西是在哪里创建的呢，很容易联想到在 onCreate 中，我们通过 setContentView 设置了 UI 界面。那么只有可能是这里了。那只能先分析 setContentView 了。

#### 1.1 setContentView 分析

**Activity.java**

```java
public void setContentView(View view) {
    getWindow().setContentView(view);
}

public Window getWindow() {
     return mWindow; // Window 到底是什么
}
```

上面有两个和 UI 相关的类：View 和 Window。View 我们接触的多能理解，Window 是什么呢。翻译过来是窗口的意思。能联想到什么呢，还是看看 SDK 文档怎么说的吧
> Window : Abstract base class for a top-level window look and behavior policy.  An instance of this class should be used as the top-level view added to the window manager. It provides standard UI policies such as a background, title area, default key processing, etc.
> The only existing implementation of this abstract class is android.view.PhoneWindow, which you should instantiate when needing a  Window.

Window 是一个抽象基类，用于控制顶层窗口的外观和行为。**它的实例应该作为一个顶层 View 被添加到 window manager 中**。它还提供标准的 UI 策略，例如绘制背景、标题栏和默认的按键处理等。

另外它还说唯一的实现是 PhoneWindow，等会可以验证看看。

> View : This class represents the basic building block for user interface components. A View occupies a rectangular area on the screen and is responsible for drawing and event handling.

View 的概念就简单点了，它表示 UI 组件的基本单元，占据屏幕的一块矩形区域，负责绘制和处理事件。

根据上面的描述，我们可以想象一下一个基本的视图构成。

--> 插图 1

- 但是有个问题：在 resume 代码中我们把 Decor View 添加进了 WindowManager，可 Window 不是应该作为一个顶层 View 被添加进去吗，为什么这里没有？其实你看看 Window 的结构就知道了，它只是一个抽象类并没有继承 View，而 WindowManager  的 `addView(View view, ViewGroup.LayoutParams params)` 方法需要的是 View 的参数。那为什么官方文档说应该作为顶层 View 呢？现在应该可以想想，DecorView 才是实际的顶层 View，而 Window 更像是个管理控制的类，它控制着 DecorView。

首先我们来看看 Window 的实现类 PhoneWindow 是在哪里被创建出来的。

##### 1.1.1 PhoneWindow 的创建

先从 Activity 创建的地方看看。我们知道 Activity 通过反射创建出来后会去调用它的内部函数 `attach` 。

**Activity.java**

```java
final void attach(Context context, ActivityThread aThread,
            Instrumentation instr, IBinder token, int ident,
            Application application, Intent intent, ActivityInfo info,
            CharSequence title, Activity parent, String id,
            Object lastNonConfigurationInstance,
            HashMap<String,Object> lastNonConfigurationChildInstances,
            Configuration config) {
        attachBaseContext(context);
        // 这里在新的 sdk 中，已经替换这行代码了 
        // mWindow = new PhoneWindow(this, window, activityConfigCallback);
        // 可以看到直接 new 了 PhoneWindow
        mWindow = PolicyManager.makeNewWindow(this);
        mWindow.setCallback(this);
        ......
        mUiThread = Thread.currentThread();

        ......
        // 通过 Window 去创建这个 WindowManager
        mWindow.setWindowManager(null, mToken, mComponent.flattenToString());
        if (mParent != null) {
            mWindow.setContainer(mParent.getWindow());
        }
  			// 保存这个 WindowManager 对象到 Activity 中
        mWindowManager = mWindow.getWindowManager();
        mCurrentConfig = config;
    }
```

通过上述代码可以知道，在 Activity 的 `attach` 函数中创建了 PhoneWindow。那么 setWindowManager 又做了什么呢，如何创建的 WindowManager

**Window.java**

```java
public void setWindowManager(WindowManager wm, IBinder appToken, String appName, boolean hardwareAccelerated) {
        mAppToken = appToken;
        mAppName = appName;
        // 传入的 wm 值为 null
        // 新的 sdk 中虽然实现不一样，但也是为了获得 WindowManagerImpl
        // wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            wm = WindowManagerImpl.getDefault();
        }
        // 新版 sdk 实现如下
        // mWindowManager = ((WindowManagerImpl)wm).createLocalWindowManager(this);
        mWindowManager = new LocalWindowManager(wm);
    }
```

这里虽然创建的是 LocalWindowManager，但是在新版 sdk 中是直接创建的 WindowManagerImpl。所以就不分析 LocalWindowManager 了，其实 LocalWindowManager 也只是个代理而已，实际工作还是交给了 WindowManagerImpl 来完成。

由此可以知道两个事情：

- Activity 中的 mWindow 是 PhoneWindow。
- Activity 和 Window 都保存了 WindowManager，实例是 WindowManagerImpl。



重回 setContentView 分析。

直接看 PhoneWindow 的 setContentView 方法。

**PhoneWindow**

```java
// 这里的参数 view 是我们从 Activity 中传进来的自己写的 View
public void setContentView(View view, ViewGroup.LayoutParams params) {
        // mContentParent 是 ViewGroup 类型，初值肯定为 null
        if (mContentParent == null) {
            installDecor();
        } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            mContentParent.removeAllViews();
        }

        if (hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            ......
        } else {
            // 把我们自己写的 view 添加到 ViewGroup 中
            mContentParent.addView(view, params);
        }
        ......
    }
```

看看 installDecor，肯定和我们前面说的 DecorView 有关了。

**PhoneWindow**

```java
private void installDecor() {
    if(mDecor == null) {
        // 创建 mDecor，它是 DecorView 类型，DecorView 继承至 FrameLayout
        // 这里实际就是 new 出来一个 DecorView
        mDecor = generateDecor();
        ......
    }
    if (mContentParent == null) {
        // 得到 mContentParent
        mContentParent = generateLayout(mDecor);

        mTitleView = (TextView)findViewById(com.android.internal.R.id.title);
        ......
    }
}
```

所以我们看看 generateLayout 是怎样根据 mDecor 得到 ContentParent 的。

**PhoneWindow**

```java
protected ViewGroup generateLayout(DecorView decor) {
        ......
        // Inflate the window decor.
        int layoutResource;
        int features = getLocalFeatures();
        if ((features & ((1 << FEATURE_LEFT_ICON) | (1 << FEATURE_RIGHT_ICON))) != 0) {
            if (mIsFloating) {
                // 根据情况取得对应标题栏的资源 id
                layoutResource = com.android.internal.R.layout.dialog_title_icons;
            } else {
                ......
            }
        } ......

        mDecor.startChanging();

        // 标题栏 View
        View in = mLayoutInflater.inflate(layoutResource, null);
        // 将标题栏加入 DecorView
        decor.addView(in, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        // ID_ANDROID_CONTENT 的值为 "com.android.internal.R.id.content"
        // contentParent 由 findViewById 返回，前面我们在 DecorView 中只添加了一个标题栏，
        // 大家看看标题栏的 xml 布局应该就清楚了
        ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT);
        ......

        mDecor.finishChanging();

        return contentParent;
}
```

这个函数返回后，我们就获得了 mContentParent 它是一个 ViewGroup，根据标题栏的 xml，我们可知实际是个 FrameLayout。然后在上面的 setContentView 函数中，我们最终完成了 `mContentParent.addView(view, params);` 这样就把我们自己的 view 给添加进去了。

--> 插图 2



#### 1.2 重回 handleResumeActivity

setContentView 分析完后，我们可以继续分析 handleResumeActivity 了，因为现在我们已经知道 

- decor 代表着 DecorView
- ViewManager 代表着 WindowManagerImpl

现在开始着重分析 addView 这个过程

**WindowManagerImpl.java**

```java
// view: 是 DecorView
private void addView(View view, ViewGroup.LayoutParams params, boolean nest) 
{
    ViewRoot root; // ViewRoot，关键点
    synchronized(this) {
        // 
        root = new ViewRoot(view.getContext);
        root.mAddNesting = -1;
        view.setLayoutParams(wparams);
        if (mViews == null) {
            index = 1;
            mViews = new View[1];
            mRoots = new ViewRoot[1];
            mParams = new WindowManager.LayoutParams[1];
        } else {
            ......
        }
        index--;
        mViews[index] = view;
        mRoots[index] = root;
        mParams[index] = wparams;
    }
    // 这里的 view 是 DecorView
    root.setView(view, wparams, panelParentView);
}
```
ViewRoot 是什么？看名字感觉是 View 的根。从类的定义来看，它并不是一个 View，但是它确实和 View 有关系，因为实现了 ViewParent。但是 ViewParent 不处理 draw，那么它的作用是啥呢
> ViewParent: Defines the responsibilities for a class that will be a parent of a View.  This is the API that a view sees when it wants to interact with its parent.

```java
public final class ViewRoot extends Handler implements ViewParent,
        View.AttachInfo.Callbacks 
{
    private final Surface mSurface = new Surface();
    final W mWindow;
    View mView; // 这个 mView 就是 DecorView，上面分析的 root.setView 这个方法会给 mView 赋值
}
```

在 ViewRoot 的成员变量中，我们看到了一个 mSurface，终于出现了 Surface，而 ViewParent 又不处理 draw，那么我们大胆猜测，ViewRoot 是与 Surface 进行交互的。它让子 View（也就是成员变量 mView 所代表的子 View）去进行绘制，然后把绘制的数据传送给 Surface。这就是我们的第一个关键点，找到了与 Surface 进行交互的桥梁。另外，我们前面说过 ViewGroup 总得有个头吧，那么 ViewRoot 或许就是这个头了，它控制着 measure、layout、draw 这些流程。

**再次记忆，ViewRoot 是在 ActivityThread 的 handleResumeActivity 中的 wm.addView(decor, params) 中第一次创建出来的。**

既然 ViewRoot 中已经有了 Surface，那么我们来粗略的看看 draw 方法，到底是不是如我们所想。

```java
private void draw(boolean fullRedrawNeeded) {
    Surface surface = mSurface;
    if (surface == null || !surface.isValid()) {
        return;
    }

    if (!dirty.isEmpty() || mIsAnimating) {
        Canvas canvas;
        try {
            ......
            canvas = surface.lockCanvas(dirty);
            ......
        } ......

        try {
            if (!dirty.isEmpty() || mIsAnimating) {
                ......
                try {
                    ......
                    mView.draw(canvas);
                } finally {
                    ......
                }
               ......
            }
        } finally {
            surface.unlockCanvasAndPost(canvas);
        }
    }
    ......
}
```

在这里终于解除了我很久的疑惑，那就是 onDraw 里的 Canvas 到底是从哪传来的。从上面的代码中，我们很清晰的看到绘制一个 View 的三部曲。第一步先从 Surface 中 lockCanvas 拿到一个 Canvas；第二步让 View 去 draw(canvas)；第三步调用 surface 的 unlockCanvasAndPost。

到这里，其实我们还是不知道 surface、canvas 到底是个啥意思。但是结合我们前面的猜测再细想下。View 只是 UI 单元，它们都通过 onDraw 里的 canvas 来完成绘制。如果把 onDraw 想象成画画，那么我们的 canvas 是不是就是画布，我们在这个画布上作画。等等，canvas 是从 surface 中 lock 出来的，所以 surface 也是我们的画布。

SDK 文档对 Surface 的说明是这样的：我可以操控一块 raw buffer，这个 raw buffer 被 screen compositor 管理。（这里的 raw buffer 可以理解为 内存或者显存，反正有一块区域给我们用，screen compositor 其实就是 SurfaceFlinger 了）
> Surface：Handle onto a raw buffer that is being managed by the screen compositor.

-> 图三

**ViewRoot 的分析另一篇文章。**

由此可知 ViewRoot 是一个中间桥梁。







### 参考

深入理解 Android 卷一