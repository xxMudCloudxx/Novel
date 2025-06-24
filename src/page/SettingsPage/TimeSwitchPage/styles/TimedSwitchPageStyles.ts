import { StyleSheet } from 'react-native';
import { wp, fp } from '../../../../utils/theme/dimensions';
import { typography } from '../../../../utils/theme/typography';
import { NovelColors } from '../../../../utils/theme/colors';

/**
 * 创建定时切换页面样式
 * @param colors 当前主题下的颜色配置
 * @returns StyleSheet 样式表
 */
export const createTimedSwitchPageStyles = (colors: NovelColors) => {
  return StyleSheet.create({
    // === 容器样式 ===
    container: {
      flex: 1,
      backgroundColor: colors.novelBackground,
    },

    // === 顶部导航栏样式 ===
    topBar: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingHorizontal: wp(20),
      paddingVertical: wp(16),
      backgroundColor: colors.novelBackground,
      borderBottomWidth: 0.5,
      borderBottomColor: colors.novelDivider,
      minHeight: wp(56),
    },

    backButton: {
      justifyContent: 'center',
      alignItems: 'center',
      minWidth: wp(32),
      minHeight: wp(32),
    },

    backArrow: {
      fontSize: fp(40),
      color: colors.novelText,
      fontWeight: '300',
      lineHeight: fp(28),
    },

    titleContainer: {
      flex: 1,
      justifyContent: 'center',
      alignItems: 'center',
    },

    topBarTitle: {
      ...typography.bodyMedium,
      fontSize: fp(18),
      color: colors.novelText,
      fontWeight: '600',
      textAlign: 'center',
    },

    rightPlaceholder: {
      width: wp(32),
      height: wp(32),
    },

    // === 内容样式 ===
    descriptionContainer: {
      paddingHorizontal: wp(20),
      paddingVertical: wp(20),
      backgroundColor: colors.novelBackground,
    },

    descriptionText: {
      ...typography.labelLarge,
      fontSize: fp(14),
      color: colors.novelTextGray,
      lineHeight: fp(20),
    },

    settingsContainer: {
      backgroundColor: colors.novelSecondaryBackground,
    },

    // === 时间选择器样式 ===
    timePickerContainer: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'center',
      paddingVertical: wp(20),
    },

    timeColumn: {
      alignItems: 'center',
      marginHorizontal: wp(20),
    },

    timeLabel: {
      ...typography.labelLarge,
      fontSize: fp(14),
      color: colors.novelTextGray,
      marginBottom: wp(10),
    },

    timeScrollView: {
      maxHeight: wp(120),
      width: wp(60),
    },

    timeItem: {
      paddingVertical: wp(8),
      paddingHorizontal: wp(12),
      borderRadius: wp(8),
      marginVertical: wp(2),
      alignItems: 'center',
    },

    timeItemSelected: {
      backgroundColor: colors.novelMain,
    },

    timeItemText: {
      ...typography.labelLarge,
      fontSize: fp(16),
      color: colors.novelText,
    },

    timeItemTextSelected: {
      color: colors.novelSecondaryBackground,
      fontWeight: '600',
    },

    timeSeparator: {
      ...typography.bodyMedium,
      fontSize: fp(24),
      color: colors.novelText,
      fontWeight: '600',
    },

    // === 时间选择器模态框样式 ===
    modalOverlay: {
      position: 'absolute',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      flex: 1,
      backgroundColor: 'rgba(0, 0, 0, 0.5)',
      justifyContent: 'flex-end',
      zIndex: 1000,
    },

    modalContainer: {
      backgroundColor: colors.novelSecondaryBackground,
      borderTopLeftRadius: wp(20),
      borderTopRightRadius: wp(20),
      maxHeight: '80%',
    },

    modalHeader: {
      flexDirection: 'row',
      justifyContent: 'space-between',
      alignItems: 'center',
      paddingHorizontal: wp(20),
      paddingVertical: wp(16),
      borderBottomWidth: 0.5,
      borderBottomColor: colors.novelDivider,
    },

    modalTitle: {
      ...typography.bodyMedium,
      fontSize: fp(18),
      color: colors.novelText,
      fontWeight: '600',
    },

    modalCancelText: {
      ...typography.labelLarge,
      fontSize: fp(16),
      color: colors.novelTextGray,
    },

    modalConfirmText: {
      ...typography.labelLarge,
      fontSize: fp(16),
      color: colors.novelMain,
      fontWeight: '600',
    },

    modalContent: {
      padding: wp(20),
    },

    timeHint: {
      marginTop: wp(20),
      padding: wp(15),
      backgroundColor: colors.novelChipBackground,
      borderRadius: wp(10),
    },

    timeHintText: {
      ...typography.labelLarge,
      fontSize: fp(14),
      color: colors.novelTextGray,
      textAlign: 'center',
      lineHeight: fp(20),
    },
  });
};
