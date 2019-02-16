# Building a media browser client（构建媒体浏览器客户端）

> [GitHub 链接](https://github.com/Yjnull/android-interview-planing/blob/master/questions/Audio%26Video/Media_App_Architecture/Building_an_audio_app/Building_a_media_browser_client.md)
> 译：https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowser-client

要完成 client / server 设计，你必须构建一个 Activity 组件，它包含你的 UI 代码、关联的 MediaController 和 MediaBrowser。

MediaBrowser 执行两个重要的功能: 它连接到 MediaBrowserService，并在连接时为你的 UI 创建 MediaController。

> **Note:** MediaBrowser 的推荐实现是 [MediaBrowserCompat](http://) ，该类被定义在 [Media-Compat support library](http://) 中。在本文中 术语 “MediaBrowser” 指的是 MediaBrowserCompat 的实例。

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

















