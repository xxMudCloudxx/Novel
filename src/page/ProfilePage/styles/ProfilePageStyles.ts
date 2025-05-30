import { StyleSheet } from 'react-native';
import { wp, fp, sp, commonSizes } from '../../../utils/theme/dimensions';
import { typography } from '../../../utils/theme/typography';
import { NovelColors } from '../../../utils/theme/colors';
import { PAGE_WIDTH } from '../utils/constants';

export const createHomePageStyles = (colors: NovelColors) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.novelBackground,
    paddingHorizontal: wp(15),
  },
  
  scrollView: {
    flex: 1,
  },
  
  scrollContent: {
    paddingBottom: wp(100),
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
    ...typography.bodyMedium,
    color: colors.novelText,
  },
  
  // 可滑动区域样式
  scrollableContainer: {
    borderRadius: sp(10),
    width: PAGE_WIDTH,
    alignItems: 'center',
    backgroundColor: '#ffffff',
    marginVertical: wp(10),
    paddingBottom: wp(15),
  },
  scrollArea: {
    width: PAGE_WIDTH,
    borderRadius: sp(10),
    overflow: 'hidden',
    paddingTop: wp(20),
  },
  page: {
    paddingHorizontal: wp(20),
    width: PAGE_WIDTH,
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
    ...typography.labelSmall,
    color: colors.novelText,
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
    backgroundColor: colors.novelMain,
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
  continueReading: {
    paddingHorizontal: wp(10),
    paddingVertical: wp(5),
  },
  continueText: {
    ...typography.labelSmall,
    color: colors.novelMain,
    fontWeight: '500',
  },
  
  // 底部方框样式
  bottomBox: {
    width: PAGE_WIDTH,
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
    ...typography.labelLarge,
    color: colors.novelText,
    fontWeight: '500',
  },
  withdrawButton: {
    paddingHorizontal: wp(10),
    paddingVertical: wp(5),
  },
  withdrawText: {
    ...typography.labelSmall,
    color: '#4caf50',
    fontWeight: '500',
  },
  bottomAd: {
    flex: 1,
    justifyContent: 'center',
  },

  // 瀑布流样式
  waterfallGrid: {
    flexDirection: 'row',
    gap: wp(15),
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
  
  // 空状态
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: wp(50),
  },
  emptyText: {
    ...typography.bodyMedium,
    color: colors.novelText,
    opacity: 0.6,
  },
  
  // 加载相关样式
  waterfallLoadingContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: wp(20),
    gap: wp(8),
  },
  waterfallLoadingText: {
    ...typography.labelLarge,
    color: colors.novelText,
    opacity: 0.6,
  },
  waterfallEndLine: {
    width: wp(30),
    height: wp(1),
    backgroundColor: '#CCCCCC',
  },
  waterfallEndText: {
    ...typography.labelSmall,
    color: colors.novelText,
    opacity: 0.5,
    marginHorizontal: wp(10),
  },
  
  // 刷新指示器样式
  refreshIndicator: {
    backgroundColor: colors.novelBackground,
    paddingVertical: wp(15),
    paddingHorizontal: wp(20),
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
    marginBottom: wp(10),
  },
  refreshContent: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  loadingSpinner: {
    marginRight: wp(10),
  },
  spinnerText: {
    fontSize: fp(14),
    color: '#333333',
    fontWeight: '500',
  },
  refreshText: {
    fontSize: fp(14),
    color: '#333333',
    fontWeight: '500',
  },
  
  // 底部加载指示器
  bottomLoadingContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: wp(20),
  },
  loadingText: {
    fontSize: fp(14),
    color: '#666666',
    fontWeight: '500',
  },
}); 