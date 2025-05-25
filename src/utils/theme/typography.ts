// React Native 主题排版定义，封装字体家族、权重、字号等

import { TextStyle } from 'react-native/Libraries/StyleSheet/StyleSheetTypes';

// 1. 引入自定义字体，需要先在项目中配置好 Font 文件并在 react-native.config.js 中声明
// 例如：
// module.exports = {
//   assets: ['./assets/fonts/'],
// };
// 然后 yarn react-native link 或自动链接

/**
 * Novel 应用的字体配置
 */
export const NovelFontFamily = {
    regular: 'PingFangSC-Regular',
    bold: 'PingFangSC-Bold',
    light: 'PingFangSC-Light',
    medium: 'PingFangSC-Medium',
    semiBold: 'PingFangSC-Semibold',
    heavy: 'PingFangSC-Heavy',
  } as const;
  
  /**
   * 字号单位，可根据需求定义基于屏幕适配的函数
   */
  export const FontSizes = {
    titleLarge: 28,
    titleSmall: 16,
    bodyMedium: 16,
    labelLarge: 14,
    labelSmall: 12,
    bodySmall: 16,
  } as const;
  
  /**
   * Novel 应用的排版样式集
   */
  export const typography: Record<
    | 'titleLarge'
    | 'titleSmall'
    | 'bodyMedium'
    | 'bodySmall'
    | 'labelLarge'
    | 'labelSmall',
    TextStyle
  > = {
    titleLarge: {
      fontFamily: NovelFontFamily.medium,
      fontWeight: '500',
      fontSize: FontSizes.titleLarge,
    },
    titleSmall: {
      fontFamily: NovelFontFamily.regular,
      fontWeight: '400',
      fontSize: FontSizes.titleSmall,
    },
    bodyMedium: {
      fontFamily: NovelFontFamily.medium,
      fontWeight: '500',
      fontSize: FontSizes.bodyMedium,
    },
    bodySmall: {
      fontFamily: NovelFontFamily.semiBold,
      fontWeight: '600',
      fontSize: FontSizes.bodySmall,
    },
    labelLarge: {
      fontFamily: NovelFontFamily.regular,
      fontWeight: '400',
      fontSize: FontSizes.labelLarge,
    },
    labelSmall: {
      fontFamily: NovelFontFamily.regular,
      fontWeight: '400',
      fontSize: FontSizes.labelSmall,
    },
  };
  