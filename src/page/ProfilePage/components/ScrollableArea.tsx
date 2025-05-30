import React, { useState } from 'react';
import { View, ScrollView, TouchableOpacity, Text } from 'react-native';
import Animated, { useAnimatedStyle, withTiming } from 'react-native-reanimated';
import IconComponent from '../../../component/IconComponent';
import { getPageIcons, PAGE_WIDTH } from '../utils/constants';
import { IconData } from '../types';
import { wp } from '../../../utils/theme/dimensions';

interface ScrollableAreaProps {
  styles: any;
  scrollX: any;
  animatedContainerStyle: any;
  firstPageIconsStyle: any;
  secondPageIconsStyle: any;
  thirdPageIconsStyle: any;
  firstPageAdStyle: any;
  colors: any;
}

export const ScrollableArea: React.FC<ScrollableAreaProps> = ({
  styles,
  scrollX,
  animatedContainerStyle,
  firstPageIconsStyle,
  secondPageIconsStyle,
  thirdPageIconsStyle,
  firstPageAdStyle,
  colors,
}) => {
  const [currentPage, setCurrentPage] = useState(0);
  const totalPages = 3;

  // 渲染图标
  const renderIcon = (iconData: IconData, index: number) => (
    <TouchableOpacity 
      key={iconData.id} 
      style={styles.iconItem} 
      onPress={iconData.onPress}
    >
      <IconComponent name={iconData.icon} width={wp(25)} height={wp(25)} />
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

  return (
    <View style={styles.scrollableContainer}>
      <Animated.View style={[styles.scrollArea, animatedContainerStyle]}>
        <ScrollView 
          horizontal 
          pagingEnabled 
          showsHorizontalScrollIndicator={false}
          onMomentumScrollEnd={(event: any) => {
            const pageIndex = Math.round(event.nativeEvent.contentOffset.x / PAGE_WIDTH);
            setCurrentPage(pageIndex);
          }}
          onScroll={(event: any) => {
            scrollX.value = event.nativeEvent.contentOffset.x;
          }}
          scrollEventThrottle={16}
        >
          {/* 第一页：4个图标 + 广告 */}
          <View style={[styles.page, { width: PAGE_WIDTH }]}>
            <Animated.View style={[styles.firstPageIcons, firstPageIconsStyle]}>
              {getPageIcons(0).map((iconData, index) => renderIcon(iconData, index))}
            </Animated.View>
            <Animated.View style={firstPageAdStyle}>
              {renderAdvertisement()}
            </Animated.View>
          </View>

          {/* 第二页：15个图标布局 */}
          <View style={[styles.page, { width: PAGE_WIDTH }]}>
            <Animated.View style={[styles.gridContainer, secondPageIconsStyle]}>
              {getPageIcons(1).map((iconData, index) => renderIcon(iconData, index))}
            </Animated.View>
          </View>

          {/* 第三页：剩余图标 */}
          <View style={[styles.page, { width: PAGE_WIDTH }]}>
            <Animated.View style={[styles.lastPageContainer, thirdPageIconsStyle]}>
              {getPageIcons(2).map((iconData, index) => renderIcon(iconData, index))}
            </Animated.View>
          </View>
        </ScrollView>
      </Animated.View>
      
      {/* 动画页面指示器 */}
      <View style={styles.pageIndicator}>
        {Array.from({ length: totalPages }).map((_, index) => {
          // 为每个指示器创建动画样式
          const animatedDotStyle = useAnimatedStyle(() => {
            const isActive = Math.round(scrollX.value / PAGE_WIDTH) === index;
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
              style={[styles.dot, animatedDotStyle]}
            />
          );
        })}
      </View>
    </View>
  );
}; 