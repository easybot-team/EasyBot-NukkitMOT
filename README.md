# EasyBot Nukkit-MOT

EasyBot Bridge 在 Nukkit-MOT 上的原生实现，目标服务端为 Nukkit-MOT `1.26.30-R1`，运行环境为 Java 17。

## 已适配功能

- 玩家异步登录前的 EasyBot 登录/强制绑定校验，支持 `service.ignore_error`
- 玩家进入、退出、聊天、死亡消息同步
- 群聊消息回服、图片/文件链接的基岩版可读降级、@全体和@玩家提醒
- `/easybot bind`、跨平台绑定确认、绑定状态、连接状态、重载与版本查询
- `/esay` 消息同步与 EconomyAPI 收费
- EasyBot 远程命令执行，并将 Nukkit 命令输出回传主程序
- PlaceholderAPI 查询和远程命令变量替换
- 在线玩家列表、Java/ViaProxy 客户端识别；优先使用 Nukkit 提供的皮肤 URL，并为 Java 客户端提供 URL 回退
- 插件安装/启用状态、Xbox 登录认证状态
- Nukkit 玩家数据和成就 NBT 的 JSON 读取；在线玩家兼容 UUID 与玩家名两种存档模式
- Bridge 生命周期清理、配置重载重连、所有 Nukkit API 的主线程调度

## 构建与安装

使用 Java 17 运行 Gradle 的 `shadowJar` 任务，产物为 `EasyBot-NukkitMOT-1.0.0.jar`。将其放入 Nukkit-MOT 的 `plugins` 目录，首次启动后填写：

```yaml
service:
  url: 'ws://127.0.0.1:26990/bridge'
  token: '在 EasyBot 主程序中生成的 Token'
```

PlaceholderAPI 和 EconomyAPI 均为可选依赖；未安装时，对应功能会明确提示不可用，不影响其余功能。
