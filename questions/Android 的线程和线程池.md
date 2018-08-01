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

清楚了这一点后，想必就没什么难点了，onStartCommand 会去调用 onStart，然后里面会将传进来的 intent 组建成一个 Message 通过 mServiceHandler 发送消息，然后在 onHandleIntent 中去处理，注意 onHandleIntent 里面所运行的都是在另一个线程，即 mServiceHandler 所在的线程。
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

关于它的一些定义啥的，这里就不阐述了。首先看这个类的声明

`public abstract class AsyncTask<Params, Progress, Result>`

根据它的声明，我们可以得到这些信息，AsyncTask 是一个抽象的泛型类，它提供了 Params, Progress, Result 这三个泛型参数。还有个 抽象方法 `protected abstract Result doInBackground(Params... params);`
- Params ： 参数的类型，即 doInBackground(Params... params)，就是这个方法的参数类型。
- Progress ：后台任务的执行进度的类型。
- Result ：后台任务的返回结果的类型。


AsyncTask 提供了4个核心方法
- onPreExecute()
- doInBackground(Params... params)   在线程池中执行，此方法用于执行异步任务。
- onProgressUpdate(Progress... values)
- onPostExecute(Result result)

这四个方法的含义也不解释了，后面看源码就会清楚的，当然你应该也了解这四个方法的含义。额，我这篇文章只是用来记录自己的随想，所以谅解、谅解。

AsyncTask 在具体的使用过程中是有一些限制条件的，主要有如下几点：
- 1、AsyncTask 的类必须在主线程中加载
- 2、AsyncTask 的对象必须在主线程中创建
- 3、execute 方法必须在主线程中调用
- 4、不要在程序中直接调用 onPreExecute()、onPostExecute、doInBackground 和 onProgressUpdate 方法
- 5、一个 AsyncTask 对象只能执行一次，即只能调用一次 execute 方法，否则会报运行时异常

现在我们针对上述几个限制条件来分析，这样带着问题去分析会让自己不迷失。

##### 3.1 AsyncTask 的类必须在主线程中加载

针对这个问题，Android4.1 之前 AsyncTask类 必须在主线程中加载，但是在之后的版本中就被系统自动完成。而在Android5.0 的版本中会在 ActivityThread 的 main方法 中执行 AsyncTask 的 init 方法，而在 Android6.0 中又将  init 方法删除。所以在使用这个 AsyncTask 的时候若是适配更多的系统的版本的话，使用的时候就要注意了。

AsyncTask 内部是通过 Handler 来进行线程切换的。所以我们要想在主线程中去处理结果，那 Handler 肯定得在主线程中去创建。

首先 AsyncTask 在成员变量位置 声明了静态的 Handler
```
private static InternalHandler sHandler;
```

然后在构造函数中可以看到这里对 mHandler 赋值，其中会调用 getMainHandler。 
我们一般调用的都是那个 无参的构造函数，这里会传null给有参的那个， 其中会判断，如果 callbackLooper == null 就会去调用 getMainHandler()
```
	public AsyncTask() {
        this((Looper) null);
    }

	public AsyncTask(@Nullable Looper callbackLooper) {
        mHandler = callbackLooper == null || callbackLooper == Looper.getMainLooper()
            ? getMainHandler()
            : new Handler(callbackLooper);

            //省略后续代码
    }
```

可以看到，这样既保证了 Handler 采用主线程的 Looper 构建，又使得 AsyncTask 在需要时才被加载。
```
	private static Handler getMainHandler() {
        synchronized (AsyncTask.class) {
            if (sHandler == null) {
                sHandler = new InternalHandler(Looper.getMainLooper());
            }
            return sHandler;
        }
    }
```

##### 3.3 execute 方法必须在主线程中调用

那为什么 execute 方法必须在主线程中调用呢， 我们前面知道 AsyncTask 的 4 个核心方法除了 doInBackground，其余的都允许在 UI 线程，那么 onPreExecute 这个方法肯定也得运行在主线程中。 

我们往往调用 AsyncTask.execute 方法去执行任务。

```
public final AsyncTask<Params, Progress, Result> execute(Params... params) {
        return executeOnExecutor(sDefaultExecutor, params);
    }

public final AsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec,
            Params... params) {
        if (mStatus != Status.PENDING) {
            switch (mStatus) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)");
            }
        }

        mStatus = Status.RUNNING;

        onPreExecute();

        mWorker.mParams = params;
        exec.execute(mFuture);

        return this;
    }
```

从上述代码可以知道， executeOnExecutor 内部会调用 onPreExecute()，这也叫解释了为什么 **execute 方法必须在主线程中调用**，只有在主线程中调用， onPreExecute() 方法才会运行在 主线程中。


##### 3.4 一个 AsyncTask 对象只能执行一次，即只能调用一次 execute 方法，否则会报运行时异常

这个问题就很好解答了，在3.3里面的 executeOnExecutor 方法内部很明确， 如果 mStatus != Status.PENDING，就会抛出异常。而我们执行一个任务的时候会把 mStatus = Status.RUNNING ，因此这个疑惑也解决了。

当然 AsyncTask 还有很多可以讲的，比如它内部的 线程池，默认是使用 SerialExecutor 串行执行的，还有 InternalHandler 的 handleMessage ， 以及它内部的 FutureTask 和 WorkerRunnable 等。 这些只要看源码就知道了。 整个 AsyncTask 并不是特别难， 所以它是一种很好用的 **轻量级的异步任务类。**


### 4. 线程池

线程池三个优点：
- 重用线程池中的线程，避免因为线程的创建和销毁所带来的性能开销
- 能有效控制线程池的最大并发数，避免大量的线程之间因互相抢占系统资源而导致的阻塞现象
- 能够对线程进行简单的管理，并提供定时执行以及指定间隔循环执行等功能

Java 通过 **Executors** 提供四种线程池，分别为：
- newFixedThreadPool ：会创建一种线程数量固定的线程池，它只有核心线程，并且这些核心线程没有超时机制，另外任务队列的大小也是没有限制的。
```
public static ExecutorService newFixedThreadPool(int nThreads) {
        return new ThreadPoolExecutor(nThreads, nThreads,
                                      0L, TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<Runnable>());
    }
```

- newCachedThreadPool ：会创建一中线程数量不定的线程池，它只有非核心线程， 并且最大线程数为 Integer.MAX_VALUE , 该线程池中的线程都有超时机制 60秒， 另外 SynchronousQueue 是一个非常特殊的队列，它可以简单理解为一个无法存储元素的队列，这就导致任何任务都会立即执行。这类线程池适合执行大量的耗时较少的任务。
```
public static ExecutorService newCachedThreadPool() {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                      60L, TimeUnit.SECONDS,
                                      new SynchronousQueue<Runnable>());
    }
```

- newScheduledThreadPool ：主要用于执行 定时任务 和 具有固定周期的重复任务
```
	public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        return new ScheduledThreadPoolExecutor(corePoolSize);
    }
    
    public ScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize, Integer.MAX_VALUE,
              DEFAULT_KEEPALIVE_MILLIS, MILLISECONDS,
              new DelayedWorkQueue());
    }
```

- newSingleThreadExecutor ：这类线程池内部只有一个核心线程，它确保所有的任务都在同一个线程中按顺序去执行。
```
public static ExecutorService newSingleThreadExecutor() {
        return new FinalizableDelegatedExecutorService
            (new ThreadPoolExecutor(1, 1,
                                    0L, TimeUnit.MILLISECONDS,
                                    new LinkedBlockingQueue<Runnable>()));
    }
```


可以看到这几种线程池的本质就是 通过不同的参数初始化一个 ThreadPoolExecutor 对象。

#####  ThreadPoolExecutor 参数解释

```
public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             threadFactory, defaultHandler);
    }
```

- **corePoolSize ：** 线程池的核心线程数，默认情况下，核心线程会在线程池中一直存活，即使它们处于闲置状态。如果将 ThreadPoolExecutor 的 allowCoreThreadTimeOut 属性设置为 true，那么闲置的核心线程在等待新任务到来时会有超时策略，超过 keepAliveTime 所指定的时长后，核心线程也会被终止。
- **maximumPoolSize ：** 线程池所能容纳的最大线程数，当活动线程数达到这个数值后，后续的新任务会被阻塞。
- **keepAliveTime ：** 非核心线程闲置时的超时时长，超过这个时长，非核心线程会被回收。allowCoreThreadTimeOut 属性设置为 true 时，keepAliveTime 也会作用于核心线程。
- **unit ：** keepAliveTime 参数的时间单位。
- **workQueue ：** 线程池中的任务队列，通过线程池的 execute 方法提交的 Runnable 对象会存储在这里。
- **threadFactory ：** 线程工厂，为线程池提供创建新线程的功能。


**AsyncTask 中的线程池 THREAD_POOL_EXECUTOR**

```
	private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    // We want at least 2 threads and at most 4 threads in the core pool,
    // preferring to have 1 less than the CPU count to avoid saturating
    // the CPU with background work
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 30;
    
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);

    public static final Executor THREAD_POOL_EXECUTOR;

    static {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                sPoolWorkQueue, sThreadFactory);
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }
```

从上面的代码可知，THREAD_POOL_EXECUTOR 配置后的规格如下：

- 核心线程数 最小为 2， 最大为 4
- 最大线程数为 CPU 核心数的 2 倍 + 1
- 超时时间为 30s ，且同样作用于 核心线程
- 任务队列的容量为 128


**参考**
Android 开发艺术探索