## 与 Surface 相关的基础知识

### 1. 显示层和屏幕组成

- 屏幕位于三维坐标系中，Z轴从屏幕内指向屏幕外
- 每一个显示层有自己的属性。例如颜色、透明度、所处屏幕的位置、宽、高等。除了属性之外，还有自己对应的显示内容，也就是需要显示的图像。

Surface 系统提供了三种属性，一共四种不同的显示层

- eFXSurfaceNormal 属性，大多数 UI 界面使用的这个，它有两个模式：
  - Normal：通过 mView.draw(canvas) 就是这种模式，绝大多数 UI 采用这个
  - PushBuffer：这种模式对应视频播放、摄像机预览等场景。以摄像机为例，当摄像机运行时，来自 Camera 的预览数据将直接 push 到 Buffer 中。
- eFXSurfaceBlur：毛玻璃效果。
- eFXSurfaceDim：感觉像隔了一层深色玻璃。



### 2. FrameBuffer 和 PageFlipping

**FrameBuffer 介绍**

- Frame：帧，就是指一幅图像，在屏幕上看到的那副图像就是一帧
- Buffer：缓冲，就是一段存储区域，不过这个区域存储的是帧

FrameBuffer 就是一个存储**图形/图像帧数据**的缓冲。

FBD（FrameBuffer Device），Linux 平台的虚拟显示设备。这个虚拟设备将不同的硬件厂商的真实设备统一在一个框架下，这样应用层可以通过标准的接口进行 图形/图像的输入和输出。

FrameBuffer 中的 Buffer 就是通过 mmap 把显示设备的 **显存** 映射到用户空间，在这块缓冲上写数据，就相当于在屏幕上绘画了。

**PageFlipping 介绍**

图像是一帧一帧的，是有边界的。所以图像数据在生产/消费过程中，使用 PageFlipping（画面交换）的技术。

- 分配一个能容纳两帧数据的缓冲，前面一个叫 FrontBuffer，后面一个叫 BackBuffer。
- 消费者使用 FrontBuffer 中的数据，生产者用新数据填充 BackBuffer。
- 当需要更新显示时，BackBuffer 变成 FrontBuffer，FrontBuffer 变成 BackBuffer。如此循环，就总能显示最新的内容了。

说白了就是一个只有两个成员的帧缓冲队列，不停的出队、入队。



### 3. 图像混合

Flinger 混合的意思。SurfaceFlinger 系统支持软硬两个层面的图像混合。

- 软件层面的混合：例如使用 copyBlt 进行源数据和目标数据的混合。copyBlt 从名字上看是数据拷贝，它也可以由硬件实现，例如很多的 2D 图形加速就是将 copyBlt 改由硬件实现，以提高速度。
- 硬件层面的混合：使用 Overlay 系统提供的接口。它主要用于视频的输出，例如视频播放、摄像等。因为视频的内容往往变化很快，所以改用硬件进行混合效率会更高。

