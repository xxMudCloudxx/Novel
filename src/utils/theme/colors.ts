// React Native 动态主题色实现，基于系统深色/浅色模式自动切换
// 使用 TypeScript + React Native 内置的 useColorScheme Hook

import { useColorScheme } from 'react-native';

/**
 * 返回当前系统模式下的颜色值
 * @param light 浅色模式下使用的十六进制颜色字符串
 * @param dark 深色模式下使用的十六进制颜色字符串
 * @returns 当前模式对应的颜色值
 */
export function useDynamicColor(light: string, dark: string): string {
  const scheme = useColorScheme();
  return scheme === 'dark' ? dark : light;
}

/**
 * Novel 应用的主题色集，支持动态切换
 * 返回一个对象，内含各项颜色属性，可直接用于样式中
 */
export function useNovelColors() {
  const scheme = useColorScheme();
  return {
    // 主色
    novelMain: '#1EA59E',
    // 浅主色
    novelMainLight: '#28CCC3',
    // 分割线颜色
    novelDivider: scheme === 'dark' ? '#1C1C1E' : '#F7F7F8',
    // 次级背景
    novelSecondaryBackground: scheme === 'dark' ? '#1C1C1E' : '#F7F7F8',
    // 页面背景
    novelBackground: scheme === 'dark' ? '#000000' : '#FFFFFF',
    // 文本灰色
    novelTextGray: scheme === 'dark' ? '#97989F' : '#7F7F7F',
    // 文本默认色
    novelText: scheme === 'dark' ? '#FFFFFF' : '#000000',
    // 浅灰背景
    novelLightGray: scheme === 'dark' ? '#1C1C1E' : '#DDDDDD',
    // Chip 背景
    novelChipBackground: scheme === 'dark' ? '#23242B' : '#EBEDF0',
  };
}