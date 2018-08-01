## Android 的线程和线程池

### 0. 几个概念

在操作系统中，**线程** 是操作系统调度的最小单元，同时线程也是一种受限的系统资源，即线程不可能无限制的产生，并且线程的创建和销毁都会有相应的开销。

在 Android 中，从**用途**上来说，线程分为 **主线程（又叫 UI 线程）** 和 **子线程（又叫工作线程）**，其中 **主线程** 主要处理和界面相关的事情，而 **子线程** 则往往用于执行耗时操作。

### 1. HandlerThread

Handler的原理前面已经讲过，往往是在一个线程中运行Looper，其他线程通过Handler来发送消息到Looper所在线程，这里涉及线程间的通信。既然涉及多个线程的通信，会有同步的问题，Android对此直接提供了HandlerThread类，下面来讲讲HandlerThread类的设计。

HandlerThread 继承了 Thread，它是一种可以使用 Handler 的 Thread，它的实现也很简单。

```
	@Override
    public void run() {
        mTid = Process.myTid();  //获取线程的 tid
        Looper.prepare();        //创建 Looper 对象
        synchronized (this) {
            mLooper = Looper.myLooper();      //获取 Looper 对象
            notifyAll();                      //唤醒等待线程
        }
        Process.setThreadPriority(mPriority);
        onLooperPrepared();                   //该方法为空实现，可自己重写实现自己的逻辑
        Looper.loop();  					  //进入循环模式
        mTid = -1;
    }
```

可以看到在 run 方法里开启了 Looper 循环。而且它有个 getThreadHandler 方法，用于获取这个线程的 Handler。如下所示。

```
	/**
     * @return a shared {@link Handler} associated with this thread
     * @hide
     */
    @NonNull
    public Handler getThreadHandler() {
        if (mHandler == null) {
            mHandler = new Handler(getLooper());
        }
        return mHandler;
    }
```

用法：
```
	// Step 1: 创建并启动HandlerThread线程，内部包含Looper
	HandlerThread handlerThread = new HandlerThread("gityuan.com");
	handlerThread.start();

	// Step 2: 得到Handler
	Handler handler = handlerThread.getThreadHandler();

	// Step 3: 发送消息
	handler.post(new Runnable() {

        @Override
        public void run() {
            System.out.println("thread id="+Thread.currentThread().getId());
        }
    });
```

当然如果你明确不需要再使用时，可以通过它的 quit 或者 quitSafely 来终止线程的执行。
它在 Android 中的一个具体的使用场景就是 IntentService。

### 2. IntentService

IntentService 源码也不复杂， 当你了解了 HandlerThread 后，会异常简单。
首先 IntentService 是一个抽象类，所以我们需要创建它的子类并实现它的抽象方法 onHandleIntent(Intent intent) 。

> IntentService 可用于执行耗时的后台任务，当任务执行完后它会自动停止，同时由于它是服务的原因，所以它的优先级比单纯的线程要高很多。

**注意：** Service 是运行在主线程的，它里面不能做耗时任务。 
Android的后台是指，它的运行是完全不依赖UI的。

所以 IntentService 的出现方便的解决了这些问题，下面我们一一解答上面所说的那些功能，比如能执行耗时的后台任务，任务执行后会自动停止等等。

首先看 IntentService 的 onCreate 方法。
```
	public void onCreate() {
        // TODO: It would be nice to have an option to hold a partial wakelock
        // during processing, and to have a static startService(Context, Intent)
        // method that would launch the service & hand off a wakelock.

        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }
```
这里面很简答，我们刚刚了解了 HandlerThread 的概念，所以这里就是创建一个 HandlerThread，然后创建一个 ServiceHandler，这个 ServiceHandler 是什么呢。

```
private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent)msg.obj);
            stopSelf(msg.arg1);
        }
    }
```
它是 IntentService 的内部类，一个 Handler 而已， onHandleIntent 是我们要实现的抽象方法，stopSelf则很好的说明了 **当任务执行后它会自动停止** 这点。

那么 **能执行耗时的后台任务** 这一点相比大家也明白了，但是还是要说清楚一点。
- 首先，我们知道 Service 是运行在主线程的，当 onCreate 回调完，我们此时拥有了 handler 的实例即mServiceHandler， 另外在 oncreate 里还创建了一个 HandlerThread，大家要注意的是 HandlerThread 就是那个执行耗时任务的线程， 它不是主线程，不要弄混了，不要觉得有 handler 的地方就是在主线程里，大家要记得Handler 的作用是 **将一个任务切换到 Handler 所在的线程中执行**。

清楚了这一点后，想必就没什么难点了，onStartCommand 会去调用 onStart，然后里面会将传进来的 intent 组建成一个 Message 传给 mServiceHandler 去处理，注意 onHandleIntent 里面所运行的都是在另一个线程，即 mServiceHandler 所在的线程。
```
@Override
    public void onStart(@Nullable Intent intent, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    /**
     * You should not override this method for your IntentService. Instead,
     * override {@link #onHandleIntent}, which the system calls when the IntentService
     * receives a start request.
     * @see android.app.Service#onStartCommand
     */
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return mRedelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }
```


### 3. AsyncTask

内部有 handler、线程池、task、Call、isCanceld、publishProgress利用 handler 发个消息。 execute、get


### 4. 线程池




**参考**
Android 开发艺术探索