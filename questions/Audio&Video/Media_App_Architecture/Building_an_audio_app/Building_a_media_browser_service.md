# Building a media browser service

你的应用必须在其 manifest 中声明 `MediaBrowserService` 并使用 intent-filter。你可以选择自己的 service name。在下面的实例中，它是 “MediaPlaybackService” 。

``` java
<service android:name=".MediaPlaybackService">
  <intent-filter>
    <action android:name="android.media.browse.MediaBrowserService" />
  </intent-filter>
</service>
```

> **Noto:** `MediaBrowserService` 的推荐实现是 `MediaBrowserServiceCompat`。这是在 media-compat 支持库中定义的。在整个页面中，术语 “MediaBrowserService” 指的是 `MediaBrowserServiceCompat` 的实例。


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