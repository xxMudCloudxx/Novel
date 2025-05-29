import React, { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  Image,
  Dimensions,
  StyleSheet,
} from 'react-native';
import { useUserStore } from '../store/userStore';
import { useHomeStore } from '../store/homeStore';
import IconComponent from '../component/IconComponent';

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

// 使用React.memo优化BookItem
const BookItem: React.FC<BookItemProps> = React.memo(({ book, onPress, index }) => {
  // 使用缓存的高度，避免每次重新计算
  const imageHeight = React.useMemo(() => {
    if (itemHeightCache.has(book.id)) {
      return itemHeightCache.get(book.id)!;
    }
    const baseHeight = 180;
    const variableHeight = (book.id * 17) % 60; // 使用ID计算，保证一致性
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
      style={[styles.waterfallBookItem, { width: (screenWidth - 45) / 2 }]}
      onPress={onPress}
      activeOpacity={0.7}
    >
      {/* 书籍封面 */}
      <View style={[styles.waterfallBookCover, { height: imageHeight }]}>
        {book.coverUrl ? (
          <Image 
            source={{ uri: book.coverUrl }} 
            style={styles.waterfallCoverImage}
            resizeMode="cover"
          />
        ) : (
          <View style={styles.waterfallPlaceholderCover}>
            <Text style={styles.waterfallPlaceholderText}>暂无封面</Text>
          </View>
        )}
      </View>
      
      {/* 书籍信息 */}
      <View style={styles.waterfallBookInfo}>
        {/* 书名 */}
        <Text 
          style={styles.waterfallBookTitle}
          numberOfLines={2}
          ellipsizeMode="tail"
        >
          {book.title}
        </Text>
        
        {/* 作者 */}
        <Text 
          style={styles.waterfallBookAuthor}
          numberOfLines={1}
          ellipsizeMode="tail"
        >
          {book.author}
        </Text>
        
        {/* 描述 */}
        {book.description && (
          <Text 
            style={styles.waterfallBookDescription}
            numberOfLines={descriptionLines}
            ellipsizeMode="tail"
          >
            {book.description}
          </Text>
        )}
        
        {/* 额外信息 */}
        {(book.readCount || book.rating) && (
          <View style={styles.waterfallBookMeta}>
            {book.readCount && (
              <Text style={styles.waterfallMetaText}>
                阅读 {formatReadCount(book.readCount)}
              </Text>
            )}
            {book.rating && (
              <Text style={styles.waterfallMetaText}>
                {book.rating.toFixed(1)}分
              </Text>
            )}
          </View>
        )}
      </View>
    </TouchableOpacity>
  );
});

// 格式化阅读数
const formatReadCount = (count: number): string => {
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}万`;
  } else if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`;
  }
  return count.toString();
};

// 加载更多指示器
const LoadMoreIndicator: React.FC<{
  loading: boolean;
  hasMore: boolean;
}> = React.memo(({ loading, hasMore }) => {
  if (loading) {
    return (
      <View style={styles.waterfallLoadingContainer}>
        <Text style={styles.waterfallLoadingText}>加载中...</Text>
      </View>
    );
  }

  if (!hasMore) {
    return (
      <View style={styles.waterfallLoadingContainer}>
        <View style={styles.waterfallEndLine} />
        <Text style={styles.waterfallEndText}>已加载全部</Text>
        <View style={styles.waterfallEndLine} />
      </View>
    );
  }

  return null;
});

const HomePage: React.FC = () => {
  // 使用Zustand stores
  const { uid, nickname, photo, isLoggedIn, balance, coins } = useUserStore();
  const { 
    recommendBooks, 
    loading, 
    isRefreshing, 
    isLoadingMore, 
    hasMore,
    fetchRecommendBooks,
    refreshBooks,
    loadMoreBooks 
  } = useHomeStore();
  
  const [currentPage, setCurrentPage] = useState(0);

  // 初始化数据
  useEffect(() => {
    fetchRecommendBooks();
  }, [fetchRecommendBooks]);

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
    if (hasMore && !isLoadingMore && !loading) {
      loadMoreBooks();
    }
  }, [hasMore, isLoadingMore, loading, loadMoreBooks]);

  // 书籍点击
  const handleBookPress = useCallback((book: any) => {
    console.log('Book pressed:', book.title);
    // 这里可以导航到书籍详情页
  }, []);

  // 图标数据
  const iconsData: IconData[] = [
    // 第一页的5个图标
    { id: 'wallet', name: '我的钱包', icon: 'wallet', onPress: () => console.log('钱包') },
    { id: 'download', name: '我的下载', icon: 'download', onPress: () => console.log('下载') },
    { id: 'history', name: '游戏中心', icon: 'history', onPress: () => console.log('历史') },
    { id: 'subscribe', name: '推书中心', icon: 'subscribe', onPress: () => console.log('订阅') },
    { id: 'game', name: '我的', icon: 'game', onPress: () => console.log('游戏') },
    
    // 第二页的12个图标（3x4布局）
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
    
    // 最后一页的剩余图标
    { id: 'my_wallet2', name: '我的钱包', icon: 'wallet', onPress: () => console.log('我的钱包') },
    { id: 'feedback_help', name: '反馈与帮助', icon: 'feedback', onPress: () => console.log('反馈与帮助') },
  ];

  // 分页图标数据
  const getPageIcons = (pageIndex: number): IconData[] => {
    if (pageIndex === 0) {
      return iconsData.slice(0, 5);
    } else if (pageIndex === 1) {
      return iconsData.slice(5, 17);
    } else {
      return iconsData.slice(17);
    }
  };

  // 渲染顶部Bar
  const renderTopBar = () => (
    <View style={styles.topBar}>
      <TouchableOpacity onPress={() => console.log('QR Code')}>
        <IconComponent name="qrscan" width={24} height={24} />
      </TouchableOpacity>
      <TouchableOpacity onPress={() => console.log('Moon Mode')}>
        <IconComponent name="moon_mode" width={24} height={24} />
      </TouchableOpacity>
      <TouchableOpacity onPress={() => console.log('Settings')}>
        <IconComponent name="settings" width={24} height={24} />
      </TouchableOpacity>
      </View>
  );

  // 渲染登录栏
  const renderLoginBar = () => (
    <View style={styles.loginBar}>
      <View style={styles.avatar}>
        {photo ? (
          <View style={styles.avatarImage} />
        ) : (
          <View style={styles.defaultAvatar} />
        )}
      </View>
      <TouchableOpacity onPress={toLogin} style={styles.loginButton}>
        <Text style={styles.loginText}>
          {isLoggedIn && nickname ? nickname : '点击登录/注册'}
        </Text>
      </TouchableOpacity>
    </View>
  );

  // 渲染图标
  const renderIcon = (iconData: IconData, index: number) => (
    <TouchableOpacity 
      key={iconData.id} 
      style={styles.iconItem} 
      onPress={iconData.onPress}
    >
      <IconComponent name={iconData.icon} width={40} height={40} />
      <Text style={styles.iconText}>{iconData.name}</Text>
    </TouchableOpacity>
  );

  // 渲染广告组件
  const renderAdvertisement = () => (
    <View style={styles.advertisement}>
      <View style={styles.adBookCover} />
      <View style={styles.adContent}>
        <Text style={styles.adTitle} numberOfLines={2}>
          加饰披摩，高冷校花消不住了
        </Text>
        <Text style={styles.adAuthor} numberOfLines={1}>
          书时真
        </Text>
      </View>
      <TouchableOpacity style={styles.continueReading}>
        <Text style={styles.continueText}>继续阅读 &gt;</Text>
      </TouchableOpacity>
    </View>
  );

  // 渲染可滑动区域
  const renderScrollableArea = () => {
    const totalPages = 3;
    
    return (
      <View style={styles.scrollableContainer}>
        <ScrollView 
          horizontal 
          pagingEnabled 
          showsHorizontalScrollIndicator={false}
          onMomentumScrollEnd={(event: any) => {
            const pageIndex = Math.round(event.nativeEvent.contentOffset.x / 350);
            setCurrentPage(pageIndex);
          }}
          style={[
            styles.scrollArea,
            { height: currentPage === 0 ? 200 : 400 }
          ]}
        >
          {/* 第一页：5个图标 + 广告 */}
          <View style={[styles.page, { width: 350 }]}>
            <View style={styles.firstPageIcons}>
              {getPageIcons(0).map((iconData, index) => renderIcon(iconData, index))}
        </View>
        {renderAdvertisement()}
      </View>

          {/* 第二页：3x4图标布局 */}
          <View style={[styles.page, { width: 350 }]}>
            <View style={styles.gridContainer}>
              {getPageIcons(1).map((iconData, index) => renderIcon(iconData, index))}
      </View>
    </View>

          {/* 最后一页：剩余图标 */}
          <View style={[styles.page, { width: 350 }]}>
            <View style={styles.lastPageContainer}>
              {getPageIcons(2).map((iconData, index) => renderIcon(iconData, index))}
      </View>
    </View>
        </ScrollView>
        
        {/* 页面指示器 */}
        <View style={styles.pageIndicator}>
          {Array.from({ length: totalPages }).map((_, index) => (
        <View
          key={index}
          style={[
                styles.dot, 
                currentPage === index && styles.activeDot
          ]}
        />
      ))}
        </View>
    </View>
  );
  };

  // 渲染底部方框
  const renderBottomBox = () => (
    <View style={styles.bottomBox}>
      {/* 第一行：金币余额信息 */}
      <View style={styles.balanceRow}>
        <Text style={styles.balanceText}>{coins} 金币</Text>
        <Text style={styles.balanceText}>{balance.toFixed(2)} 余额（元）</Text>
        <TouchableOpacity style={styles.withdrawButton}>
          <Text style={styles.withdrawText}>微信提现 &gt;</Text>
        </TouchableOpacity>
      </View>
      
      {/* 第二行：广告 */}
      <View style={styles.bottomAd}>
      {renderAdvertisement()}
      </View>
    </View>
  );

  // 渲染推荐瀑布流
  const renderWaterfallGrid = () => {
    if (recommendBooks.length === 0 && !loading) {
      return (
        <View style={styles.emptyContainer}>
          <Text style={styles.emptyText}>暂无推荐书籍</Text>
        </View>
      );
    }

    // 将书籍分为两列
    const leftColumnBooks = recommendBooks.filter((_, index) => index % 2 === 0);
    const rightColumnBooks = recommendBooks.filter((_, index) => index % 2 === 1);

    return (
      <View style={styles.waterfallContainer}>
        <Text style={styles.waterfallTitle}>📚 推荐书籍</Text>
        
        <View style={styles.waterfallGrid}>
          {/* 左列 */}
          <View style={styles.waterfallColumn}>
            {leftColumnBooks.map((book, index) => (
              <BookItem 
                key={`left-${book.id}`}
                book={book} 
                index={index * 2}
                onPress={() => handleBookPress(book)}
              />
            ))}
          </View>
          
          {/* 右列 */}
          <View style={styles.waterfallColumn}>
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
        {hasMore && (
          <TouchableOpacity 
            style={styles.loadMoreButton} 
            onPress={handleLoadMore}
            disabled={isLoadingMore}
          >
            <Text style={styles.loadMoreText}>
              {isLoadingMore ? '加载中...' : '加载更多'}
            </Text>
          </TouchableOpacity>
        )}
        
        <LoadMoreIndicator loading={isLoadingMore} hasMore={hasMore} />
      </View>
    );
  };

  return (
    <ScrollView 
      style={styles.container} 
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
    paddingHorizontal: 15,
  },
  
  // 顶部Bar样式
  topBar: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    alignItems: 'center',
    paddingVertical: 10,
    gap: 15,
  },
  
  // 登录栏样式
  loginBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 15,
    gap: 15,
  },
  avatar: {
    width: 50,
    height: 50,
    borderRadius: 25,
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
    borderRadius: 25,
  },
  loginButton: {
    flex: 1,
  },
  loginText: {
    fontSize: 16,
    color: '#333333',
    fontWeight: '500',
  },
  
  // 可滑动区域样式
  scrollableContainer: {
    alignItems: 'center',
    marginVertical: 10,
  },
  scrollArea: {
    width: 350,
    backgroundColor: '#ffffff',
    borderRadius: 10,
    padding: 20,
  },
  page: {
    paddingHorizontal: 20,
  },
  
  // 第一页样式
  firstPageIcons: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-around',
    gap: 15,
    marginBottom: 20,
  },
  
  // 第二页网格布局
  gridContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    gap: 20,
  },
  
  // 最后一页布局
  lastPageContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'flex-start',
    gap: 20,
  },
  
  // 图标样式
  iconItem: {
    width: 60,
    alignItems: 'center',
    marginBottom: 15,
  },
  iconText: {
    fontSize: 12,
    color: '#666666',
    textAlign: 'center',
    lineHeight: 16,
    marginTop: 5,
  },
  
  // 页面指示器
  pageIndicator: {
    flexDirection: 'row',
    justifyContent: 'center',
    marginTop: 10,
    gap: 8,
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#cccccc',
  },
  activeDot: {
    backgroundColor: '#ff6b6b',
  },
  
  // 广告组件样式
  advertisement: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#f8f8f8',
    borderRadius: 8,
    padding: 15,
    gap: 10,
  },
  adBookCover: {
    width: 30,
    height: 20,
    backgroundColor: '#000000',
    borderRadius: 5,
  },
  adContent: {
    flex: 1,
    gap: 5,
  },
  adTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333333',
    lineHeight: 18,
  },
  adAuthor: {
    fontSize: 12,
    color: '#666666',
    lineHeight: 16,
  },
  continueReading: {
    paddingHorizontal: 10,
    paddingVertical: 5,
  },
  continueText: {
    fontSize: 12,
    color: '#ff6b6b',
    fontWeight: '500',
  },
  
  // 底部方框样式
  bottomBox: {
    width: 350,
    height: 200,
    backgroundColor: '#ffffff',
    borderRadius: 10,
    padding: 20,
    alignSelf: 'center',
    marginVertical: 10,
  },
  balanceRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 20,
  },
  balanceText: {
    fontSize: 14,
    color: '#333333',
    fontWeight: '500',
  },
  withdrawButton: {
    paddingHorizontal: 10,
    paddingVertical: 5,
  },
  withdrawText: {
    fontSize: 12,
    color: '#4caf50',
    fontWeight: '500',
  },
  bottomAd: {
    flex: 1,
    justifyContent: 'center',
  },
  waterfallContainer: {
    padding: 10,
  },
  waterfallTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 50,
  },
  emptyText: {
    fontSize: 16,
    color: '#666666',
  },
  // 瀑布流样式
  waterfallGrid: {
    flexDirection: 'row',
    gap: 10,
  },
  waterfallColumn: {
    flex: 1,
  },
  waterfallBookItem: {
    backgroundColor: '#FFFFFF',
    borderRadius: 8,
    marginBottom: 10,
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
    fontSize: 12,
    color: '#999999',
  },
  waterfallBookInfo: {
    padding: 10,
  },
  waterfallBookTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333333',
    lineHeight: 18,
    marginBottom: 5,
  },
  waterfallBookAuthor: {
    fontSize: 12,
    color: '#666666',
    lineHeight: 16,
    marginBottom: 5,
  },
  waterfallBookDescription: {
    fontSize: 12,
    color: '#666666',
    lineHeight: 16,
    marginBottom: 5,
  },
  waterfallBookMeta: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 5,
  },
  waterfallMetaText: {
    fontSize: 10,
    color: '#999999',
  },
  waterfallLoadingContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 20,
    gap: 8,
  },
  waterfallLoadingText: {
    fontSize: 14,
    color: '#666666',
  },
  waterfallEndLine: {
    width: 30,
    height: 1,
    backgroundColor: '#CCCCCC',
  },
  waterfallEndText: {
    fontSize: 12,
    color: '#999999',
    marginHorizontal: 10,
  },
  loadMoreButton: {
    backgroundColor: '#FF6B6B',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 6,
    alignSelf: 'center',
    marginVertical: 15,
  },
  loadMoreText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '600',
  },
});

export default HomePage; 