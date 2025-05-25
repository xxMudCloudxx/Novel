/**
 * @format
 */

import { AppRegistry } from 'react-native';
import App from './App';
import appConfig from './app.json';

const appName: string = appConfig.name;

// 注册组件（与 React Native 保持一致）
AppRegistry.registerComponent(appName, () => App);

// 在 Web 平台上挂载到 HTML 的 root 节点
AppRegistry.runApplication(appName, {
  rootTag: document.getElementById('root') as HTMLElement,
});
