# Novel - 仿番茄小说App

基于 **Kotlin + React Native** 混合架构开发的小说阅读应用，采用现代化的开发模式和最佳实践。

## 🎯 项目简介

Novel App 是一个高性能的小说阅读应用，采用 **MVI + Repository + 单向数据流** 架构模式，Android 侧使用 **ViewModel/Hilt/Paging3/Room**，RN 侧使用 **Zustand + immer + middleware**，通过 **Shared Flow** 实现双端事件通信。

## 🚀 技术栈

### 前端 (React Native)
- **React Native 0.74** - 跨平台移动应用框架
- **TypeScript** - 类型安全的JavaScript超集
- **React Native Reanimated 3** - 高性能动画库
- **Zustand + immer** - 轻量级状态管理
- **React Navigation 7** - 导航路由管理
- **React Native Fast Image** - 图片加载优化

### Android 原生
- **Kotlin** - 现代化的JVM语言
- **Jetpack Compose** - 声明式UI框架
- **Hilt** - 依赖注入框架
- **Room + Paging3** - 数据库与分页
- **OkHttp 5 + Retrofit** - 网络请求
- **Coil-Compose** - 图片加载

## 📱 已实现功能

### ✅ 混合架构核心
- **Kotlin + React Native** - 混合架构，发挥两端优势
- **Hilt 依赖注入** - 模块化依赖管理
- **React Native Bridge** - 原生与RN双向通信
- **NavigationPackage** - 自定义RN原生模块

### ✅ Android Compose 原生实现

#### 🏠 首页 (HomePage)
- **MVI + Repository** - 完整的架构实现
- **HomeViewModel** - 状态管理与数据流控制
- **下拉刷新 + 上拉加载** - SwipeRefresh集成
- **书籍推荐系统** - 轮播图、热门、新书、VIP书籍
- **分类筛选器** - 动态分类加载与切换
- **榜单功能** - 访问榜、收藏榜等多种榜单
- **瀑布流布局** - 高性能的书籍展示
- **搜索功能** - 实时搜索与结果展示
- **书籍点击导航** - 点击书籍跳转到详情页

#### 📖 书籍详情页 (BookDetailPage)
- **模块化架构** - 组件化设计，职责分离
  - `components/` - UI组件：封面、标题、作者、统计、简介、书评
  - `viewmodel/` - 业务逻辑：BookDetailViewModel + BookDetailUiState  
  - `utils/` - 工具类：数据处理和格式化
- **MVI + StateHolder** - 统一状态管理架构
- **LoadingStateComponent** - 加载、错误、空状态处理
- **书籍信息展示** - 封面、标题、作者、分类信息
- **统计数据** - 评分、阅读人数、字数、更新时间
- **简介展开/收起** - HTML标签清理与文本展示
- **热门书评** - 星级评分、用户评论展示

#### 🧭 导航系统 (NavigationUtil)
- **NavHost路由** - 支持参数化路由导航
- **NavViewModel** - 统一导航管理
- **页面跳转** - 首页 ↔ 书籍详情页 ↔ 登录页
- **参数传递** - 书籍ID等关键数据传递
- **返回导航** - 完整的页面栈管理

#### 🔐 登录页面 (LoginPage)
- **登录注册功能** - 手机号验证码登录
- **动画交互** - 丰富的页面切换动画
- **运营商识别** - 智能识别手机号运营商
- **输入验证** - 表单验证与错误提示
- **协议确认** - 用户协议和隐私政策

#### 🧭 底部导航
- **HorizontalPager** - 流畅的页面切换
- **五大模块** - 首页、分类、福利、书架、我的
- **状态保持** - 页面状态自动保存

### ✅ React Native 实现

#### 👤 我的页面 (ProfilePage)
- **模块化架构** - 职责分离，代码可维护性强  
- **下拉刷新** - 智能触发机制，流畅的状态切换
- **瀑布流布局** - 高性能的书籍展示
- **滚动动画** - 丰富的交互动效
  - 动态高度变化 
  - 图标透明度渐变  
  - 广告隐藏动画
- **主题系统** - 完整的主题色彩方案
- **TypeScript** - 完整的类型定义

## 🔗 相关链接

- **后端接口**: [novel-cloud](https://github.com/201206030/novel-cloud)

## 📝 开发计划

### 🚧 进行中
- [ ] 用户登录/注册
- [ ] 阅读器功能
- [ ] 搜索功能

### 📋 待开发
- [ ] 个人中心
- [ ] 书架管理
- [ ] 离线阅读
- [ ] 夜间模式
- [ ] 字体设置
- [ ] 阅读进度同步

## 👥 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📄 许可证

本项目基于 MIT 许可证开源 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🤝 致谢

感谢所有为这个项目做出贡献的开发者！

---

> 💡 **提示**: 这是一个学习项目，展示了现代移动应用开发的最佳实践。
