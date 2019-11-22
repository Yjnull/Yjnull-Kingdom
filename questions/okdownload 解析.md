# OkDownload 解析

> 可靠，灵活，高性能以及强大的下载引擎。



## Debug OkDownload

使用 `okcat -y=okcat-okdownload -c` 来打印日志。 `-c` 表示清除旧的日志

[okcat](https://github.com/Jacksgong/okcat)



## 简单使用

#### 1. 启动和取消任务

**1. Start a task** 开启一个任务

```java
task = new DownloadTask
                .Builder(DemoUtil.URL, DemoUtil.getParentFile(this))
                .setFilename(filename)
                .setPassIfAlreadyCompleted(false)
                .setMinIntervalMillisCallbackProcess(80)
                .build();

task.enqueue(listener);
//task.execute(listener); 同步启动
task.cancel();
```

**2. Start tasks** 开启一堆任务

```java
DownloadTask.enqueue(tasks, listener);
DownloadTask.cancel(tasks);
```

## 源码分析

### 1. DownloadTask 门面
![DownloadTask](http://www.plantuml.com/plantuml/png/TT11IiKm40RWVKunIrsyTm8N3xI5GeMYxSALJ9Df3QQ9J4PTrDxTH2hQGfUPR_Y7_9SeoF8-EMMz40-e2Mx3z8QClIE5VHH6BQ0TnWYL8Izsi6HQZAymAu0v1OZL2pmAMpz2ST-qJy_TG_tVIkjebRAg2vNjDtg1T1b6aifSNMzXZKpMiTjZpipKSFYmXao3yf9dXtl8v2M_ZizazJZbZKNjFdlpTJh1NVZqBk3FHFtrbF1ytgAwa3Ufrsy0)

下载这件事情，抽象成一个任务，一个 url 对应一个下载任务
- 你可以通过 `Builder` 自定义你的下载任务
- 也可以通过 `setTag` 或者 `addTag` 存储一些临时数据
- `StatusUtil` 获取这个任务的当前状态 `Status`
- `OkDownload#breakpointStore()` 获取这个任务的断点信息

#### 1.1 DownloadTask.Builder
很明显构建下载任务是通过 Builder 模式实现。那么构建一个 DownloadTask 需要些什么参数呢，如下所示：

```java
public static class Builder {
    // 该任务下载的 url
    @NonNull final String url;
    // 该任务的文件路径
    @NonNull final Uri uri;
    // 该任务的请求头
    private volatile Map<String, List<String>> headerMapFields;
		// 任务优先级，值越大优先级越高
		private int priority;
    // read 缓冲区大小，默认 4096byte
    public static final int DEFAULT_READ_BUFFER_SIZE = 4096/* byte **/;
    private int readBufferSize = DEFAULT_READ_BUFFER_SIZE;
    // flush 刷新缓冲区数据流大小，默认 16384byte
    public static final int DEFAULT_FLUSH_BUFFER_SIZE = 16384/* byte **/;
    private int flushBufferSize = DEFAULT_FLUSH_BUFFER_SIZE;
    // 确保同步到物理文件系统，默认同步缓冲区大小 65536byte
    public static final int DEFAULT_SYNC_BUFFER_SIZE = 65536/* byte **/;
    private int syncBufferSize = DEFAULT_SYNC_BUFFER_SIZE;
    // 默认每隔 2s 同步一次
    public static final int DEFAULT_SYNC_BUFFER_INTERVAL_MILLIS = 2000/* millis **/;
    private int syncBufferIntervalMillis = DEFAULT_SYNC_BUFFER_INTERVAL_MILLIS;

    // 是否需要自动回调到 UI 线程，默认 true
    public static final boolean DEFAULT_AUTO_CALLBACK_TO_UI_THREAD = true;
    private boolean autoCallbackToUIThread = DEFAULT_AUTO_CALLBACK_TO_UI_THREAD;
    // progress 进度回调最小间隔。默认 3s
    public static final int DEFAULT_MIN_INTERVAL_MILLIS_CALLBACK_PROCESS = 3000/* millis **/;
    private int minIntervalMillisCallbackProcess = DEFAULT_MIN_INTERVAL_MILLIS_CALLBACK_PROCESS;

    // 文件名
    private String filename;
    // 该字段为 true: 如果该任务已经完成过，那么直接回调已完成，而不是重新开始下载
    // 该字段为 false: 如果该任务已经完成过，重新开始下载
    public static final boolean DEFAULT_PASS_IF_ALREADY_COMPLETED = true;    
    private boolean passIfAlreadyCompleted = DEFAULT_PASS_IF_ALREADY_COMPLETED;
    // 是否需要 wifi 才能下载 
    public static final boolean DEFAULT_IS_WIFI_REQUIRED = false;
    private boolean isWifiRequired = DEFAULT_IS_WIFI_REQUIRED;
    // 文件名是否必须来自 response header or url path
    private Boolean isFilenameFromResponse;
    // 设置为此任务建立的连接数。如果这个任务在过去等待的时候已经分块了，那么该值的 set 方法不会生效
    private Integer connectionCount;
    // 在通过 trial-connection 拿到资源的 length 后，是否需要为 file 预分配 length
    private Boolean isPreAllocateLength;
```

#### 1.2 DownloadTask
Builder 分析完后，我们大概知道 DownloadTask 都携带些什么属性。接下来分析 DownloadTask 内的一些方法。
其实也没啥，主要就三点吧
- 一些 get 方法
- 启动取消任务的方法，execute、enqueue、cancel 等
- 构造函数

**1.2.1 一些 get 方法**
其实就是由 Builder 进行 set，自己提供 get。

**1.2.2 启动取消任务**
这里实际是将任务丢给 OkDownload 的 downloadDispatcher 去处理的。

**1.2.3 构造函数**
下载任务的 id 是很重要的，可用于断点存储，而 id 是在构造函数中生成的。
构造函数开始先把 Builder 传过来的参数进行赋值。
然后初始化 directoryFile、filenameHolder、id。
其中有一大段是在构造目录文件，主要通过各种判断来初始化 directoryFile。
最后通过 `OkDownload.with().breakpointStore().findOrCreateId(this);` 来创建 id

**总结：** 对于 DownloadTask 简单来说，就是一个封装好的可配置的下载任务，用户将下载任务扔给 OkDownload 即可开始下载。所以接下来开始分析 OkDownload。

### 2. OkDownload
OkDownload 这个类里持有了整个下载框架中最核心的一些对象，例如下载调度器、下载策略、下载监视器等。
我们还是从 Builder 来粗略的看看，另外既然是 Builder，意味自己可以扩展这里面所有核心内容

```java
public static class Builder {
    // 下载调度器
    private DownloadDispatcher downloadDispatcher;
    // 回调调度器
    private CallbackDispatcher callbackDispatcher;
    // 下载存储区域，猜测记录断点下载信息
    private DownloadStore downloadStore;
    // 下载连接工厂
    private DownloadConnection.Factory connectionFactory;
    // 文件处理策略
    private ProcessFileStrategy processFileStrategy;
    // 下载策略
    private DownloadStrategy downloadStrategy;
    // 输出流工厂
    private DownloadOutputStream.Factory outputStreamFactory;
    // 监视器
    private DownloadMonitor monitor;
    // 上下文，这里持有的是 ContentProvider 的 Context.getApplicationContext()
    private final Context context;
```

从类名可以大概看出来有什么作用。

其实整个类没啥，就是持有这些引用而已，同时是个单例，所有的逻辑都从这个类开始。

如果不进行自定义配置，那么上述都是默认对象，如下所示：

```java
// 以下不是真实代码，只是简单例子
downloadDispatcher = new DownloadDisptcher();
callbackDispatcher = new CallbackDispatcher();
/* downloadStore 实际指向 com.liulishuo.okdownload.core.breakpoint.BreakpointStoreOnSQLite 或者 BreakpointStoreOnCache*/
downloadStore = Util.createDefaultDatabase(context);
/* connectionFactory 实际指向 com.liulishuo.okdownload.core.connection.DownloadOkHttp3Connection$Factory 或者 DownloadUrlConnection.Factory()*/
connectionFactory = Util.createDefaultConnectionFactory();
outputStreamFactory = new DownloadUriOutputStream.Factory();
processFileStrategy = new ProcessFileStrategy();
downloadStrategy = new DownloadStrategy();
```

上述分析可知，downloadStore 和 connectionFactory 比较特殊，实际他们是为了做兼容，downloadStore 既可以是原生 sqlite 存储也可以是普通的内存缓存做存储；connectionFactory 则默认采用 OkHttp 做为底层实现，其次才是 URLConnection。

最后在构造函数里面，downloadStore 则赋值给了 breakpointStore，用于断点下载。而 downloadDisptcher 则自己设置了 DownloadStore，采用的是 Remit 数据库。

构造函数如下：

```java
OkDownload(Context context, DownloadDispatcher downloadDispatcher,
           CallbackDispatcher callbackDispatcher, DownloadStore store,
           DownloadConnection.Factory connectionFactory,
           DownloadOutputStream.Factory outputStreamFactory,
           ProcessFileStrategy processFileStrategy, DownloadStrategy downloadStrategy) {
        this.context = context;
        this.downloadDispatcher = downloadDispatcher;
        this.callbackDispatcher = callbackDispatcher;
        this.breakpointStore = store;
        this.connectionFactory = connectionFactory;
        this.outputStreamFactory = outputStreamFactory;
        this.processFileStrategy = processFileStrategy;
        this.downloadStrategy = downloadStrategy;

        this.downloadDispatcher.setDownloadStore(Util.createRemitDatabase(store));
    }
```

接下来我们分析 OkDownload 里的 breakpointStore ，因为前面 DownloadTask 构造函数里最后一行代码是 

`OkDownload.with().breakpointStore().findOrCreateId(this);` 通过 breakpointStore 来创建 id。

#### 2.1 BreakpointStore

![BreakpointStore](http://www.plantuml.com/plantuml/png/bLDHRjim3FttAVHf0-aDzf7CRO40MmlClO0YqOvWYJGekgknxUwJYZfAiwNO_468V2BqtYEzZQ9Pvk0hXmBSQmFgCuCU9qg5JeX1_QXAPOyzgG6aJcVbR7C9cPGAxD3kCG9BlNqWV9YzLxVq7Jrf-wJZg2I59h-eUeVsaTlNhhtzyFg1o8D6DKzM2vIh5mmExzAcUYPd5KEWPpYHxeGT3bd7DuWJOOHxvm5rWFeDcjd_eIrptI794JZdzqoXfJ1fXWzuRohrNzyrMMv0Oh351XSVqIzV8H0lhJP7gC-nhEVMiImdpR_AX4yir_c-UaJOBMYUABV69Gg4RZO6OgolSafXawAeWoSpxj72op_fzOH3wilB4k4Bu237PYMHdUj4SrcnC-0ExLjvKvnAnLRF4PJ9CGEfgWyUrDsBWKaSeOBrrFnbcNUz0ETHUX1O7oXk1irZyVCQsgQCpzo-_8i-_RovUMzyLMq1xHpyRm00)

可以看到 BreakpointStore 定义了 findOrCreateId 操作，通过前面 OkDownload 的分析，我们知道 breakpointStore 的具体实现类有三个

- BreakpointStoreOnSQLite：内部是通过 sqlite 实现存储，同时持有内存缓存 BreakpointStoreOnCache
- BreakpointStoreOnCache：内存缓存
- RemitStoreOnSQLite：包装了一层，里面持有 RemitSyncToDBHelper 以及 BreakpointStoreOnSQLite

KeyToIdMap 以 `url + uri + filename` 为 key

#### 2.2 DownloadDispatcher

```java
// 自定义线程池
private @Nullable volatile ExecutorService executorService;
// 准备好的异步调用
private final List<DownloadCall> readyAsyncCalls;
// 运行中的异步调用
private final List<DownloadCall> runningAsyncCalls;
// 运行中的同步调用
private final List<DownloadCall> runningSyncCalls;
// 完成中的调用
private final List<DownloadCall> finishingCalls;
// for the case of tasks has been cancelled but didn't remove from runningAsyncCalls list yet.
private final AtomicInteger flyingCanceledAsyncCallCount = new AtomicInteger();
// 为了避免当 enqueue/cancel 操作时，去处理调用
private final AtomicInteger skipProceedCallCount = new AtomicInteger();
// 最大并行数目
int maxParallelRunningCount = 5;
// 具体实现是 RemitStoreOnSQLite
private DownloadStore store;
```
其中 `DownloadCall` 是一个封装好的可以被命名的 `Runnable`，并且实现了 Comparable 接口，比较的是任务的优先级。**也就是说这个 DownloadCall 是具体执行任务的地方了。**
不过我们暂时不分析具体怎么执行的，先分析怎么调度的。

首先调度器内部有一个自定义的线程池，该线程池可分配 Integer.MAX_VALUE 个线程，存活时间 60s，没有核心线程，有一个 ThreadFactory，该 ThreadFactory 生成出的线程名字都叫 `OkDownload Download`。
当我们调用 `enqueue(task)` 时（注意此处是单个任务，多个任务是 tasks），首先会增加 skipProceedCallCount 的值，这里应该是一种类似加锁的处理机制，==防止...==
然后调用 `enqueueLocked(task)`，接着会减少 skipProceedCallCount 的值。当然 skipProceedCallCount 是 AtomicInteger 类型，所以对它进行加减都是原子操作，是线程安全的。
接着着重看看 `enqueueLocked(task)` 方法：

```java
private synchronized void enqueueLocked(DownloadTask task) {
    Util.d(TAG, "enqueueLocked for single task: " + task);
    // 检查该任务是否已经完成
    if (inspectCompleted(task)) return;
    // 检查该任务是否有冲突
    if (inspectForConflict(task)) return;
    final int originReadyAsyncCallSize = readyAsyncCalls.size();
    // 任务入队，忽略优先级
    enqueueIgnorePriority(task);
    // 重排
    if (originReadyAsyncCallSize != readyAsyncCalls.size()) {
        Collections.sort(readyAsyncCalls);
    }
}
```

- 如何判断一个任务是否已经完成
任务如果设置 passIfAlreadyCompleted 为 false，那就代表即使任务已经完成也需要重新下载。
那么当 StatusUtils.isCompleted(task) 为 true，就代表一个任务已经完成了。而这个时候如果你的  passIfAlreadyCompleted 设置为 true，就代表不出意外的情况下（可能文件名被修改了）不需要重新下载。
如果任务确实已经完成了，且不需要重新下载，那么在 inspectCompleted 方法中会通过 downloadStrategy 给该任务设置一个下载完成的断点信息，然后通过 callbackDispatcher 回调
`taskEnd(task, EndCause.COMPLETED, null)` 方法。
那么 StatusUtils.isCompleted(task) 的判断依据是什么呢？
  1. 拿到 BreakpointStore ，然后根据 task.getId() 在 store 里拿到相应的 BreakpointInfo。然后拿到 task 的 filename、parentFile、targetFile。
  2. 开始判断。如果该任务的断点信息 info 不为空，可以判断 4 个情况。
    2.1 如果任务没有分块且 totalLenght <= 0，会返回 `UNKNOWN` 状态。
    2.2 如果 targetFile 不为空，且等于 info.getFile() 且 targetFile 存在 且 info 的 totalOffset 等于 totalLength，会返回 `COMPLETED` 状态。
    2.3 filename 为空，且 info 的 file 不存在，返回 `IDLE空闲`
    2.4 同 2.2 情况，但是 totalOffset 不等于 totalLength, `IDLE`
  3. 以上是有断点信息的判断，接下来是没有断点信息的判断。
    3.1 如果 store 只是 MemoryCache 或者 `store.isFileDirty(task.getId())` 会返回 `UNKNOWN`
    3.2 如果 targetFile 不为空且存在，会返回 `COMPLETED`
    3.3 通过 store.getResponseFilename(task.getUrl()) 拿到 filename，然后看 filename 所代表的文件存不存在，存在就返回 `COMPLETED`
  4. 都不符合以上条件返回 `UNKNOWN`
  
- 如何判断一个任务冲突
  1. 如果该任务已经存在于 readyAsyncCalls、runningAsyncCalls、runningSyncCalls 这三个列表中任何一个，且该任务未完成，那么会通过 callbackDispatcher 回调 `taskEnd(task, EndCause.SAME_TASK_BUSY, null)` 结束。当然如果该任务刚好已经完成了，那么会将该任务添加进 finishingCalls 列表中，并继续这个新的下载任务。
  2. 如果该任务的下载文件与上述三个列表中的任务的下载文件一致，也会回调 `taskEnd(task, EndCause.FILE_BUSY, null);` 原因是文件冲突

- 进入 `enqueueIgnorePriority(task)` 方法
  1. 首先会创建一个 DownloadCall，然后判断 runningAsyncSize() 的值是否小于 maxParallelRunningCount，也就是是否小于设定的并发数，如果小于则将该 call 添加进 runningAsyncCalls 中，并交给线程池去执行。如果大于设定的并发数，则会将 call 添加进 readyAsyncCalls。

- 最后会判断 readyAsyncCalls.size() 是否跟原来的不一样，如果不一样则会进行重新排序。DownloadCall 是实现了 Comparable 接口的，是根据任务优先级进行排序的。

自此单个任务的调度差不多快分析完了，还剩最后一点，那就是 readyAsyncCalls 里的任务如何被执行。在 DownloadCall 里任务执行完会调用 finished() 方法，这里面又会继续调用
`OkDownload.with().downloadDispatcher().finish(this);` ，从而将逻辑交回给了DownloadDispatcher。接着看看下载调度器里的 finish 方法。

```java
public synchronized void finish(DownloadCall call) {
    // 改调用是否执行完毕
    final boolean asyncExecuted = call.asyncExecuted;
    final Collection<DownloadCall> calls;
    if (finishingCalls.contains(call)) {
        calls = finishingCalls;
    } else if (asyncExecuted) {
        calls = runningAsyncCalls;
    } else {
        calls = runningSyncCalls;
    }
    // 将相应任务从集合中删除，如果删除失败会抛出异常
    if (!calls.remove(call)) throw new AssertionError("Call wasn't in-flight!");
    if (asyncExecuted && call.isCanceled()) flyingCanceledAsyncCallCount.decrementAndGet();
    // 如果已经执行完，则处理接下来的调用
    if (asyncExecuted) processCalls();
}
```
在 `processCalss()` 里面会遍历 readyAsyncCalls 里的 DownloadCall。当然前提是 `runningAsyncSize() >= maxParallelRunningCount` 或者 readyAsyncCalls 里还有 DownloadCall。

这样整个任务的调度就大体结束了。

#### 2.3 DownloadCall 重要
0. 开始之前检查基本参数，这里就是检查 url 是否合法
1. 如果 BreakpointStore 里不存在该任务的 断点信息 BreakpointInfo，就创建一个新的，并插入到数据库里。接着创建 DownloadCache，这个 DownloadCache 初步判断里面有各种状态值，以及一个 MultiPointOutputStream
2. 创建 BreakpointRemoteCheck，开始远程检查。这里实际是在执行连接试探 ConnectTrial，如果能分块、断点下载会自动添加 header If-Match、Range。这里会回调一个 **listener.connectTrialStart**。然后去执行请求 `connection.execute()`，接下来会构建一些列相关的值，redirectLocation、acceptRange、instanceLength、etag、filename 等等。构建完后会回调 **listener.connectTrialEnd**。连接试探完毕后，会回到 BreakpointRemoteCheck，进行一些值的构建，主要是拿到 chunked、etag、instanceLength、acceptRange 等值。
3. 确认文件路径后，等待文件锁释放




