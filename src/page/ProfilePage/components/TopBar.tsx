import React from 'react';
import { View, TouchableOpacity } from 'react-native';
import IconComponent from '../../../component/IconComponent';
import { commonSizes } from '../../../utils/theme/dimensions';

interface TopBarProps {
  styles: any;
}

export const TopBar: React.FC<TopBarProps> = ({ styles }) => (
  <View style={styles.topBar}>
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