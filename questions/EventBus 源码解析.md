## EventBus 源码随想

首先网上已经有不少优秀的EventBus的源码分析文章，这篇只是为了记录自己的理解，毕竟自己亲自写出来才能理解的更深，所以如有不对的地方，还望谅解。

**参考**
https://www.jianshu.com/p/f057c460c77e
http://p.codekk.com/blogs/detail/54cfab086c4761e5001b2538
https://kymjs.com/code/2015/12/16/01/



### 0. 几个问题

EventBus 的使用过程无非就是 注册、post、响应事件函数。
那么我们得弄清楚这几个问题

1、怎样进行注册的 ？
2、post 的事件是 什么，即 post 里的参数到底代表着什么？
3、post 时，订阅者是怎样收到响应的，是怎么通知到所有与事件相关的订阅者的？


### 1. 注册
```
// 把当前类注册为订阅者(Subscriber)
EventBus.getDefault().register(this);

// 解除当前类的注册
EventBus.getDefault().unregister(this);
```
代码很简单，就一行，那么我们来看看到底是怎样注册的。在注册的时候究竟做了什么。在整个 EventBus 的使用过程中，除了注册大部分就是 post 和 事件响应函数了，所以我们猜测在注册的时候，应该会把事件和订阅者绑定起来。那么我们带着这个疑惑去看这段代码。

#### 1.1 获取 EventBus 对象

首先 Event.getDefault() 看到这个应该能想到是个 单例模式。
```
    static volatile EventBus defaultInstance;

	public static EventBus getDefault() {
        if (defaultInstance == null) {
            synchronized (EventBus.class) {
                if (defaultInstance == null) {
                    defaultInstance = new EventBus();
                }
            }
        }
        return defaultInstance;
    }
```
果不其然，一个双重校验锁的单例模式。

```
    private static final EventBusBuilder DEFAULT_BUILDER = new EventBusBuilder();

	public EventBus() {
        this(DEFAULT_BUILDER);
    }

    EventBus(EventBusBuilder builder) {
        // 省略
    }
```
可以看到上面构造函数传入了一个 builder，很明显 **建造者模式**。 至此，我们可以获取到了一个单例的 EventBus 的对象了，另外关于 builder 的更详细的内容可自己看源码。

#### 1.2 register
获取到对象后，就调用 register 方法了，里面传入了 this 参数。
```
	public void register(Object subscriber) {
        Class<?> subscriberClass = subscriber.getClass();
        List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
        synchronized (this) {
            for (SubscriberMethod subscriberMethod : subscriberMethods) {
                subscribe(subscriber, subscriberMethod);
            }
        }
    }

```
我们可以看到 register 方法的参数名是 subscriber，说明我们传进去的 this 就是订阅者对象。此时我们清楚了 **当前类就是订阅者。**
这个方法里面做了什么呢，我们来看一看。首先获取到传进去的 this （即当前类）的 Class 对象，然后调用 subscriberMethodFinder.findSubscriberMethods(subscriberClass); 我们根据名字可以猜想，这应该是去找到当前订阅者的所有 **事件响应函数（即带有 @Subscribe 注解的方法）**。 找到所有事件响应函数后，就调用 subscribe， 那这里我们就可以猜想是把 当前类对象 与这些 事件响应函数 关联起来。下面验证我们的猜想。

##### 1.2.1 findSubscriberMethods
首先我们得找到当前类的所有 合法的事件响应函数
```
	List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
        List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);
        if (subscriberMethods != null) {
            return subscriberMethods;
        }

        if (ignoreGeneratedIndex) {
            subscriberMethods = findUsingReflection(subscriberClass);
        } else {
            subscriberMethods = findUsingInfo(subscriberClass);
        }
        if (subscriberMethods.isEmpty()) {
            throw new EventBusException("Subscriber " + subscriberClass
                    + " and its super classes have no public methods with the @Subscribe annotation");
        } else {
            METHOD_CACHE.put(subscriberClass, subscriberMethods);
            return subscriberMethods;
        }
    }
```
这里面我们直接找到 findUsingReflection 方法， 根据方法名也知道这是什么意思了。 继续跟进

```
	private List<SubscriberMethod> findUsingReflection(Class<?> subscriberClass) {
        FindState findState = prepareFindState();
        findState.initForSubscriber(subscriberClass);
        while (findState.clazz != null) {
            findUsingReflectionInSingleClass(findState);
            findState.moveToSuperclass();
        }
        return getMethodsAndRelease(findState);
    }
```
这里的 FindState 用于做 事件响应函数 的校验和保存。继续跟进 findUsingReflectionInSingleClass

```
	private void findUsingReflectionInSingleClass(FindState findState) {
        Method[] methods;
        try {
            // This is faster than getMethods, especially when subscribers are fat classes like Activities
            methods = findState.clazz.getDeclaredMethods();
        } catch (Throwable th) {
            // Workaround for java.lang.NoClassDefFoundError, see https://github.com/greenrobot/EventBus/issues/149
            methods = findState.clazz.getMethods();
            findState.skipSuperClasses = true;
        }
        for (Method method : methods) {
            int modifiers = method.getModifiers();
            if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 1) {
                    Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
                    if (subscribeAnnotation != null) {
                        Class<?> eventType = parameterTypes[0];
                        if (findState.checkAdd(method, eventType)) {
                            ThreadMode threadMode = subscribeAnnotation.threadMode();
                            findState.subscriberMethods.add(new SubscriberMethod(method, eventType, threadMode,
                                    subscribeAnnotation.priority(), subscribeAnnotation.sticky()));
                        }
                    }
                } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                    String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                    throw new EventBusException("@Subscribe method " + methodName +
                            "must have exactly 1 parameter but has " + parameterTypes.length);
                }
            } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                throw new EventBusException(methodName +
                        " is a illegal @Subscribe method: must be public, non-static, and non-abstract");
            }
        }
    }
```

这里如果知道反射的应该都看得懂，没什么复杂的地方。 从这里的实现我们可以知道，这个 eventType 其实是第一个参数类型， 并且保证只能有一个参数。 这样我们的事件其实是以 **事件响应函数的 参数类型** 为基准的，可以看到如果参数的数量不为 1 ，会抛出异常。

最后再通过 findUsingReflection 方法的 getMethodsAndRelease 返回一个 `List<SubscriberMethod>`
至此， register 方法的 `List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);` 这一行代码就走完了，我们拿到了该订阅类的所有 事件响应函数 ， 即 SubscriberMethod 的集合。

##### 1.2.2 subscribe
接着继续 register 往下走，可以看到会循环 `List<SubscriberMethod>` 这个集合，取出每个 SubscriberMethod（事件响应函数），然后调用 subscribe，我们看看这个方法到底做了什么。
这里先给出结果，这个方法就是我们注册的关键了，它完成了我们每个 事件响应函数的注册过程。


首先这个方法会涉及两个变量，我一开始特别懵，完全不知道这两个变量啥意思，不过图一画出来就立马清晰了。
这两个变量是 subscriptionsByEventType 和 typesBySubscriber 。基本上理解这两个变量是什么意思，整个注册流程就通了。

首先我们定义两个类和方法，如下所示，这个得认真理清楚。
```
public class A {
    @Subscribe
    public void testA1(Event1 event1) {
        //do something
    }

    @Subscribe
    public void testA2(Event2 event2) {
        //do something
    }
}

public class B {
    @Subscribe
    public void testB1(Event1 event1) {
        //do something
    }

    @Subscribe
    public void testB2(Event2 event2) {
        //do something
    }
}
```
可以看到这两个类分别有着两个事件响应函数，事件类型有两种，Event1 和 Event2
**subscriptionsByEventType** 这个变量是 `Map<Class<?>, CopyOnWriteArrayList<Subscription>> ` 类型，它的图示如下：
![subscriptionsByEventType](img\subscriptionsByEventType.png)

**typesBySubscriber** 这个变量是 ` Map<Object, List<Class<?>>>` 类都，它的图示如下：
![typesBySubscriber](img\typesBySubscriber.png)

至于其中的 **SubscriberMethod** 对象，在 **findUsingReflectionInSingleClass** 方法中可以看到是怎样构造出来的。这个对象也是非常重要的，里面封装了 方法信息 method、线程模式 threadMode、参数类型 eventType、优先级 priority、sticky 布尔值 等信息。


相信这两个图可以帮助你很好的理解接下来的 subscribe 流程。关于 subscribe  的源码，以及流程我就不分析了，自己看源码对着上面两个图绝对能理解。 不能理解的话，还有 **参考** 内的文章，这里一定得自己去弄懂。当然并不复杂，所以耐心点。


当 subscribe 走完后，我们的 EventBus 拥有了什么？ 
它拥有了 事件类型(例 Event1) 所对应的 Subscription 信息列表，什么意思，**就是这个 事件 在 哪些类的哪些方法存在**，有了这些信息，EventBus 就能很方便的将一个 事件 传递给所有 订阅了这个事件的 方法了。

那么 typesBySubscriber 有什么用呢，在 unregister 时，它可以发挥它的作用。那么趁热打铁，看看 unregister 是如何工作的。噢，对了，放上注册的流程图，是取自 [codekk](http://p.codekk.com/blogs/detail/54cfab086c4761e5001b2538) 的流程图，相信再看这个图，你会非常清晰。
![register](img\register-flow-chart.png)


### 2 unregister 解除注册
```
public synchronized void unregister(Object subscriber) {
        List<Class<?>> subscribedTypes = typesBySubscriber.get(subscriber);
        if (subscribedTypes != null) {
            for (Class<?> eventType : subscribedTypes) {
                unsubscribeByEventType(subscriber, eventType);
            }
            typesBySubscriber.remove(subscriber);
        } else {
            logger.log(Level.WARNING, "Subscriber to unregister was not registered before: " + subscriber.getClass());
        }
    }
```

第一行就用到了刚刚的 typesBySubscriber， 这里将当前订阅者的 事件类型列表 取出来，也就是拿到了当前类的所有 事件。
然后判断 事件列表 是否为空，不为空则 循环事件列表，依次调用 `unsubscribeByEventType(subscriber, eventType);` 最后调用 `typesBySubscriber.remove(subscriber);` 这个是把 当前订阅者 从 typesBySubscriber 中删掉，这样就完成了解除绑定的操作。当然最重要的是 unsubscribeByEventType 这个方法。

##### 2.1 unsubscribeByEventType
```
private void unsubscribeByEventType(Object subscriber, Class<?> eventType) {
        List<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions != null) {
            int size = subscriptions.size();
            for (int i = 0; i < size; i++) {
                Subscription subscription = subscriptions.get(i);
                if (subscription.subscriber == subscriber) {
                    subscription.active = false;
                    subscriptions.remove(i);
                    i--;
                    size--;
                }
            }
        }
    }
```

这个根据上面的 subscriptionsByEventType 图示，应该不难理解。
首先从 subscriptionsByEventType 中拿到当前 事件（eventType）的 Subscription 列表。 然后循环这个列表，从中找到传进来的 订阅者（subscriber 即 当前解除注册的那个 类，找到后，从 Subscription 列表 中删除即可。



### 3. post 发布事件
前面注册和解除注册两个流程都走通了，那么只剩下 post 了。关于 post 的流程，**参考**内的文章都讲的很好，碍于篇幅，就不重复阐述了，这里说说我刚看时，不懂的几个地方。

1、 postSingleEvent  方法会先去得到该事件类型的所有父类及接口类型，然后循环 调用 postSingleEventForEventType 函数发布每个事件到每个订阅者？ 这里说了这么多是什么意思。
2、 ThreadMode 对应的四种状态的 Poster。

针对第一个问题很好解释，我们基于上面的 Event1 事件解释，假设有一个 订阅者订阅了 Object 类型的事件，而 Event1 的父类是 Object，那么我们 post Event1事件 时， **订阅了 Event1 事件的订阅者** 和 **订阅了 Object 事件的订阅者** 是不是都得接收到这个事件。所以我们需要处理父类及其接口类型的 post。


第二个问题，看了 post 的源码应该知道最后会进入到 postToSubscription 这个方法，判断 subscription.subscriberMethod.threadMode 然后调用 invokeSubscriber 完成 post流程。
那么这个 threadMode 有四种状态，


#### 3.1 POSTING
首先看下这个 POSTING 状态，这是 默认的 ThreadMode，表示在执行 Post 操作的线程直接调用订阅者的事件响应方法，不论该线程是否为主线程（UI 线程），也就是说你是哪个线程就会在哪个线程执行。 代码中可以看到直接调用了 invokeSubscriber ，看看 invokeSubscriber 方法的源码。
```
void invokeSubscriber(Subscription subscription, Object event) {
        try {
            subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
        } catch (InvocationTargetException e) {
            handleSubscriberException(subscription, event, e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception", e);
        }
    }
```
就是通过反射执行了 method ， 这样我们也就清楚了，事件函数得到了响应。
**注意: 若 Post 线程为主线程，别忘了不能进行耗时操作**

#### 3.2 MAIN
```
			case MAIN:
                if (isMainThread) {
                    invokeSubscriber(subscription, event);
                } else {
                    mainThreadPoster.enqueue(subscription, event);
                }
                break;
```
这个状态我们可以看到，首先会判断是否是 主线程， 如果是直接调用 invokeSubscriber ，如果不是会去调用 mainThreadPoster.enqueue 。 那么这个 mainThreadPoster 是什么？
##### 3.2.1 mainThreadPoster
这个 mainThreadPoster 会在构造函数中进行初始化，自己可以去源码里看看， 可以知道最后就是 new 一个 HandlerPoster。
看看 HandlerPoster 的构造函数
```
protected HandlerPoster(EventBus eventBus, Looper looper, int maxMillisInsideHandleMessage) {
        super(looper);
        this.eventBus = eventBus;
        this.maxMillisInsideHandleMessage = maxMillisInsideHandleMessage;
        queue = new PendingPostQueue();
    }
```
这个 looper 是主线程的 looper，也就是说最后发送的消息，会在**主线程去处理**。
然后会 new 一个 PendingPostQueue, 这个 PendingPostQueue 是一个 PendingPost 类型的队列。
而 PendingPost 则是 订阅者和事件信息实体类，并含有同一队列中指向下一个对象的指针。通过缓存存储不用的对象，减少下次创建的性能消耗。
会看的很懵，其实很好理解， PendingPostQueue就相当于 MessageQueue， PendingPost 则相当于 Message。当然只是类比，在调用 enqueue 的时候，会发送一个空的 Message，如下代码：
```
public void enqueue(Subscription subscription, Object event) {
        PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
        synchronized (this) {
            queue.enqueue(pendingPost);
            if (!handlerActive) {
                handlerActive = true;
                if (!sendMessage(obtainMessage())) {
                    throw new EventBusException("Could not send handler message");
                }
            }
        }
    }
```

可以看到我们把 pendingPost 放入到 PendingPostQueue， 然后发送一个空的 Message， 我们理解 Handler 机制，所以去看 handleMessage 吧。
```
	@Override
    public void handleMessage(Message msg) {
        boolean rescheduled = false;
        try {
            long started = SystemClock.uptimeMillis();
            while (true) {
                PendingPost pendingPost = queue.poll();
                if (pendingPost == null) {
                    synchronized (this) {
                        // Check again, this time in synchronized
                        pendingPost = queue.poll();
                        if (pendingPost == null) {
                            handlerActive = false;
                            return;
                        }
                    }
                }
                eventBus.invokeSubscriber(pendingPost);
                long timeInMethod = SystemClock.uptimeMillis() - started;
                if (timeInMethod >= maxMillisInsideHandleMessage) {
                    if (!sendMessage(obtainMessage())) {
                        throw new EventBusException("Could not send handler message");
                    }
                    rescheduled = true;
                    return;
                }
            }
        } finally {
            handlerActive = rescheduled;
        }
    }
```

代码很长，但是你可以很清楚的看到这句代码  `eventBus.invokeSubscriber(pendingPost);` 还记得 invokeSubscriber 方法吗，没错就是 通过反射执行 method。 当然这里重载了该 方法， 但是最终还是会走到 那个有两个参数的 invokeSubscriber 方法。

到这里，我们的 MAIN 状态的 post 流程也走完了。

#### 3.3 BACKGROUND 和 ASYNC
这两个状态希望大家自己去看源码了，并没有什么复杂的，无非就是 backgroundPoster 和 asyncPoster 对线程处理的不同，这两个 Poster 内部同样有 PendingPostQueue 。

```
private final static ExecutorService DEFAULT_EXECUTOR_SERVICE = Executors.newCachedThreadPool();
```
这是 AsyncPoster 和 BackgroundPoster 内部的线程池 在 EventBusBuilder 中的定义。







