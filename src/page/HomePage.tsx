import React, { useEffect, useState, useCallback } from 'react';
import { useUserStore } from '../store/userStore';
import { useHomeStore, HomeBook } from '../store/homeStore';
import IconComponent from '../component/IconComponent';
import { useNovelColors } from '../utils/theme/colors';
import { typography } from '../utils/theme/typography';
import { wp, fp, sp, commonSizes } from '../utils/theme/dimensions';
import { View, Text, TouchableOpacity, Image, Dimensions, ScrollView ,StyleSheet } from 'react-native';
import Animated, { 
  useSharedValue, 
  useAnimatedStyle, 
  withTiming,
  interpolate,
  Extrapolate,
  runOnJS,
} from 'react-native-reanimated';


const { width: screenWidth } = Dimensions.get('window');

// 预计算的项目高度缓存
const itemHeightCache = new Map<number, number>();

interface IconData {
  id: string;
  name: string;
  icon: string;
  onPress: () => void;
}

interface BookItemProps {
  book: any;
  onPress?: () => void;
  index: number;
}

// 将HomeBook转换为Book格式的辅助函数
const convertHomeBooksToBooks = (homeBooks: HomeBook[]) => {
  return homeBooks.map(homeBook => ({
    id: homeBook.bookId,
    title: homeBook.bookName,
    author: homeBook.authorName,
    description: homeBook.bookDesc,
    coverUrl: homeBook.picUrl,
    categoryId: homeBook.type,
    readCount: Math.floor(Math.random() * 10000), // 模拟数据
    rating: Math.random() * 5, // 模拟数据
  }));
};

const HomePage: React.FC = () => {
  // 使用Zustand stores
  const { uid, nickname, photo, isLoggedIn, balance, coins } = useUserStore();
  const { 
    homeRecommendBooks,
    homeRecommendLoading,
    isRefreshing,
    hasMoreHomeRecommend,
    loadHomeRecommendBooks,
    refreshBooks,
    loadMoreBooks 
  } = useHomeStore();
  
  const [currentPage, setCurrentPage] = useState(0);
  const colors = useNovelColors();
  
  //动画相关的状态
  const scrollX = useSharedValue(0);
  const pageWidth = wp(350); // 恢复页面宽度
  
  // 预计算动画高度值，避免在worklet中调用wp函数
  const minHeight = wp(200);
  const maxHeight = wp(250);

  // 将HomeBook转换为Book格式
  const convertedBooks = React.useMemo(() => {
    return convertHomeBooksToBooks(homeRecommendBooks);
  }, [homeRecommendBooks]);

  // 初始化数据
  useEffect(() => {
    loadHomeRecommendBooks();
  }, [loadHomeRecommendBooks]);

  // 创建动态高度动画样式
  const animatedContainerStyle = useAnimatedStyle(() => {
    // 根据滑动距离插值高度：从minHeight到maxHeight
    const height = interpolate(
      scrollX.value,
      [0, pageWidth, pageWidth * 2],
      [minHeight, maxHeight, maxHeight],
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
      [0, pageWidth * 0.5, pageWidth],
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
      [0, pageWidth * 0.5, pageWidth, pageWidth * 1.5, pageWidth * 2],
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
      [pageWidth, pageWidth * 1.5, pageWidth * 2],
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
      [0, pageWidth * 0.5, pageWidth],
      [1, 0, 0],
      Extrapolate.CLAMP
    );
    
    const translateY = interpolate(
      scrollX.value,
      [0, pageWidth * 0.5, pageWidth],
      [0, -20, -20],
      Extrapolate.CLAMP
    );
    
    return {
      opacity: opacity,
      transform: [{ translateY: translateY }],
    };
  });

  // 登录函数
  const toLogin = useCallback(() => {
    console.log('Navigate to login page');
    // 这里实现跳转到登录页面的逻辑
  }, []);

  // 下拉刷新
  const handleRefresh = useCallback(() => {
    refreshBooks();
  }, [refreshBooks]);

  // 上拉加载更多
  const handleLoadMore = useCallback(() => {
    if (hasMoreHomeRecommend && !homeRecommendLoading) {
      loadMoreBooks();
    }
  }, [hasMoreHomeRecommend, homeRecommendLoading, loadMoreBooks]);

  // 书籍点击
  const handleBookPress = useCallback((book: any) => {
    console.log('Book pressed:', book.title);
    // 这里可以导航到书籍详情页
  }, []);

  // 图标数据
  const iconsData: IconData[] = [
    // 第一页的4个图标
    { id: 'wallet', name: '我的钱包', icon: 'wallet', onPress: () => console.log('钱包') },
    { id: 'download', name: '我的下载', icon: 'download', onPress: () => console.log('下载') },
    { id: 'history', name: '游戏中心', icon: 'history', onPress: () => console.log('历史') },
    { id: 'subscribe', name: '推书中心', icon: 'subscribe', onPress: () => console.log('订阅') },
    
    // 第二页的15个图标
    { id: 'game', name: '我的', icon: 'game', onPress: () => console.log('游戏') },
    { id: 'my_preorder', name: '我的预约', icon: 'member', onPress: () => console.log('我的预约') },
    { id: 'my_download', name: '我的下载', icon: 'download', onPress: () => console.log('我的下载') },
    { id: 'game_center', name: '游戏中心', icon: 'game', onPress: () => console.log('游戏中心') },
    { id: 'push_center', name: '推书中心', icon: 'recommend_book', onPress: () => console.log('推书中心') },
    { id: 'video_creation', name: '视频创作', icon: 'vedio_creation', onPress: () => console.log('视频创作') },
    { id: 'reading_preference', name: '阅读偏好', icon: 'reading_preference', onPress: () => console.log('阅读偏好') },
    { id: 'my_note', name: '我的笔记', icon: 'note', onPress: () => console.log('我的笔记') },
    { id: 'who_seen', name: '看过的人', icon: 'who_have_seen', onPress: () => console.log('看过的人') },
    { id: 'liked_video', name: '赞过的视频', icon: 'vedio_have_favorited', onPress: () => console.log('赞过的视频') },
    { id: 'help_guide', name: '帮助指南', icon: 'guide', onPress: () => console.log('帮助指南') },
    { id: 'my_public_welfare', name: '我的公益', icon: 'public_welfare', onPress: () => console.log('我的公益') },
    { id: 'member_center', name: '会员中心', icon: 'member', onPress: () => console.log('会员中心') },
    { id: 'my_wallet2', name: '我的钱包', icon: 'wallet', onPress: () => console.log('我的钱包') },
    { id: 'feedback_help', name: '反馈与帮助', icon: 'feedback', onPress: () => console.log('反馈与帮助') },
    
    // 第三页的剩余图标
    { id: 'my_wallet21', name: '我的钱包', icon: 'wallet', onPress: () => console.log('我的钱包') },
    { id: 'feedback_help1', name: '反馈与帮助', icon: 'feedback', onPress: () => console.log('反馈与帮助') },
    { id: 'my_wallet22', name: '我的钱包', icon: 'wallet', onPress: () => console.log('我的钱包') },
    { id: 'feedback_help2', name: '反馈与帮助', icon: 'feedback', onPress: () => console.log('反馈与帮助') },
  ];

  // 分页图标数据
  const getPageIcons = (pageIndex: number): IconData[] => {
    if (pageIndex === 0) {
      return iconsData.slice(0, 4);
    } else if (pageIndex === 1) {
      return iconsData.slice(4, 16);
    } else {
      return iconsData.slice(19, 23);
    }
  };

  // 渲染顶部Bar
  const renderTopBar = () => (
    <View style={themedStyles.topBar}>
      <TouchableOpacity onPress={() => console.log('QR Code')}>
        <IconComponent name="qrscan" width={commonSizes.iconSize} height={commonSizes.iconSize} />
      </TouchableOpacity>
      <TouchableOpacity onPress={() => console.log('Moon Mode')}>
        <IconComponent name="moon_mode" width={commonSizes.iconSize} height={commonSizes.iconSize} />
      </TouchableOpacity>
      <TouchableOpacity onPress={() => console.log('Settings')}>
        <IconComponent name="settings" width={commonSizes.iconSize} height={commonSizes.iconSize} />
      </TouchableOpacity>
    </View>
  );

  // 渲染登录栏
  const renderLoginBar = () => (
    <View style={themedStyles.loginBar}>
      <View style={themedStyles.avatar}>
        {photo ? (
          <View style={themedStyles.avatarImage} />
        ) : (
          <View style={themedStyles.defaultAvatar} />
        )}
      </View>
      <TouchableOpacity onPress={toLogin} style={themedStyles.loginButton}>
        <Text style={themedStyles.loginText}>
          {isLoggedIn && nickname ? nickname : '点击登录/注册'}
        </Text>
      </TouchableOpacity>
    </View>
  );

  // 渲染图标
  const renderIcon = (iconData: IconData, index: number) => (
    <TouchableOpacity 
      key={iconData.id} 
      style={themedStyles.iconItem} 
      onPress={iconData.onPress}
    >
      <IconComponent name={iconData.icon} width={wp(25)} height={wp(25)} />
      <Text style={themedStyles.iconText}>{iconData.name}</Text>
    </TouchableOpacity>
  );

  // 渲染广告组件
  const renderAdvertisement = () => (
    <View style={themedStyles.advertisement}>
      <View style={themedStyles.adBookCover} />
      <View style={themedStyles.adContent}>
        <Text style={themedStyles.adTitle} numberOfLines={2}>
          加饰披摩，高冷校花消不住了
        </Text>
        <Text style={themedStyles.adAuthor} numberOfLines={1}>
          书时真
        </Text>
      </View>
      <TouchableOpacity style={themedStyles.continueReading}>
        <Text style={themedStyles.continueText}>继续阅读 &gt;</Text>
      </TouchableOpacity>
    </View>
  );

  // 渲染可滑动区域 - 修复间距问题并添加动画
  const renderScrollableArea = () => {
    const totalPages = 3;
    
    return (
      <View style={themedStyles.scrollableContainer}>
        <Animated.View style={[themedStyles.scrollArea, animatedContainerStyle]}>
          <ScrollView 
            horizontal 
            pagingEnabled 
            showsHorizontalScrollIndicator={false}
            onMomentumScrollEnd={(event: any) => {
              const pageIndex = Math.round(event.nativeEvent.contentOffset.x / pageWidth);
              setCurrentPage(pageIndex);
            }}
            onScroll={(event: any) => {
              scrollX.value = event.nativeEvent.contentOffset.x;
            }}
            scrollEventThrottle={16}
          >
            {/* 第一页：4个图标 + 广告 */}
            <View style={[themedStyles.page, { width: pageWidth }]}>
              <Animated.View style={[themedStyles.firstPageIcons, firstPageIconsStyle]}>
                {getPageIcons(0).map((iconData, index) => renderIcon(iconData, index))}
              </Animated.View>
              <Animated.View style={firstPageAdStyle}>
                {renderAdvertisement()}
              </Animated.View>
            </View>

            {/* 第二页：15个图标布局 */}
            <View style={[themedStyles.page, { width: pageWidth }]}>
              <Animated.View style={[themedStyles.gridContainer, secondPageIconsStyle]}>
                {getPageIcons(1).map((iconData, index) => renderIcon(iconData, index))}
              </Animated.View>
            </View>

            {/* 第三页：剩余图标 */}
            <View style={[themedStyles.page, { width: pageWidth }]}>
              <Animated.View style={[themedStyles.lastPageContainer, thirdPageIconsStyle]}>
                {getPageIcons(2).map((iconData, index) => renderIcon(iconData, index))}
              </Animated.View>
            </View>
          </ScrollView>
        </Animated.View>
        
        {/* 动画页面指示器 */}
        <View style={themedStyles.pageIndicator}>
          {Array.from({ length: totalPages }).map((_, index) => {
            // 为每个指示器创建动画样式
            const animatedDotStyle = useAnimatedStyle(() => {
              const isActive = Math.round(scrollX.value / pageWidth) === index;
              return {
                backgroundColor: withTiming(
                  isActive ? colors.novelMain : '#cccccc',
                  { duration: 200 }
                ),
                transform: [
                  {
                    scale: withTiming(
                      isActive ? 1.2 : 1,
                      { duration: 200 }
                    )
                  }
                ]
              };
            });

            return (
              <Animated.View
                key={index}
                style={[themedStyles.dot, animatedDotStyle]}
              />
            );
          })}
        </View>
      </View>
    );
  };

  // 渲染底部方框
  const renderBottomBox = () => (
    <View style={themedStyles.bottomBox}>
      {/* 第一行：金币余额信息 */}
      <View style={themedStyles.balanceRow}>
        <Text style={themedStyles.balanceText}>{coins} 金币</Text>
        <Text style={themedStyles.balanceText}>{balance.toFixed(2)} 余额（元）</Text>
        <TouchableOpacity style={themedStyles.withdrawButton}>
          <Text style={themedStyles.withdrawText}>微信提现 &gt;</Text>
        </TouchableOpacity>
      </View>
      
      {/* 第二行：广告 */}
      <View style={themedStyles.bottomAd}>
        {renderAdvertisement()}
      </View>
    </View>
  );

  // 渲染推荐瀑布流
  const renderWaterfallGrid = () => {
    if (convertedBooks.length === 0 && !homeRecommendLoading) {
      return (
        <View style={themedStyles.emptyContainer}>
          <Text style={themedStyles.emptyText}>暂无推荐书籍</Text>
        </View>
      );
    }

    // 将书籍分为两列
    const leftColumnBooks = convertedBooks.filter((_, index) => index % 2 === 0);
    const rightColumnBooks = convertedBooks.filter((_, index) => index % 2 === 1);

    return (
      <View>
        <View style={themedStyles.waterfallGrid}>
          {/* 左列 */}
          <View style={themedStyles.waterfallColumn}>
            {leftColumnBooks.map((book, index) => (
              <BookItem 
                key={`left-${book.id}`}
                book={book} 
                index={index * 2}
                onPress={() => handleBookPress(book)}
              />
            ))}
          </View>
          
          <View style={{
            width: wp(10),
          }}></View>

          {/* 右列 */}
          <View style={themedStyles.waterfallColumn}>
            {rightColumnBooks.map((book, index) => (
              <BookItem 
                key={`right-${book.id}`}
                book={book} 
                index={index * 2 + 1}
                onPress={() => handleBookPress(book)}
              />
            ))}
          </View>
        </View>
        
        {/* 加载更多按钮 */}
        {hasMoreHomeRecommend && (
          <TouchableOpacity 
            style={themedStyles.loadMoreButton} 
            onPress={handleLoadMore}
            disabled={homeRecommendLoading}
          >
            <Text style={themedStyles.loadMoreText}>
              {homeRecommendLoading ? '加载中...' : '加载更多'}
            </Text>
          </TouchableOpacity>
        )}
        
        <LoadMoreIndicator loading={homeRecommendLoading} hasMore={hasMoreHomeRecommend} />
      </View>
    );
  };

  // 创建使用主题的动态样式
  const createThemedStyles = () => StyleSheet.create({
    // 继承基础样式
    ...styles,
    // 覆盖需要使用主题的样式
    container: {
      ...styles.container,
      backgroundColor: colors.novelBackground,
    },
    loginText: {
      ...typography.bodyMedium,
      color: colors.novelText,
    },
    iconText: {
      ...typography.labelSmall,
      color: colors.novelText,
      textAlign: 'center',
      lineHeight: fp(16),
      marginTop: wp(5),
    },
    adTitle: {
      ...typography.labelLarge,
      fontWeight: '600',
      color: colors.novelText,
      lineHeight: fp(18),
    },
    adAuthor: {
      ...typography.labelSmall,
      color: colors.novelText,
      opacity: 0.7,
      lineHeight: fp(16),
    },
    continueText: {
      ...typography.labelSmall,
      color: colors.novelMain,
      fontWeight: '500',
    },
    balanceText: {
      ...typography.labelLarge,
      color: colors.novelText,
      fontWeight: '500',
    },
    withdrawText: {
      ...typography.labelSmall,
      color: '#4caf50',
      fontWeight: '500',
    },
    emptyText: {
      ...typography.bodyMedium,
      color: colors.novelText,
      opacity: 0.6,
    },
    waterfallTitle: {
      ...typography.titleLarge,
      color: colors.novelText,
      marginBottom: wp(10),
    },
    waterfallBookTitle: {
      ...typography.labelLarge,
      fontWeight: '600',
      color: colors.novelText,
      lineHeight: fp(18),
      marginBottom: wp(5),
    },
    waterfallBookAuthor: {
      ...typography.labelSmall,
      color: colors.novelText,
      opacity: 0.7,
      lineHeight: fp(16),
      marginBottom: wp(5),
    },
    waterfallBookDescription: {
      ...typography.labelSmall,
      color: colors.novelText,
      opacity: 0.6,
      lineHeight: fp(16),
      marginBottom: wp(5),
    },
    loadMoreText: {
      ...typography.labelLarge,
      color: '#FFFFFF',
      fontWeight: '600',
    },
    waterfallLoadingText: {
      ...typography.labelLarge,
      color: colors.novelText,
      opacity: 0.6,
    },
    waterfallEndText: {
      ...typography.labelSmall,
      color: colors.novelText,
      opacity: 0.5,
      marginHorizontal: wp(10),
    },
    bottomBox: {
      width: wp(350),
      height: wp(200),
      backgroundColor: '#ffffff',
      borderRadius: sp(10),
      padding: wp(20),
      alignSelf: 'center',
      marginVertical: wp(10),
    },
    balanceRow: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      marginBottom: wp(20),
    },
    withdrawButton: {
      paddingHorizontal: wp(10),
      paddingVertical: wp(5),
    },
    waterfallGrid: {
      flexDirection: 'row',
      gap: wp(10),
    },
    waterfallColumn: {
      flex: 1,
    },
    loadMoreButton: {
      backgroundColor: colors.novelMain,
      paddingVertical: wp(12),
      paddingHorizontal: wp(24),
      borderRadius: sp(6),
      alignSelf: 'center',
      marginVertical: wp(15),
    },
  });
  
  const themedStyles = createThemedStyles();

  // 格式化阅读数
  const formatReadCount = (count: number): string => {
    if (count >= 10000) {
      return `${(count / 10000).toFixed(1)}万`;
    } else if (count >= 1000) {
      return `${(count / 1000).toFixed(1)}k`;
    }
    return count.toString();
  };

  // 使用React.memo优化BookItem - 移到组件内部
  const BookItem: React.FC<BookItemProps> = React.memo(({ book, onPress, index }) => {
    // 使用缓存的高度，避免每次重新计算
    const imageHeight = React.useMemo(() => {
      if (itemHeightCache.has(book.id)) {
        return itemHeightCache.get(book.id)!;
      }
      const baseHeight = wp(180); // 基于852px高度适配
      const variableHeight = (book.id * 17) % wp(60); // 使用ID计算，保证一致性
      const height = baseHeight + variableHeight;
      itemHeightCache.set(book.id, height);
      return height;
    }, [book.id]);

    // 根据描述长度决定显示行数
    const descriptionLines = React.useMemo(() => {
      if (!book.description) return 1;
      if (book.description.length > 80) return 3;
      if (book.description.length > 40) return 2;
      return 1;
    }, [book.description]);

    return (
      <TouchableOpacity 
        style={[themedStyles.waterfallBookItem, { width: (screenWidth - wp(45)) / 2 }]}
        onPress={onPress}
        activeOpacity={0.7}
      >
        {/* 书籍封面 */}
        <View style={[themedStyles.waterfallBookCover, { height: imageHeight }]}>
          {book.coverUrl ? (
            <Image 
              source={{ uri: book.coverUrl }} 
              style={themedStyles.waterfallCoverImage}
              resizeMode="cover"
            />
          ) : (
            <View style={themedStyles.waterfallPlaceholderCover}>
              <Text style={themedStyles.waterfallPlaceholderText}>暂无封面</Text>
            </View>
          )}
        </View>
        
        {/* 书籍信息 */}
        <View style={themedStyles.waterfallBookInfo}>
          {/* 书名 */}
          <Text 
            style={themedStyles.waterfallBookTitle}
            numberOfLines={2}
            ellipsizeMode="tail"
          >
            {book.title}
          </Text>
          
          {/* 作者 */}
          <Text 
            style={themedStyles.waterfallBookAuthor}
            numberOfLines={1}
            ellipsizeMode="tail"
          >
            {book.author}
          </Text>
          
          {/* 描述 */}
          {book.description && (
            <Text 
              style={themedStyles.waterfallBookDescription}
              numberOfLines={descriptionLines}
              ellipsizeMode="tail"
            >
              {book.description}
            </Text>
          )}
        </View>
      </TouchableOpacity>
    );
  });

  // 加载更多指示器 - 移到组件内部
  const LoadMoreIndicator: React.FC<{
    loading: boolean;
    hasMore: boolean;
  }> = React.memo(({ loading, hasMore }) => {
    if (loading) {
      return (
        <View style={themedStyles.waterfallLoadingContainer}>
          <Text style={themedStyles.waterfallLoadingText}>加载中...</Text>
        </View>
      );
    }

    if (!hasMore) {
      return (
        <View style={themedStyles.waterfallLoadingContainer}>
          <View style={themedStyles.waterfallEndLine} />
          <Text style={themedStyles.waterfallEndText}>已加载全部</Text>
          <View style={themedStyles.waterfallEndLine} />
        </View>
      );
    }

    return null;
  });

  return (
    <ScrollView 
      style={themedStyles.container} 
      showsVerticalScrollIndicator={false}
    >
      {renderTopBar()}
      {renderLoginBar()}
      {renderScrollableArea()}
      {renderBottomBox()}
      {renderWaterfallGrid()}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    paddingHorizontal: wp(15),
  },
  
  // 顶部Bar样式
  topBar: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    alignItems: 'center',
    paddingVertical: wp(10),
    gap: wp(15),
  },
  
  // 登录栏样式
  loginBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: wp(15),
    gap: wp(15),
  },
  avatar: {
    width: sp(50),
    height: sp(50),
    borderRadius: sp(25),
    overflow: 'hidden',
  },
  avatarImage: {
    width: '100%',
    height: '100%',
  },
  defaultAvatar: {
    width: '100%',
    height: '100%',
    backgroundColor: '#000000',
    borderRadius: sp(25),
  },
  loginButton: {
    flex: 1,
  },
  loginText: {
    fontSize: fp(16),
    color: '#333333',
    fontWeight: '500',
  },
  
  // 可滑动区域样式
  scrollableContainer: {
    borderRadius: sp(10),
    width: wp(350), // 调整为与pageWidth一致
    alignItems: 'center',
    backgroundColor: '#ffffff',
    marginVertical: wp(10),
    paddingBottom: wp(15),
  },
  scrollArea: {
    width: wp(350),
    borderRadius: sp(10),
    overflow: 'hidden', // 确保内容不会溢出动画容器
    paddingTop: wp(20),
  },
  page: {
    paddingHorizontal: wp(20),
    width: wp(350), // 确保页面宽度一致
  },
  
  // 第一页样式
  firstPageIcons: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-around',
    gap: wp(10),
    marginBottom: wp(10),
  },
  
  // 第二页网格布局
  gridContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-around',
    gap: wp(10),
  },
  
  // 最后一页布局
  lastPageContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'flex-start',
    gap: wp(10),
  },
  
  // 图标样式
  iconItem: {
    width: wp(60),
    alignItems: 'center',
    marginBottom: wp(15),
  },
  iconText: {
    fontSize: fp(12),
    color: useNovelColors().novelText,
    textAlign: 'center',
    lineHeight: fp(16),
    marginTop: wp(5),
  },
  
  // 页面指示器
  pageIndicator: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: wp(3),
  },
  dot: {
    width: sp(3),
    height: sp(3),
    borderRadius: sp(3),
    backgroundColor: useNovelColors().novelMain, // 默认颜色，动画会覆盖这个值
  },
  
  // 广告组件样式
  advertisement: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#f8f8f8',
    borderRadius: sp(8),
    padding: wp(15),
    gap: wp(10),
  },
  adBookCover: {
    width: wp(30),
    height: wp(20),
    backgroundColor: '#000000',
    borderRadius: sp(5),
  },
  adContent: {
    flex: 1,
    gap: wp(5),
  },
  adTitle: {
    fontSize: fp(14),
    fontWeight: '600',
    color: '#333333',
    lineHeight: fp(18),
  },
  adAuthor: {
    fontSize: fp(12),
    color: '#666666',
    lineHeight: fp(16),
  },
  continueReading: {
    paddingHorizontal: wp(10),
    paddingVertical: wp(5),
  },
  continueText: {
    fontSize: fp(12),
    color: '#ff6b6b',
    fontWeight: '500',
  },
  
  // 底部方框样式
  bottomBox: {
    width: wp(350),
    height: wp(200),
    backgroundColor: '#ffffff',
    borderRadius: sp(10),
    padding: wp(20),
    alignSelf: 'center',
    marginVertical: wp(10),
  },
  balanceRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: wp(20),
  },
  balanceText: {
    fontSize: fp(14),
    color: '#333333',
    fontWeight: '500',
  },
  withdrawButton: {
    paddingHorizontal: wp(10),
    paddingVertical: wp(5),
  },
  withdrawText: {
    fontSize: fp(12),
    color: '#4caf50',
    fontWeight: '500',
  },
  bottomAd: {
    flex: 1,
    justifyContent: 'center',
  },

  waterfallTitle: {
    fontSize: fp(18),
    fontWeight: 'bold',
    marginBottom: wp(10),
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: wp(50),
  },
  emptyText: {
    fontSize: fp(16),
    color: '#666666',
  },
  // 瀑布流样式
  waterfallGrid: {
    flexDirection: 'row',
    gap: wp(10),
  },
  waterfallColumn: {
    flex: 1,
  },
  waterfallBookItem: {
    backgroundColor: '#FFFFFF',
    borderRadius: sp(8),
    marginBottom: wp(10),
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
    overflow: 'hidden',
  },
  waterfallBookCover: {
    width: '100%',
    backgroundColor: '#F0F0F0',
  },
  waterfallCoverImage: {
    width: '100%',
    height: '100%',
  },
  waterfallPlaceholderCover: {
    width: '100%',
    height: '100%',
    backgroundColor: '#E0E0E0',
    justifyContent: 'center',
    alignItems: 'center',
  },
  waterfallPlaceholderText: {
    fontSize: fp(12),
    color: '#999999',
  },
  waterfallBookInfo: {
    padding: wp(10),
  },
  waterfallBookTitle: {
    fontSize: fp(14),
    fontWeight: '600',
    color: '#333333',
    lineHeight: fp(18),
    marginBottom: wp(5),
  },
  waterfallBookAuthor: {
    fontSize: fp(12),
    color: '#666666',
    lineHeight: fp(16),
    marginBottom: wp(5),
  },
  waterfallBookDescription: {
    fontSize: fp(12),
    color: '#666666',
    lineHeight: fp(16),
    marginBottom: wp(5),
  },
  waterfallBookMeta: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: wp(5),
  },
  waterfallMetaText: {
    fontSize: fp(10),
    color: '#999999',
  },
  waterfallLoadingContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: wp(20),
    gap: wp(8),
  },
  waterfallLoadingText: {
    fontSize: fp(14),
    color: '#666666',
  },
  waterfallEndLine: {
    width: wp(30),
    height: wp(1),
    backgroundColor: '#CCCCCC',
  },
  waterfallEndText: {
    fontSize: fp(12),
    color: '#999999',
    marginHorizontal: wp(10),
  },
  loadMoreText: {
    ...typography.labelLarge,
    color: '#FFFFFF',
    fontWeight: '600',
  },
});

export default HomePage; 