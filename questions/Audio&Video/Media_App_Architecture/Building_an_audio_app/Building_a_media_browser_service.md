# Building a media browser service（构建媒体浏览器服务）

> 译：https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice

你的应用必须在其 manifest 中声明 `MediaBrowserService` 并使用 intent-filter。你可以选择自己的 service name。在下面的实例中，它是 “MediaPlaybackService” 。

``` java
<service android:name=".MediaPlaybackService">
  <intent-filter>
    <action android:name="android.media.browse.MediaBrowserService" />
  </intent-filter>
</service>
```

> **Note:** `MediaBrowserService` 的推荐实现是 `MediaBrowserServiceCompat`。这是在 media-compat 支持库中定义的。在整个页面中，术语 “MediaBrowserService” 指的是 `MediaBrowserServiceCompat` 的实例。


## Initialize the media session

当服务收到 `onCreate（）`生命周期回调方法时，它应该执行以下步骤：
- 创建和 [初始化 media session](https://developer.android.com/guide/topics/media-apps/working-with-a-media-session#init-session)
- 设置 `media session` 的 callback
- 设置 `media session token`

下面的 `onCreate()` 代码演示了以下步骤：

``` java
public class MediaPlaybackService extends MediaBrowserServiceCompat {
    private static final String MY_MEDIA_ROOT_ID = "media_root_id";
    private static final String MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id";

    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create a MediaSessionCompat
        mMediaSession = new MediaSessionCompat(context, LOG_TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mMediaSession.setFlags(
              MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
              MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        mStateBuilder = new PlaybackStateCompat.Builder()
                            .setActions(
                                PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mMediaSession.setPlaybackState(mStateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
        mMediaSession.setCallback(new MySessionCallback());

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mMediaSession.getSessionToken());
    }
}
```

## Manage client connections

`MediaBrowserService` 有两种处理客户端连接的方法： `onGetRoot()` 控制对服务的访问， `onLoadChildren()` provides the ability for a client to build and display a menu of the `MediaBrowserService`'s content hierarchy.

#### Controlling client connections with onGetRoot()

`onGetRoot()` 方法返回内容层次结构的根节点。如果方法返回 null，则拒绝连接。

要允许客户端连接到你的服务并浏览其媒体内容，`onGetRoot()` 必须返回一个非空的 BrowserRoot，它是表示你的内容层次结构的 root ID 。

要允许客户端在不浏览媒体内容的情况下连接到 `MediaSession`，`onGetRoot()` 仍必须返回非空的 BrowserRoot，但 root ID 应该表示空的内容层次结构。

`onGetRoot()` 的典型实现可能如下所示：

``` java
@Override
public BrowserRoot onGetRoot(String clientPackageName, int clientUid,
    Bundle rootHints) {

    // (Optional) Control the level of access for the specified package name.
    // You'll need to write your own logic to do this.
    if (allowBrowsing(clientPackageName, clientUid)) {
        // Returns a root ID that clients can use with onLoadChildren() to retrieve
        // the content hierarchy.
        return new BrowserRoot(MY_MEDIA_ROOT_ID, null);
    } else {
        // Clients can connect, but this BrowserRoot is an empty hierachy
        // so onLoadChildren returns nothing. This disables the ability to browse for content.
        return new BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null);
    }
}
```

在某些情况下，您可能希望实施白/黑名单方案来控制连接。有关白名单的示例，请参阅 [Universal Android Music Player](https://github.com/googlesamples/android-UniversalMusicPlayer) 中的 [PackageValidator](https://github.com/googlesamples/android-UniversalMusicPlayer/blob/v1/mobile/src/main/java/com/example/android/uamp/PackageValidator.java) 类。

> **Note:** 你应该考虑提供不同的内容层次结构，具体取决于进行查询的客户端类型。特别是，Android Auto 限制了用户与音频应用的互动方式。有关更多信息，请参阅 [Playing Audio for Auto](https://developer.android.com/training/auto/audio/#build_hierarchy) 。你可以在连接时查看 `clientPackageName` 以确定客户端类型，并根据客户端（或 `rootHints`，如果有的话）返回不同的 `BrowserRoot`。

#### Communicating content with onLoadChildren()

在客户端连接之后，它可以通过重复调用 `MediaBrowserCompat.subscribe()` 来遍历内容层次结构，以构建 UI 的本地表示。`subscribe()` 方法将回调 service 的 `onLoadChildren()` ，该方法的参数中会返回 [MediaBrowser.MediaItem](https://developer.android.com/reference/android/media/browse/MediaBrowser.MediaItem)对象的 List。

每个 MediaItem 都有一个唯一的 ID，它是一个不透明的 token。当客户端想要打开子菜单或播放一个项目时，它会传递 ID。你的服务负责将 ID 与相应的菜单节点或内容项相关联。

一个简单的 `onLoadChildren()` 的实现：

``` java
@Override
public void onLoadChildren(final String parentMediaId,
    final Result<List<MediaItem>> result) {

    //  Browsing not allowed
    if (TextUtils.equals(MY_EMPTY_MEDIA_ROOT_ID, parentMediaId)) {
        result.sendResult(null);
        return;
    }

    // Assume for example that the music catalog is already loaded/cached.

    List<MediaItem> mediaItems = new ArrayList<>();

    // Check if this is the root menu:
    if (MY_MEDIA_ROOT_ID.equals(parentMediaId)) {
        // Build the MediaItem objects for the top level,
        // and put them in the mediaItems list...
    } else {
        // Examine the passed parentMediaId to see which submenu we're at,
        // and put the children of that menu in the mediaItems list...
    }
    result.sendResult(mediaItems);
}
```
> **Note:** `MediaBrowserService` 提供的 **MediaItem** 对象不应包含 icon bitmaps。在为每个 item 构建 [MediaDescription](https://developer.android.com/reference/android/media/MediaDescription) 时，通过调用 [setIconUri](https://developer.android.com/reference/android/media/MediaDescription.Builder#setIconUri(android.net.Uri)) 来使用 Uri。

有关如何实现 `onLoadChildren()` 的示例，请参阅 [MediaBrowserService](https://github.com/googlesamples/android-MediaBrowserService) 和 [Universal Android Music Player](https://github.com/googlesamples/android-UniversalMusicPlayer) 。

## The media browser service lifecycle

Android [Service](https://developer.android.com/guide/components/services) 的行为取决于它是 启动 还是 绑定 到一个或多个客户端。
























