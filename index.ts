/**
 * @format
 */

import { AppRegistry } from 'react-native';
import App from './App';
import appConfig from './app.json';
// 导入SettingsPageComponent以确保组件被注册
import './src/page/SettingsPage/settingspage/SettingsPageComponent';

const appName: string = appConfig.name;

// 注册根组件
AppRegistry.registerComponent(appName, () => App);
