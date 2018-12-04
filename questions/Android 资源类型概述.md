Android 资源类型概述
> 译自：[Resource types overview](https://developer.android.com/guide/topics/resources/available-resources)

以下描述了您可以在 **项目资源目录 (res/) ** 中提供的特定类型的 app resource 的 **用法、格式和语法。**


- **Animation Resources**
	> 定义动画资源。
	> Property animation (属性动画) 保存在 `res/animator/` 中，通过 `R.animator` 访问。
	> Tween animations （补间动画） 保存在 `res/anim/` 中，通过 `R.anim` 访问。
	> Frame animations （帧动画） 保存在 `res/drawable/` 中，通过 `R.drawable` 访问。

- **Color State List Resource**
	> 定义基于 View 状态更改的颜色资源。
	> 保存在 `res/color/` 中，通过 `R.color` 访问。

- **Drawable Resources**
	> 使用 bitmaps 或者 XML 定义各种图形。
	> 保存在 `res/drawable/` 中，通过 `R.drawable` 访问。

- **Layout Resource**
	> 给 UI 定义布局
	> 保存在 `res/layout/` 中，通过 `R.layout` 访问。

- **Menu Resource**
	> 定义 app 菜单的内容
	> 保存在 `res/menu/` 中，通过 `R.menu` 访问。

- **String Resources**
	> 定义 String , String arrays 和 plurals。
	> 保存在 `res/values/` 中，通过 `R.string`, `R.array`, `R.plurals` 访问。

- **Style Resource**
	> 定义 UI 元素的外观和格式。
	> 保存在 `res/values/` 中，通过 `R.style` 访问。

- **Font Resources**
	> 定义字体系列，并在 XML 中包含自定义字体。
	> 保存在 `res/font/` 中，通过 `R.font` 访问。

- **More Resource Types**
	> 将其他 原始值 定义为 静态资源，包括以下内容：
	- Bool
	- Color
	- Dimension
	- ID
	- Integer
	- Integer Array
	- Typed Array










