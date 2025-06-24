import React, { useState, useRef, useEffect, useMemo, useCallback } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  SafeAreaView,
  ScrollView,
} from 'react-native';
import { createTimedSwitchPageStyles } from '../styles/TimedSwitchPageStyles';
import { useNovelColors } from '../../../../utils/theme/colors';

// 简单的时间选择器组件
interface TimePickerProps {
  value: string;
  onTimeChange: (time: string) => void;
}

const TimePicker: React.FC<TimePickerProps> = ({ value, onTimeChange }) => {
  const colors = useNovelColors();
  const styles = createTimedSwitchPageStyles(colors);

  const hours = useMemo(() => Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0')), []);
  const minutes = useMemo(() => ['00', '15', '30', '45'], []); // 简化为15分钟间隔

  const [hour, minute] = value.split(':');

  const hourScrollRef = useRef<any>(null);
  const minuteScrollRef = useRef<any>(null);
  const [hourScrollReady, setHourScrollReady] = useState(false);
  const [minuteScrollReady, setMinuteScrollReady] = useState(false);

  // 滚动到指定时间位置的函数
  const scrollToTime = useCallback(() => {
    // 小时滚动位置
    const hourIndex = hours.findIndex(h => h === hour);
    if (hourIndex >= 0 && hourScrollRef.current && hourScrollReady) {
      const itemHeight = 40; // 对应timeItem的高度
      hourScrollRef.current.scrollTo({
        y: hourIndex * itemHeight,
        animated: true,
      });
    }

    // 分钟滚动位置
    const minuteIndex = minutes.findIndex(m => m === minute);
    if (minuteIndex >= 0 && minuteScrollRef.current && minuteScrollReady) {
      const itemHeight = 40;
      minuteScrollRef.current.scrollTo({
        y: minuteIndex * itemHeight,
        animated: true,
      });
    }
  }, [hour, minute, hours, minutes, hourScrollReady, minuteScrollReady]);

  // 当时间值改变时，重新滚动到正确位置
  useEffect(() => {
    if (hourScrollReady && minuteScrollReady) {
      // 稍微延迟一下确保渲染完成
      const timer = setTimeout(scrollToTime, 200);
      return () => clearTimeout(timer);
    }
  }, [scrollToTime, hourScrollReady, minuteScrollReady]);

  // 小时ScrollView布局完成回调
  const onHourScrollLayout = () => {
    setHourScrollReady(true);
  };

  // 分钟ScrollView布局完成回调
  const onMinuteScrollLayout = () => {
    setMinuteScrollReady(true);
  };

  return (
    <View style={styles.timePickerContainer}>
      {/* 小时选择 */}
      <View style={styles.timeColumn}>
        <Text style={styles.timeLabel}>时</Text>
        <ScrollView
          ref={hourScrollRef}
          style={styles.timeScrollView}
          showsVerticalScrollIndicator={false}
          decelerationRate="fast"
          snapToInterval={40} // 对应itemHeight
          snapToAlignment="center"
          onLayout={onHourScrollLayout}
        >
          {hours.map((h) => (
            <TouchableOpacity
              key={h}
              style={[
                styles.timeItem,
                h === hour && styles.timeItemSelected,
              ]}
              onPress={() => onTimeChange(`${h}:${minute}`)}
            >
              <Text
                style={[
                  styles.timeItemText,
                  h === hour && styles.timeItemTextSelected,
                ]}
              >
                {h}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      <Text style={styles.timeSeparator}>:</Text>

      {/* 分钟选择 */}
      <View style={styles.timeColumn}>
        <Text style={styles.timeLabel}>分</Text>
        <ScrollView
          ref={minuteScrollRef}
          style={styles.timeScrollView}
          showsVerticalScrollIndicator={false}
          decelerationRate="fast"
          snapToInterval={40} // 对应itemHeight
          snapToAlignment="center"
          onLayout={onMinuteScrollLayout}
        >
          {minutes.map((m) => (
            <TouchableOpacity
              key={m}
              style={[
                styles.timeItem,
                m === minute && styles.timeItemSelected,
              ]}
              onPress={() => onTimeChange(`${hour}:${m}`)}
            >
              <Text
                style={[
                  styles.timeItemText,
                  m === minute && styles.timeItemTextSelected,
                ]}
              >
                {m}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>
    </View>
  );
};

interface TimePickerModalProps {
  visible: boolean;
  onClose: () => void;
  onConfirm: (time: string) => void;
  initialTime: string;
  title: string; // 动态标题，如"设置日间模式时间"或"设置夜间模式时间"
}

/**
 * 时间选择器模态框
 * 用于选择单个时间（日间模式或夜间模式）
 */
export const TimePickerModal: React.FC<TimePickerModalProps> = ({
  visible,
  onClose,
  onConfirm,
  initialTime,
  title,
}) => {
  const colors = useNovelColors();
  const styles = createTimedSwitchPageStyles(colors);

  const [selectedTime, setSelectedTime] = useState(initialTime);

  // 当初始时间改变时，更新选中时间
  useEffect(() => {
    setSelectedTime(initialTime);
  }, [initialTime]);

  // 当模态框显示状态改变时，重置TimePicker的状态
  useEffect(() => {
    if (visible) {
      // 模态框显示时，确保选中正确的时间
      setSelectedTime(initialTime);
    }
  }, [visible, initialTime]);

  const handleConfirm = () => {
    onConfirm(selectedTime);
    onClose();
  };

  const handleCancel = () => {
    // 重置为初始值
    setSelectedTime(initialTime);
    onClose();
  };

  if (!visible) {
    return null;
  }

  return (
    <View style={styles.modalOverlay}>
      <SafeAreaView style={styles.modalContainer}>
        {/* 标题栏 */}
        <View style={styles.modalHeader}>
          <TouchableOpacity onPress={handleCancel}>
            <Text style={styles.modalCancelText}>取消</Text>
          </TouchableOpacity>
          <Text style={styles.modalTitle}>{title}</Text>
          <TouchableOpacity onPress={handleConfirm}>
            <Text style={styles.modalConfirmText}>确认</Text>
          </TouchableOpacity>
        </View>

        {/* 时间选择内容 */}
        <View style={styles.modalContent}>
          <TimePicker value={selectedTime} onTimeChange={setSelectedTime} />

          {/* 提示信息 */}
          <View style={styles.timeHint}>
            <Text style={styles.timeHintText}>
              当前选择时间：{selectedTime}
            </Text>
          </View>
        </View>
      </SafeAreaView>
    </View>
  );
};
