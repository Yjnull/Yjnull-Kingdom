## Retrofit2 源码解析

Retrofit：一个 Restful 设计风格的 HTTP 网络请求框架的封装。基于 OkHttp

> A type-safe HTTP client for Android and Java


### 0. 基本使用
1、Retrofit 将我们的 HTTP API 转换成一个 接口形式。所以我们第一步定义一个 interface

```
public interface GitHubService {
	@GET("user/{user}/repos")
    Call<List<Repo>> listRepos(@Path("user") String user);
}
```

2、然后构建一个 Retrofit，通过 create 方法生成 GitHubService 的一个实现。
```
Retrofit retrofit = new Retrofit.Builder()
	.baseUrl("https://api.github.com/")
    .build();

GitHubService service = retrofit.create(GitHubService.class);
```

3、调用 listRepos 拿到 Call 对象可以做同步或异步请求。
```
Call<List<Repo>> repos = service.listRepos("octocat");
```


### 1. 所见分析
在上述基本使用中我们对这些名词并不熟悉，
- @GET、@Path
- Retrofit、Retrofit.Builder
- Call< List < Repo > >

#### 1.1 Retrofit
首先看看 Retrofit 吧，这个类里面有7个实例变量。我们根据类型和变量名先猜猜是干什么用的，留个大体印象即可。
```
  // 一个线程安全的、支持高效并发的HashMap，Key 是 Method，Value 是 ServiceMethod。Method 我们能猜到应该就是上面接口中定义的 listRepos，而这个方法中有很多注解，@GET、@Path 啥的，那这个 ServiceMethod 很有可能是这个方法的封装。而变量名带个 Cache 说明，会把这个 Method 对应的 ServiceMethod 缓存起来。
  private final Map<Method, ServiceMethod<?, ?>> serviceMethodCache = new ConcurrentHashMap<>();

  // 如果你不熟悉 okhttp 也没关系，想必你也知道 Retrofit 就是基于 OkHttp 的封装，那这个 Call.Factory，明显就是 Call 的工厂类。至于 Call 是干嘛的，负责创建 HTTP 请求，HTTP 请求被抽象为了 okhttp3.Call 类，它表示一个已经准备好，可以随时执行的 HTTP 请求；
  final okhttp3.Call.Factory callFactory;
  // 这个很好理解了，就是上面 基本使用 中的 baseUrl，可是这是个 HttpUrl 类型的，我们传的可是 String 类型的呀，那估计是通过 Builder 做了处理的。
  final HttpUrl baseUrl;
  // Converter 根据字面意思可得 这应该是个转换器，用于把我们的响应转换成特定的格式
  final List<Converter.Factory> converterFactories;
  // CallAdapter 根据字母意思，难道是对 Call 的一个适配？
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
- defaultCallbackExecutor ：这个方法返回的是个 Executor ，我们想到 Retrofit 正好有个 Executor 类型的变量，那么想必就是它了，它是 MainThreadExecutor 类型的，内部采用 handler 执行任务。
- defaultCallAdapterFactory ：这个方法返回的是个 CallAdapter.Factory，Retrofit 成员变量中也正好有个 CallAdapter.Factory 类型的变量，所以说这个 Platform 很重要嘛，跟我们 Retrofit 类中的两个成员变量都有重大的关系。这里最终返回的是个 ExecutorCallAdapterFactory ，话说我们一开始就不知道这个 CallAdapter 是什么，更不用说这个 Factory 了，那我们先看看这个 ExecutorCallAdapterFactory 吧。

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

到这里，我们的 Retrofit 就构建完成了。


**参考**
https://blog.csdn.net/justloveyou_/article/details/72783008