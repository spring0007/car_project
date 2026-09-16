# CLAUDE.md

本文件为 Claude Code 在本仓库工作时使用的指南。基于当前项目代码整理,修改代码前请先阅读与本任务相关的部分。

## 项目概述

- 项目名:`Launcher2_awell`(工作目录 `AwellLauncher_New`)
- 类型:**Android 车机 Launcher(桌面)应用**,基于 **AOSP Launcher2** 深度定制,通过 **RePlugin 插件化框架**支持多套 UI 主题(白牌/定制/海外版)动态切换。
- 入口:宿主 `launcher` 模块的 `com.awell.launcher.host.MainActivity`,声明了 `HOME` 类 Intent-filter,是系统桌面。
- 运行身份:`android:sharedUserId="android.uid.system"` + **platform 签名**(`keystore/platform.jks`),即**系统级应用**。

## 技术栈

| 项 | 版本/说明 |
|---|---|
| Gradle / AGP | Gradle 7.4+ / `com.android.tools.build:gradle:7.4.2` |
| Kotlin | 1.9.24(主模块 `app` 为 **Java + Kotlin 混编**) |
| compileSdk / targetSdk | `app`=33;`launcher`/插件=34 |
| minSdk | 26 |
| 插件化 | RePlugin(宿主 gradle 插件 `replugin-host-gradle`、插件 `replugin-plugin-gradle`),`libs.replugin.*` 统一定义在 `gradle/libs.versions.toml` |
| 架构 | 主体 AOSP Launcher2(老 MVC/回调),新增模块用 Kotlin + ViewModel/LiveData(MVVM) |
| 依赖 | androidx(appcompat、material、constraintlayout、recyclerview)、kotlinx-coroutines、okhttp/Glide、RePlugin host/plugin lib、AwellLibrary(AIDL 车机接口) |

## 模块结构(`settings.gradle`)

```
:app                            桌面核心逻辑,编译成 Android **library**
                                 namespace com.awell.launcher.library
:launcher                       RePlugin **宿主** LauncherHost.apk(applicationId com.awell.launcher.host)
                                声明 HOME,依赖 :app 和 :AwellLibrary
:AwellLibrary                   车机/系统接口共享库(隐藏 API、AIDL、工具类)
:plugin:plugins:ui1..ui27       各 UI 主题**插件** APK(ui15/17/22-25 为 freeform)
:plugin:plugins:yfd_ui01..05    逸卡思(YFD)定制主题
:plugin:plugins:yks_ui01        逸卡思(YKS)定制主题
:plugin:plugins:zy_ui01..07     中控/白牌(z y)定制主题
```

## 模块职责

### `app`(桌面核心,com.awell.launcher.library)
基于 AOSP Launcher2 的完整桌面:工作区、应用抽屉、图标、文件夹、小部件、拖拽。关键类在 `app/src/main/java/com/awell/launcher2/`:

- `Launcher.java` 主 Activity(实现 `LauncherModel.Callbacks` 回调获取数据);`LauncherApplication.java` 继承 `RePluginApplication`(RePlugin 宿主 Application)+ `ViewModelStoreOwner`
- `LauncherModel.java` 数据模型:扫描/绑定应用与工作区,**应用排序比较器**在此(`getAppNameComparator` / `getAppLevelComparator`)
- `AppsCustomizePagedView.java` 应用抽屉(分页 Grid),入口方法 `setApps()` / `addApps()` / `updateApps()`
- `Workspace.java`、`CellLayout.java`、`Folder*.java` 桌面工作区与文件夹
- `LauncherProvider.java`(ContentProvider,保存桌面数据)、`IconCache.java`(图标缓存)、`ItemInfo.java`(图标数据基类,含 `level` 字段)
- 其他 com.awell 子包:
  - `com.awell.control`:`AppsCustomizeConfig`/`AppsCustomizeControl`(抽屉配置/控制)、`MediaViewModel`(音乐媒体, Kotlin)
  - `com.awell.impl`:`ModelImpl`(数据实现)
  - `com.awell.model`:媒体/专辑相关数据模型
  - `com.awell.service`:`HostToPluginService`(宿主↔插件通信)、`GpsSimulationService`
  - `com.awell.ui`:`AnimationHandler`、`AppsCustomizeIndicatorPanel`
  - `com.awell.utils`:`Utils.java`(**应用分级列表 `mAppLevel_1..9`**)、`LogUtil`、`LocationHelper`、`SocketThread` 等
  - `com.awell.ctrlview`:`MusicWidget`、`VisualizerView`、`MyQAnalogClock` 等自定义控件
  - `com.awell.addapp`:应用添加/选择流程(本地 SQLite `MyDbHelper`)

### `launcher`(宿主)
- `MainActivity.java` 仅作 HOME 入口,真正 UI 由 `:app` 提供,主题切换由插件完成。
- `repluginHostConfig { useAppCompat=true; useAndroidX=true }`。

### `AwellLibrary`
- 隐藏 API 封装:`android.os.SystemProperties`、`ServiceManager`、`IPowerManager`、`android.media.Audio*`、`com.mediatek.pq.PictureQuality`
- 车机 AIDL:`com.awell.aidl.awellautointer`、`com.awell.aidl.awellface`(CAN bus、车事件等)
- 工具:`LogUtil`、`SystemUIClient`、`WeatherHelper/Contract`、`ClickUtils`、`PageData`

### 插件模块(`plugin/plugins/*`)
每个插件是一个独立 APK(输出名如 `LauncherUI1.apk`),以 `compileOnly` 引入 `:app` 与 `:AwellLibrary`,打包后由宿主动态加载为桌面 UI。

## 构建与常用命令

```bash
# 宿主 APK
./gradlew :launcher:assembleRelease        # → launcher/build/outputs/apk/release/LauncherHost.apk

# 单个插件
./gradlew :plugin:plugins:ui1:assembleRelease

# 全部模块 + 收集所有 APK 到根目录 /release(按模块重命名目录)
./gradlew assembleAllRelease
```

- 所有模块统一用 **platform 签名**(`keystore/platform.jks`,alias `platform`,密码 `android`),`sharedUserId=android.uid.system`,需 push 到 `/system/priv-app` 或作为系统桌面安装。
- 根 `build.gradle` 的 `copyAllReleaseApks` 会把各模块 APK 收集到根 `/release` 目录。

## RePlugin 插件(UI 主题)开发规范

见根目录 `readme`,要点:

1. 插件 `build.gradle` 用 `isPlugin = true` 走 `replugin-plugin-gradle`;`aaptOptions.additionalParameters` 设置 `--package-id 0x7e`(**资源包 ID 必须为 0x7e**,避免与宿主冲突)。
2. 宿主通过元数据 `LAUNCHER_KEY`(app 名)、`LAUNCHER_CLAZZ`(展示 UI 的 Activity)定位插件界面;主题 APK 内 `app_name`、`ui_key` 必须与 **APK 文件名一致**,`ui_clazz` 为要显示 Activity 的完整类名。
3. 展示的 UI Activity 必须是 `launchMode="singleTask"`,并重写 `onBackPressed`、`onNewIntent`,以及长按弹出壁纸选择。
4. `ActivityRecord.setPluginToHome()` 将插件 UI Activity 设为 HOME 类型;`ActivityStarter` 在启动 app 时替换 Intent 的 Component。

## 关键业务规则:应用列表排序(近期改动)

应用抽屉(`AppsCustomizePagedView`)按**等级分组 + 组内名称排序**:

- 等级分配:`setAppShowLevel(app)`(`AppsCustomizePagedView.java`)根据 `Utils.mAppLevel_1..9`(`app/.../com/awell/utils/Utils.java:407`)命中打 1~9 级,**未命中默认第 10 级**。
- 排序:`LauncherModel.getAppLevelComparator()`:
  - 主键 `Integer.compare(level)`(1→10)
  - 所有等级组内统一按应用名 `Collator.getInstance(Locale.getDefault())` 排序,**跟随设备当前语言**:中文按拼音、英文按字母、阿拉伯/希伯来按各自字母序等(此前第 10 级用 `title.compareToIgnoreCase` 纯码点排序,中文名会乱序;后固定为 SIMPLIFIED_CHINESE,现在改为设备语言自适应)
  - 同名再按 `componentName`
- 入口:`setApps()`(全量刷新,`mApps.sort(getAppLevelComparator())`);动态新增/更新走 `addAppsWithoutInvalidate()`,已改为**先 `setAppShowLevel()` 补等级、再用等级比较器 binarySearch 插入**,保证落在正确的等级组且不破坏组内字母序。

> 修改排序相关逻辑时,请保持「1~9 级分组优先 + 第 10 级字母序」的现状,除非明确要求改变。

## 关键业务规则:宿主主题切换后的 info 刷新(近期改动)

宿主 `launcher/.../host/MainActivity.java` 维护 `info`(`PluginInfo`,最后一次成功安装的插件)。**主题切换后 `info` 不会自动更新**,若直接用旧 `info` + 新 clazz 启动,会出现「旧包名 + 新类名」错配:`RePlugin.createIntent` 生成的 Intent 指向旧插件包,`onStartActivityCompleted`(`LauncherApplication.java:362`)里的 `plugin` 与 `activity` 不一致,导致假成功/假失败、界面不跳转。

- **校验依据**:插件 UI Activity 类名统一为 `com.launcher.uiX.MainActivityUIX` —— **目标类名以所属插件包名为前缀**。据此判断 `info` 是否过期:`clazz.startsWith(info.getPackageName() + ".")`。
- **统一入口校验**:`onResume` / `onNewIntent` 启动插件前先经 `infoMatchesTargetClazz(apkClazz)`;不匹配则不走缓存 `info`,改走 `initInstallThread()` → `simulateInstallExternalPlugin()` 重新解析/安装目标插件、刷新 `info` 后再启动。
- **安装复用分支**:`simulateInstallExternalPlugin()` 命中"插件已安装"分支时,必须把 `info = existingPlugin`(**指向目标插件本身**),而非沿用残留旧 `info`;并校验 `clazz` 确实属于该插件包,否则 fall-through 继续走安装。
- **兜底防御**:`realStartPlugin()` 启动前再校验一次 clazz 属于 info 包,不匹配直接 `scheduleRetryOrFallback()`(重试 `MAX_RETRY=3` 后降级内置桌面 `Launcher`)。

> 新增/调整主题插件时,**插件 Activity 类名必须保持 `包名.` 前缀约定**(如 `com.launcher.uiX.MainActivityUIX`),否则上述 `info` 校验会失效。

## Android 设计理念(通用)

- **组件化**:四大组件(Activity/Service/BroadcastReceiver/ContentProvider)各自独立,通过 **Intent 松耦合**通信;本项目的 `LauncherProvider`(ContentProvider)即用于持久化桌面数据。
- **生命周期**:组件受系统生命周期约束,必须在 `onPause/onDestroy` 等回调中释放资源、避免在非前台线程长期持有 UI;新代码用 `ViewModel`+`LiveData` 感知生命周期(宿主 `LauncherApplication` 已实现 `ViewModelStoreOwner`)。
- **任务栈与启动模式**:`singleTask`/`singleTop` 控制返回栈,桌面必须保持唯一实例(宿主 MainActivity 与插件 UI 均 `singleTask`)。
- **资源系统**:`R` 资源集中管理 UI/文案/图标;项目开启 `android.nonTransitiveRClass=true`,插件固定 `0x7e` 包 ID 规避资源冲突。
- **权限模型**:权限在 Manifest 声明、运行时由用户/系统授权;本项目因是系统桌面,采用 **platform 签名 + `sharedUserId=android.uid.system`**,使用 `WRITE_SETTINGS`、`WRITE_SECURE_SETTINGS`、隐藏 API(`SystemProperties` 等)均依赖系统身份。
- **内存与性能**:避免主线程 I/O,图标/图片用缓存(`IconCache`、Glide),长列表分页渲染(`AppsCustomizePagedView`)。

## 架构思想(本项目体现)

- **回调式数据绑定(Launcher2 经典模式)**:`LauncherModel` 负责数据加载与排序,通过 `LauncherModel.Callbacks`(`bindAllApplications`/`bindAppsAdded` 等)回调到 UI,`Launcher` 与 `AppsCustomizePagedView` 只消费排序好的数据 —— **数据源唯一(LauncherModel),展示层无业务排序**。
- **宿主 + 插件(RePlugin)**:桌面核心(`:app`)与主题 UI(插件)解耦 —— 宿主提供框架与接口,插件以 `compileOnly` 编译期依赖、运行期动态加载,实现「一套核心、多套皮肤、无需发版换肤」。依赖方向:**插件依赖宿主提供的接口,宿主不反向依赖具体插件**。
- **接口下沉共享库**:车机能力(隐藏 API、AIDL 车事件/CAN bus)集中在 `AwellLibrary`,宿主与插件共同依赖,避免重复实现、统一版本。
- **业务规则集中化**:应用分级(1~9 级)集中定义在 `Utils.mAppLevel_*` 常量表,比较器只做「读等级→比较」,便于后续按客户调整分组。
- **新旧架构并存**:核心沿用 AOSP Launcher2 的 MVC/回调(Java),新增媒体/控制模块用 Kotlin MVVM(`MediaViewModel`+LiveData)。改动老代码时注意兼容 Java 回调风格;新功能优先用 Kotlin + ViewModel。

## 常见注意点 / 坑

- 两个模块都声明 `android:sharedUserId="android.uid.system"` 且用平台签名;**改包名/签名会导致无法作为系统桌面启动**。
- 插件资源必须 `--package-id 0x7e`,否则与宿主 R 类冲突;插件内引用 `:app` 资源用 `compileOnly`,**不会打包进插件 APK**,运行时由宿主提供。
- `LauncherModel.getAppLevelComparator()` 依赖 `ApplicationInfo.level`,动态添加应用前必须先 `setAppShowLevel()`(等级默认 0 会排到最前)。
- `AwellLibrary` 内的 `SystemProperties`/`ServiceManager` 为隐藏 API,`compileOnly` 或系统签名下可用,普通调试安装会失败。
- 根目录 `release/`、各模块 `*/release/` 为构建产物,勿提交。
- 大量旧代码注释为「乱码/繁体/旧逻辑」(如 `//huang` 调试日志、被注释掉的旧排序),勿被误导,以当前生效代码为准。