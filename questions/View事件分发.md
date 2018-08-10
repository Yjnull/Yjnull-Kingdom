## View 事件分发机制
本文是自己看过一些资料后的总结，如要详细了解事件分发机制，请看**[参考]**内的文章。

### 一、事件分发基础认知
#### 1.1 当我们再谈论事件分发时，到底再谈论什么？
- 当用户触摸屏幕时，会产生点击事件 (Touch 事件)
*而 Touch 事件的相关细节（发生触摸的位置、时间等）都被封装成了 **MotionEvent** 对象*
所以当我们讨论事件分发时，实际是在讨论，是谁来处理这个 MotionEvent 对象，这个 MotionEvent 会不断传递。

#### 1.2 那么 MotionEvent 会在哪些对象之间传递呢？
- **Activity、ViewGroup、View**
**牢记：**Android 事件分发的流程是 **Activity -> ViewGroup -> View**
*即：1个点击事件发生后，事件会先传递到 Activity， 再传到 ViewGroup，最终再传到 View。*
所以当我们分析时，会首先从 Activity 开始分析

#### 1.3 事件分发过程中都会经历哪些方法来进行 MotionEvent 的传递？
- **dispatchTouchEvent()、onInterceptTouchEvent()、onTouchEvent()**
- dispatchTouchEvent：用于分发点击事件，当点击事件传递给当前 View 时，就会调用该方法。
- onInterceptTouchEvent：拦截某个事件，只有 ViewGroup 存在这个方法。在 ViewGroup 的 dispatchTouchEvent 内部调用。
- onTouchEvent：用于处理点击事件，在 dispatchTouchEvent 内部调用。

### 二、事件分发机制
#### 2.1 Activity 事件分发机制
- 当一个点击事件开始时，会进行如下传递 Activity.dispatchTouchEvent() -> PhoneWindow.superDispatchTouchEvent() -> DecorView.superDispatchTouchEvent() -> ViewGroup.dispatchTouchEvent()
- 我们可以看到在这个过程中，事件传到了 ViewGroup，到此 Activity 的分发过程基本结束了。其实在 DecorView 调用的时候，我们就可以结束 Activity 的传递了，根据源码，此时 DecorView 的 superDispatchTouchEvent 会调用 ViewGroup 的 dispatchTouchEvent。
- 当 DecorView 的 superDispatchTouchEvent 返回true 的话，则说明有子 View 消费了事件，则 Activity.dispatchTouchEvent 会返回 true，事件分发结束。如果返回 false 的话，会调用 Activity 的 onTouchEvent 方法来消费事件，这个时候无论onTouchEvent 返回什么，事件分发都结束了。


#### 2.2 ViewGroup 事件分发机制
- ViewGroup 每次事件分发，都会调用 onInterceptTouchEvent 询问是否拦截事件。
- 如果 ViewGroup.onIterceptTouchEvent 返回 true 代表拦截此事件。返回 true 的情况有两种（一、自己手动重写返回 true。 二、无 View接收事件，即点击空白处时）。
- 当 ViewGroup 拦截事件时，会调用 ViewGroup 父类的 dispatchTouchEvent 即 View.dispatchTouchEvent。 然后会自己处理该事件，**调用自身的 onTouch -> onTouchEvent ->performClick -> onClick**，这是 View 的调用过程，具体看 View 的事件分发。
- 当 ViewGroup 不拦截事件时，会循环子View，找到被点击的相应子 View 控件，然后调用子 View 控件的 dispatchTouchEvent，这个时候也就实现了事件从 ViewGroup 到 View 的传递。

##### 2.2.1 ViewGroup 怎么判断哪个子 View 被点击了
循环子 View 中，有这么一段代码，用来判断当前 View 是否被点击了
```
     //child可接受触摸事件：是指child是可见的(VISIBLE)；或者虽然不可见，但是位于动画状态。
	if (!canViewReceivePointerEvents(child)
            || !isTransformedTouchPointInView(x, y, child, null)) {
                 ev.setTargetAccessibilityFocus(false);
                 continue;
       }
```
这里面有两个方法用来判断， `canViewReceivePointerEvents` 和 `isTransformedTouchPointInView`
首先是判断当前 View 是否能接收到 PointerEvents ，如果不能接收到，那就直接 continue 循环下一个了。

如果上面判断是可以接受触摸事件的，那么就会去判断触摸坐标(x,y)是否在 child 的可视范围之内。
接下来具体看看 `isTransformedTouchPointInView`
```
	protected boolean isTransformedTouchPointInView(float x, float y, View child,
            PointF outLocalPoint) {
        // 首先 new 一个 float 数组，用来存放点击的 x、y 坐标
        final float[] point = getTempPoint();
        point[0] = x;
        point[1] = y;
        transformPointToViewLocal(point, child);
        //这是用来判断点击是否在 View 内的具体方法。
        final boolean isInView = child.pointInView(point[0], point[1]);
        if (isInView && outLocalPoint != null) {
            outLocalPoint.set(point[0], point[1]);
        }
        return isInView;
    }
```

```
	final boolean pointInView(float localX, float localY) {
        return localX >= 0 && localX < (mRight - mLeft)
                && localY >= 0 && localY < (mBottom - mTop);
    }
```
通过这个方法可以看到，View 是怎样判断的。
- localX、localY ： 是通过 ev.getX 和  ev.getY 拿到的，在 View 基础篇有讲过，这两个方法拿到的是相对当前 View 的坐标。
- mRight、mLeft、mBottom、mTop ：View 的四个顶点，具体可复习 [View 基础篇](https://blog.csdn.net/u014306335/article/details/81140580)。


#### 2.3 View 事件分发机制
- 当事件传递到 View 时，会调用 View.dispatchTouchEvent，在这个里面会首先判断 View.onTouch() 所返回的值。**注意是 onTouch 方法，并不是我们三大主要方法 onTouchEvent。**
- View.onTouch 返回 true， 则事件被消费，不会再往下传递，即会调用如下代码块，不会调用 onClick 了。
```
btn.setOnTouchListener(new OnTouchListener) {
	@Override
    public boolean onTouch(View v, MotionEvent evnet) {
    // 若在onTouch（）返回true，从而使得View.dispatchTouchEvent（）直接返回true，事件分发结束
    // 若在onTouch（）返回false，从而使得View.dispatchTouchEvent（）中跳出If，执行onTouchEvent(event)
    	return true;
    }
}
```

- View.onTouch 返回 false，则会去调用自身的 onTouchEvent()，这个方法里会具体判断当前是什么事件，从而做出相应操作，比如当前是 MotionEvent.ACTION_UP 时会调用 performClick()，这个方法里就会消费我们经常写的 setOnClickListener 里的 onClick 方法了，然后返回 true。
- View 的 onTouchEvent 是事件传递的最后一个地方了，如果该 View 是可点击的，则一定会返回 true，此时事件分发结束。  如果不可点击，会返回 false，此时事件就会回传到 ViewGroup 的dispatchTouchEvent，然后会自己处理该事件，**调用自身的 onTouch -> onTouchEvent ->performClick -> onClick**。 如果 ViewGroup 也返回 false，则会回传到 Activity 的dispatchTouchEvent，去执行 Activity 的 onTouchEvent 方法来消费事件。如此就完成了事件的分发传递。
- 最后根据上述分析，可得知 OnTouchListener 优先于 onClickListener， **即 onTouch() 的执行优先于 onClick()。**
- 最后的最后：若1个控件不可点击（即非enable），那么给它注册onTouch事件将永远得不到执行，具体原因看如下代码
```
// && 为短路与，即如果前面条件为 false，将不再往下执行
// 所以 onTouch() 能够执行需2个前提条件：
//    1. mOnTouchListener 的值不能为空
//    2. 当前控件必须是 enable 的。
				if (mOnTouchListener != null
                    && (mViewFlags & ENABLED_MASK) == ENABLED
                    && mOnTouchListener.onTouch(this, event)) {
                result = true;
            }
// 对于该类 非 enable 控件，若需监听它的 touch 事件，就必须通过在该控件中重写 onTouchEvent（）来实现
```


#### 参考
https://blog.csdn.net/carson_ho/article/details/54136311
http://www.gcssloop.com/customview/dispatch-touchevent-source
http://gityuan.com/2015/09/19/android-touch/