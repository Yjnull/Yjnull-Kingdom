# Media app architec

> 译：https://developer.android.com/guide/topics/media-apps/media-apps-overview

本节介绍如何将 媒体播放应用 分割为 media controller（用于 UI）和 media session（用于实际播放）。
它描述了两种媒体应用程序架构：适用于音频应用程序的 client/server 设计，以及 适用于视频播放的单 Activity 设计。还说明了如何使媒体应用程序响应硬件控制并与使用音频输出流的其他应用程序协作。

## Player 和 UI

播放音频或视频的多媒体应用程序通常包含两部分：
- 将数字媒体呈现为视频或音频的 player
- 具有传输控制的 UI，用于运行 player 并显示播放状态。

![ui-and-player](../../img/ui-and-player.png)

在 Android 中，你可以从头开始构建自己的播放器，或者从以下选项中进行选择：
- [MediaPlayer](https://developer.android.com/guide/topics/media/mediaplayer) 为 **支持最常见音频/视频格式和数据源的简单播放器** 提供基本功能。
- [ExoPlayer](https://developer.android.com/guide/topics/media/exoplayer) 是一个开源库，可以公开较低级别的 Android 音频 API。 ExoPlayer 支持 MediaPlayer 中没有的高性能功能，如 DASH 和 HLS流。你可以自定义 ExoPlayer 代码，从而轻松添加新组件。 ExoPlayer 只能用于 Android 4.1 及更高版本。