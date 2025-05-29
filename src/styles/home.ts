import { StyleSheet, Dimensions } from 'react-native';

const { width: screenWidth } = Dimensions.get('window');

export const homeStyles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    paddingHorizontal: 15,
  },
  
  // 顶部导航栏
  topBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 15,
    backgroundColor: '#fff',
    marginHorizontal: -15,
    paddingHorizontal: 15,
  },
  topBarLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  topBarRight: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  iconSpacing: {
    marginLeft: 20,
  },

  // 登录栏
  loginSection: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    paddingVertical: 15,
    paddingHorizontal: 15,
    marginHorizontal: -15,
    marginTop: 10,
  },
  avatar: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: '#000',
    marginRight: 15,
  },
  loginText: {
    fontSize: 16,
    color: '#333',
    flex: 1,
  },

  // 可滑动区域
  pagerContainer: {
    width: 350,
    height: 400,
    alignSelf: 'center',
    marginVertical: 20,
  },
  pageView: {
    flex: 1,
    paddingHorizontal: 20,
    paddingVertical: 20,
  },
  
  // 第一页布局
  firstPageContainer: {
    height: 200,
  },
  iconsRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: 20,
  },
  iconItem: {
    alignItems: 'center',
    width: 60,
  },
  iconWrapper: {
    width: 40,
    height: 40,
    marginBottom: 8,
  },
  iconText: {
    fontSize: 12,
    color: '#666',
    textAlign: 'center',
  },

  // 广告区域
  adContainer: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 15,
    alignItems: 'center',
    marginBottom: 15,
    shadowColor: '#000',
    shadowOpacity: 0.1,
    shadowOffset: { width: 0, height: 1 },
    shadowRadius: 3,
    elevation: 2,
  },
  adImage: {
    width: 20,
    height: 30,
    backgroundColor: '#000',
    borderRadius: 5,
    marginRight: 15,
  },
  adContent: {
    flex: 1,
  },
  adTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    marginBottom: 5,
  },
  adAuthor: {
    fontSize: 12,
    color: '#666',
  },
  continueReading: {
    fontSize: 12,
    color: '#007AFF',
    paddingHorizontal: 10,
  },

  // 页面指示器
  indicator: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 10,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#ddd',
    marginHorizontal: 3,
  },
  activeDot: {
    backgroundColor: '#007AFF',
  },

  // 第二页网格布局
  gridContainer: {
    flex: 1,
    paddingHorizontal: 10,
  },
  gridItem: {
    alignItems: 'center',
    marginBottom: 20,
  },

  // 金币余额区域
  balanceContainer: {
    width: 350,
    height: 200,
    alignSelf: 'center',
    backgroundColor: '#fff',
    borderRadius: 10,
    padding: 15,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOpacity: 0.1,
    shadowOffset: { width: 0, height: 1 },
    shadowRadius: 3,
    elevation: 2,
  },
  balanceRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 15,
  },
  balanceItem: {
    alignItems: 'center',
  },
  balanceValue: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
  },
  balanceLabel: {
    fontSize: 12,
    color: '#666',
    marginTop: 5,
  },
  wechatWithdraw: {
    fontSize: 14,
    color: '#07C160',
  },

  // 瀑布流区域
  waterfallContainer: {
    paddingHorizontal: 15,
  },
  bookItem: {
    width: 167.5,
    backgroundColor: '#fff',
    borderRadius: 8,
    marginBottom: 15,
    shadowColor: '#000',
    shadowOpacity: 0.1,
    shadowOffset: { width: 0, height: 1 },
    shadowRadius: 3,
    elevation: 2,
  },
  bookCover: {
    width: 167.5,
    height: 280,
    borderTopLeftRadius: 8,
    borderTopRightRadius: 8,
    backgroundColor: '#f0f0f0',
  },
  bookInfo: {
    padding: 10,
  },
  bookTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    lineHeight: 20,
    marginBottom: 10,
  },
  bookDescription: {
    fontSize: 12,
    color: '#666',
    lineHeight: 18,
  },
  textEllipsis: {
    overflow: 'hidden',
  },
}); 