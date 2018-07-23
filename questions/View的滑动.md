## View 的滑动

### 一、瞬时滑动
#### 1. View 本身提供的 scrollTo / scrollBy 方法
- scrollTo 基于传递参数的 **绝对滑动**
- scrollBy 基于当前位置的 **相对滑动**，实际上也是调用了 scrollTo 方法。
- **缺点：只能滑动View的内容，并不能滑动View本身**


#### 2. 通过动画给 View 施加平移效果实现滑动
#### 3. 改变 View 的 LayoutParams 使得 View 重新布局实现滑动 

### 二、弹性滑动





