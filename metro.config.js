const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');

/**
 * Metro configuration
 * https://reactnative.dev/docs/metro
 *
 * @type {import('@react-native/metro-config').MetroConfig}
 */

const defaultConfig = getDefaultConfig(__dirname);

module.exports = mergeConfig(defaultConfig, {
  transformer: {
    babelTransformerPath: require.resolve('react-native-svg-transformer'),
  },
  resolver: {
    // 移除默认 assetExts 中的 svg
    assetExts: defaultConfig.resolver.assetExts.filter(ext => ext !== 'svg'),
    // 增加对 svg 的源码解析
    sourceExts: [...defaultConfig.resolver.sourceExts, 'svg'],
  },
});
