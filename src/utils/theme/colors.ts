// React Native 动态主题色实现，基于系统深色/浅色模式自动切换
// 使用 TypeScript + React Native 内置的 useColorScheme Hook


import { useColorScheme } from "react-native/Libraries/Utilities/Appearance";
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

export interface NovelColors {
  novelBackground: string;
  novelMain: string;
  novelText: string;
  novelDivider: string;
}

const lightColors: NovelColors = {
  novelBackground: '#f5f5f5',
  novelMain: '#007AFF',
  novelText: '#333333',
  novelDivider: '#e0e0e0',
};

const darkColors: NovelColors = {
  novelBackground: '#1a1a1a',
  novelMain: '#007AFF',
  novelText: '#ffffff',
  novelDivider: '#333333',
};

export const useNovelColors = (): NovelColors => {
  const scheme = useColorScheme();
  return scheme === 'dark' ? darkColors : lightColors;
};