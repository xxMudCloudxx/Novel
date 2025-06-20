/**
 * @format
 */

import { AppRegistry } from 'react-native';
import App from './App';
import appConfig from './app.json';

// 引入Web端EventEmitter实现
import './src/utils/webEventEmitter';

// 引入Web端调试工具
import './src/utils/webDebugTools';

// Web端NativeModules mock
if (typeof window !== 'undefined') {
  (globalThis as any).NativeModules = {
    NavigationUtil: {
      goToLogin: () => {
        console.log('🌐 Web端模拟登录导航');
        // 可以在这里打开一个模拟登录弹窗
      },
    },
  };

  // 添加全局样式重置
  const style = document.createElement('style');
  style.textContent = `
    * {
      box-sizing: border-box;
    }
    body {
      margin: 0;
      padding: 0;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    }
    #root {
      width: 100%;
      height: 100vh;
    }
  `;
  document.head.appendChild(style);
}

const appName: string = appConfig.name;

// 注册组件（与 React Native 保持一致）
AppRegistry.registerComponent(appName, () => App);

// 在 Web 平台上挂载到 HTML 的 root 节点
AppRegistry.runApplication(appName, {
  rootTag: document.getElementById('root') as HTMLElement,
});
