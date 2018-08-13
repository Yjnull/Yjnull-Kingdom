## Binder 机制再探

`Read The Fucking Source Code`


### 1、ProcessState
Process::self 到底干了什么。

- 打开 dev/binder 设备，这就相当于与内核的 Binder 驱动有了交互的通道。
- 对返回的 fd 使用 mmap ，这样 Binder 驱动就会分配一块内存来接收数据，
- 由于 ProcessState 的唯一性 ， 因此一个进程只打开设备一次。


### 2、defaultServiceManager
拿到 ServiceManager 用于注册自己的服务 。
defaultServiceManager 函数的实现在 IServiceManager.cpp 中完成。它会返回一个 IServiceManager 对象 。通过这个对象，我们可以神奇地与另一个进程 ServiceManager 进行交互。

IServiceManager.defaultServiceManager() -> ProcessState.getContextObject(NULL) 该函数返回 IBinder -> ProcessState.getStrongProxyForHandle(0) ，这个方法里面会创建一个 BpBinder 。

```
BpBinder :: BpBindser(int32_t handle) 
	: mHandle(handle)
    , mAlive(1)
    , mObitsSent(0)
    , mObituaries(NULL)
    {
    	//另一个重要对象是 IPCThreadState
        IPCThreadState::self()->incWeakHandle(handle);
    }
```


### 3、Binder 通信模型
A(Client) 与 B(Server) 打电话的场景，还需要 通讯录(ServiceManager) 和 基站(Binder)

其中最重要的工作就是 基站， 即 Binder 驱动 ，驱动是整个通信过程的核心，因此IPC 的秘密全在驱动里。

### 4、Binder 机制 原理

假设Client进程想要调用Server进程的object对象的一个方法add 

Client 向 SM 查询，我需要一个名字叫 zhangsan 的进程里的 object 对象。
这时候关键来了：进程之间通信的数据都会经过运行在内核空间里面的Binder驱动，Binder驱动在数据流过的时候做了一点手脚，它会返回一个跟 object 一模一样的代理对象 objectProxy 给 Client ，objectProxy 的 add 只是一个傀儡，它唯一做的事情就是把参数包装然后交给驱动。










##### 具体的分析可查看如下文章
[Gityuan Binder 系列](http://gityuan.com/2015/10/31/binder-prepare/)
[Binder 学习指南](http://weishu.me/2016/01/12/binder-index-for-newer/)
[Android 接口定义语言 (AIDL)](https://developer.android.com/guide/components/aidl?hl=zh-cn)
[老罗的 Android 之旅](https://blog.csdn.net/luoshengyang/article/details/6618363)
[Android Bander设计与实现 - 设计篇](https://blog.csdn.net/universus/article/details/6211589)
[相见恨晚 Binder 机制](https://blog.csdn.net/freekiteyu/article/details/70082302)
http://qiangbo.space/2017-01-15/AndroidAnatomy_Binder_Driver/


