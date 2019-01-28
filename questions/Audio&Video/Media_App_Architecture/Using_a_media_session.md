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

## Maintain the playback state and metadata
