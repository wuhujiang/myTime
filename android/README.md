# myTime Android

`two` 分支的 Android 版本是一个极简主屏幕时钟小组件，同时显示北京时间和阿尔及利亚内陆廷杜夫的当地时间，不需要网络、账号或特殊权限。

## 构建

用 Android Studio 打开本目录 `android/`，等待 Gradle 同步完成后运行 `app` 配置即可。也可以在已安装 Android SDK 和 Gradle 8.2 的环境中运行：

```bash
gradle --no-daemon :app:assembleDebug
gradle --no-daemon :app:assembleRelease
```

APK 输出在 `app/build/outputs/apk/`，文件名分别类似：

- `two-0.1.0-debug.apk`
- `two-0.1.0-release.apk`

发布构建默认未签名，正式分发前需要使用自己的 Android 签名密钥签名。

## 添加小组件

1. 安装 APK。
2. 长按 Android 手机主屏幕，选择“小组件”。
3. 找到 `myTime 时钟`，拖到主屏幕。
4. 调整小组件尺寸即可。

小组件会在主屏幕上显示北京时间和阿尔及利亚·廷杜夫时间，系统负责在后台唤醒更新；不同手机系统可能会对更新时间做少量延迟。
