# <center>agripos 项目重构</center>

## 前言
参考[diycode](https://github.com/GcsSloop/diycode)项目

## 框架搭建
### 1. 确定网络请求方案
Android网络请求方式有很多：
- HttpURLConnection 最原始
- 封装过的OkHttp
- 封装程度较高的Volley Google官方 - star 1.5k
- 封装程度较高的Retrofit - star 27k

根据star数，选择Retrofit，毕竟 Retrofit 使用的人数多，就算踩坑了也好找解决方案。

### 2. 事件总线 EventBus
- 方便事件传递
- 解耦
- 避免回调地狱


## 项目结构
- EventBus相关
BaseEvent