
#Supported media formats (支持的媒体格式)

> **译:** https://developer.android.com/guide/topics/media/media-formats

> **media codec**（媒体编解码器）
> **container**（容器）
> **network protocol support**（网络协议支持）

本文档描述了 Android 平台提供的 **media codec**、**container** 和 **network protocol support**。

作为应用程序开发人员，您可以使用任何 Android 设备上提供的任何 media codec，包括  Android 平台提供的设备和 device-specific 的设备。**但是，最佳做法是使用与设备无关的媒体编码配置文件。**

下表描述了 Android 平台内置的媒体格式支持。不保证在所有 Android 平台版本上都可用的 codec 会在括号中注明，例如：（Android 3.0+）。请注意，任何给定的移动设备都可能支持表中未列出的其他格式或文件类型。

[Android Compatibility Definition](https://source.android.com/compatibility/android-cdd#5_multimedia_compatibility) 中的 Section 5 指定了设备必须支持的媒体格式，以便与 Android 8.1 兼容。

## Audio support
#### Audio formats and codecs

| Format/Codec | Encoder | Decoder | Details | Supported File Type(s) / Container Formats |
| --- | --- | --- | --- | --- |
| AAC LC  | • | • | Support for mono/stereo/5.0/5.1 content with standard sampling rates from 8 to 48 kHz. | • 3GPP (.3gp)<br>• MPEG-4 (.mp4, .m4a)<br>• ADTS raw AAC (.aac, decode in Android 3.1+, encode in Android 4.0+, ADIF not supported)<br>• MPEG-TS (.ts, not seekable, Android 3.0+) |
| 待续 | 待续 | 待续 | 待续 | 待续 |

## Video support
#### Video formats and codecs

| Format/Codec | Encoder | Decoder | Details | Supported File Type(s) / Container Formats |
| --- | --- | --- | --- | --- |
| H.263  | • | • | Support for H.263 is optional in Android 7.0+ | • 3GPP (.3gp)<br>• MPEG-4 (.mp4)|
| 待续 | 待续 | 待续 | 待续 | 待续 |

#### Video encoding recommendations
The table below lists the Android media framework video encoding profiles and parameters recommended for playback using the H.264 Baseline Profile codec. The same recommendations apply to the Main Profile codec, which is only available in Android 6.0 and later.

| | SD (Low quality) | SD (High quality) | HD 720p (N/A on all devices) |
| --- | --- | --- | --- |
| Video resolution 分辨率 | 176 x 144 px | 480 x 360 px | 1280 x 720 px |
| Video frame rate 帧率 | 12 fps | 30 fps | 30 fps |
| Video bitrate 比特率 | 56 Kbps | 500 Kbps | 2 Mbps |
| Audio codec | AAC-LC | AAC-LC | AAC-LC |
| Audio channels | 1 (mono) | 2 (stereo) | 2 (stereo) |
| Audio bitrate | 24 Kbps | 128 Kbps | 192 Kbps |

#### Video decoding recommendations（视频解码建议）

设备实现必须支持动态视频分辨率和帧速率切换，通过同一流中的标准 Android API 实时为所有 VP8，VP9，H.264 和 H.265 编解码器提供支持，并达到每个设备编解码器支持的最大分辨率。

支持 Dolby Vision 解码器的实现必须遵循以下准则：
- 提供支持杜比视界的提取器。
- 在设备屏幕或标准视频输出端口（例如 HDMI）上正确显示 Dolby Vision 内容。
- 将向后兼容的基础库（如果存在）的 track index 设置为与杜比视觉层的 track idnex 相同。

#### Video Streaming requirements (视频流要求)

对于通过 HTTP 或 RTSP 流式传输的视频内容，还有其他要求：
- 对于 3GPP 和 MPEG-4 containers，`moov atom` 必须在任何 `mdat atom` 之前，但必须接替 `ftyp atom`。
- For 3GPP，MPEG-4，和 WebM containers，对应于相同时间偏移的音频和视频样本可以相隔不超过 500KB。为了最大限度地减少音频/视频漂移，请考虑以较小的块大小交错音频和视频。


## Image support

| Format/Codec | Encoder | Decoder | Details | Supported File Type(s) / Container Formats |
| --- | --- | --- | --- | --- |
| BMP  |  | • |  | BMP (.bmp) |
| GIF  |  | • |  | GIF (.gif) |
| JPEG  | • | • | Base+progressive  | JPEG (.jpg) |
| PNG  | • | • |  | PNG (.png) |
| WebP  | •<br>(Android 4.0+)<br>(Lossless, Transparency, Android 4.2.1+) | •<br>(Android 4.0+)<br>(Lossless, Transparency, Android 4.2.1+) |  | WebP (.webp) |
| HEIF  |  | •<br>(Android 8.0+) | | HEIF (.heic; .heif) |


## Network protocols

音频和视频播放支持以下网络协议：

- RTSP（RTP，SDP）
- HTTP/HTTPS progressive streaming
- HTTP/HTTPS live streaming draft protocol:
	- MPEG-2 TS media files only
	- Protocol version 3 (Android 4.0 and above)
	- Protocol version 2 (Android 3.x)
	- Not supported before Android 3.0

> **Note:** HTTPS is not supported before Android 3.1

