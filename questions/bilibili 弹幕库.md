# 烈焰弹幕使源码分析

## 技能掌握

1. View 绘制流程

2. TreeSet、TreeMap、SortedSet 实现原理
3. Renderer、Displayer 概念
4. SurfaceView 掌握

**终极目标：Android 应用程序 UI 架构**



## 1. 简单使用

1. 定义一个 DanmakuView，它继承自 View 并实现了 IDanmakuView，IDanmakuViewController 接口。
2. 创建 DanmakuContext，给 Context 设置相应的属性，比如弹幕的风格、弹幕的大小、弹幕的速度等。
3. 创建一个 BaseDanmakuParser 用来解析弹幕资源。
4. 最后调用 DanmakuView.prepare（Parser，Context） 即可。

## 2. 问题

- 特殊弹幕
- 自定义弹幕样式

## 3. DanmakuView

IDanmuView 如何驱动数据，以弹幕的形式滚动起来？通过 DrawHandler

- 可以在 prepare() 之前通过 setDrawingThreadType() 来调整 draw Thread 优先级
- Displayer 显示器，Render 渲染器

DrawHandler(任务分发器) 应该会通过 UpdateThread 驱动 Displayer 和 DrawTask

1. **prepare：**记录 mTimeBase，创建 DrawTask，如果创建成功则回调 Callback.prepared()



1. 把循环先跑起来。通过每一帧的监听，去 drawDanmakus()
2. problem 绘制一卡一卡的
3. ==明日== problem SEEK_POS 和 RESUME 重复？





```mermaid
sequenceDiagram
    MainActivity ->> MainActivity : 创建 DanmakuView，设置 Callback
    MainActivity ->> + DanmakuView : danmakuView.prepare(parser, config)
    DanmakuView ->> + DrawHandler : handler.prepare(parser, config)
    DrawHandler ->> DrawHandler : createDrawTask()
    DrawHandler ->> + DrawTask : drawTask.prepare()
    DrawTask ->> DrawTask : loadDanmakus(mParser)
    DrawTask -->> - DrawHandler : mTaskListener.ready()
    DrawHandler -->> - DanmakuView : mCallback.prepared()
    DanmakuView -->> - MainActivity : mCallback.prepared()
    
    MainActivity ->> DanmakuView : 弹幕库准备好了，mDanmakuView.start()
    DanmakuView ->> DrawHandler : 给 handler 发送 START 消息
    DrawHandler ->> DrawHandler : 发送 UPDATE 消息，调用 updateInChoreographer()
    DrawHandler ->> DanmakuView :  在 updateInChoreographer 方法内调用 drawDanmakus()
    DanmakuView ->> + DanmakuView : lockCanvas，回调 onDraw()，阻塞
    DanmakuView ->> DrawHandler : handler.draw(canvas)
    DrawHandler ->> DrawTask : drawTask.draw(mDisp)
    DrawTask ->> IRenderer : mRenderer.draw(disp, danmakus, time, state)
    IRenderer ->> IDanmakus : danmakus.forEachSync(mConsumer)
    loop 遍历弹幕集合
        IDanmakus ->> DanmakuRenderer.Consumer : accept(drawItem)
        DanmakuRenderer.Consumer ->> BaseDanmaku : measure, layout(由 Retainer 完成), draw
        BaseDanmaku ->> IDisplayer : measure, layout, draw
        IDisplayer ->> BaseCacheStuffer : 完成真正的 draw
        BaseCacheStuffer -->> IDisplayer : canvas 绘制结束
        IDisplayer -->> BaseDanmaku : 返回 IRenderer 的三种状态之一
        BaseDanmaku -->> DanmakuRenderer.Consumer : 返回 IRenderer 的三种状态之一
        DanmakuRenderer.Consumer ->> DanmakuRenderer.Consumer : 根据状态值添加缓存
        DanmakuRenderer.Consumer -->> IDanmakus: 返回 ACTION_CONTINUE 
    end
    IDanmakus -->> IRenderer : forEachSync 结束
    IRenderer -->> DrawTask : 渲染器绘制结束，返回 RenderingState
    DrawTask -->> DrawHandler : 返回 RenderingState
    DrawHandler -->> DanmakuView : handler.draw(canvas) 方法结束
    DanmakuView -->> DanmakuView : unlockCanvasAndPost 解锁
    
```



------
git rebase 测试:
当前在 feat1，提交 1。
当前在 feat1，提交 2。









