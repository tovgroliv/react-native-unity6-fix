module.exports = {
  presets: ['@react-native/babel-preset', '@babel/preset-typescript'],
  env: {
    production: {}
  },
  plugins: [
    'module:react-native-dotenv',
    '@babel/plugin-transform-export-namespace-from',
    [
      'module-resolver',
      {
        root: ['./src'],
        alias: { '^~(.+)': './src/\\1' },
        extensions: ['.ios.js', '.android.js', '.ios.ts', '.android.ts', '.ios.tsx', '.android.tsx', '.js', '.ts', '.tsx', '.json']
      }
    ],
    'react-native-reanimated/plugin'
  ]
}
