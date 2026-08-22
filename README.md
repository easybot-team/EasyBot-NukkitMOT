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
- 在线玩家列表与 Nukkit-MOT 原生的 Java/ViaProxy 客户端识别
- Nukkit 插件安装、启用状态查询
- Nukkit 玩家数据 NBT 的 JSON 读取；UUID 存档模式支持在线与离线玩家，名字存档模式只支持在线玩家
- Bridge 生命周期清理、配置重载重连、所有 Nukkit API 的主线程调度

## 平台边界

- Nukkit-MOT 只提供 Bedrock 皮肤像素和不透明的皮肤标识，不提供 Bridge 所需的公网皮肤 URL，因此不伪造或猜测皮肤地址。
- Nukkit 的旧式 `Achievements` 不是 Java 版 Advancements，且没有对应的玩家 Statistics NBT，因此这两类 Bridge 查询明确返回不支持。
- Xbox 登录链由 Nukkit-MOT 自身在玩家上线前完成校验；本适配不把它冒充 AuthMe 等第三方登录插件状态。

## 构建与安装

使用 Java 17 运行 Gradle 的 `shadowJar` 任务，产物为 `EasyBot-NukkitMOT-1.0.0.jar`。将其放入 Nukkit-MOT 的 `plugins` 目录，首次启动后填写：

```yaml
service:
  url: 'ws://127.0.0.1:26990/bridge'
  token: '在 EasyBot 主程序中生成的 Token'
```

PlaceholderAPI 和 EconomyAPI 均为可选依赖；未安装时，对应功能会明确提示不可用，不影响其余功能。
