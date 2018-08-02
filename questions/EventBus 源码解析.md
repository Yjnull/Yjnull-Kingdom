## EventBus 源码解析

首先网上已经有不少优秀的EventBus的源码分析文章，这篇只是为了记录自己的理解，毕竟自己亲自写出来才能理解的更深，所以如有不对的地方，还望谅解。

**参考**
https://www.jianshu.com/p/f057c460c77e
http://p.codekk.com/blogs/detail/54cfab086c4761e5001b2538
https://kymjs.com/code/2015/12/16/01/



### 0. 几个问题
1、添加了注解的方法是怎样注册的 ？
2、一个事件以什么为准（我一开始以为是根据方法名来的）？
3、订阅者即当前类，是怎样与事件关联的？
4、post 时，订阅者是怎样收到响应的，是怎么通知到所有与事件相关的订阅者的？


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
这个方法里面做了什么呢，我们来看一看。首先获取到传进去的 this （即当前类对象）的 Class 对象，然后调用 subscriberMethodFinder.findSubscriberMethods(subscriberClass); 我们根据名字可以猜想，这应该是去找到当前订阅者的所有 **事件响应函数**。 找到所有事件响应函数后，就调用 subscribe， 那这里我们就可以猜想是把 当前类对象 与这些 事件响应函数 关联起来。下面验证我们的猜想。

##### 1.2.1 findSubscriberMethods
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
这两个变量是 subscriptionsByEventType 和 typesBySubscriber 。



