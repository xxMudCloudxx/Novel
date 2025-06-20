import { useState, useCallback, useEffect } from 'react';
import { PULL_THRESHOLD } from '../utils/constants';

interface UseRefreshLogicProps {
  isRefreshing: boolean;
  homeRecommendLoading: boolean;
  hasMoreHomeRecommend: boolean;
  refreshBooks: () => Promise<void>;
  loadMoreBooks: () => Promise<void>;
}

export const useRefreshLogic = ({
  isRefreshing,
  homeRecommendLoading,
  hasMoreHomeRecommend,
  refreshBooks,
  loadMoreBooks,
}: UseRefreshLogicProps) => {
  const [isPullingDown, setIsPullingDown] = useState(false);
  const [pullDistance, setPullDistance] = useState(0);
  const [hasTriggeredRefresh, setHasTriggeredRefresh] = useState(false);

  // 下拉刷新
  const handleRefresh = useCallback(async () => {
    console.log('触发下拉刷新');
    try {
      await refreshBooks();
      console.log('下拉刷新完成');
    } catch (error) {
      console.error('刷新失败:', error);
    }
  }, [refreshBooks]);

  // 上拉加载更多
  const handleLoadMore = useCallback(async () => {
    if (hasMoreHomeRecommend && !homeRecommendLoading) {
      console.log('触发上拉加载更多');
      try {
        await loadMoreBooks();
        console.log('加载更多完成');
      } catch (error) {
        console.error('加载更多失败:', error);
      }
    }
  }, [hasMoreHomeRecommend, homeRecommendLoading, loadMoreBooks]);

  // 处理滚动事件
  const handleScroll = useCallback((event: any) => {
    const { layoutMeasurement, contentOffset, contentSize } = event.nativeEvent;
    const paddingToBottom = 50;

    // 检测下拉刷新
    if (contentOffset.y < 0) {
      const distance = Math.abs(contentOffset.y);
      setPullDistance(distance);

      if (distance > 10) {
        setIsPullingDown(true);
      }

      if (distance > PULL_THRESHOLD && !isRefreshing && !hasTriggeredRefresh) {
        console.log('触发下拉刷新，距离:', distance);
        setHasTriggeredRefresh(true);
        handleRefresh();
      }
    } else if (contentOffset.y >= 0) {
      if (isPullingDown || pullDistance > 0) {
        setPullDistance(0);
        if (!isRefreshing) {
          setIsPullingDown(false);
          setHasTriggeredRefresh(false);
        }
      }
    }

    // 检测底部加载
    if (contentSize.height > layoutMeasurement.height) {
      const isNearBottom = layoutMeasurement.height + contentOffset.y >= contentSize.height - paddingToBottom;
      if (isNearBottom && hasMoreHomeRecommend && !homeRecommendLoading) {
        handleLoadMore();
      }
    }
  }, [handleLoadMore, handleRefresh, isRefreshing, hasTriggeredRefresh, isPullingDown, pullDistance, hasMoreHomeRecommend, homeRecommendLoading]);

  // 监听刷新状态变化，确保刷新完成后重置状态
  useEffect(() => {
    if (!isRefreshing) {
      const resetStates = () => {
        setIsPullingDown(false);
        setPullDistance(0);
        setHasTriggeredRefresh(false);
      };

      const timer = setTimeout(resetStates, 300);
      return () => clearTimeout(timer);
    }
  }, [isRefreshing]);

  return {
    isPullingDown,
    pullDistance,
    hasTriggeredRefresh,
    handleScroll,
    handleRefresh,
    handleLoadMore,
    PULL_THRESHOLD,
  };
};
