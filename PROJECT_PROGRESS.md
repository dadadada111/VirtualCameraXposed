# 虚拟摄像头 Xposed 模块项目文档

## 1. 项目背景
为了快速验证“无人带货直播”中替换物理摄像头的技术可行性，我们基于 **方案二：Xposed/LSPosed Hook** 开发了这个 Android 模块。
目标是通过 Hook 系统 API，将直播应用获取的摄像头画面替换为本地的视频文件 (`/sdcard/virtual_camera.mp4`)。

## 2. 项目进度
- [x] **项目初始化**：创建了完整的 Android Studio 项目结构。
- [x] **核心代码实现**：
    - `VirtualCameraModule.java`: 实现了 Xposed 入口，Hook 了 `ImageReader` 以拦截图像数据。
    - `VideoDecoder.java`: 实现了基于 `MediaCodec` 的视频解码器，用于读取本地 MP4 文件。
- [x] **编译环境搭建**：
    - 配置了 GitHub Actions 自动构建流程。
    - 解决了 Gradle 版本兼容性、Lint 检查报错、资源缺失 (AAPT error) 等一系列构建问题。
    - 切换为 Debug 构建以自动签名 APK，解决安装时的 `PackageInfo is null` 错误。
- [x] **资源补全**：创建了缺失的 `strings.xml` 和各分辨率的 `ic_launcher` 图标。

## 3. 编译与安装指南

### 3.1 获取 APK
推荐直接从 GitHub Actions 下载最新构建的 Artifact：
1. 访问项目的 [GitHub Actions 页面](https://github.com/dadadada111/VirtualCameraXposed/actions)。
2. 点击最新的成功构建（绿色对勾）。
3. 在页面底部的 **Artifacts** 区域下载 `app-debug`。
4. 解压 ZIP 包得到 `app-debug.apk`。

### 3.2 安装与配置
1. **安装**：将 APK 安装到已 Root 并激活 LSPosed 的手机上。
2. **激活模块**：
   - 打开 LSPosed Manager。
   - 启用 "Virtual Camera Hook" 模块。
   - **重要**：勾选目标直播应用（如抖音、快手等）。
   - 重启目标应用。
3. **准备素材**：
   - 准备一个 MP4 视频文件（建议分辨率与直播伴侣设置的推流分辨率一致，如 1080x1920）。
   - 重命名为 `virtual_camera.mp4`。
   - 放入手机根目录：`/sdcard/virtual_camera.mp4`。

## 4. 常见问题排查
- **安装失败 (PackageInfo is null)**: 请确保下载的是 `app-debug.apk`（已自动签名），而不是 release 版。
- **画面黑屏/花屏**:
    - 检查 `/sdcard/virtual_camera.mp4` 是否存在且有读取权限。
    - 视频分辨率可能不匹配，尝试调整视频分辨率。
    - 目标应用可能使用了非 `ImageReader` 的预览方式（如直接 Surface），当前方案可能不生效。
- **构建失败**: 请检查 GitHub Actions 日志，确保 `ic_launcher` 等资源文件完整。

## 5. 后续计划 (TODO)
- [ ] 优化视频解码性能，尝试硬件加速。
- [ ] 增加动态配置界面，允许在 App 内选择视频文件。
- [ ] 研究 Hook `Camera2` 底层 Surface 的方法，以支持更多应用。
