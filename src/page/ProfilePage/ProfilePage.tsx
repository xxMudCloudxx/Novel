import React, { useEffect, useCallback } from 'react';
import { View, ScrollView, NativeModules } from 'react-native';
import { useUserStore } from './store/userStore';
import { useHomeStore } from './store/BookStore';
import { useNovelColors } from '../../utils/theme/colors';
import { useProfilePageAnimations } from './hooks/useProfilePageAnimations';
import { useRefreshLogic } from './hooks/useRefreshLogic';
import { convertHomeBooksToBooks } from './utils/helpers';
import { createHomePageStyles } from './styles/ProfilePageStyles';
import { Book } from './types';
import { useThemeStore } from '../../utils/theme/themeStore';
import {
  TopBar,
  LoginBar,
  RefreshIndicator,
  ScrollableArea,
  BottomBox,
  WaterfallGrid,
} from './components';

const { NavigationUtil } = NativeModules;

const ProfilePage: React.FC = () => {

  // 使用Zustand stores
  const { nickname, photo, isLoggedIn, balance, coins } = useUserStore();
  const {
    homeRecommendBooks,
    homeRecommendLoading,
    isRefreshing,
    hasMoreHomeRecommend,
    loadHomeRecommendBooks,
    refreshBooks,
    loadMoreBooks,
  } = useHomeStore();

  // 添加主题store
  const { initializeFromNative } = useThemeStore();

  const colors = useNovelColors();
  const styles = createHomePageStyles(colors);

  // 将HomeBook转换为Book格式
  const convertedBooks = React.useMemo(() => {
    return convertHomeBooksToBooks(homeRecommendBooks);
  }, [homeRecommendBooks]);

  // 使用自定义hooks
  const refreshLogic = useRefreshLogic({
    isRefreshing,
    homeRecommendLoading,
    hasMoreHomeRecommend,
    refreshBooks,
    loadMoreBooks,
  });

  const animations = useProfilePageAnimations(
    isRefreshing,
    refreshLogic.isPullingDown,
    refreshLogic.pullDistance,
    refreshLogic.PULL_THRESHOLD
  );

  // 初始化数据和主题
  useEffect(() => {
    const initializePageData = async () => {
      try {
        // 🎯 首先从原生端获取最新的主题状态
        console.log('[ProfilePage] 开始初始化主题和数据');
        await initializeFromNative();
        console.log('[ProfilePage] 主题初始化完成');
        
        // 然后加载数据
        await loadHomeRecommendBooks();
        console.log('[ProfilePage] 数据加载完成');
      } catch (error) {
        console.error('[ProfilePage] 初始化失败:', error);
        // 即使主题初始化失败，也要尝试加载数据
        loadHomeRecommendBooks();
      }
    };

    initializePageData();
  }, [loadHomeRecommendBooks, initializeFromNative]);

  // 登录函数
  const toLogin = useCallback(() => {
    console.log('Navigate to login page');
    // 这里实现跳转到登录页面的逻辑
  }, []);

  // 书籍点击
  const handleBookPress = useCallback((book: Book) => {
    console.log('Book pressed:', book.title);
    // 这里可以导航到书籍详情页
  }, []);

  // 设置按钮点击
  const handleSettingsPress = useCallback(() => {
    if (NavigationUtil?.navigateToSettings) {
      NavigationUtil.navigateToSettings();
    } else {
      console.log('NavigationUtil.navigateToSettings not available');
    }
  }, []);

  return (
    <View style={styles.container}>
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
        onScroll={refreshLogic.handleScroll}
        scrollEventThrottle={16}
        bounces={true}
        alwaysBounceVertical={true}
        scrollEnabled={true}
      >
        <RefreshIndicator
          styles={styles}
          isPullingDown={refreshLogic.isPullingDown}
          isRefreshing={isRefreshing}
          pullDistance={refreshLogic.pullDistance}
          threshold={refreshLogic.PULL_THRESHOLD}
          spinStyle={animations.spinStyle}
        />

        <TopBar styles={styles} onSettingsPress={handleSettingsPress} />

        <LoginBar
          styles={styles}
          photo={photo || undefined}
          isLoggedIn={isLoggedIn}
          nickname={nickname || undefined}
          onLogin={toLogin}
        />

        <ScrollableArea
          styles={styles}
          scrollX={animations.scrollX}
          animatedContainerStyle={animations.animatedContainerStyle}
          firstPageIconsStyle={animations.firstPageIconsStyle}
          secondPageIconsStyle={animations.secondPageIconsStyle}
          thirdPageIconsStyle={animations.thirdPageIconsStyle}
          firstPageAdStyle={animations.firstPageAdStyle}
          colors={colors}
        />

        <BottomBox
          styles={styles}
          coins={coins}
          balance={balance}
        />

        <WaterfallGrid
          styles={styles}
          books={convertedBooks}
          loading={homeRecommendLoading}
          hasMore={hasMoreHomeRecommend}
          spinStyle={animations.spinStyle}
          onBookPress={handleBookPress}
        />
      </ScrollView>
    </View>
  );
};

export default ProfilePage;
