# 推送到 GitHub 后自动发布绿色版

工作流位于 `.github/workflows/release.yml`。**每次推送到 `dev` 分支，GitHub Actions 都会自动编译 Windows 绿色版、生成 ZIP，并发布到 Releases。** Android 版本由 `.github/workflows/android.yml` 负责，在 `two` 分支推送时构建 APK。不需要手动打标签、上传附件或安装 GitHub CLI。

项目仓库为 [wuhujiang/myTime](https://github.com/wuhujiang/myTime)，Git 远程地址为 `git@github.com:wuhujiang/myTime.git`。

用户打开[最新版本下载页](https://github.com/wuhujiang/myTime/releases/latest)，下载 Assets 中的 `myTime-版本号-windows.zip`，完整解压后双击 `myTime.exe` 即可运行。首次成功发布后才会有可下载的版本。

## 首次提交与推送

当前本地仓库已使用 `dev` 分支，并已将 `origin` 关联到 `git@github.com:wuhujiang/myTime.git`，无需重复初始化或添加远程。

在项目根目录打开 PowerShell，先检查分支、待提交文件和远程地址：

```powershell
git status
git remote -v
```

确认当前分支为 `dev`、`origin` 地址与上文一致后，提交并推送：

```powershell
git add .
git commit -m "Prepare myTime portable release"
git push -u origin dev
```

推送使用 SSH，需要本机的 SSH 密钥已配置到有仓库写入权限的 GitHub 账号。

`.gitignore` 已排除 `portable/` 和 `release/`，默认输出目录中的本机配置及构建产物不会提交。源码、`assets/`、`docs/`、构建与打包脚本、`.gitignore` 和 `.github/workflows/release.yml` 都应提交。

首次推送就会触发自动发布。仓库需要启用 GitHub Actions，并允许使用工作流中的 Actions。工作流已声明发布所需的 `contents: write` 权限，使用仓库自带的 `GITHUB_TOKEN`，无需额外配置个人访问令牌。

## 日常更新

修改完成后，在项目根目录运行：

```powershell
git add .
git commit -m "Update myTime"
git push
```

随后：

1. 到仓库 [Actions 页面](https://github.com/wuhujiang/myTime/actions)查看 `Release portable ZIP` 工作流。
2. 等待构建和发布完成。
3. 打开 [Releases 页面](https://github.com/wuhujiang/myTime/releases)，最新版本的 Assets 中会出现程序 ZIP。

自动版本号为 `0.0.N`，其中 `N` 是该工作流的运行编号，例如 `myTime-0.0.1-windows.zip`。每次成功执行都会发布对应版本，并将其设为 Latest；ZIP 对应本次推送的源码提交。程序 EXE 的内部版本信息不随此编号变化。

Windows 版本只有推送到 `dev` 才会自动发布。Android 版本推送到 `two` 后由独立工作流自动发布 APK。工作流也支持在 Actions 页面选择 **Run workflow** 手动执行；重新运行同一次工作流会更新同一版本的附件。密集推送时，GitHub 并发队列可能合并尚未开始的任务，以最新待运行提交为准。

如果失败，打开 Actions 中失败步骤的日志，修复后再次提交并推送即可。ZIP 在构建和压缩包检查通过后才会上传。GitHub 上的自动发布效果需要首次推送后确认。

## 用户下载入口

用户打开[最新版本下载页](https://github.com/wuhujiang/myTime/releases/latest)，下载 Assets 中的 `myTime-版本号-windows.zip`。

如果尚未成功发布任何版本，最新版本链接暂时不可用，可先到 [Actions 页面](https://github.com/wuhujiang/myTime/actions)查看发布进度。

**GitHub 自动生成的 Source code (zip)、Source code (tar.gz) 和首页 Code → Download ZIP 都是源码包，不能直接运行。** 已编译的绿色版在 Releases 附件中。

## 可选：本机生成 ZIP

需要提前验证或离线分发时，可以在 Windows 上运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\New-Release.ps1 -Version 1.0.0
```

生成的目录结构：

```text
release/
└── myTime-1.0.0-windows.zip
    └── myTime/
        ├── myTime.exe
        └── README.md
```

若项目根目录存在 `LICENSE`，也会一并打包。脚本会在新临时目录编译，不会包含 `portable` 中的本机配置；检查压缩包后输出 SHA256，并清理临时目录。

指定输出目录或覆盖同版本 ZIP：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\New-Release.ps1 -Version 1.0.0 -OutputDirectory "D:\Releases\myTime"
powershell -NoProfile -ExecutionPolicy Bypass -File .\New-Release.ps1 -Version 1.0.0 -Force
```

建议发布前将 ZIP 解压到新目录，检查默认时钟、托盘和退出操作。首次运行后应在解压目录生成配置文件。
