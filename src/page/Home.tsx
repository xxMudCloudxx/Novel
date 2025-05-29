import React, { useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Image,
  Dimensions,
  FlatList,
  RefreshControl,
  ActivityIndicator,
  TextInput,
} from 'react-native';
import { useUserStore } from '../store/userStore';
import { useHomeStore } from '../store/homeStore';
import type { Book } from '../store/homeStore';

const { width: screenWidth } = Dimensions.get('window');
const itemWidth = (screenWidth - 45) / 2; // 15px边距 + 15px间距

interface BookItemProps {
  book: Book;
  onPress: (book: Book) => void;
}

const BookItem: React.FC<BookItemProps> = React.memo(({ book, onPress }) => {
  return (
    <TouchableOpacity 
      style={[styles.bookItem, { width: itemWidth }]}
      onPress={() => onPress(book)}
      activeOpacity={0.7}
    >
      <Image 
        source={{ uri: book.coverUrl }} 
        style={styles.bookCover}
        resizeMode="cover"
      />
      <View style={styles.bookInfo}>
        <Text style={styles.bookTitle} numberOfLines={2}>
          {book.title}
        </Text>
        <Text style={styles.bookAuthor} numberOfLines={1}>
          {book.author}
        </Text>
        {book.readCount && (
          <Text style={styles.readCount}>
            阅读量: {book.readCount.toLocaleString()}
          </Text>
        )}
      </View>
    </TouchableOpacity>
  );
});

const RankTab: React.FC<{
  title: string;
  type: 'hot' | 'new' | 'recommend';
  isSelected: boolean;
  onPress: () => void;
}> = ({ title, type, isSelected, onPress }) => {
  return (
    <TouchableOpacity
      style={[styles.rankTab, isSelected && styles.rankTabActive]}
      onPress={onPress}
    >
      <Text style={[styles.rankTabText, isSelected && styles.rankTabTextActive]}>
        {title}
      </Text>
    </TouchableOpacity>
  );
};

const LoadMoreIndicator: React.FC<{
  isLoading: boolean;
  hasMore: boolean;
  onLoadMore: () => void;
}> = ({ isLoading, hasMore, onLoadMore }) => {
  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="small" color="#FF6B6B" />
        <Text style={styles.loadingText}>加载中...</Text>
      </View>
    );
  }

  if (!hasMore) {
    return (
      <View style={styles.loadingContainer}>
        <Text style={styles.loadingText}>没有更多数据了</Text>
      </View>
    );
  }

  return null;
};

const HomePage: React.FC = () => {
  // 使用Zustand stores
  const {
    recommendBooks,
    loading,
    error,
    isRefreshing,
    isLoadingMore,
    hasMore,
    searchQuery,
    selectedCategoryId,
    rankBooks,
    selectedRankType,
    fetchRecommendBooks,
    refreshBooks,
    loadMoreBooks,
    setSearchQuery,
    setCategoryFilter,
    setRankType,
    fetchRankBooks,
  } = useHomeStore();

  // 初始化数据
  useEffect(() => {
    fetchRecommendBooks();
  }, [fetchRecommendBooks]);

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
  const handleBookPress = useCallback((book: Book) => {
    console.log('Book pressed:', book.title);
    // 这里可以导航到书籍详情页
  }, []);

  // 搜索
  const handleSearch = useCallback((query: string) => {
    setSearchQuery(query);
    // 触发搜索
    if (query.trim()) {
      // 可以添加搜索逻辑
    } else {
      fetchRecommendBooks();
    }
  }, [setSearchQuery, fetchRecommendBooks]);

  // 榜单切换
  const handleRankTypeChange = useCallback((type: 'hot' | 'new' | 'recommend') => {
    setRankType(type);
    // 获取对应榜单数据
    fetchRankBooks(type);
  }, [setRankType, fetchRankBooks]);

  // 渲染头部
  const renderHeader = () => (
    <View style={styles.header}>
      {/* 搜索框 */}
      <View style={styles.searchContainer}>
        <TextInput
          style={styles.searchInput}
          placeholder="搜索书籍..."
          value={searchQuery}
          onChangeText={handleSearch}
          returnKeyType="search"
          onSubmitEditing={() => handleSearch(searchQuery)}
        />
      </View>

      {/* 榜单切换 */}
      <View style={styles.rankContainer}>
        <Text style={styles.sectionTitle}>热门榜单</Text>
        <View style={styles.rankTabs}>
          <RankTab
            title="热门"
            type="hot"
            isSelected={selectedRankType === 'hot'}
            onPress={() => handleRankTypeChange('hot')}
          />
          <RankTab
            title="最新"
            type="new"
            isSelected={selectedRankType === 'new'}
            onPress={() => handleRankTypeChange('new')}
          />
          <RankTab
            title="推荐"
            type="recommend"
            isSelected={selectedRankType === 'recommend'}
            onPress={() => handleRankTypeChange('recommend')}
          />
        </View>
      </View>
    </View>
  );

  // 渲染书籍项
  const renderBookItem = ({ item, index }: { item: Book; index: number }) => (
    <BookItem 
      key={item.id} 
      book={item} 
      onPress={handleBookPress}
    />
  );

  const renderFooter = () => (
    <LoadMoreIndicator
      isLoading={isLoadingMore}
      hasMore={hasMore}
      onLoadMore={handleLoadMore}
    />
  );

  if (loading && recommendBooks.length === 0) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#FF6B6B" />
        <Text style={styles.loadingText}>加载中...</Text>
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.errorContainer}>
        <Text style={styles.errorText}>{error}</Text>
        <TouchableOpacity style={styles.retryButton} onPress={fetchRecommendBooks}>
          <Text style={styles.retryText}>重试</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <FlatList
        data={recommendBooks}
        renderItem={renderBookItem}
        keyExtractor={(item) => item.id.toString()}
        numColumns={2}
        columnWrapperStyle={styles.row}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={isRefreshing}
            onRefresh={handleRefresh}
            colors={['#FF6B6B']}
            tintColor="#FF6B6B"
          />
        }
        onEndReached={handleLoadMore}
        onEndReachedThreshold={0.1}
        ListHeaderComponent={renderHeader}
        ListFooterComponent={renderFooter}
        contentContainerStyle={styles.contentContainer}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  listContent: {
    paddingBottom: 20,
  },
  header: {
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 15,
    paddingVertical: 10,
    marginBottom: 10,
  },
  searchContainer: {
    marginBottom: 15,
  },
  searchInput: {
    height: 40,
    borderRadius: 20,
    backgroundColor: '#F0F0F0',
    paddingHorizontal: 15,
    fontSize: 14,
  },
  rankContainer: {
    marginBottom: 15,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333333',
    marginBottom: 10,
  },
  rankTabs: {
    flexDirection: 'row',
    backgroundColor: '#F0F0F0',
    borderRadius: 20,
    padding: 2,
  },
  rankTab: {
    flex: 1,
    paddingVertical: 8,
    alignItems: 'center',
    borderRadius: 18,
  },
  rankTabActive: {
    backgroundColor: '#FF6B6B',
  },
  rankTabText: {
    fontSize: 14,
    color: '#666666',
  },
  rankTabTextActive: {
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
  row: {
    justifyContent: 'space-between',
    paddingHorizontal: 15,
    marginBottom: 15,
  },
  bookItem: {
    backgroundColor: '#FFFFFF',
    borderRadius: 8,
    overflow: 'hidden',
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
  },
  bookCover: {
    width: '100%',
    height: 160,
  },
  bookInfo: {
    padding: 10,
  },
  bookTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#333333',
    marginBottom: 5,
    lineHeight: 18,
  },
  bookAuthor: {
    fontSize: 12,
    color: '#666666',
    marginBottom: 5,
  },
  readCount: {
    fontSize: 10,
    color: '#999999',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  loadingText: {
    fontSize: 16,
    color: '#666666',
    marginTop: 10,
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  errorText: {
    fontSize: 16,
    color: '#FF6B6B',
    textAlign: 'center',
    marginBottom: 20,
  },
  retryButton: {
    backgroundColor: '#FF6B6B',
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 20,
  },
  retryText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: 'bold',
  },
  contentContainer: {
    paddingBottom: 20,
  },
});

export default HomePage; 