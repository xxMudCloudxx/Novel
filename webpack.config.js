// webpack.config.js
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = {
  mode: 'development',  // 或 'production'
  entry: path.resolve(__dirname, 'index.web.js'),
  output: {
    path: path.resolve(__dirname, 'web-build'),
    filename: 'bundle.js',
  },
  resolve: {
    // 支持 .web.js 优先，然后常规后缀
    extensions: ['.web.js', '.js', '.ts', '.tsx', '.json'],
    alias: {
      // 把 react-native 全部指向 react-native-web
      'react-native$': 'react-native-web',
    },
  },
  module: {
    rules: [
      {
        test: /\.[jt]sx?$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            babelrc: false,
            configFile: false,
            // 保留 RN 的 preset 并加上 Web 编译
            presets: [
              '@babel/preset-env',                    // ES 特性降级
              ['@babel/preset-react', { runtime: 'automatic' }], // React 自动 jsx
              '@babel/preset-typescript',             // TS 支持
            ],
            plugins: [
              // 如有需要，可加上其他插件
            ],
          },
        },
      },
      {
        test: /\.(png|jpe?g|gif|svg)$/,
        type: 'asset/resource',
      },
      {
        test: /\.(ttf|woff2?)$/,
        type: 'asset/resource',
      },
    ],
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: path.resolve(__dirname, 'web/index.html'),
    }),
  ],
  devServer: {
    static: { directory: path.resolve(__dirname, 'web') },
    hot: true,
    port: 8080,
  },
};
