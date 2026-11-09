# myTime Android

`two` 分支的 Android 版本是一个极简主屏幕时钟小组件，同时显示北京时间和阿尔及利亚内陆廷杜夫的当地日期、星期和时间，不需要网络、账号或特殊权限。

## 构建

用 Android Studio 打开本目录 `android/`，等待 Gradle 同步完成后运行 `app` 配置即可。也可以在已安装 Android SDK 和 Gradle 8.2 的环境中运行：

```bash
gradle --no-daemon :app:assembleDebug
gradle --no-daemon :app:assembleRelease
```

APK 输出在 `app/build/outputs/apk/`，当前工作流提供使用 Android 调试签名、可以直接安装的测试包：

- `two-0.1.0-debug.apk`

正式发布构建需要配置自己的 Android 签名密钥；未签名的 `release` APK 不能直接安装。

## 添加小组件

1. 安装 APK。
2. 长按 Android 手机主屏幕，选择“小组件”。
3. 找到 `myTime 时钟`，拖到主屏幕。
4. 调整小组件尺寸即可。

小组件会在主屏幕上显示北京时间和阿尔及利亚·廷杜夫的月日、星期和时间，系统负责在后台唤醒更新；不同手机系统可能会对更新时间做少量延迟。
