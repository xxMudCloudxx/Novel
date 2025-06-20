import React from 'react';
import { View, Text, TouchableOpacity } from 'react-native';

interface BottomBoxProps {
  styles: any;
  coins: number;
  balance: number;
}

export const BottomBox: React.FC<BottomBoxProps> = ({ styles, coins, balance }) => {
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
};
