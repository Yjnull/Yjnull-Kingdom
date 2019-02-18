# Building a media browser client（构建媒体浏览器客户端）

> [GitHub 链接](https://github.com/Yjnull/android-interview-planing/blob/master/questions/Audio%26Video/Media_App_Architecture/Building_an_audio_app/Building_a_media_browser_client.md)
> 译：https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowser-client

要完成 client / server 设计，你必须构建一个 Activity 组件，它包含你的 UI 代码、关联的 MediaController 和 MediaBrowser。

MediaBrowser 执行两个重要的功能: 它连接到 MediaBrowserService，并在连接时为你的 UI 创建 MediaController。

> **Note:** MediaBrowser 的推荐实现是 [MediaBrowserCompat](https://developer.android.com/reference/android/support/v4/media/MediaBrowserCompat) ，该类被定义在 [Media-Compat support library](https://developer.android.com/topic/libraries/support-library/features#v4-media-compat) 中。在本文中 术语 “MediaBrowser” 指的是 MediaBrowserCompat 的实例。

## Connect to the MediaBrowserService

创建客户端 Activity 后，它将连接到 MediaBrowserService 。有一点需要提醒，修改 Activity 的生命周期回调，如下所示：

- `onCreate()` 构造一个 MediaBrowserCompat，传递你已定义的 MediaBrowserService 和 MediaBrowserCompat.ConnectionCallback 。

- `onStart()` 连接到 MediaBrowserService。如果连接成功，自己需要手动在 ConnectionCallback 的 `onConnected()` 回调里去创建 media controller，将其链接到 media session，将 UI 控件链接到 MediaController，and registers the controller to receive callbacks from the media session.

- `onResume()` 设置音频流，以便你的应用响应设备上的音量控制

- `onStop()` 断开 MediaBrowser，并在 Activity stop 时取消注册 MediaController.Callback 。

```java
public class MediaPlayerActivity extends AppCompatActivity {
  private MediaBrowserCompat mediaBrowser;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // ...
    // Create MediaBrowserServiceCompat
    mediaBrowser = new MediaBrowserCompat(this,
      new ComponentName(this, MediaPlaybackService.class),
        connectionCallbacks,
        null); // optional Bundle
  }

  @Override
  public void onStart() {
    super.onStart();
    mediaBrowser.connect();
  }

  @Override
  public void onResume() {
    super.onResume();
    setVolumeControlStream(AudioManager.STREAM_MUSIC);
  }

  @Override
  public void onStop() {
    super.onStop();
    // (see "stay in sync with the MediaSession")
    if (MediaControllerCompat.getMediaController(MediaPlayerActivity.this) != null) {
      MediaControllerCompat.getMediaController(MediaPlayerActivity.this).unregisterCallback(controllerCallback);
    }
    mediaBrowser.disconnect();

  }
}
```

## Customize MediaBrowserCompat.ConnectionCallback

当你的 Activity 构造 MediaBrowserCompat 时，你必须创建 ConnectionCallback 的实例。修改其 `onConnected` 方法，检索来自`MediaBrowserService` 的 media session token，并使用该 token 创建 `MediaControllerCompat`。

使用便捷方法 [MediaControllerCompat.setMediaController()](https://developer.android.com/reference/android/support/v4/media/session/MediaControllerCompat#setMediaController(android.app.Activity,%20android.support.v4.media.session.MediaControllerCompat)) 来保存 controller 的引用。这样可以处理 [media buttons](https://developer.android.com/guide/topics/media-apps/mediabuttons) 。它还允许你在构建 transport controls 时调用 [MediaControllerCompat.getMediaController()](https://developer.android.com/reference/android/support/v4/media/session/MediaControllerCompat#getMediaController()) 来获取 controller。

下面的代码示例展示了如何修改 `onConnected()` 方法。

```java
private final MediaBrowserCompat.ConnectionCallback connectionCallbacks =
  new MediaBrowserCompat.ConnectionCallback() {
    @Override
    public void onConnected() {

      // Get the token for the MediaSession
      MediaSessionCompat.Token token = mediaBrowser.getSessionToken();

      // Create a MediaControllerCompat
      MediaControllerCompat mediaController =
        new MediaControllerCompat(MediaPlayerActivity.this, // Context
        token);

      // Save the controller
      MediaControllerCompat.setMediaController(MediaPlayerActivity.this, mediaController);

      // Finish building the UI
      buildTransportControls();
    }

    @Override
    public void onConnectionSuspended() {
      // The Service has crashed. Disable transport controls until it automatically reconnects
    }

    @Override
    public void onConnectionFailed() {
      // The Service has refused our connection
    }
  };
```

## Connect your UI to the media controller

在上面的 `ConnectionCallback` 示例代码中，包含对 `buildTransportControls()` 的调用以充实你的 UI。你需要为控制播放器的 UI 元素设置 `onClickListeners`。为每个元素选择适当的 [MediaControllerCompat.TransportControls](https://developer.android.com/reference/android/support/v4/media/session/MediaControllerCompat.TransportControls) 方法。

你的代码看起来像这样，每个按钮都有一个 onClickListener：

```java
void buildTransportControls()
{
  // Grab the view for the play/pause button
  playPause = (ImageView) findViewById(R.id.play_pause);

  // Attach a listener to the button
  playPause.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      // Since this is a play/pause button, you'll need to test the current state
      // and choose the action accordingly

      int pbState = MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getPlaybackState().getState();
      if (pbState == PlaybackStateCompat.STATE_PLAYING) {
        MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().pause();
      } else {
        MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().play();
      }
  });

  MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(MediaPlayerActivity.this);

  // Display the initial state
  MediaMetadataCompat metadata = mediaController.getMetadata();
  PlaybackStateCompat pbState = mediaController.getPlaybackState();

  // Register a Callback to stay in sync
  mediaController.registerCallback(controllerCallback);
}
}
```

The TransportControls methods send callbacks to your service's media session. Make sure you've defined a corresponding [MediaSessionCompat.Callback](https://developer.android.com/reference/android/support/v4/media/session/MediaSessionCompat.Callback) method for each control.


## Stay in sync with the media session

UI 应显示 media session 的当前状态，如其 PlaybackState 和 Metadata 所述。当你创建 transport controls 时，你可以获取 session 的当前状态，在 UI 中显示它，并根据状态及其可用的操作 启用和禁用 transport controls。

为了在每次状态或元数据发生改变时，能从 media session 接收回调。请定义一个 [MediaControllerCompat.Callback](http://) ：

```java
MediaControllerCompat.Callback controllerCallback =
  new MediaControllerCompat.Callback() {
    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {}

    @Override
    public void onPlaybackStateChanged(PlaybackStateCompat state) {}
  };
```

在构建 transport controls 注册回调（请参阅上面的 `buildTransportControls()`），并在 Activity stop 时取消注册（在 Activity 的 `onStop()` 生命周期方法中）





