declare module '*.svg' {
    import React from 'react';
    import type { SvgProps } from 'react-native-svg';
    const Component: React.FC<SvgProps>;
    export default Component;
  }