## Android 消息机制

从开发的角度来讲，Handler 是 Android 消息机制的上层接口。因此我们主要讨论的是 Handler 的运行机制。

那么首先回答个问题，为什么要有 Handler 机制？

### 0. 为什么要有 Handler 机制？

回答这个问题，首先我们得知道 Handler 有什么作用。

**作用：** Handler 的主要作用是将一个 **任务** 切换到 **Handler 所在的线程中**去执行。

而 Android 规定访问 UI 这个 **任务** 只能在主线程中进行，如果在子线程中访问 UI，就会抛出异常。每次操作 UI 时会进行验证，这个验证是通过 ViewRootImpl 类里面的 checkThread 方法 完成的。
```
void checkThread() {
        if (mThread != Thread.currentThread()) {
            throw new CalledFromWrongThreadException(
                    "Only the original thread that created a view hierarchy can touch its views.");
        }
    }
```

只能在主线程就只能在主线程访问呗，那为什么还要这个 Handler 呢，因为 Android 又建议在主线程中不能进行耗时操作，否则会导致 ANR 。那考虑这样一种情况，比如我们要从服务端拉取一些数据并将其显示在 UI 上，这个必须在子线程中进行拉取动作，拉取完毕后又不能在子线程中访问 UI，那我该如何将访问 UI 的工作切换到主线程中呢？对，就是 Handler。

因此系统之所以提供 Handler，主要原因就是为了 **解决在子线程中无法访问 UI 的矛盾。**

**Q 为什么不允许在子线程中访问 UI？**

因为 Android 的 UI 控件**不是线程安全的**，如果在 **多线程中并发访问** 可能会导致 UI 控件处于不可预期的状态。

**Q 那为什么不对 UI 控件的访问加上 锁机制呢？**

- 首先加锁会让 UI 访问的逻辑变得复杂
- 其次锁机制会降低 UI 访问的效率，因为锁机制会阻塞某些线程的执行。


### 1. Looper、MessageQueue、Message、Handler

消息机制的模型主要由以上四个类组成：

- **Message：**消息 分为 **硬件产生的消息（如按钮、触摸等）** 和 **软件生成的消息**。
- **MessageQueue：**消息队列 ，内部采用 **单链表** 的数据结构来存储 **消息列表**。
- **Handler：**消息处理者，主要用于向 **消息队列 发送**各种消息事件 和 **处理**相应消息事件。
- **Looper：**消息循环，不断**循环**执行（Looper.loop），用于从 **消息队列中取出 消息**，并按**分发机制**将取出的消息分发给 **消息处理者。**


### 2. 示意图

下图完整类图取自 http://gityuan.com/2015/12/26/handler-message-framework/

![消息机制main](img\消息机制main.jpg)

下图为简化版本：
![消息机制](img\消息机制.png)

针对简化版做个解释：

- 1、整个消息机制从 Looper 开始。我们都知道使用Handler 时，如果没有 Looper 会抛出异常, 这个在源码中很清楚。所以整个消息机制这个大机器启动的源头就是 Looper， 通过调用 Looper.prepare() 我们会 new 一个 Looper 对象，并存放在 ThreadLocal 里，这个 ThreadLocal 稍后解释。而在 Looper 的构造函数中，会 new MessageQueue 对象。

- 2、调用 Looper.loop() 启动整个消息机制。 在调用 loop 方法后， Looper 就开始了自己的 无限循环之路， 会一直从 MessageQueue 中取 Message， 这个操作对应的代码是 loop 方法里的 queue.next()， MessageQueue 中的 next 也是一个 无限循环，如果 MessageQueue 中没有消息， 那么 next 方法就会阻塞在这里，相应的 Looper 也会阻塞。 当有新消息到来时， 会唤醒他们。 至此，整个消息机制已经运转起来了，就等 消息 发过来了。

- 3、Handler 发送消息。 首先我们得 new 一个 handler 才能发送消息，在我们 new handler 的时候，会将当前线程的 Looper 取出来，同时 得到 Looper 里的 MessageQueue。 有了消息队列，我们就能往队列里插入数据了。 handler 的消息发送最终都是调用的 MessageQueue.enqueueMessage() 方法。 这样我们就把一个 Message 发送到了 MessageQueue 里。噢，对了，Message 是自己构建的，这个就不说了。

- 4、此时 消息队列里有了 Message，那么 MessageQueue 的next 就会把刚刚那个 消息返回给Looper， Looper 收到 Message 后， 会开始 消息的分发，就是调用 Message.target.dispatchMessage(msg)， 这个 Message 就是刚刚发送的 Message，那 target 就是 Message 里持有的 Handler 对象。因此 这个消息的处理 又回到了 Handler 这里。 那么 Message 是什么时候持有的 Handler 对象呢，没错，就是在 Handler 发送消息时，即调用 enqueueMessage 的时候。这个方法内部 第一行代码就是 `msg.target = this;`  ，这样就把 Handler 赋给了 Message 的 target 变量。


上述是整个 消息机制的 大致流程，嗯，这么长估计没人看，我自己都不想看。
看过这么一大段后，对于 Handler 的主要作用 想必还是一头雾水。 还记得 Handler 的主要作用吗？
Handler主要作用：将一个任务切换到 Handler 所在的线程中去执行。

了解流程后，我知道了一个 消息 怎么发给消息队列， 然后 Handler 会自己处理这个消息。但是这个线程是怎么切换的，完全不知道。

上述简化图中有个灰色的块块， **ThreadLocal**。 对，它就是关键。

### 3. ThreadLocal 工作原理

ThreadLocal 是一个线程内部的数据存储类。 还是自己去找定义看吧，这里就不写了，我们还是关心它有什么用。

它的主要作用就是：**可以在不同的线程中维护一套数据的副本并且彼此互不干扰。**

这又是什么意思，别慌，这里有个例子可以方便理解。

```
public class ThreadLocalSample {

    private ThreadLocal<Boolean> mThreadLocal = new ThreadLocal<>();

    public void test() {
        //1、在主线程中设置 mThreadLocal 的值为 true
        mThreadLocal.set(true);
        System.out.println(Thread.currentThread().getName() + " : mThreadLocal = " + mThreadLocal.get());

        new Thread("Thread#1") {
            @Override
            public void run() {
                //1、在子线程1中设置 mThreadLocal 的值为 false
                mThreadLocal.set(false);
                System.out.println(Thread.currentThread().getName() + " : mThreadLocal = " + mThreadLocal.get());
            }
        }.start();

        new Thread("Thread#2") {
            @Override
            public void run() {
                //1、在子线程2中 不设置 mThreadLocal，那么get得到的值应该为 null
                System.out.println(Thread.currentThread().getName() + " : mThreadLocal = " + mThreadLocal.get());
            }
        }.start();

    }

    public static void main(String[] args) {
        new ThreadLocalSample().test();
    }

}
```
这段代码执行的结果如下：

![code](img\threadlocal_code.png)

从结果可知，虽然在不同的线程访问的是同一个 ThreadLocal 对象，但是他们通过 ThreadLocal 获取到的值却是不一样的。

这是为什么呢？

##### 3.1 ThreadLocal.Values

ThreadLoacal 内部有个 静态内部类 Values，Values 内部维护的是一个 Object [ ] ，当我们通过 ThreadLoacal 进行 set() 方法调用时，实际是在 Values.put 。 当然不同版本的api，实现不一样，比如最新版本把Values改为了ThreadLocalMap，并且内部维护的是 Entry [ ] 数组。但是原理都一样，这里就 Values 分析简单点。

```
/**
     * Sets the value of this variable for the current thread. If set to
     * {@code null}, the value will be set to null and the underlying entry will
     * still be present.
     *
     * @param value the new value of the variable for the caller thread.
     */
    public void set(T value) {
        Thread currentThread = Thread.currentThread();
        Values values = values(currentThread);
        if (values == null) {
            values = initializeValues(currentThread);
        }
        values.put(this, value);
    }
```

可以看到，当我们调用 set 方法时， 会先取得当前的 Thread， 然后把 值 put 进去。 那么中间那句 `Values values = values(currentThread);` 是怎么回事呢。 看 Thread 源码。

```
	/**
     * Normal thread local values.
     */
    ThreadLocal.Values localValues;
```

你可以看到 Thread 的成员变量里持有 ThreadLocal.Values 。 所以当我们 set 时，会先从当前线程那里获取到 Values 对象， 也就是说我们实际是在给 每个线程的 Values 赋值。那么 values(currentThread) 做了什么呢。

```
	/**
     * Gets Values instance for this thread and variable type.
     */
    Values values(Thread current) {
        return current.localValues;
    }
```
很简单，就是返回 线程的 localValues 变量。 那么当我们第一次 set 时， 这个 Values 肯定为空， 那么就会调用 `values = initializeValues(currentThread);` 来进行初始化。

那么 ThreadLocal 类里的 initializeValues 又做了什么呢。

```
	/**
     * Creates Values instance for this thread and variable type.
     */
    Values initializeValues(Thread current) {
        return current.localValues = new Values();
    }
```
没错，直接 new Values（），并且把它赋给 Thread 类的 localValues 变量。 这样当前线程就拥有了 **ThreadLocal.Values** ，当下次set时，就会从这个线程中取出这个 Values 并对它进行赋值。 每个线程的 Values 是不一样的。 那 get 就不用说了，也是从当前线程中取出这个 Values ，然后获取相应的值，具体可自行查看源码。

到这里 ThreadLocal 就分析的差不多了，想必有了个大概印象，针对上述例子给个图理解。

![ThreadLocalValues](img\ThreadLocalValues.png)


### 4. Looper 中的ThreadLocal

分析过 ThreadLocal 后，看 Looper 就不一样了。
再来看一看 Looper.prepare() 方法。
```
	private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(quitAllowed));
    }
```
sThreadLocal 就是 ThreadLocal 的引用，如下
```
    static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>();
```

这段我们说过，会new 一个 looper， 同时会把这个 looper 赋值给 ThreadLocal，我们知道 ThreadLocal 调用 set 方法的时候，会先获取当前线程，然后调用 Values.put ， 那么这个时候，我们就把 主线程 与 Looper 绑定在一起了。

Handler 的构造方法如下：
```
	public Handler(Callback callback, boolean async) {
        if (FIND_POTENTIAL_LEAKS) {
            final Class<? extends Handler> klass = getClass();
            if ((klass.isAnonymousClass() || klass.isMemberClass() || klass.isLocalClass()) &&
                    (klass.getModifiers() & Modifier.STATIC) == 0) {
                Log.w(TAG, "The following Handler class should be static or leaks might occur: " +
                    klass.getCanonicalName());
            }
        }

        mLooper = Looper.myLooper();
        if (mLooper == null) {
            throw new RuntimeException(
                "Can't create handler inside thread that has not called Looper.prepare()");
        }
        mQueue = mLooper.mQueue;
        mCallback = callback;
        mAsynchronous = async;
    }
```
我们可以看到首先会获取 Looper 对象， 而 Looper.myLooper() 的实现很简单，就一句话 `return sThreadLocal.get();`   这样，如果你是在主线程中 new 的 handler， 那么你也就会在 主线程中 处理消息了。

![Handler work](img\Handler工作过程.png)

### 5. Handler 中的dispatchMessage 消息分发

这个方法比较简单，看源码就能看懂。

```
	/**
     * Handle system messages here.
     */
    public void dispatchMessage(Message msg) {
        if (msg.callback != null) {
            handleCallback(msg);
        } else {
            if (mCallback != null) {
                if (mCallback.handleMessage(msg)) {
                    return;
                }
            }
            handleMessage(msg);
        }
    }
```

- Message 里的 callback 是个 Runnable， 如果不为空的话，就会调用 handleCallback ， 这里面也就一句话 `message.callback.run();`
- mCallback 是 Handler类 的一个内部接口
```
	public interface Callback {
        public boolean handleMessage(Message msg);
    }
```

- handleMessage， 当我们自己 new handle 时会重写这个方法，用来处理 message， 这个方法在 Handler 里是空实现.
```
	/**
     * Subclasses must implement this to receive messages.
     */
    public void handleMessage(Message msg) {
    }
```