## Surface 深入理解

[Android 显示系统](https://www.cnblogs.com/blogs-of-lxl/p/11272756.html)

### 1. SurfaceComposerClient 分析

**SurfaceComposerClient：这个对象会和 SurfaceFlinger 交互，因为 SurfaceFlinger 派生于 SurfaceComposer。**

#### 1.1 SurfaceComposerClient 的初始化

**[SurfaceComposerClient.cpp]**

```c++
SurfaceComposerClient::SurfaceComposerClient()
{
    // getComposerService 将返回 SF 的 Binder 代理端的 BpSurfaceFlinger 对象。
    sp<ISurfaceComposer> sm(getComposerService());
    // 创建连接后，调用 _init
    _init(sm, sm->createConnection());
    if (mClient != 0) {
        Mutex:AutoLock _l(gLock);
        // 把刚刚创建的 Clinet 保存到这个 map 中
        gActiveConnections.add(mClient->asBinder(), this);
    }
}
```

这里 SurfaceFlinger 的 createConnection 做了啥，貌似是返回了一个 BClient。我们先看 _init 函数，再看 `sm->createConnection`

**[SurfaceComposerClient.cpp]**

```c++
void SurfaceComposerClient::_init(
  const sp<ISurfaceComposer>& sm, const sp<ISurfaceFlingerClient>& conn)
{
    ......
    mClient = conn; // mClient 就是 createConnection 返回的 BClient
    mControlMemory = mClient->getControlBlock();
    mSignalServer = sm;
    mControl = static_cast<SharedClient *>(mControlMemory->getBase());
}
```

可以看到 SurfaceComposerClient 在调用 SurfaceFlinger 的 createConnection 后，通过 _init 初始化了一些成员变量。

- mSignalServer：其实就是 SurfaceFlinger 在客户端的代理 BpSurfaceFlinger
- mControl：是跨进程共享的 SharedClient，是 Surface 系统的 ControlBlock 对象。（其实就是一块共享内存的具象化）
- mClient：是 SurfaceFlinger 创建的 BClient 在客户端的对应物 BpSurfaceFlingerClient

->图 5

#### 1.2 创建 Client

再来看看 SurfaceFlinger 的 createConnection 做了啥。

**[SurfaceFlinger.cpp]**

```c++
sp<ISurfaceFlingerClient> SurfaceFlinger::createConnection()
{
    Mutex::Autolock _l(mStateLock);
    uint32_t token = mTokens.acquire();
    // 创建一个 Client，Client 会创建一块共享内存，该内存由 getControlBlockMemory 返回
    sp<Client> client = new Client(token, this);
    // 把这个 Client 保存到 mClientMap 中，token 是它的标识
    status_t err = mClientMap.add(token, client);
    // 创建一个用于 IPC 的 BClient，BClient 派生于 ISurfaceFlingerClient
    // 它的作用是接收客户端的请求，然后把处理提交给 SurfaceFlinger。
    sp<BClient> bclient = new BClient(this, token, client->getControlBlockMemory());
    return bclient;
}
```

上面的 Client 会创建一块共享内存，这块内存就是 Surface 的 ControlBlock 对象了。是在 Client 的构造函数中创建的。

**[SurfaceFlinger.cpp]**

```c++
Client::Client(ClientID clientID, const sp<SurfaceFlinger>& flinger)
    : ctrlblk(0), cid(clientID), mPid(0), mBitmap(0), mFlinger(flinger)
{
    const int pgsize = getpagesize();
    // 这个操作会使 cblksize 为页的大小，目前是 4096 字节。
    const int cblksize = ((sizeof(SharedClient)+(pgsize-1))&~(pgsize-1));
    // 分配共享内存
    mCblkHeap = new MemoryHeapBase(cblksize, 0,
            "SurfaceFlinger Client control-block");
    // 拿到共享内存首地址
    ctrlblk = static_cast<SharedClient *>(mCblkHeap->getBase());
    if (ctrlblk) { // construct the shared structure in-place.
        // 在这块共享内存上 new 一个 SharedClient 对象
        new(ctrlblk) SharedClient;
    }
}
```

所以 Client 创建共享内存最终的目的是在这块内存里创建了一个 SharedClient 对象。

#### 1.3 SharedClient 定义

**SharedBufferStack.h**

```c++
class SharedClient
{
public:
    SharedClient();
    ~SharedClient();
    status_t validate(size_t token) const;
    ......
private:
    ......
    Mutex lock;
    Condition cv;
    // NUM_LAYERS_MAX 的值是 31
    SharedBufferStack surfaces[ SharedBufferStack::NUM_LAYERS_MAX ];
};
```

看起来 SharedBufferStack 是关键。

**一个 Client 最多支持 31 个显示层。每一个显示层的生产/消费步调都会由对应的 SharedBufferStack 来控制，而它内部就是用几个成员变量来控制读写位置的。**

**SharedBufferStack.h**

```c++
class SharedBufferStack {
    ......
    // these attributes are part of the conditions/updates
    // Buffer 是按块使用的，每个 Buffer 都有自己的编号，其实就是数组中的索引
    volatile int32_t head;      // server's current front buffer
    volatile int32_t available; // number of dequeue-able buffers
    volatile int32_t queued;    // number of buffers waiting for post
    volatile int32_t inUse;     // buffer currently in use by SF
    volatile status_t status;   // surface's status code
}
```

我们再来理一理 共享内存 里的事情。

- SF 的一个 Client 创建了一块共享内存 4096 字节，并且在里面分配了一个 SharedClient 对象，这个对象有 31 个 SharedBufferStack 元素，每一个 SharedBufferStack 对应一个显示层。
- 一个显示层将创建两个 Buffer，后续的 pageFlipping 就是基于这两个 Buffer 展开的。另外，每一个显示层中其数据的生产消费并不是直接使用 SharedClient 来进行控制，而是基于 SharedBufferServer 和 SharedBufferClient ，这两个结构来对该显示层的 SharedBufferStack 进行操作。

-> 图 6





### 2. SurfaceControl 分析

根据前面的分析，再看这个名字，我们可以联想到这是不是与 显示层、SharedBufferStack、SharedBufferServer、SharedBufferClient 有关。它是控制这些的？

#### 2.1 SurfaceControl 的来历

还记得它是在哪里创建出来的吗？在前面调用 relayoutWindow 的时候，我们会通过 WidowState 的 createSurfaceLocked 来创建一个 Surface（通过有参的构造函数）。而在 Surface 的 init 方法里会调用 SurfaceComposerClient 的 createSurface，得到一个 SurfaceControl。

所以来看看 SurfaceComposerClient 的 createSurface 函数。

**[SurfaceComposerClient.cpp]**

```c++
sp<SurfaceControl> SurfaceComposerClient::createSurface(
        int pid, const String8& name, DisplayID display,
        uint32_t w, uint32_t h, PixelFormat format, uint32_t flags)
{
    sp<SurfaceControl> result;
    if (mStatus == NO_ERROR) {
        ISurfaceComposerClient::surface_data_t data;
        // 调用 BClient 的 createSurface
        sp<ISurface> surface = mClient->createSurface(&data, pid, name,
                display, w, h, format, flags);
        if (surface != 0) {
            // 根据上面创建的 surface 对象创建一个 SurfaceControl 对象
            result = new SurfaceControl(this, surface, data, w, h, format, flags);
        }
    }
    return result;
}
```

SurfaceComposerClient 我们上一节刚分析过，这里的 mClient 是 SurfaceFlinger 创建的 BClient 在客户端的对应物 BpSurfaceFlingerClient。

所以我们直接看 SurfaceFlinger 的 createSurface 做了什么

**[SurfaceFlinger.cpp]**

```c++
sp<ISurface> SurfaceFlinger::createSurface(ClientID clientId, int pid,
        const String8& name, ISurfaceComposerClient::surface_data_t* params,
        DisplayID d, uint32_t w, uint32_t h, PixelFormat format,
        uint32_t flags)
{
    // 相当于 Layer 的基类吧
    sp<LayerBaseClient> layer;
    sp<LayerBaseClient::Surface> surfaceHandle;
    ......
    
    Mutex::Autolock _l(mStateLock);
    sp<Client> client = mClientsMap.valueFor(clientId)
      
    // 注意这个 id，它的值表示 Client 创建的是第几个显示层，同时也表示将使用 SharedBufferStack 数组的第 id 个元素
    int32_t id = client->generateId(pid);
    
    // 根据 flags 参数来创建不同的显示层
    sp<Layer> normalLayer;
    switch (flags & eFXSurfaceMask) {
        case eFXSurfaceNormal:
            if (UNLIKELY(flags & ePushBuffers)) {
                // 创建 PushBuffer 类型的显示层
                layer = createPushBuffersSurface(client, d, id, w, h, flags);
            } else {
                // 创建 normal 类型的显示层
                normalLayer = createNormalSurface(client, d, id, w, h, flags, format);
                layer = normalLayer;
            }
            break;
        case eFXSurfaceBlur:
            layer = createBlurSurface(client, d, id, w, h, flags);
            break;
        case eFXSurfaceDim:
            layer = createDimSurface(client, d, id, w, h, flags);
            break;
    }

    if (layer != 0) {
        layer->setName(name);
        setTransactionFlags(eTransactionNeeded);
        // 从显示层中取出一个 Surface 对象赋值给 surfaceHandle
        // 学习了后面的内容，回过头来看，这里取出的其实是 SurfaceLayer 对象
        surfaceHandle = layer->getSurface();
        if (surfaceHandle != 0) { 
            params->token = token;
            params->identity = surfaceHandle->getIdentity();
            params->width = w;
            params->height = h;
            params->format = format;
            if (normalLayer != 0) {
                Mutex::Autolock _l(mStateLock);
                mLayerMap.add(surfaceHandle->asBinder(), normalLayer);
            }
        }
    }

    return surfaceHandle;
}

// --- 承接上面的创建 Layer 部分 ---
sp<Layer> SurfaceFlinger::createNormalSurface(
        const sp<Client>& client, DisplayID display, int32_t id,
        uint32_t w, uint32_t h, uint32_t flags,
        PixelFormat& format)
{
    // initialize the surfaces
    switch (format) { // TODO: take h/w into account
    case PIXEL_FORMAT_TRANSPARENT:
    case PIXEL_FORMAT_TRANSLUCENT:
        format = PIXEL_FORMAT_RGBA_8888;
        break;
    case PIXEL_FORMAT_OPAQUE:
        format = PIXEL_FORMAT_RGB_565;
        break;
    }
    // 创建一个 Layer 类型的对象
    sp<Layer> layer = new Layer(this, display, client, id);
    // 设置 Buffer
    status_t err = layer->setBuffers(w, h, format, flags);
    if (LIKELY(err != NO_ERROR)) {
        // 初始化这个新 layer 的一些状态
        layer->initStates(w, h, flags);
        // 调用 SF 的 addLayer_l 函数把这个 Layer 加入到 Z 轴中
        addLayer_l(layer);
    }
    return layer;
}
```

- LayerBaseClient：前面提到的显示层在代码中的对应物就是这个，这个算是一个基类，不同的显示层会创建不同类型的 LayerBaseClient。
- LayerBaseClient 中有一个内部类，叫 Surface，这是一个支持 Binder 通信的类，派生于 ISurface

ok，现在已经有了一个创建 SurfaceControl 的重要参数，就是这个 ISurface。

接下来看看 new SurfaceControl 到底做了啥。

**SurfaceControl.cpp**

```c++
SurfaceControl::SurfaceControl(
        const sp<SurfaceComposerClient>& client, 
        const sp<ISurface>& surface,
        const ISurfaceComposerClient::surface_data_t& data,
        uint32_t w, uint32_t h, PixelFormat format, uint32_t flags)
    : mClient(client), mSurface(surface),
      mToken(data.token), mIdentity(data.identity),
      mWidth(data.width), mHeight(data.height), mFormat(data.format),
      mFlags(flags)
{
}
```

SurfaceControl 可以看做是一个 wrapper 类：它封装了一些函数，通过这些函数可以方便的调用 mClient 或 ISurface 提供的函数。仅此而已。

#### 2.2 Layer 分析

前面似乎都没啥重要的，无非就是初始化各种成员变量。但是 Layer 的构造函数还没分析，现在就剩它了，估计有点东西。

**[Layer.cpp]**

```c++
Layer::Layer(SurfaceFlinger* flinger, DisplayID display,
        const sp<Client>& client, int32_t i) // 这个 i 表示 SharedBufferStack 的数组索引
    :   LayerBaseClient(flinger, display, client, i), // 调用父类的构造函数
        mSecure(false),
        mNoEGLImageForSwBuffers(false),
        mNeedsBlending(true),
        mNeedsDithering(false)
{
    // 去除 FrontBuffer 的位置
    mFrontBufferIndex = lcblk->getFrontBuffer();
}

// --- 再看下 LayerBaseClient 的构造函数 ---
LayerBaseClient::LayerBaseClient(SurfaceFlinger* flinger, DisplayID display,
        const sp<Client>& client, int32_t i)
    : LayerBase(flinger, display), lcblck(NULL), client(client), mIndex(i),
      mIdentity(uint32_t(android_atomic_inc(&sIdentity)))
{
    // 创建一个 SurfaceBufferServer 对象，注意它使用了 SharedClient 对象
    // 并且传入了表示 SharedBufferStack 数组索引的 i 和一个常量 NUM_BUFFERS == 2
    lcblk = new SharedBufferServer(client->ctrlblk, i, NUM_BUFFERS, mIdentity);
}
```

SharedBufferServer 我们前面提过，消费者使用 SharedBufferServer 操作 SharedBufferStack 中的 FrontBuffer，控制数据的读取。

构造函数结束了，接下来看下 **setBuffers**。这个函数的目的其实就是创建用于 PageFlipping 的 FrontBuffer 和 BackBuffer 了。

**[Layer.cpp]**

```c++
status_t Layer::setBuffers( uint32_t w, uint32_t h,
                            PixelFormat format, uint32_t flags)
{
    // this surfaces pixel format
    PixelFormatInfo info;
    status_t err = getPixelFormatInfo(format, &info);
    if (err) return err;

    // DisplayHardware 是代表显示设备的 HAL 对象，0 代表第一块屏幕的显示设备
    // 这里将从 HAL 中取出一些和显示相关的参数信息。
    const DisplayHardware& hw(graphicPlane(0).displayHardware());
    uint32_t const maxSurfaceDims = min(
            hw.getMaxTextureSize(), hw.getMaxViewportDims());

    PixelFormatInfo displayInfo;
    getPixelFormatInfo(hw.getFormat(), &displayInfo);
    const uint32_t hwFlags = hw.getFlags();
    
    ......

    // 这里将创建两个 GraphicBuffer
    // 这两个 GraphicBuffer 就是我们前面所说的 FrontBuffer 和 BackBuffer
    for(size_t i = 0; i < NUM_BUFFERS; i++) {
        mBuffers[i] = new GraphicBuffer();
    }
    // 这里又来一个 SurfaecLayer ?
    mSurface = new SurfaceLayer(mFlinger, this);
    return NO_ERROR;
}
```

setBuffers 做了两件事

- 创建 GraphicBuffer 缓冲数组，元素个数为 2，即 FrontBuffer 和 BackBuffer
- 创建 SurfaceLayer

> GraphicBuffer 是 Android 提供的 显示内存 管理的类。

最后了，`addLayer_l` 分析。

**[SufaceFlinger.cpp]**

```c++
status_t SurfaceFlinger::addLayer_l(const sp<LayerBase>& layer)
{
    // mCurrentState 是 SurfaceFlinger 定义的一个结构，它有一个成员变量叫 layersSortedByZ
    // 其实就是一个排序数组，下面这个 add 函数把 layer 按照它在 Z 轴的位置加入到数组中。
    // mCurrentState 保存了所有显示层
    ssize_t i = mCurrentState.layersSortedByZ.add(
      layer, &LayerBase::compareCurrentStateZ);
    ......
    return (i < 0) ? status_t(i) : status_t(NO_ERROR);
}
```

-> Layer 家族图

#### 2.3 SurfaceControl 的总结

SurfaceControl 创建后得到了什么呢？

- mClient 指向 SurfaceComposerClient
- mSurface 的 Binder 通信响应端为 SurfaceLayer
- SurfaceLayer 有一个变量 mOwner 指向它的外部类 Layer，而 Layer 有个成员变量 mSurface 指向 SurfaceLayer。这个 SurfaceLayer 对象由 Layer 的 getSurface 函数返回

-> 图 7

SurfaceControl 实际在干嘛，通过 SF 的 createSurface 拿到了一个 SurfaceLayer，通过这个 SurfaceLayer 创建了 SurfaceControl。

而 SurfaceLayer 的创建依赖于 Layer 的创建，Layer 会在 setBuffers 的时候把 FrontBuffer 和 BackBuffer 创建出来，并且Layer 把自己加入到 Z轴去。That's all。

**回答开头的问题：**

SurfaceControl 本身与显示层、SharedBufferStack 没啥关系，它只是一个包装类，封装了一些函数方便调用而已。但是它的成员 mSurface，一个 SurfaceLayer 类型的变量，是与显示层有很大关系的。

SurfaceLayer 在构造函数中会创建 SharedBufferServer 与 SharedClient 中的 SharedBufferStack 交互，

它的 mOwner 变量持有外部类 Layer 的指针，而 Layer 拥有 FrontBuffer 和 BackBuffer。

通过 SurfaceControl 参与创建的 Native Surface，会在构造函数中创建 SharedBufferClient。



### 3. writeToParcel 和 Surface 对象的创建

**[SurfaceControl.cpp]**

```c++
status_t SurfaceControl::writeSurfaceToParcel(
        const sp<SurfaceControl>& control, Parcel* parcel)
{
    uint32_t flags = 0;
    uint32_t format = 0;
    SrufaceID token = -1;
    uint32_t identity = 0;
    uint32_t width = 0;
    uint32_t height = 0;
    sp<SurfaceComposerClient> client;
    sp<ISurface> sur;
    if (SurfaceControl::isValid(control)) {
        token    = control->mToken;
        identity = control->mIdentity;
        client   = control->mClient;
        sur      = control->mSurface;
        width    = control->mWidth;
        height   = control->mHeight;
        format   = control->mFormat;
        flags    = control->mFlags;
    }
    // SurfaceComposerClient 的信息需要传递到 Activity 端，实际上传递的是 BClient
    parcel->writeStrongBinder(client!=0 ? client->connection() : NULL);
    // 把 ISurface 对象信息写到 Parcel 中，这样 Activity 端那边也会构造一个 ISurface 对象
    parcel->writeStrongBinder(sur!=0 ? sur->asBinder() : NULL);
    parcel->writeInt32(identity);
    parcel->writeInt32(width);
    parcel->writeInt32(height);
    parcel->writeInt32(format);
    parcel->writeInt32(flags);
    return NO_ERROR;
}
```

Parcel 发到 Activity 端后，readFromParcel 将根据这个 Parcel 包构造一个 Native 的 Surface 对象

**[android_view_Surface.cpp]**

```c++
static void Surface_readFromParcel(
        JNIEnv* env, jobject clazz, jobject argParcel)
{
    Parcel* parcel = (Parcel*)env->GetIntField( argParcel, no.native_parcel);
    const sp<Surface>& control(getSurface(env, clazz));
    // 根据服务端的 parcel 信息来构造客户端的 Surface
    sp<Surface> rhs = new Surface(*parcel);
    ......
}
```

Native 的 Surface 是怎么根据这个 parcel 包构造的呢？

**[Surface.cpp]**

```c++
Surface::Surface(const Parcel& parcel)
    : mBufferMapper(GraphicBufferMapper::get()),
      mSharedBufferClient(NULL),
{
    // 得到 BpSurfaceComposerClient
    sp<IBinder> clientBinder = parcel.readStrongBinder();
    // 得到 BpSurface，SurfaceLayer 的代理
    mSurface    = interface_cast<ISurface>(parcel.readStrongBinder());
    mIdentity   = parcel.readInt32();
    mWidth      = parcel.readInt32();
    mHeight     = parcel.readInt32();
    mFormat     = parcel.readInt32();
    mFlags      = parcel.readInt32();
    if(clientBinder != NULL) {
        mClient = SurfaceComposerClient::clientForConnection(clientBinder);
        // 创建 SharedBufferClient
        mSharedBufferClient = new SharedBufferClient(
                mClient->mControl, mToken, 2, mIdentity
        );
    }
    init();
}
```

现在 Surface 创建完后，拥有了什么？

- mSurface，SurfaceLayer 的代理，可以与 SurfaceLayer 交互
- mSharedBufferClient 通过 SharedBufferStack 控制 Layer 的 Buffer

->图8







### 4. lockCanvas 和 unlockCanvasAndPost 分析

根据前文的分析可知，android_view_Surface.cpp 中的 lockCanvas 会去调用 Surface.cpp 的 lock 函数。是为了得到一块存储空间，即 BackBuffer。

**[Surface.cpp]**

```c++
// other: 用来接收一些返回信息
// dirtyIn: 表示需要重绘的区域
status_t Surface::lock(SurfaceInfo* other, Region* dirtyIn, bool blocking) 
{
    ......
    // 多线程的情况下要锁住
    if (mApiLock.tryLock() != NO_ERROR) {
        ......
        return WOULD_BLOCK;
    }
    ......
    // we're intending to do software rendering from this point
    // 这个标志在 GraphicBuffer 分配缓冲时有指导作用
    setUsage(GRALLOC_USAGE_SW_READ_OFTEN | GRALLOC_USAGE_SW_WRITE_OFTEN);

    sp<GraphicBuffer> backBuffer;
    // 从 2 个元素的缓冲队列中取出一个空闲缓冲
    status_t err = dequeueBuffer(&backBuffer);
    LOGE_IF(err, "dequeueBuffer failed (%s)", strerror(-err));
    if (err == NO_ERROR) {
        // 锁住这块 buffer
        err = lockBuffer(backBuffer.get());
        if (err == NO_ERROR) {
            const Rect bounds(backBuffer->width, backBuffer->height);
            Region scratch(bounds);
            Region& newDirtyRegion(dirtyIn ? *dirtyIn : scratch);
            newDirtyRegion &= boundsRegion;

            // mPostedBuffer 是上一次绘制时用的 Buffer，也就是现在的 FrontBuffer
            const sp<GraphicBuffer>& frontBuffer(mPostedBuffer);
            const bool canCopyBack = (frontBuffer != 0 &&
                    backBuffer->width  == frontBuffer->width &&
                    backBuffer->height == frontBuffer->height &&
                    backBuffer->format == frontBuffer->format &&
                    !(mFlags & ISurfaceComposer::eDestroyBackbuffer));

            ......
            mDirtyRegion = newDirtyRegion;

            if (canCopyBack) {
                // copy the area that is invalid and not repainted this round
                const Region copyback(mOldDirtyRegion.subtract(newDirtyRegion));
                if (!copyback.isEmpty()) {
                    // 把 frontBuffer 的数据拷贝到 backBuffer 中
                    copyBlt(backBuffer, frontBuffer, copyback);
                }
                    
            } else {
                ......
                newDirtyRegion = boundsRegion;
            }

            ......
            mOldDirtyRegion = newDirtyRegion;

            void* vaddr;
            // 调用 GraphicBuffer 的 lock 得到一块内存，内存地址赋值给 vaddr
            status_t res = backBuffer->lock(
                    GRALLOC_USAGE_SW_READ_OFTEN | GRALLOC_USAGE_SW_WRITE_OFTEN,
                    newDirtyRegion.bounds(), &vaddr);
            
            LOGW_IF(res, "failed locking buffer (handle = %p)", 
                    backBuffer->handle);

            mLockedBuffer = backBuffer;
            // other 用来接收一些信息
            other->w      = backBuffer->width;
            other->h      = backBuffer->height;
            other->s      = backBuffer->stride;
            other->usage  = backBuffer->usage;
            other->format = backBuffer->format;
            other->bits   = vaddr;  // *** 最重要的是这个内存地址 ***
        }
    }
    mApiLock.unlock();
    return err;
}
```









### 5. GraphicBuffer 分析

### 6. 总结

参考：
[Android 显示系统](https://www.cnblogs.com/blogs-of-lxl/p/11272756.html)

