# VirtualCameraXposed

这是一个 Android Xposed 模块，用于拦截并替换摄像头数据，实现使用本地视频文件替换物理摄像头画面的功能。

## 功能
- Hook `ImageReader` 拦截摄像头数据。
- 使用本地视频文件 (`/sdcard/virtual_camera.mp4`) 替换摄像头画面。
- 仅用于测试和学习目的。

## 编译方法

### 方法一：使用 GitHub Actions (推荐)

如果您没有本地 Android 开发环境，可以使用 GitHub 的云端构建功能。

1. **Fork 本仓库**：将本项目 Fork 到您的 GitHub 账号。
2. **启用 Actions**：在仓库页面点击 "Actions" 标签页，启用 Workflow。
3. **触发构建**：
   - 任意 Push 代码到 `main` 分支。
   - 或者在 Actions 页面手动触发。
4. **下载 APK**：构建完成后，在 Actions 运行记录的 "Artifacts" 部分下载 `app-release`，解压后即可得到 APK。

### 方法二：本地编译

需要安装 [Android Studio](https://developer.android.com/studio)。

1. 使用 Android Studio 打开本项目根目录 (`VirtualCameraXposed`)。
2. 等待 Gradle 同步完成。
3. 点击菜单栏 `Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`。
4. 编译完成后，APK 通常位于 `app/build/outputs/apk/debug/` 或 `app/build/outputs/apk/release/`。

## 使用说明

1. **安装 APK**：将编译好的 APK 安装到已 Root 并激活 LSPosed 的 Android 设备上。
2. **准备视频**：
   - 准备一个 MP4 视频文件。
   - 重命名为 `virtual_camera.mp4`。
   - 复制到手机根目录：`/sdcard/virtual_camera.mp4`。
3. **激活模块**：
   - 打开 LSPosed Manager。
   - 启用 "Virtual Camera Hook" 模块。
   - **勾选目标应用**（您想替换摄像头的 App）。
   - 重启目标应用。
4. **验证**：打开目标应用的摄像头预览，应显示视频内容。

## 注意事项
- 本项目仅供学习研究，请勿用于非法用途。
- 视频解码目前较基础，可能存在兼容性问题（如颜色错位），建议使用与目标 App 摄像头分辨率一致的视频。
