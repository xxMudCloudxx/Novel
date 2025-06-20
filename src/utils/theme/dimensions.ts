import { Dimensions } from 'react-native';

// 设计稿基准尺寸
const DESIGN_WIDTH = 375;
const DESIGN_HEIGHT = 852;

// 获取设备屏幕尺寸
const { width: deviceWidth, height: deviceHeight } = Dimensions.get('window');

// 计算缩放比例
const scaleWidth = deviceWidth / DESIGN_WIDTH;
const scaleHeight = deviceWidth / DESIGN_WIDTH;

// 取较小的缩放比例，确保内容不会被截断
const scale = Math.min(scaleWidth, scaleHeight);

/**
 * 宽度适配函数 - 基于375px设计稿
 * @param size 设计稿中的宽度
 * @returns 适配后的宽度
 */
export const wp = (size: number): number => {
  return Math.round(size * scaleWidth);
};

/**
 * 高度适配函数 - 基于852px设计稿
 * @param size 设计稿中的高度
 * @returns 适配后的高度
 */
export const hp = (size: number): number => {
  return Math.round(size * scaleHeight);
};

/**
 * 字体大小适配函数 - 使用统一缩放比例
 * @param size 设计稿中的字体大小
 * @returns 适配后的字体大小
 */
export const fp = (size: number): number => {
  return Math.round(size * scale);
};

/**
 * 通用尺寸适配函数 - 使用统一缩放比例
 * 适用于边框、圆角、间距等需要保持比例的尺寸
 * @param size 设计稿中的尺寸
 * @returns 适配后的尺寸
 */
export const sp = (size: number): number => {
  return Math.round(size * scale);
};

// 导出设备信息供其他组件使用
export const deviceInfo = {
  width: deviceWidth,
  height: deviceHeight,
  scaleWidth,
  scaleHeight,
  scale,
  designWidth: DESIGN_WIDTH,
  designHeight: DESIGN_HEIGHT,
};

// 导出常用的适配尺寸
export const commonSizes = {
  // 基于375px宽度的常用尺寸
  containerPadding: wp(15),
  borderRadius: sp(10),
  iconSize: sp(24),
  iconSizeSmall: sp(16),
  iconSizeLarge: sp(32),

  // 基于852px高度的常用尺寸
  headerHeight: hp(60),
  tabBarHeight: hp(80),
  buttonHeight: hp(44),

  // 字体尺寸
  fontSizeSmall: fp(12),
  fontSizeNormal: fp(14),
  fontSizeMedium: fp(16),
  fontSizeLarge: fp(18),
  fontSizeXLarge: fp(20),
  fontSizeXXLarge: fp(24),
};

export default {
  wp,
  hp,
  fp,
  sp,
  deviceInfo,
  commonSizes,
};
