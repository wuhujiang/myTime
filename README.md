# myTime

Windows 版本与 Android 版本分别维护在不同分支：`dev` 分支发布 Windows 绿色版，`two` 分支发布 Android 主屏幕时钟小组件。

Windows 绿色版默认显示洛杉矶和北京的时间，支持秒数、窗口置顶和托盘隐藏。每个城市名称下方显示当地的月、日和星期，例如“11月3日 周一”。各行的日期和时间对应同一时刻，分别按各自时区换算、跨日更新，不受电脑本地时区设置影响。

![myTime 界面预览：洛杉矶和北京分别显示当地日期、星期和时间](assets/screenshot.png)

## 下载与使用

1. 打开本仓库的 **Releases**，进入最新版本。
2. 在 **Assets** 中下载 `myTime-版本号-windows.zip`。
3. 将压缩包完整解压到可写目录，例如 `D:\Apps`。
4. 打开解压后的 `myTime` 文件夹，双击 `myTime.exe`。

请下载 Releases 中的程序 ZIP；GitHub 自动提供的 **Source code (zip)** 和首页 **Code → Download ZIP** 是源码包，需要自行编译。

程序基于 Windows 自带的 .NET Framework 4.x 构建，建议使用 Windows 10 / 11。无需安装 Node.js、Visual Studio 或额外的运行脚本，不会创建快捷方式或自动设置开机启动。

## 操作

- 拖动窗口标题区域：移动窗口。
- 点击 `-`：隐藏到系统托盘。
- 双击托盘图标，或右键选择“显示”：恢复窗口。
- 点击 `x`，或右键托盘选择“退出”：退出程序。
- `Ctrl+Alt+M`：隐藏或恢复窗口。
- `Ctrl+Alt+Q`：退出程序。

全局快捷键在窗口隐藏时也可使用；如果快捷键被其他程序占用，请使用窗口按钮或托盘菜单。

当前版本固定使用紧凑视图，设置入口暂未开放；已有时钟配置会继续保留。

## 配置、更新与删除

程序在 `myTime.exe` 所在文件夹保存本地文件：

| 文件 | 用途 |
| --- | --- |
| `settings.txt` | 窗口位置 |
| `clocks.tsv` | 时钟列表 |
| `migrations.txt` | 配置迁移记录 |

首次运行会自动生成所需配置。请勿直接在压缩包内运行，也不要放在没有写入权限的目录。移动程序时请移动整个文件夹。

更新时先退出程序，再用新版 ZIP 中的 `myTime.exe` 和 `README.md` 覆盖旧文件，保留上述配置文件。卸载时退出程序并删除整个文件夹即可。

程序仅在本机读取和保存配置，不要求账号，不上传数据，也不包含自动更新功能。

## 从源码构建

在 Windows 上，从项目根目录运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-Portable.ps1
```

输出到 `portable`，可直接运行 `portable\myTime.exe`。已有配置文件不会被覆盖。

指定输出目录：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-Portable.ps1 -OutputDirectory "D:\Apps\myTime"
```

构建使用系统自带的 C# 编译器和 .NET Framework 4.x。程序图标为 `assets/myTime.ico`，构建时嵌入 EXE，无需另带图标文件。

## 推送到 GitHub 自动发布

项目已包含 GitHub Actions 工作流。**推送到 `dev` 分支会自动构建 Windows 绿色版 ZIP 并发布到 Releases；推送到 `two` 分支会自动构建 Android 时钟小组件 APK 并发布到 Releases。** 无需手动打标签或上传压缩包。

版本号自动使用 `0.0.N`，其中 `N` 为工作流运行编号。构建完成后，用户到 Releases 下载最新 ZIP，解压后即可运行。

首次建仓库、推送命令和查看发布结果的步骤见 [GitHub 发布指南](docs/RELEASING.md)。

## Android 版本

`two` 分支是一个只显示北京时间和阿尔及利亚内陆廷杜夫时间的 Android 主屏幕小组件，不包含 Windows 版的其他功能。详细构建和安装说明见 [Android 说明](android/README.md)。

推送 `two` 后，Android 工作流会生成带 `two-` 前缀的可安装测试 APK，例如 `two-0.0.1-debug.apk`。安装后长按手机主屏幕，在小组件列表中添加“myTime 时钟”。

## 可选：本机生成发布压缩包

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\New-Release.ps1 -Version 1.0.0
```

生成 `release\myTime-1.0.0-windows.zip`，内部包含 `myTime\myTime.exe` 和面向用户的 `myTime\README.md`。打包时从源码重新构建，不会包含本机配置。若根目录存在 `LICENSE`，也会一并打包。

同版本文件已存在时，脚本会停止；确认需要替换后可添加 `-Force`。

本机生成的 ZIP 可用于提前验证或离线分发，不需要提交到 Git 仓库。
