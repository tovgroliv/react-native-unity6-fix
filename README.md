# @azesmway/react-native-unity

The plugin that allows you to embed a Unity project into React Native as a full-fledged component. The plugin now supports the new architecture.

> [!NOTE]
> 
> **Unity 6 & Expo Router Compatibility Fix**
> 
> This version fixes compilation and runtime crash issues in Unity 6 and Expo Router (Expo SDK 53) environments:
> - Fixed `UnityPlayer` type compatibility issues, supporting Unity 6+ and earlier versions
> - Resolved "The specified child already has a parent" runtime crashes
> - Improved view parent-child relationship management to prevent memory leaks
> - Enhanced error handling mechanisms for better app stability

Original Library:
https://github.com/azesmway/react-native-unity

You can either patch /android/src/main/java into original version or install this repo locally