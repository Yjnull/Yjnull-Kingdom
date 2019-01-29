# Using a media session

> 译：https://developer.android.com/guide/topics/media-apps/working-with-a-media-session

`media session` 与其管理的 `player` 并存。您应该在拥有 `media session` 和它所关联的 `player` 的 Activity 或 Service 的 `onCreate（）`方法中创建和初始化 `media session` 。

> **Note:** 编写媒体应用程序的最佳做法是使用 [media-compat library](https://developer.android.com/guide/topics/media-apps/media-apps-overview#compat-library)。在本节中，术语 `media session` 表示 MediaSessionCompat 的实例，`media controller` 表示 MediaControllerCompat 的实例。


## Initialize media session

一个新创建的 media session 没有任何功能。您必须通过执行以下步骤来进行初始化：

- 设置 flags，以便 `media session` 可以从 `media controller` 和 `media button` 接收回调。
- 创建并初始化 `PlaybackStateCompat` 实例并将其分配给 session。`playback state`的改变贯穿整个 session（原文：The playback state changes throughout the session），因此我们建议缓存 `PlaybackStateCompat.Builder` 以便重复使用。
- 创建 `MediaSessionCompat.Callback` 实例并将其分配给 session。

您应该在拥有 session 的 Activity 或 Service 的 onCreate（）方法中创建和初始化 `media session`。

为了在 app 新初始化（或 stopped）时 [media buttons](https://developer.android.com/guide/topics/media-apps/mediabuttons) 正常工作。其 PlaybackStateCompat 必须包含与 media button 发送的 intent 相匹配的播放操作。这就是为什么在初始化期间将 ACTION_PLAY 分配给 session state 的原因。更多信息，参考 [Responding to Media Buttons](https://developer.android.com/guide/topics/media-apps/mediabuttons)

## Maintain the playback state and metadata（保持播放状态和元数据）

这里有两个类代表 `media session` 的状态。

[PlaybackStateCompat](https://developer.android.com/reference/android/support/v4/media/session/PlaybackStateCompat) 类描述了播放器的当前操作状态。这包括：
- The transport state （播放器是否正在播放/暂停/缓冲等。请参阅 [getState()](https://developer.android.com/reference/android/support/v4/media/session/PlaybackStateCompat#getState()) ）
- 合适的时候会出现 错误代码和可选错误消息。（请参阅 [getErrorCode()](https://developer.android.com/reference/android/support/v4/media/session/PlaybackStateCompat#getErrorCode()) 和下面的 [States and errors](#States and errors)）
- The player position
- 可在当前状态下处理的有效 controller action

[MediaMetadataCompat](https://developer.android.com/reference/android/support/v4/media/MediaMetadataCompat) 类描述正在播放的元数据 ：
- The name of the artist, album, and track
- The track duration
- 在锁屏上显示的 Album 图稿。图像是 bitmap，最大尺寸为 320 x 320 dp （如果更大，则缩小）。
- [ContentUris](https://developer.android.com/reference/android/content/ContentUris) 的一个实例，指向更大版本的图稿。

播放器状态和元数据可以在 media session 的整个生命周期中改变。每次状态或元数据更改时，必须为每个类使用相应的 builder，`PlaybackStateCompat.Builder()` 或 `MediaMetadataCompat.Builder()` ，然后通过调用 [setPlaybackState( )](https://developer.android.com/reference/android/support/v4/media/session/MediaSessionCompat#setPlaybackState(android.support.v4.media.session.PlaybackStateCompat)) 或 [setMetaData( )](https://developer.android.com/reference/android/support/v4/media/session/MediaSessionCompat#setMetadata(android.support.v4.media.MediaMetadataCompat)) ，将新实例传递给 `media session` 。为了减少这些频繁操作的总体内存消耗，最好一次创建 builder 并在 session 的整个生命周期中重用它们。


## States and errors

请注意，`PlaybackState` 是一个对象，它包含 session 的 playback state （ [getState()](https://developer.android.com/reference/android/support/v4/media/session/PlaybackStateCompat#getState()) ） 的单独值，并在必要时包含相关的错误代码（ [getErrorCode()](https://developer.android.com/reference/android/support/v4/media/session/PlaybackStateCompat#getErrorCode()) ）。错误可能是致命或非致命的。

每当播放中断时，你应该生成一个 `fatal error` ： 并使用 [`setErrorMessage(int, CharSequence)`](https://developer.android.com/reference/android/support/v4/media/session/PlaybackStateCompat.Builder#setErrorMessage(int,%20java.lang.CharSequence)) 将传输状态设置为 `STATE_ERROR` 和指定关联的错误信息。只要播放被这个错误阻塞着，`PlaybackState` 就应该继续报告 `STATE_ERROR` 和这个错误信息。

当你的应用无法处理请求但可以继续播放时，会发生 `non-fatal`错误。 传输仍处于 “正常” 状态（例如 `STATE_PLAYING`），但 `PlaybackState` 持有了一个错误代码。例如，如果正在播放最后一首歌并且用户请求跳到下一首歌，则可以继续播放，但是你应该使用错误代码 `ERROR_CODE_END_OF_QUEUE` 创建一个新的 `PlaybackState`，然后调用 `setPlaybackState()` 。 连接到 session 的 Media Controller 将接收 [`onPlaybackStateChanged()`](https://developer.android.com/reference/android/support/v4/media/session/MediaControllerCompat.Callback#onPlaybackStateChanged(android.support.v4.media.session.PlaybackStateCompat)) 的回调，并向用户解释发生了什么。`non-fatal` 错误只应在其发生时报告一次。下次 session 更新 `PlaybackState` 时，不会再次设置相同的 `non-fatal` （除非响应新请求时发生了错误）。


## Media session lock screens

从 Android 4.0（API级别14）开始，系统可以访问 media session 的 `playback state` 和 `metadata`。这就是锁定屏幕可以显示 `media controls and artwork` 的方式。显示行为因 Android 版本而异。

#### Album artwork 

在 Android 4.0 （API level 14）及更高版本中，锁屏的背景显示你的专辑图片——但前提是 `media session metadata` 包含一个 background bitmap 。

#### Transport controls

在 Android 4.0 （API level 14）到 Android 4.4（API level 19）中，当 `media session` 处于活动状态且 `media session metadata` 包括一个 background bitmap 时，锁屏会自动显示 transport controls。

在 Android 5.0 （API level 21）或更高版本中，系统不会在锁屏上提供 transport controls 。相反，你应该使用 [`MediaStyle notification`](https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice#mediastyle-notifications) 来显示 transport controls。

#### Add custom actions

你可以使用 [`addCustomAction`](https://developer.android.com/reference/android/support/v4/media/session/PlaybackStateCompat.Builder#addCustomAction(android.support.v4.media.session.PlaybackStateCompat.CustomAction)) 添加自定义操作。例如，添加控制以实现 thumbs-up 操作。

```
stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
    CUSTOM_ACTION_THUMBS_UP, mResources.getString(R.string.thumbs_up), thumbsUpIcon)
    .setExtras(customActionExtras)
    .build());
```

See the [Universal Music Player](https://github.com/googlesamples/android-UniversalMusicPlayer/blob/f3154af7ac972ee9b7b1fd32ca3c935e02268a18/mobile/src/main/java/com/example/android/uamp/playback/PlaybackManager.java#L150-L171) for a complete example. 

You respond to the action with [`onCustomActioin()`](https://developer.android.com/reference/android/support/v4/media/session/MediaSessionCompat.Callback#onCustomAction(java.lang.String,%20android.os.Bundle)) 。

```
@Override
public void onCustomAction(@NonNull String action, Bundle extras) {
    if (CUSTOM_ACTION_THUMBS_UP.equals(action)) {
        ...
    }
}
```

Also see the [Universal Music Player](https://github.com/googlesamples/android-UniversalMusicPlayer/blob/f3154af7ac972ee9b7b1fd32ca3c935e02268a18/mobile/src/main/java/com/example/android/uamp/playback/PlaybackManager.java#L328-L346)


## Media session callbacks

`media session` 的主要回调方法是 `onPlay()`, `onPause()` 和 `onStop()` 。你可以在此处添加控制播放器的代码。

由于你在运行时实例化并设置 session's callback （在 `onCreate()` 中），你的应用可以定义 `alternative callbacks` 来使用不同的播放器，并根据设备或系统级别选择适当的 callback / player 组合。 你可以在不更改应用程序其余部分的情况下更改播放器。例如，您可以在 Android 4.1（API级别16）或更高版本上运行时使用 ExoPlayer，并在早期系统上使用 MediaPlayer。

除了控制播放器和管理媒体会话状态转换外，callback 还可以启用和禁用应用程序的功能，并控制其与其他应用程序和设备硬件交互的方式。 （请参阅 [Controlling Audio Output](https://developer.android.com/guide/topics/media-apps/volume-and-earphones)）。

`media session callback` 方法的实现取决于你的应用程序的架构。See the separate pages that describe how to use callbacks in [audio apps](https://developer.android.com/guide/topics/media-apps/audio-app/mediasession-callbacks) and [video apps](https://developer.android.com/guide/topics/media-apps/video-app/mediasession-callbacks), describe how the callbacks should be implemented for each kind of app.




