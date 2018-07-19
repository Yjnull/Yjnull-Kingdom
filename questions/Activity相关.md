## 一. activity生命周期
按照惯例，先上张 Activity 生命周期图

回答一个问题: Activity是什么？
Activity表示为具有用户界面的单一屏幕，可理解为"界面"。正常情况下，除了Window、Dialog和Toast，我们能见到的界面的确只有Activity。

![生命周期](img/activity.png "Activity生命周期")

### 1.1 activity生命周期分析
#### 1.1.1 生命周期概述
- **onCreate ()**
必须实现此回调，该回调在**系统创建Activity时被触发**。在这个方法中，应该初始化一些基本组件，例如创建视图并进行数据绑定等。最重要的是，调用 setContentView() 去加载界面布局资源。
另外，当 onCreate () 完成后，接下来的回调永远是 onStart ()。

- **onStart ()**
Activity 进入 "已启动" 状态，并且**对用户可见。但是还没有出现在前台，也不能用户进行交互**，这个回调做了最后的一些准备工作。

- **onResume ()**
**可见、出现在前台、可交互**。 系统在Activity开始与用户进行交互之前调用此回调。此时，Activity位于activity堆栈的顶部，并捕获用户的所有输入。
onPause () 回调总是跟在 onResume ()之后。

- **onPause ()**
当 activity **失去焦点并进入暂停状态时**，系统会调用。 例如，当用户点击 **"后退" 或 "最近"** 按钮时，会出现此状态。
当系统调用 onPause () 时，从技术上讲，Activity仍然是部分可见的，但通常表示用户正在离开Activity，Activity 将很快进入 Stopped 或者 Resumed 状态。
	如果用户期望 UI 更新，则处于 Paused 状态的 Activity 可以继续更新 UI。 例如 导航地图屏幕或媒体播放器播放，即使它们失去了焦点，用户也希望他们的 UI 继续更新。
    你不应该在 onPause 里去保存 应用程序或用户数据，进行网络调用，或执行数据库事务等耗重量级操作。
    一旦 onPause() 执行完毕，接下来的回调将 onStop() 或 onResume()，根据活动进入暂停状态以后会发生什么决定。

- **onStop ()**
当 activity **不再对用户可见时**，系统会调用。 这可能是因为 activity 被破坏，或者新 activity 正在开始，又或者 一个存在的activity正在进入 Resumed 状态并且正在覆盖已停止的 activity。

 如果这个 activity 准备返回和用户进行交互，则会回调 onRestart()。
 如果这个 activity 完全终止，则会回调 onDestroy()


- **onRestart ()**
当处于 **"Stopped"** 状态的 activity 即将重新启动时，系统会调用此方法。 onRestart（）会**还原** activity 在 stopped时的状态数据。

- **onDestroy ()**
系统在activity **销毁之前**调用此回调。
此回调是activity 收到的最后一个回调。在这里，可以做一些回收工作和最终的资源释放。

#### 1.1.2 生命周期具体说明
- 针对一个Activity，第一次启动，回调如下：onCreate -> onStart -> onResume。
- 当用户打开新的 activity 或者切换到桌面 或者按最近按钮时，回调如下：onPause -> onStop。
  特殊情况, 如果新Activity采用透明主题时，回调如下：onPause。
  
- 当用户再次回到原 activity 时，回调如下：onRestart -> onStart -> onResume。
- 当用户弹出一个对话框( AlertDialog )时，activity 生命周期不会发生变化。
- 当用户按back回退时，回调如下：onPause -> onStop ->onDestroy。

**问题：当前Activity 为A， 此时用户打开一个新的Activity B，那么 B 的 onResume 和 A 的 onPause 哪个先执行?**
1. 可自己动手试一试，答案是 旧 Activity A 先 onPause，然后 新 Activity B 再启动。其实很好理解，onResume代表着可见、可交互，如果旧的 Activity 不先 onPause，那岂不是会出现两个可见、可交互的界面，不就乱套了。
2. 看源码更能加深理解。


### 1.2 activity状态
只有三个状态是静态的，可以存在较长时间保持状态不变。（其他状态只是过渡状态，系统快速切换并切换到下一状态）

- **运行(Resumed)**
	- 当前 activity 处于栈顶，用户可以与它进行交互。（通常也被理解为 **"running"** 状态）
	- 此状态由 onResume() 进入，onPause() 退出

- **暂停(Paused)**
	- 当前 activity 仍然是可见的，但被另一个 activity 处在最上方，最上方的 activity 是半透明的，或者是部分覆盖整个屏幕。被暂停的 activity 不会再接收用户的输入。
	- 处于活着的状态 （Activity 对象存留在内存，保持着所有的 状态和成员信息，**仍然吸附**在 window manager）。
	- 当资源内存极度不足时，系统会杀掉该 activity 释放相应资源。
	- 此状态由 onPaues() 进入，退出可能是从 onResume() 重新唤醒软件，或者被 onStop() 杀掉。

- **停止(Stopped)**
	- 当前 activity 完全被隐藏，不被用户可见，可以认为是处于后台。
	- 处于活着的状态 （Activity 对象存留在内存，保持着所有的 状态和成员信息，**不再吸附**在 window manager）。
	- 由于对用户不再可见，只要有内存的需要，系统会杀掉该 activity 来释放相应资源。
	- 此状态由 onStop() 进入，退出是从 onRestart() 重新唤醒软件，或者被 onDestroy() 彻底死亡。其他状态（Created与 Started ）都是短暂的，系统快速执行那些回调函数并通过。

### 1.3 android进程优先级

- **前台进程**
一般情况是，在前台与用户进行交互的 activity，或与前台进程 绑定的 service。

- **可见进程**
处于 paused 状态，用户可见，但是不能进行交互。

- **服务进程**
如果一个进程中运行着 service，这个service 是通过 startService() 开启的，并且不属于上面两种高优先级的情况，那它就是一个服务进程。

- **后台进程**
处于 stopped 状态。

- **空进程**
如果一个进程不包含任何活跃的应用组件，则认为是空进程。


## 二. android任务栈
任务栈是一种“后进先出”的栈结构。
任务栈分为 **前台任务栈** 和 **后台任务栈** ，后台任务栈中的 activity 位于暂停状态。

- TaskAffinity 任务相关性， 这个参数标识了一个 Activity 所需要的任务栈的名字。
- 默认情况下，所有 Activity 所需的任务栈的名字为包名。
- 通过指定 TaskAffinity 可以为 Activity 指定新的任务栈的名字，当然必须不能和包名相同。
- TaskAfiinity 主要是和 singleTask 启动模式或者 allowTaskReparenting 属性配对使用。
- 在 AndroidManifest 文件中指定。
- 命令** adb shell dumpsys activity** 可导出 Activity 信息。


## 三. activity启动模式

- **standard(标准模式)**
系统默认模式，每启动一个 Activity 就会重新创建一个新的实例。
这种模式下， Activity A 启动了 Activity B，那么 B 就会进入到 A 所在的任务栈中。

- **singleTop(栈顶复用模式)**
如果新 Activity 已经位于栈顶，那么此 Activity 不会再重新创建，同时它的 onNewIntent 方法会被回调。

- **singleTask(栈内复用模式)**
只要 Activity 在一个栈中存在，那么多次启动此 Activity 都不会重新创建实例。

- **singleInstance(单实例模式, 加强的 singleTask 模式)**
除了具有 singleTask 模式的所有特性外，还加强了一点，那就是具有此种模式的 Activity 只能单独的位于一个任务栈中。

**例1**
任务栈（com.yjnull.slowdev4android）有个 ExampleActivity ，启动模式为standard。 
任务栈（com.yy.task1）有个 ThreeActivity，启动模式为 singleInstance。 
启动ExampleActivity，然后启动ThreeActivity，然后两个互相启动，任务栈如下截图。 
![activity_task1](img/activity_task1.png)

**例2**
![启动模式](img/launchmode.png)



## 四. IntentFilter 的匹配规则
隐式启动 Activity 需要 Intent 能够匹配目标组件的 IntentFilter中所设置的过滤信息，如果不匹配则无法启动目标组件。
IntentFilter 的过滤信息有 action、category、data

- **action：**
1. action是一个字符串 区分大小写
2. 当过滤规则中有 action 时，那么只要 Intent 中的 action 能够和过滤规则中的任何一个相同即可匹配成功。需注意：如果Intent 没有指定 action，将匹配失败。
3. 也就是说，当过滤规则有 action 时，Intent 中必须存在 action。

- **category：**
1. category 和 action 不同，它不强制要求 Intent 中必须含有 category。
2. 如果 Intent 中没有 category，那么可以匹配成功。
3. 如果 Intent 中有 category，那么不管有几个 category，都必须和过滤规则中的 category 相同才能匹配成功。
4. 为什么不设置 category 也可以匹配成功，因为 startActivity 时会默认为 Intent 加上 "android.intent.category.DEFAULT" 这个 category。
5. 所以为了 activity 能够接收隐式调用，必须在intent-filter 中指定 "android.intent.category.DEFAULT" 这个 category。

- **data**
1. data 由两部分组成：mimeType 和 URI。
2. mimeType 指媒体类型：如 image/jpeg、video/*等。
3. URI 结构： `<schema>://<host>:<port>/[<path>|<pathPrefit>|<pathPattern>]`
4. 如果没有指定 URI ，则 URI 的默认值为 content 或 file
5. 例如指定 mimeType 为 **image/png**，未指定 URI 。 则如下代码可匹配过滤规则
`intent.setDataAndType(Uri.parse("file://abc"), "image/png");` 
或者
`intent.setDataAndType(Uri.parse("content://abc"), "image/png");`

- **总结**
1. 隐式启动 Activity 时，IntentFilter 一定要指定 **"android.intent.category.DEFAULT"** 这个 category。
2. action、category、data ,如果匹配了 action，那么其余两个也得匹配成功才能找到 Activity。
3.  如果只匹配 data，那么 action 不指定也可以运行成功，不会返回指定 Activity，而是返回ResolverActivity，让你选择默认程序运行。















