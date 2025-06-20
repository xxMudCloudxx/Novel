import React from 'react';
import { View, Text } from 'react-native';
import Animated from 'react-native-reanimated';

interface RefreshIndicatorProps {
  styles: any;
  isPullingDown: boolean;
  isRefreshing: boolean;
  pullDistance: number;
  threshold: number;
  spinStyle: any;
}

export const RefreshIndicator: React.FC<RefreshIndicatorProps> = ({
  styles,
  isPullingDown,
  isRefreshing,
  pullDistance,
  threshold,
  spinStyle,
}) => {
  if (!isPullingDown && !isRefreshing) {return null;}

  const getRefreshText = () => {
    if (isRefreshing) {return '正在刷新...';}
    if (pullDistance > threshold) {return '释放刷新';}
    return '下拉刷新';
  };

  const shouldShowSpinner = isRefreshing || pullDistance > threshold;

  return (
    <View style={styles.refreshIndicator}>
      <View style={styles.refreshContent}>
        {shouldShowSpinner && (
          <View style={styles.loadingSpinner}>
            <Animated.Text style={[styles.spinnerText, spinStyle]}>⟳</Animated.Text>
          </View>
        )}
        <Text style={styles.refreshText}>
          {getRefreshText()}
        </Text>
      </View>
    </View>
  );
};
