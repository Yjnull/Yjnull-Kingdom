## Retrofit2 源码解析 (呕心沥血)

**注意：** 本文是对源码的一个跟踪，会对每一行代码有具体的阐述，但是不会介绍 Retrofit 的设计模式。

Retrofit：一个 Restful 设计风格的 HTTP 网络请求框架的封装。基于 OkHttp

> A type-safe HTTP client for Android and Java


### 0. 基本使用
1、Retrofit 将我们的 HTTP API 转换成一个 接口形式。所以我们第一步定义一个 interface

```
public interface GitHubService {
	@GET("user/{user}/repos")
    Call<List<Integer>> listRepos(@Path("user") String user);
}
```

2、然后构建一个 Retrofit，通过 create 方法生成 GitHubService 的一个实现。
```
Retrofit retrofit = new Retrofit.Builder()
	.baseUrl("https://api.github.com/")
    .build();

GitHubService service = retrofit.create(GitHubService.class);
```

3、调用 listRepos 拿到 Call 实例，可以做同步或异步请求。
```
 Call<List<Integer>> repos = service.listRepos("octocat");
```
每个 Call 实例只能使用一次，但调用 clone() 将创建一个可以使用的新实例。


### 1. Retrofit 构建

#### 1.1 Retrofit
首先看看 Retrofit 吧，这个类里面有7个实例变量。我们根据类型和变量名先猜猜是干什么用的，留个大体印象即可。
```
  // 一个线程安全的、支持高效并发的HashMap，Key 是 Method，Value 是 ServiceMethod。Method 我们能猜到应该就是上面接口中定义的 listRepos，而这个方法中有很多注解，@GET、@Path 啥的，那这个 ServiceMethod 很有可能是这个方法的封装。而变量名带个 Cache 说明，会把这个 Method 对应的 ServiceMethod 缓存起来。
  private final Map<Method, ServiceMethod<?, ?>> serviceMethodCache = new ConcurrentHashMap<>();

  // 想必你知道 Retrofit 就是基于 OkHttp 的封装，那这个 Call.Factory，明显就是 Call 的工厂类。至于 Call 是干嘛的，负责创建 HTTP 请求，HTTP 请求被抽象为了 okhttp3.Call 类，它表示一个已经准备好，可以随时执行的 HTTP 请求；
  final okhttp3.Call.Factory callFactory;
  // 这个很好理解了，就是上面 基本使用 中的 baseUrl，可是这是个 HttpUrl 类型的，我们传的可是 String 类型的呀，那估计是通过 Builder 做了处理的。
  final HttpUrl baseUrl;
  // Converter 根据字面意思可得 这应该是个转换器，用于把我们的 响应 转换成特定的格式
  final List<Converter.Factory> converterFactories;
  // CallAdapter 根据字面意思，难道是对 Call 的一个适配？
  final List<CallAdapter.Factory> callAdapterFactories;
  // Executor 很熟悉了，这是个回调 Executor，想必就是用来切换线程的了
  final @Nullable Executor callbackExecutor;
  // 这个就猜不出了，只能暂时理解为一个标志位
  final boolean validateEagerly;
```

再来看看 Retrofit 的构造函数
```
  Retrofit(okhttp3.Call.Factory callFactory, HttpUrl baseUrl,
      List<Converter.Factory> converterFactories, List<CallAdapter.Factory> callAdapterFactories,
      @Nullable Executor callbackExecutor, boolean validateEagerly) {
    this.callFactory = callFactory;
    this.baseUrl = baseUrl;
    this.converterFactories = converterFactories; // Copy+unmodifiable at call site.
    this.callAdapterFactories = callAdapterFactories; // Copy+unmodifiable at call site.
    this.callbackExecutor = callbackExecutor;
    this.validateEagerly = validateEagerly;
  }
```
并没做什么特殊的处理，就是简单的赋值，那想必所有初始化的操作都在 Builder 里了。

那么成功建立一个 Retrofit 对象的标准就是：**配置好Retrofit 里的成员变量。**
- callFactory : 网络请求 工厂
- baseUrl ：网络请求的基本 Url 地址
- converterFactories ：数据转换器 工厂集合
- callAdapterFactories ：网络请求适配器 工厂集合
- callbackExecutor ：回调方法执行器

#### 1.2 Retrofit.Builder
```
public static final class Builder {
    private final Platform platform;
    private @Nullable okhttp3.Call.Factory callFactory;
    private HttpUrl baseUrl;
    private final List<Converter.Factory> converterFactories = new ArrayList<>();
    private final List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>();
    private @Nullable Executor callbackExecutor;
    private boolean validateEagerly;

    Builder(Platform platform) {
      this.platform = platform;
    }

    public Builder() {
      this(Platform.get());
    }

    // ... ...
    }
```
我们可以看到 Builder 与 Retrofit 的参数几乎一样，只是少了 serviceMethodCache，多了个 Platform。这个 Platform 很重要。我们通过 Builder 的构造函数可以知道，调用了 Platform.get()方法，然后赋值给自己的 platform 变量。 我们看看这个 Platform 类。
```
class Platform {
  private static final Platform PLATFORM = findPlatform();

  static Platform get() {
    return PLATFORM;
  }

  private static Platform findPlatform() {
    try {
      Class.forName("android.os.Build");
      if (Build.VERSION.SDK_INT != 0) {
        return new Android();
      }
    } catch (ClassNotFoundException ignored) {
    }
    try {
      Class.forName("java.util.Optional");
      return new Java8();
    } catch (ClassNotFoundException ignored) {
    }
    return new Platform();
  }
  
  // ... ...
 }
```
get 方法会去调用 findPlatform 方法，这个里面很明显跟平台相关，Class.forName 要求 JVM 根据 className 查找并加载指定的类，如果未找到则抛出 ClassNotFoundException 。这里很明显我们分析 Android 平台，所以会 return 一个 Android（）对象。

```
//Platform 内部
static class Android extends Platform {
    @Override public Executor defaultCallbackExecutor() {
      return new MainThreadExecutor();
    }

    @Override CallAdapter.Factory defaultCallAdapterFactory(@Nullable Executor callbackExecutor) {
      if (callbackExecutor == null) throw new AssertionError();
      return new ExecutorCallAdapterFactory(callbackExecutor);
    }

    static class MainThreadExecutor implements Executor {
      private final Handler handler = new Handler(Looper.getMainLooper());

      @Override public void execute(Runnable r) {
        handler.post(r);
      }
    }
  }
```
我们在这里面可以看到两个重要的方法
- **defaultCallbackExecutor** ：这个方法返回的是个 Executor ，我们想到 Retrofit 正好有个 Executor 类型的变量，那么想必就是它了，它是 MainThreadExecutor 类型的，内部采用 handler 执行任务。
- **defaultCallAdapterFactory** ：这个方法返回的是个 CallAdapter.Factory，Retrofit 成员变量中也正好有个 CallAdapter.Factory 类型的变量，所以说这个 Platform 很重要嘛，跟我们 Retrofit 类中的两个成员变量都有重大的关系。这里最终返回的是个 ExecutorCallAdapterFactory ，话说我们一开始就不知道这个 CallAdapter 是什么，更不用说这个 Factory 了，那我们先看看这个 ExecutorCallAdapterFactory 吧。

```
final class ExecutorCallAdapterFactory extends CallAdapter.Factory {
  final Executor callbackExecutor;

  ExecutorCallAdapterFactory(Executor callbackExecutor) {
    this.callbackExecutor = callbackExecutor;
  }

  @Override
  public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
    if (getRawType(returnType) != Call.class) {
      return null;
    }
    final Type responseType = Utils.getCallResponseType(returnType);
    return new CallAdapter<Object, Call<?>>() {
      @Override public Type responseType() {
        return responseType;
      }

      @Override public Call<Object> adapt(Call<Object> call) {
        return new ExecutorCallbackCall<>(callbackExecutor, call);
      }
    };
  }

  //... ... 省略
}
```
这里我们可以看到，把我们传进来的 Executor 保存起来了，这个 Executor 想必就是 MainThreadExecutor 了。至于 get 方法，我们暂时还不知道哪里用到了，所以后面的暂时不看了，到了这里还是不知道 CallAdapter.Factory 干嘛用的。

看来 Builder 方法很复杂呀，写了这么多只是讲了个 Platform，不过幸好这里面也包括了 Executor 和 CallAdapter.Factory ，那么现在我们正式看看 Builder.build()方法。
```
public Retrofit build() {
	  // 这一句告诉我们，baseUrl 是必不可少的。
      if (baseUrl == null) {
        throw new IllegalStateException("Base URL required.");
      }

      // 这里如果你没配置 callFactory , 会默认配置为 OkHttpClient
      okhttp3.Call.Factory callFactory = this.callFactory;
      if (callFactory == null) {
        callFactory = new OkHttpClient();
      }

	  // 同样的，没配置的话，会默认配置为 Platform 的 defaultCallbackExecutor，这里我们之前分析过，它所返回的就是 MainThreadExecutor
      Executor callbackExecutor = this.callbackExecutor;
      if (callbackExecutor == null) {
        callbackExecutor = platform.defaultCallbackExecutor();
      }

	  //这里会把你所配置的 CallAdapter.Factory 加到 List 里去，最后把 Platform 默认的 defaultCallAdapterFactory 即 ExecutorCallAdapterFactory 加到 List 的最后边，
      // Make a defensive copy of the adapters and add the default Call adapter.
      List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>(this.callAdapterFactories);
      callAdapterFactories.add(platform.defaultCallAdapterFactory(callbackExecutor));

 	  //这里一样会把你配置的 Converter.Factory 加到 List 里去，但是会把一个 BuiltInConverters 加到第一个，而不是最后一个，请注意这点。
      // Make a defensive copy of the converters.
      List<Converter.Factory> converterFactories =
          new ArrayList<>(1 + this.converterFactories.size());

      // Add the built-in converter factory first. This prevents overriding its behavior but also
      // ensures correct behavior when using converters that consume all types.
      converterFactories.add(new BuiltInConverters());
      converterFactories.addAll(this.converterFactories);

	  //最后返回一个 Retrofit 对象
      return new Retrofit(callFactory, baseUrl, unmodifiableList(converterFactories),
          unmodifiableList(callAdapterFactories), callbackExecutor, validateEagerly);
    }
```

到这里，我们的 Retrofit 就构建完成了。如果按照我们 基本使用 中的例子，那么此刻，Retrofit 成员变量的值如下：
- serviceMethodCache ：暂时为空的 HashMap 集合
- callFactory ： OkHttpClient 对象
- baseUrl ： 根据配置的baseUrl " https://api.github.com/ " 字符串， 构建出了一个 HttpUrl 对象
- converterFactories ：一个 ArrayList 对象，里面存放着一个BuiltInConverters 对象
- callAdapterFactories ：一个 ArrayList 对象，里面存放着一个 ExecutorCallAdapterFactory 对象
- callbackExecutor ：MainThreadExecutor 对象
- validateEagerly ：默认值 false


### 2. 创建网络请求接口实例，即 `GitHubService service = retrofit.create(GitHubService.class);`

接下来我们看看是怎样获得 GitHubService 实例的。
同样上源码，**注意**这里的 create 是非常重要的一个方法，这里使用了 外观模式 和 代理模式。

```
	public <T> T create(final Class<T> service) {
    Utils.validateServiceInterface(service);
    if (validateEagerly) {
      eagerlyValidateMethods(service);
    }
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        new InvocationHandler() {
          private final Platform platform = Platform.get();

          @Override public Object invoke(Object proxy, Method method, @Nullable Object[] args)
              throws Throwable {
            // If the method is a method from Object then defer to normal invocation.
            if (method.getDeclaringClass() == Object.class) {
              return method.invoke(this, args);
            }
            if (platform.isDefaultMethod(method)) {
              return platform.invokeDefaultMethod(method, service, proxy, args);
            }
            ServiceMethod<Object, Object> serviceMethod =
                (ServiceMethod<Object, Object>) loadServiceMethod(method);
            OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);
            return serviceMethod.adapt(okHttpCall);
          }
        });
  }
```

这里我们看到了 validateEagerly 变量，让我们看看它到底控制了什么。进 eagerlyValidateMethods 方法。
```
  private void eagerlyValidateMethods(Class<?> service) {
    Platform platform = Platform.get();
    for (Method method : service.getDeclaredMethods()) {
      if (!platform.isDefaultMethod(method)) {
        loadServiceMethod(method);
      }
    }
  }
  
  ServiceMethod<?, ?> loadServiceMethod(Method method) {
    ServiceMethod<?, ?> result = serviceMethodCache.get(method);
    if (result != null) return result;

    synchronized (serviceMethodCache) {
      result = serviceMethodCache.get(method);
      if (result == null) {
        result = new ServiceMethod.Builder<>(this, method).build();
        serviceMethodCache.put(method, result);
      }
    }
    return result;
  }
```
这里又见到了 Platform ，在 Retrofit.Builder 我们知道它返回的是 Android() 对象。 接着是个 循环 ，循环取出接口中的 Method ，接着调用 loadServiceMethod 。 loadServiceMethod 里面会根据 Method 生成一个 ServiceMethod，然后存入 serviceMethodCache ， 那么我们大概知道，这是属于**提前验证**，会提前把接口中每个方法进行**解析**得到一个 **ServiceMethod** 对象，然后存入缓存中。 在 loadServiceMethod 中会取缓存中的值，如果有就直接返回 ServiceMethod。

由此可以知道 **validateEagerly** 变量是用于 判断是否需要提前验证解析的。

create 方法中 继续往下走，会看到 return 一个 代理对象 Proxy ，并转成了 T 类型，即 GitHubService 。
此时我们这句代码 `GitHubService service = retrofit.create(GitHubService.class);`  中的 service 有值了，它指向一个 实现了 GitHubService 接口的 代理对象 Proxy 。

### 3. 拿到 Call 对象 ， 即 `Call<List<Repo>> repos = service.listRepos("octocat");`

这里我们的 service 是个代理对象，所以执行 listRepos 方法时， 会先走 InvocationHandler 中的 invoke 方法。
```
  public <T> T create(final Class<T> service) {
    Utils.validateServiceInterface(service);
    if (validateEagerly) {
      eagerlyValidateMethods(service);
    }
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        new InvocationHandler() {
          private final Platform platform = Platform.get();

          @Override public Object invoke(Object proxy, Method method, @Nullable Object[] args)
              throws Throwable {
            // If the method is a method from Object then defer to normal invocation.
            // 如果这个方法是声明在 Object 类中，那么不拦截，直接执行
            if (method.getDeclaringClass() == Object.class) {
              return method.invoke(this, args);
            }
            // 这个总是返回的false，所以不用关心
            if (platform.isDefaultMethod(method)) {
              return platform.invokeDefaultMethod(method, service, proxy, args);
            }

            // 下面三行代码非常重要，重点分析，分别对应 3.1 3.2 3.3 三个小节
            ServiceMethod<Object, Object> serviceMethod =
                (ServiceMethod<Object, Object>) loadServiceMethod(method);
            OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);
            return serviceMethod.adapt(okHttpCall);
          }
        });
  }
```

#### 3.1 `ServiceMethod<Object, Object> serviceMethod = (ServiceMethod<Object, Object>) loadServiceMethod(method);`

首先 ServiceMethod 我们之前猜测过，应该是对 Method 的一个封装， 而这个 loadServiceMethod ，如果你还记得的话，我们在 create 的时候就碰到过，eagerlyValidateMethods 这个方法内部调用过 loadServiceMethod ，是为了加载这个 ServiceMethod 。现在我们来深入分析这个 loadServiceMethod 方法。
```
  ServiceMethod<?, ?> loadServiceMethod(Method method) {
    // 首先从 缓存 serviceMethodCache 中取 ServiceMethod ，如果存在就返回，不存在继续往下走。
    // 也就是说 我们的 ServiceMethod 只会创建一次。
    ServiceMethod<?, ?> result = serviceMethodCache.get(method);
    if (result != null) return result;

    synchronized (serviceMethodCache) {
      //这里又从缓存取了一遍，看到这里有没有一种熟悉的感觉，是不是跟 DCL 单例模式特别像，双重校验。
      result = serviceMethodCache.get(method);
      if (result == null) {
        result = new ServiceMethod.Builder<>(this, method).build();
        serviceMethodCache.put(method, result);
      }
    }
    return result;
  }
```
到这里其实 loadServiceMethod 已经分析完了，很简单，就是个 DCL 单例模式，然后获得 ServiceMethod 。
那其实我们现在的分析任务就很明确了，弄清楚这个 ServiceMethod 究竟是什么 。

##### 3.1.1 ServiceMethod 分析

```
 final class ServiceMethod<R, T> {
  // ... 省略部分代码
  private final okhttp3.Call.Factory callFactory;
  private final CallAdapter<R, T> callAdapter;
  private final HttpUrl baseUrl;
  private final Converter<ResponseBody, R> responseConverter;

  // 同样先猜猜什么意思吧
  // 应该是网络请求的 Http 方法，比如 GET、POST 啥的
  private final String httpMethod;
  // 相对地址 ，应该就是 "user/{user}/repos" 这一段
  private final String relativeUrl;
  // http 请求头
  private final Headers headers;
  // 网络请求的 http 报文的 body 的类型
  private final MediaType contentType;
  // 是否有 body
  private final boolean hasBody;
  // post 提交数据时，是否使用 表单提交 方式
  private final boolean isFormEncoded;
  // post 提交数据时，是否使用 Mutipart 方式，一般用来文件上传
  private final boolean isMultipart;
  // 方法参数处理器，应该是解析方法中的 参数 的吧，这个估计也得详细分析下。
  private final ParameterHandler<?>[] parameterHandlers;

  ServiceMethod(Builder<R, T> builder) {
    this.callFactory = builder.retrofit.callFactory();
    this.callAdapter = builder.callAdapter;
    this.baseUrl = builder.retrofit.baseUrl();
    this.responseConverter = builder.responseConverter;
    this.httpMethod = builder.httpMethod;
    this.relativeUrl = builder.relativeUrl;
    this.headers = builder.headers;
    this.contentType = builder.contentType;
    this.hasBody = builder.hasBody;
    this.isFormEncoded = builder.isFormEncoded;
    this.isMultipart = builder.isMultipart;
    this.parameterHandlers = builder.parameterHandlers;
  }

  // ... 省略部分代码
```

首先看看 ServiceMethod 的构造方法。 也是通过建造者模式构建的。其中很多变量其实都很熟悉了，比如 callFactory 、 baseUrl 。 对于 callAdapter、responseConverter 我们别弄混了，我们在 Retrofit 类中的变量是 callAdapterFactories 和 converterFactories ， 是它们的工厂，是生产它们的地方。

接下来看 Builder 吧，毕竟这是真正做事的。

```
  public ServiceMethod build() {
      // 拿到具体的 CallAdapter 即 网络请求适配器，具体看 3.1.1.1
      callAdapter = createCallAdapter();
      // 根据上面拿到的 callAdapter 获取 响应类型，在 3.1.1.1 小节分析完后可知道
      // 在我们的例子中 responseType = java.util.List<java.lang.Integer>
      responseType = callAdapter.responseType();
      if (responseType == Response.class || responseType == okhttp3.Response.class) {
        throw methodError("'"
            + Utils.getRawType(responseType).getName()
            + "' is not a valid response body type. Did you mean ResponseBody?");
      }
      // 获取 响应转换器 ，具体看 3.1.1.2 小节
      responseConverter = createResponseConverter();
      // 解析网络请求接口中方法的注解，这里我们就只有一个 @GET 注解，具体看 3.1.1.3 小节
      // 这里解析完可以拿到 Http 请求方法、请求体、相对 url、相对 url 中的参数
      for (Annotation annotation : methodAnnotations) {
        parseMethodAnnotation(annotation);
      }
      //解析完方法上的注解后，做校验
      if (httpMethod == null) {
        throw methodError("HTTP method annotation is required (e.g., @GET, @POST, etc.).");
      }

      if (!hasBody) {
        if (isMultipart) {
          throw methodError(
              "Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
        }
        if (isFormEncoded) {
          throw methodError("FormUrlEncoded can only be specified on HTTP methods with "
              + "request body (e.g., @POST).");
        }
      }

      // 解析当前方法的参数，这里就我们的例子而言
      // parameterAnnotationsArray 就是 @Path ，所以这里的 length 就是 1
      int parameterCount = parameterAnnotationsArray.length;
      parameterHandlers = new ParameterHandler<?>[parameterCount];
      for (int p = 0; p < parameterCount; p++) {
        // parameterTypes 是参数类型，就本例而言是 String
        Type parameterType = parameterTypes[p];
        if (Utils.hasUnresolvableType(parameterType)) {
          throw parameterError(p, "Parameter type must not include a type variable or wildcard: %s",
              parameterType);
        }

        // 拿到第一个参数的 注解数组
        Annotation[] parameterAnnotations = parameterAnnotationsArray[p];
        if (parameterAnnotations == null) {
          throw parameterError(p, "No Retrofit annotation found.");
        }
        // 解析参数
        // p : 0
        // parameterType : String
        // parameterAnnotations : 虽然是数组，但是就一个元素 @Path
        // 这个 parseParameter 就不分析了，大家自己看看源码就清楚了，无非就是构建 ParameterHandler 数组，而这个 ParameterHandler 其实就是负责解析 API 定义时每个方法的参数，并在构造 HTTP 请求时设置参数
        parameterHandlers[p] = parseParameter(p, parameterType, parameterAnnotations);
      }
      // 解析完方法中参数的注解后，做校验
      if (relativeUrl == null && !gotUrl) {
        throw methodError("Missing either @%s URL or @Url parameter.", httpMethod);
      }
      if (!isFormEncoded && !isMultipart && !hasBody && gotBody) {
        throw methodError("Non-body HTTP method cannot contain @Body.");
      }
      if (isFormEncoded && !gotField) {
        throw methodError("Form-encoded method must contain at least one @Field.");
      }
      if (isMultipart && !gotPart) {
        throw methodError("Multipart method must contain at least one @Part.");
      }

      return new ServiceMethod<>(this);
    }
```

##### 3.1.1.1 createCallAdapter ()
```
  private CallAdapter<T, R> createCallAdapter() {
      // 拿到网络请求接口里方法的返回值类型，在我们的例子中会返回如下类型
      // retrofit2.Call<java.util.List<java.lang.Integer>>
      Type returnType = method.getGenericReturnType();

      if (Utils.hasUnresolvableType(returnType)) {
        throw methodError(
            "Method return type must not include a type variable or wildcard: %s", returnType);
      }
      // 如果返回类型是 void ，抛出异常
      if (returnType == void.class) {
        throw methodError("Service methods cannot return void.");
      }
      // 拿到方法的 注解 ，在我们的例子中就是如下所示，大家可以自己实验下
      // @retrofit2.http.GET(value=users/{user}/repos)
      Annotation[] annotations = method.getAnnotations();
      try {
        // 拿到注解后，返回个 CallAdapter ，跟进去看看究竟是做了什么
        return (CallAdapter<T, R>) retrofit.callAdapter(returnType, annotations);
      } catch (RuntimeException e) { // Wide exception range because factories are user code.
        throw methodError(e, "Unable to create call adapter for %s", returnType);
      }
    }


  public CallAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
    // 这里会去调用 nextCallAdapter
    return nextCallAdapter(null, returnType, annotations);
  }

  // 这里的参数大家注意
  // skipPast 上面传的是 null
  // returnType 就是  retrofit2.Call<java.util.List<java.lang.Integer>>
  // annotations 在我们的例子中就是 @retrofit2.http.GET(value=users/{user}/repos)
  public CallAdapter<?, ?> nextCallAdapter(@Nullable CallAdapter.Factory skipPast, Type returnType, Annotation[] annotations) {
    checkNotNull(returnType, "returnType == null");
    checkNotNull(annotations, "annotations == null");

    // callAdapterFactories 是一个 ArrayList 对象，里面存放着一个 ExecutorCallAdapterFactory 对象 ，这个是在 Retrofit Builder 的时候创建的，也就是我们上面所说的生产 CallAdapter 的地方，大家可以回过头去看看。 这里的 skipPast 是null， 所以 indexOf 肯定返回的 -1， 所以这里 start = 0
    int start = callAdapterFactories.indexOf(skipPast) + 1;
    // 循环， 这里由于我们的 callAdapterFactories 只有一个 元素， 所以直接看 ExecutorCallAdapterFactory 的 get方法
    for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
      CallAdapter<?, ?> adapter = callAdapterFactories.get(i).get(returnType, annotations, this);
      if (adapter != null) {
        return adapter;
      }
    }

    // 错误信息 builder
    StringBuilder builder = new StringBuilder("Could not locate call adapter for ")
        .append(returnType)
        .append(".\n");
    if (skipPast != null) {
      builder.append("  Skipped:");
      for (int i = 0; i < start; i++) {
        builder.append("\n   * ").append(callAdapterFactories.get(i).getClass().getName());
      }
      builder.append('\n');
    }
    builder.append("  Tried:");
    for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
      builder.append("\n   * ").append(callAdapterFactories.get(i).getClass().getName());
    }
    throw new IllegalArgumentException(builder.toString());
  }
```
到这里，我们别忘了我们是在干嘛，我们是在获取 `CallAdapter<T, R>` ，好了，继续看 **ExecutorCallAdapterFactory 的 get方法 **。 解释都在代码注释里哟，一定要看看才知道现在到底是在干啥。话说源码分析，还是得靠自己认认真真读一次源码才行。

```
  @Override
  public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
    // getRawType 会返回该类型的原始类类型 , 比如传进去的是 List<? extends Runnable> 会返回 List.class
    // 那么在我们的例子中，我们的 returnType 是 retrofit2.Call<java.util.List<java.lang.Integer>>
    // 那么 getRawType 后，返回的是 retrofit2.Call ，所以这里是相等的
    if (getRawType(returnType) != Call.class) {
      return null;
    }
    // 根据 returnType 拿到 responseType ，这里就不跟进了，可以自己去看看
    // 在我们的例子中， responseType = java.util.List<java.lang.Integer>
    final Type responseType = Utils.getCallResponseType(returnType);
    // 最后返回一个 CallAdapter
    return new CallAdapter<Object, Call<?>>() {
      @Override public Type responseType() {
        return responseType;
      }

      @Override public Call<Object> adapt(Call<Object> call) {
        return new ExecutorCallbackCall<>(callbackExecutor, call);
      }
    };
  }
```
到这里，其实我们大概知道这个 CallAdapter 有什么用了，就是提供两个东西
- 网络请求响应要返回的类型 responseType
- retrofit2.Call< T >  ，注意这里不是 okhttp3 下的 Call ，这里暂不深究。

因为我们不要忘了现在在做什么，我们现在是在获取 ServiceMethod 中的 callAdapter 变量值。所以看到这里返回了一个 CallAdapter 对象即可。


##### 3.1.1.2 createResponseConverter ()
这里个方法是获取 响应转换器， 就是把网络请求得到的响应数据转换成相应的格式。

```
  private Converter<ResponseBody, T> createResponseConverter() {
      // 拿到方法上所有的注解，在我们的例子中就只有 @GET 注解
      Annotation[] annotations = method.getAnnotations();
      // 这里的 responseType 就是上面我们得到的 List<Integer>
      try {
        return retrofit.responseBodyConverter(responseType, annotations);
      } catch (RuntimeException e) { // Wide exception range because factories are user code.
        throw methodError(e, "Unable to create converter for %s", responseType);
      }
    }
```
这里想必大家也知道套路了，跟获取 CallAdapter 是一样的，代码就不贴了，代码里同样是循环遍历 Retrofit 里的 converterFactories 变量。而这个 converterFactories 在我们的例子中是没有设置转换器的，所以它也只有一个默认的元素，即 BuiltInConverters 。 那么我们直接查看 它的 responseBodyConverter 方法。

```
 final class BuiltInConverters extends Converter.Factory {
  // 注意这里的参数，别忘了到底是什么
  // type : 就是我们的 responseType ，即 List<Integer>
  // annotations : 这里我们方法的注解只有一个，所以就是 @GET
  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
      Retrofit retrofit) {
    if (type == ResponseBody.class) {
      return Utils.isAnnotationPresent(annotations, Streaming.class)
          ? StreamingResponseBodyConverter.INSTANCE
          : BufferingResponseBodyConverter.INSTANCE;
    }
    if (type == Void.class) {
      return VoidResponseBodyConverter.INSTANCE;
    }
    return null;
  }
```
通过这里我们可以知道，其实它会返回 null 。 所以我们 **ServiceMethod 中的 Builder 中的 responseConverter 变量就等于 null 。**




##### 3.1.1.3 parseMethodAnnotation ()
我们来看看 解析方法注解 ，注意我们例子中这个方法里传的参数是 @GET 注解
```
   private void parseMethodAnnotation(Annotation annotation) {
      if (annotation instanceof DELETE) {
        parseHttpMethodAndPath("DELETE", ((DELETE) annotation).value(), false);
      } else if (annotation instanceof GET) {
        //我们这里是 GET 注解，所以进这个方法
        parseHttpMethodAndPath("GET", ((GET) annotation).value(), false);
      } else if (annotation instanceof HEAD) {
        parseHttpMethodAndPath("HEAD", ((HEAD) annotation).value(), false);
        if (!Void.class.equals(responseType)) {
          throw methodError("HEAD method must use Void as response type.");
        }
      }
      // 省略后续代码，后续还有很多其他类型的判断
    }


   // 这里的三个参数的值
   // httpMethod : GET
   // value : users/{user}/repos
   // hasBody : false
   private void parseHttpMethodAndPath(String httpMethod, String value, boolean hasBody) {
      // 此处判断 httpMethod 的值是否存在，说明只允许一个 HTTP 方法存在
      if (this.httpMethod != null) {
        throw methodError("Only one HTTP method is allowed. Found: %s and %s.",
            this.httpMethod, httpMethod);
      }
      this.httpMethod = httpMethod;
      this.hasBody = hasBody;

      if (value.isEmpty()) {
        return;
      }

      // 下面是解析 value 中的 相对 url
      // Get the relative URL path and existing query string, if present.
      int question = value.indexOf('?');
      if (question != -1 && question < value.length() - 1) {
        // Ensure the query string does not have any named parameters.
        String queryParams = value.substring(question + 1);
        Matcher queryParamMatcher = PARAM_URL_REGEX.matcher(queryParams);
        if (queryParamMatcher.find()) {
          throw methodError("URL query string \"%s\" must not have replace block. "
              + "For dynamic query parameters use @Query.", queryParams);
        }
      }

      this.relativeUrl = value;
      // 相对地址中的参数名字，这里不具体分析了，可以把结果告诉你
      // 在我们的例子中 value = “users/{user}/repos”
      // 这里的 relativeUrlParamNames 是个 Set<String> 集合 ，里面只有一个元素 user 。
      this.relativeUrlParamNames = parsePathParameters(value);
    }
```
至此，我们的 Builder 把 Http 的方法以及它的 Url 给分析完了，现在只剩 **参数解析了**。参数解析在 ServiceMethod 的 build 方法里已经讲过了 ，记得看注释。
呼~ 终于讲完了 ServiceMethod 的构造。这么大篇幅，由此可以看出 ServiceMethod 这个类非常重要。现在来总结一下，我们究竟拥有了些什么。

- callFactory : ExecutorCallAdapterFactory 实例
- callAdapter : ExecutorCallAdapterFactory中的get 方法返回的 CallAdapter 实例
- baseUrl ： HttpUrl 实例
- responseConverter : 由于我们没设置，所以为 null
- httpMethod : 字符串 GET
- relativeUrl ：字符串 users/{user}/repos
- headers : 没有设置 Headers ，所以为 null
- contentType : null
- hasBody : false
- isFormEncoded : false
- isMultipart : false
- parameterHandlers : 就我们例子而已，该数组有一个元素，Path 对象，它是 ParameterHandler 抽象类里的一个静态内部类。

由此可以看出，**ServiceMethod 对象包含了访问网络的所有基本信息。**

好吧，接下来还是得继续前行，别忘了，我们构建 ServiceMethod 只是在 invoke 方法内，并且这还只是第一步。接下来看第二步。

#### 3.2 `OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);`

这里是 new 一个 OkHttpCall 对象，这个 OkHttpCall 是 Retrofit 的 Call，它里面就是做请求的地方，会有 request、enqueue 等同步、异步请求方法，但是在这里面真正执行请求的是 okhttp3.Call ，即把请求委托给 okHttp 去执行。下面简要看看它的构造方法和一些成员变量吧，因为这里只是 new 操作，所以暂时不分析其余方法，用到的时候再看。
```
 final class OkHttpCall<T> implements Call<T> {
  // 含有所有网络请求参数信息的 ServiceMethod
  private final ServiceMethod<T, ?> serviceMethod;
  private final @Nullable Object[] args;

  private volatile boolean canceled;

  // 实际进行网络请求的 Call
  private @Nullable okhttp3.Call rawCall;
  @GuardedBy("this") // Either a RuntimeException, non-fatal Error, or IOException.
  private @Nullable Throwable creationFailure;
  @GuardedBy("this")
  private boolean executed;

  // 传入配置好的 ServiceMethod 和 请求参数
  OkHttpCall(ServiceMethod<T, ?> serviceMethod, @Nullable Object[] args) {
    this.serviceMethod = serviceMethod;
    this.args = args;
  }
```

这样就把 OkHttpCall 给构建好了，接下来看第三步。

#### 3.3 `return serviceMethod.adapt(okHttpCall);`
直接上代码
```
 T adapt(Call<R> call) {
    return callAdapter.adapt(call);
  }
```
这是 前面构建好的 ServiceMethod 中的 adapt 方法，会去调用 callAdapter 的 adapt 方法，我们知道 ServiceMethod 中的 callAdapter 是 ExecutorCallAdapterFactory中的get 方法返回的 CallAdapter 实例。而这个实例的 adapt 方法会返回一个 ExecutorCallbackCall 对象。
```
 <!-- ExecutorCallAdapterFactory 内部类 -->
 static final class ExecutorCallbackCall<T> implements Call<T> {
    // 这里在之前创建ExecutorCallAdapterFactory时，就知道它的值了，就是 MainThreadExecutor ，用来切换线程的
    final Executor callbackExecutor;
    // 这就是刚刚传进来的 OkHttpCall
    final Call<T> delegate;

    ExecutorCallbackCall(Executor callbackExecutor, Call<T> delegate) {
      this.callbackExecutor = callbackExecutor;
      this.delegate = delegate;
    }
```

到这里为止，我们已经成功的返回了一个 `Call<List<Integer>>` 


### 4. 调用 Call 的 enqueue
趁热打铁，我们执行异步请求，看看怎样切换线程的。
```
 <!-- ExecutorCallbackCall 内部 -->
 @Override
 public void enqueue(final Callback<T> callback) {
      checkNotNull(callback, "callback == null");

      // 真正的 Call 去执行请求
      delegate.enqueue(new Callback<T>() {
        @Override public void onResponse(Call<T> call, final Response<T> response) {
          // 回调后 利用 MainThreadExecutor 中的 Handler 切换到主线程中去。
          callbackExecutor.execute(new Runnable() {
            @Override public void run() {
              if (delegate.isCanceled()) {
                // Emulate OkHttp's behavior of throwing/delivering an IOException on cancellation.
                callback.onFailure(ExecutorCallbackCall.this, new IOException("Canceled"));
              } else {
                callback.onResponse(ExecutorCallbackCall.this, response);
              }
            }
          });
        }

        @Override public void onFailure(Call<T> call, final Throwable t) {
          callbackExecutor.execute(new Runnable() {
            @Override public void run() {
              callback.onFailure(ExecutorCallbackCall.this, t);
            }
          });
        }
      });
    }
```
可以看到是 delegate 执行了 enqueue 操作，而 delegate 就是我们的 OkHttpCall ，在 OkHttpCall 里的 enqueue 方法是这样工作的。
通过 `okhttp3.Call call = serviceMethod.toCall(args);` 构建一个真正执行请求的 Call ，即把请求交给 okhttp 去完成。而构建一个 Call 利用到了 ServiceMethod 中的 ParameterHandler 对象，这个对象是用来处理参数的。 它会把具体参数的值与 RequestBuilder 绑定起来。当然也用到了 ServiceMethod 自己，ServiceMethod 类似请求响应的大管家。

别忘了拿到响应后，在 okhttp3.Callback 中会去调用 `response = parseResponse(rawResponse);` 将响应转换成自己想要的格式，即定义的 Converter 。


到这里终于结束了，当然在响应解析这里还有许多没讲，但是 Retrofit 一个主体的流程已经走完了。真累。。。
没啥总结的了，这篇文章只是用来跟踪具体的源码，具体到每一句代码都有解释。至于 Retrofit 的设计思路，别的文章都有讲。
总之，在自己跟着分析完这么一大段后，已经对 Retrofit 相当熟悉了，遇到问题，相信也可以定位到源码中去找到问题的根源，然后解决，至此，目标已达成。



**参考**
https://blog.csdn.net/carson_ho/article/details/73732115
https://www.jianshu.com/p/fb8d21978e38
https://blog.csdn.net/justloveyou_/article/details/72783008
https://imququ.com/post/four-ways-to-post-data-in-http.html