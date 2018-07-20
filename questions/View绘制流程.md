## View 绘制流程

如需详细了解，请看 [参考] 链接。

### 1. measure
measure 用于测量 view 的宽 / 高

MeasureSpec

|模式 | 具体描述 | 应用场景 | 备注 | 思考 |
| -- | -----   | ------ | -- |   ---: |
| UNSPECIFIED |
| EXACTLY | 子视图代下必须在父视图指定的确切尺寸内 | match_parent 或 具体数值(如100dp) | 当为具体数值时，View的最终大小就是Spec指定的值，所以父控件可通过 MeasureSpec.getSize()直接得到子控件的尺寸 | |
|AT_MOST | 父视图为子视图指定一个最大尺寸，子视图必须确保自身和它是所有子视图可适应在该尺寸内 | 自适应大小(wrap_content) | 该模式下，父控件无法确定子View的尺寸，只能由子控件自身根据需求计算尺寸。该模式= 自定义视图需要实现测量逻辑的情况 | 也就是说该模式下，自定义View需要自行实现onMeasure方法，确保测量准确？ |

![measure](img/measure.png)


#### 1.1 问题
分析完后，知道了 MeasureSpec 的作用，以及 ViewGroup 中的 getChildMeasureSpec 方法。在分析这个方法的时候，知道了如果子 View 没有给出具体的 dp 大小，那么测量出的大小会等于父容器当前剩余空间的大小。
即 `int size = Math.max(0, specSize - padding);`
在看的时候没有任何问题，但是自己想着想着的时候，突然被绕进去了，想到一个问题
**Q1： 当父ViewGroup 为 match_parent，子 View 是 wrap_content 时，子 View 的大小应该是多少 ?**
**A1：** 
因为根据 getChildMeasureSpec 方法可以知道，这个时候子 View 的大小是等于 父容器剩余空间的大小的，可是当我们用 ImageView，TextView 等做例子时，会发现他们并不是填充整个父容器，而是有着**刚好适应内容的最小尺寸**的。这个我就晕了，为啥跟结论不对呢。这个疑惑一直困扰着我看源代码。 后面才发现，ImageView、TextView 他们是复写了 onMeasure 的，在里面针对 wrap_content 的情况，会给宽/高一个默认值，当然这个默认值是有特殊处理的，至于怎么处理，查看他们的源码即可。
到这里终于解决了这个疑惑，原来是通过指定一个默认大小 (宽 / 高) 解决的这个问题。

**TextView onMeasure 部分源码**
```
			// Check against our minimum width
            // width 在上面还会做各种处理,为了找到最小的 width
            width = Math.max(width, getSuggestedMinimumWidth());

            if (widthMode == MeasureSpec.AT_MOST) {
                width = Math.min(widthSize, width);
            }
```

### 2. layout

![layout](img/layout.png)

**下面的代码为 ViewGroup 中的 onLayout 伪码实现**
```
@Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        //1. 遍历子View：循环所有子View
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);

            //2. 计算当前子View的四个位置值
              //2.1 位置的计算逻辑
                //需自己实现，也是自定义View的关键

            //2.2 对计算后的位置值进行赋值
            int mLeft = left;
            int mTop = top;
            int mRight = right;
            int mBottom = bottom;

            //3. 根据上述4个位置的计算值，设置View的4个顶点，调用子View的layout
            child.layout(mLeft, mTop, mRight, mBottom);
        }
    }
```


### 3. draw

![draw](img/draw.png)

**问题 ： onDraw 只有在 View 里生效，ViewGroup 重写了也无用。**
学习了 draw 流程后立马尝试了下，却发现在 ViewGroup 中不生效，此时我的心是拨凉拨凉的，后面查阅资料发现是由于一个小细节。
`View.setWillNotDraw()`
这个方法在捣蛋。
- 这是 View 中的特殊方法，它的作用是：当一个 View 不需要进行绘制时，系统会进行相应优化。
- 设为 **false** 代表不启动该标志位，即 **需要进行绘制**；
- 设为 **true** 代表启动该标志位，即 **不需要进行绘制**。
- 在默认情况下：View 是设为 false， 而 ViewGroup 是设为 true 的，所以导致了ViewGroup 没生效。
- **应用场景**
 a. setWillNotDraw参数设置为true：当自定义View继承自 ViewGroup 、且本身并不具备任何绘制时，设置为 true 后，系统会进行相应的优化。
 b. setWillNotDraw参数设置为false：当自定义View继承自 ViewGroup 、且需要绘制内容时，那么设置为 false，来关闭 WILL_NOT_DRAW 这个标记位。


#### 4. 参考
https://www.jianshu.com/p/146e5cec4863
https://blog.csdn.net/yanbober/article/details/46128379



