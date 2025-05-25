// index.web.js
import { AppRegistry } from 'react-native';
import App from './App';
import appConfig from './app.json';

const appName = appConfig.name;
// 注册组件（与 index.js 保持一致）
AppRegistry.registerComponent(appName, () => App);

// 挂载到 HTML 中 id=root 的元素
AppRegistry.runApplication(appName, {
  rootTag: document.getElementById('root'),
});
