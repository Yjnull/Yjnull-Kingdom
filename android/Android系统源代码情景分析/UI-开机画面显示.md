## Android 的开机画面显示过程

启动过程，最多可以出现三个画面。无论哪个画面，都是在帧缓冲区（frame buffer）的硬件设备上渲染的。

### 1. 第一个开机画面(内核启动)

frame buffer 在内核中有一个对应的驱动程序模块 fbmem

- fbmem 初始化： 在 /proc 目录下创建一个 fb 文件，接着又调用函数 register_chrdev 注册一个名为 fb 的字符设备，最后调用函数 class_create 在 /sys/class 目录下创建一个 graphics 目录，用来描述内核的图形系统。
- 内核的启动过程会被调用 register_framebuffer 函数：用来执行注册帧缓冲区硬件设备的操作
- 每一个被注册的帧缓冲区硬件设备在 /dev/graphics/ 目录下都有一个对应的设备文件 fb。例如第一个设备是 `/dev/graphics/fb0`

### 2. 第二个开机画面(init 进程启动)

- `fb_var_screeninfo` 中，xres、yres 描述屏幕所用的分辨率（可视分辨率）。xres_virtual 和 yres_virtual 描述虚拟分辨率

### 3. 第三个开机画面(系统服务启动)

- 当 SurfaceFlinger 服务启动的时候，会通过修改系统属性 ctl.start 来通知 init 进程启动应用程序 bootanimation，以便可以显示第三个开机画面。而当 System 进程将系统中的关键服务都启动起来之后，AMS 就会通知 SurfaceFlinger 来修改系统属性 ctl.stop 的值，以便通知 init 进程停止执行 bootanimation