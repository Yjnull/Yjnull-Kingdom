# Groovy 简介

### 前提知识
- Groovy 语句可以不用分号结尾
- Groovy 支持动态类型
    即定义变量的时候可以不指定其类型。 变量定义使用 def 关键字。但 def 不是必须的。
    函数定义时，参数的类型也可以不指定。
    函数的返回值也可以是无类型的，但必须使用 def 关键字

- 函数返回值：Groovy 的函数里，如果不使用 return 语句，则函数里最后一句代码的执行结果就是返回值。

- Groovy 对字符串的支持强大
    1. 单引号中的内容严格对应 Java 中的 String，不对 $ 符号进行转义
    2. 双引号""的内容则和脚本语言的处理有点像，如果字符中有$符号的话，则它会**$表达式**先求值。
    3. 三个引号'''xxx'''中的字符串支持随意换行

- Groovy 中函数调用的时候可以不加括号，比如  `println("test") --> println "test"`
    注意：虽然写代码的时候，对于函数调用可以不带括号，但是 Groovy 经常把属性和函数调用混淆
    所以，调用函数要不要带括号，个人意见是如果这个函数是 Groovy API 或者 Gradle API 中比较常用的，比如 println，就可以不带括号。否则还是带括号。Groovy 自己也没有 太好的办法解决这个问题，只能兵来将挡水来土掩了。


### Groovy 中的数据类型
1. Groovy 中所有事物都是对象，所以 java 中的基本数据类型，在 Groovy 中都是对应的包装数据类型。比如 int 对应 Integer

2. Groovy 中的容器类就三种：
  - List：链表，底层对应 Java 中的 List 接口，一般用 ArrayList 实现
  - Map：底层对应 Java 中的 LinkedHashMap
  - Range：范围，List 的一种扩展

3. 闭包 Closure，非常重要的一个数据类型或者说一个概念
  闭包的定义格式是：
  
  ```groovy
  def xxx = {parameters -> code} // 或者
  def xxx = {无参数, 纯 code}  // 这种 case 不需要 -> 符号
  ```
  
  例子:
  ```groovy
  def aClosure = {//闭包是一段代码，所以需要用花括号括起来
     String param1, int param2 -> //这个箭头很关键。箭头前面是参数定义，箭头后面是代码
     println "this is code" //这是代码，最后一句是返回值，
     //也可以使用 return，和 Groovy 中普通函数一样 
   }
  ```

**闭包使用中的注意点**
1. 省略圆括号：当函数的最后一个参数是闭包的话，可以省略圆括号
2. 如何确定 Closure 的参数，查询 [api](http://www.groovy-lang.org/api.html)


### 脚本类、文件 IO 和 XML 操作

#### 脚本类
Groovy 会把 xxx.groovy 文件中的内容转换成一个 Java 类。
可以执行 `groovyc -d 目标目录 test.groovy`

#### 文件 IO
查看 `~/codes/groovy_codes/src` 目录下的 `file_test.groovy` 操作

#### XML 操作
例子：获取 AndroidManifest.xml 中的版本号（versionName）
```groovy

def manifest = new XmlSlurper().parse("AndroidManifest.xml")
println manifest['@android:versionName']
// 或者
println manifest.@'android:versionName'
// 获取包名
println manifest.@package

```







## 参考资料

[文档](http://docs.groovy-lang.org/docs/latest/html/documentation/)
[groovy-jdk](http://docs.groovy-lang.org/docs/latest/html/groovy-jdk/)

I/O 操作相关的 SDK 地址
java.io.File: http://docs.groovy-lang.org/latest/html/groovy-jdk/java/io/File.html 
java.io.InputStream: http://docs.groovy-lang.org/latest/html/groovy-jdk/java/io/InputStream.html java.io.OutputStream: http://docs.groovy-lang.org/latest/html/groovy-jdk/java/io/OutputStream.html java.io.Reader: http://docs.groovy-lang.org/latest/html/groovy-jdk/java/io/Reader.html 
java.io.Writer: http://docs.groovy-lang.org/latest/html/groovy-jdk/java/io/Writer.html 
java.nio.file.Path: http://docs.groovy-lang.org/latest/html/groovy-jdk/java/nio/file/Path.html





