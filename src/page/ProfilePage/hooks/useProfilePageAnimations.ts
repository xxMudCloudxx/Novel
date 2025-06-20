import { useSharedValue, useAnimatedStyle, withTiming, interpolate, Extrapolate } from 'react-native-reanimated';
import { useEffect } from 'react';
import { PAGE_WIDTH, MIN_HEIGHT, MAX_HEIGHT } from '../utils/constants';

export const useProfilePageAnimations = (isRefreshing: boolean, isPullingDown: boolean, pullDistance: number, PULL_THRESHOLD: number) => {
  const scrollX = useSharedValue(0);
  const spinValue = useSharedValue(0);

  // 创建动态高度动画样式
  const animatedContainerStyle = useAnimatedStyle(() => {
    const height = interpolate(
      scrollX.value,
      [0, PAGE_WIDTH, PAGE_WIDTH * 2],
      [MIN_HEIGHT, MAX_HEIGHT, MAX_HEIGHT],
      Extrapolate.CLAMP
    );

    return {
      height: height,
    };
  });

  // 第一页图标透明度动画
  const firstPageIconsStyle = useAnimatedStyle(() => {
    const opacity = interpolate(
      scrollX.value,
      [0, PAGE_WIDTH * 0.5, PAGE_WIDTH],
      [1, 0.3, 0.3],
      Extrapolate.CLAMP
    );

    return {
      opacity: opacity,
    };
  });

  // 第二页图标透明度动画
  const secondPageIconsStyle = useAnimatedStyle(() => {
    const opacity = interpolate(
      scrollX.value,
      [0, PAGE_WIDTH * 0.5, PAGE_WIDTH, PAGE_WIDTH * 1.5, PAGE_WIDTH * 2],
      [0.3, 0.3, 1, 0.3, 0.3],
      Extrapolate.CLAMP
    );

    return {
      opacity: opacity,
    };
  });

  // 第三页图标透明度动画
  const thirdPageIconsStyle = useAnimatedStyle(() => {
    const opacity = interpolate(
      scrollX.value,
      [PAGE_WIDTH, PAGE_WIDTH * 1.5, PAGE_WIDTH * 2],
      [0.3, 0.3, 1],
      Extrapolate.CLAMP
    );

    return {
      opacity: opacity,
    };
  });

  // 第一页广告显示/隐藏动画
  const firstPageAdStyle = useAnimatedStyle(() => {
    const opacity = interpolate(
      scrollX.value,
      [0, PAGE_WIDTH * 0.5, PAGE_WIDTH],
      [1, 0, 0],
      Extrapolate.CLAMP
    );

    const translateY = interpolate(
      scrollX.value,
      [0, PAGE_WIDTH * 0.5, PAGE_WIDTH],
      [0, -20, -20],
      Extrapolate.CLAMP
    );

    return {
      opacity: opacity,
      transform: [{ translateY: translateY }],
    };
  });

  // 转圈动画控制
  useEffect(() => {
    const shouldSpin = isRefreshing || (isPullingDown && pullDistance > PULL_THRESHOLD);

    if (shouldSpin) {
      const startRotation = () => {
        spinValue.value = withTiming(360, { duration: 1000 }, (finished) => {
          if (finished && (isRefreshing || (isPullingDown && pullDistance > PULL_THRESHOLD))) {
            spinValue.value = 0;
            startRotation();
          }
        });
      };
      startRotation();
    } else {
      spinValue.value = 0;
    }
  }, [isRefreshing, isPullingDown, pullDistance, spinValue, PULL_THRESHOLD]);

  // 创建旋转动画样式
  const spinStyle = useAnimatedStyle(() => {
    return {
      transform: [{ rotate: `${spinValue.value}deg` }],
    };
  });

  return {
    scrollX,
    animatedContainerStyle,
    firstPageIconsStyle,
    secondPageIconsStyle,
    thirdPageIconsStyle,
    firstPageAdStyle,
    spinStyle,
  };
};
