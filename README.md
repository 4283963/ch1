# 智慧升降地锚（车位锁）与车牌识别全栈联控系统

> 城市老旧小区改造 - 智能停车管理解决方案

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        前端监控大屏 (Vue3 + Canvas)                  │
│  │  实时车位平面图 · 事件日志 · 状态统计卡片 · WebSocket实时推送       │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ HTTP(API) / WS(实时)
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    核心业务后端 (Java Spring Boot)                   │
│  │  车牌白名单 · 车位状态 · 业主管理 · 进出记录 · Go网关调度          │
│  │  MySQL持久化 · WebSocket广播 · REST API接口                       │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ HTTP REST (升降指令)
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       底层网关服务 (Go 语言)                          │
│  │  TCP长连接 · 地锚连接池 · 心跳检测 · 指令下发与ACK确认              │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ TCP 长连接 (自定义协议)
                                 ▼
                ┌──────────────────────────────────┐
                │   P001  P002  P003  ...  P008    │
                │   液压升降地锚硬件 (共8个车位)     │
                └──────────────────────────────────┘
```

## 📁 项目结构

```
ch1/
├── gateway-go/              # Go 底层网关服务
│   ├── go.mod
│   └── main.go              # TCP服务端 + HTTP API
│
├── backend-java/            # Java Spring Boot 核心业务后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/smartpark/
│       │   ├── SmartParkApplication.java        # 启动类
│       │   ├── config/                          # 跨域/WebSocket/RestTemplate配置
│       │   ├── controller/                      # REST API控制器
│       │   ├── service/                         # 核心业务逻辑
│       │   ├── entity/                          # 数据库实体
│       │   ├── mapper/                          # MyBatis Mapper
│       │   ├── dto/                             # 数据传输对象
│       │   └── websocket/                       # WebSocket处理器
│       └── resources/
│           ├── application.yml                  # 应用配置
│           ├── schema.sql                       # 建表+测试数据
│           └── mapper/                          # MyBatis XML映射
│
└── frontend-vue/           # Vue3 前端大屏与管理后台
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── main.js
        ├── App.vue
        ├── router/index.js
        ├── stores/parking.js                    # Pinia状态管理
        ├── utils/{api.js,websocket.js}          # API与WS工具
        ├── components/{ParkingMap.vue,HeaderStats.vue}
        └── views/{ParkingMap.vue,AdminPanel.vue}
```

## ⚡ 核心业务流程 (车牌进场)

```
红外相机(大门)
    │ POST /api/plate/recognize  {plateNumber: "京A12345"}
    ▼
Java 后端 PlateController
    │
    ├─ 1. 查询 plate_whitelist 表校验白名单
    ├─ 2. 获取业主关联的固定车位 (space_id=1, space_code=P001)
    ├─ 3. 更新车位状态 → "升降中(2)"
    ├─ 4. WebSocket推送前端 → 车位P001闪烁绿色(引导停车)
    ├─ 5. 事件日志广播
    │
    ├─ 6. HTTP调用 Go网关 POST /api/anchor/down {spaceId:"P001"}
    │       │
    │       ▼ Go网关
    │         ├─ 从连接池取出 P001 的TCP连接
    │         ├─ 发送 "DOWN\n" 指令
    │         └─ 等待地锚回复 "ACK:OK\n"
    │
    ├─ 7. 等待1.5秒 (液压升降时间)
    ├─ 8. 更新车位状态 → "占用(1)"
    ├─ 9. WebSocket推送前端 → 车位变蓝色
    ├─ 10. 写入 access_record (进场记录)
    └─ 11. 路闸放行信号 (模拟)
```

## 🚀 启动指南

### 前置依赖

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 21+ | Spring Boot 3.4.x 要求 |
| Go | 1.19+ | 网关服务 |
| Node.js | 18+ | 前端构建 |
| MySQL | 8.0+ | 业务数据库 |
| Maven | 3.8+ | Java构建 |

---

### 步骤一：初始化数据库

```bash
# 登录MySQL并创建数据库
mysql -u root -p -e "CREATE DATABASE smartpark DEFAULT CHARSET utf8mb4;"

# 表结构和测试数据会在应用启动时自动执行
# (配置项: spring.sql.init.mode=always)
```

**默认测试数据：**
- 8个车位：`P001-P008` (A区4个 + B区4个，4x2布局)
- 2位业主：张三(13800138001)、李四(13900139002)
- 4条白名单：
  | 车牌 | 业主 | 车位 |
  |------|------|------|
  | 京A12345 | 张三 | P001 |
  | 京B67890 | 张三 | P002 |
  | 沪C11111 | 李四 | P005 |
  | 粤D22222 | 李四 | P006 |

---

### 步骤二：启动 Go 网关服务

```bash
cd gateway-go
go mod download          # 首次运行下载依赖
go run main.go
```

**启动后输出：**
```
[TCP] 服务启动，监听端口 :9000
[HTTP] 服务启动，监听端口 :8080
```

| 端口 | 协议 | 用途 |
|------|------|------|
| 9000 | TCP | 地锚硬件长连接 (REGISTER:{code} 注册) |
| 8080 | HTTP | 接收Java后端升降指令 |

**模拟地锚注册测试** (另开终端)：
```bash
# 模拟P001地锚上线
echo "REGISTER:P001" | nc -c localhost 9000

# 模拟所有8个地锚上线(可用脚本)
for code in P001 P002 P003 P004 P005 P006 P007 P008; do
  (echo "REGISTER:$code"; sleep 3600) | nc localhost 9000 &
done
```

---

### 步骤三：启动 Java 业务后端

```bash
cd backend-java
# 修改 src/main/resources/application.yml 中MySQL连接信息
mvn spring-boot:run
```

| 端口 | 用途 |
|------|------|
| 9090 | HTTP REST API + WebSocket |

**健康检查：**
```bash
curl http://localhost:9090/api/spaces | python3 -m json.tool
```

---

### 步骤四：启动 Vue3 前端大屏

```bash
cd frontend-vue
npm install              # 首次安装依赖
npm run dev              # 开发模式启动
```

访问：**http://localhost:5173**

| 路由 | 功能 |
|------|------|
| `/` | 大屏监控：Canvas车位图 + 实时事件日志 + 统计卡片 |
| `/admin` | 管理后台：白名单/车位控制/车牌模拟/进出记录 |

---

## 📡 API 接口清单

### Java 后端 (端口9090)

#### 车牌识别 (核心入口)
```http
POST /api/plate/recognize
Content-Type: application/json

{"plateNumber": "京A12345"}
```

#### 车位管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/spaces` | 查询所有车位(含Canvas坐标) |
| GET | `/api/spaces/{id}` | 查询单个车位 |
| PUT | `/api/spaces/{id}/status` | 更新状态+广播(0空/1占用/2升降) |
| POST | `/api/spaces/broadcast` | 广播全部车位状态(WS) |

#### 白名单/业主管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/plate/whitelist/detail` | 白名单详情(关联业主+车位) |
| POST | `/api/plate/whitelist` | 新增白名单 |
| DELETE | `/api/plate/whitelist/{id}` | 删除白名单 |
| GET | `/api/plate/owners` | 业主列表 |

#### 手动地锚控制
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/plate/anchor/up` | 升地锚 {spaceId:"P001"} |
| POST | `/api/plate/anchor/down` | 降地锚 {spaceId:"P001"} |

#### 进出记录
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/access-records` | 查询所有记录 |
| GET | `/api/access-records/plate/{pn}` | 按车牌查询 |

#### WebSocket
```
ws://localhost:9090/ws/parking
```

接收消息格式：
```json
// 车位状态更新
{
  "type": "space_update",
  "spaceId": 1,
  "spaceCode": "P001",
  "name": "P001",
  "status": "guiding",       // free(空闲灰) / occupied(占用蓝) / guiding(引导绿)
  "color": "#059669",
  "x": 50, "y": 50, "width": 100, "height": 60
}

// 事件日志
{
  "type": "event_log",
  "data": {
    "plateNumber": "京A12345",
    "spaceCode": "P001",
    "eventType": "IN",          // IN / OUT
    "description": "车辆识别，地锚下降中，引导停车",
    "time": 1718xxxxxxxxx
  }
}
```

---

### Go 网关 (端口8080)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/anchor/up` | 升地锚 {spaceId:"P001"} |
| POST | `/api/anchor/down` | 降地锚 {spaceId:"P001"} |
| GET | `/api/anchor/status` | 查询在线地锚 |

**TCP 协议规范 (地锚↔网关)：**
- 所有消息以 `\n` 分隔
- 地锚上线先发：`REGISTER:{车位编码}\n` (如 REGISTER:P001)
- 网关下发指令：`UP\n` 或 `DOWN\n`
- 地锚执行后回复：`ACK:OK\n`
- 心跳：网关60秒超时，地锚可发送 `PING`，网关回复 `PONG`

---

## 🎯 功能演示步骤

1. **启动所有服务**：MySQL → Go网关 → Java后端 → Vue前端
2. **模拟地锚上线**：8个nc进程向9000端口注册P001-P008
3. **打开大屏**：访问 http://localhost:5173，确认8个车位均为灰色(空闲)
4. **切换到管理后台 → Tab3车牌模拟**：
   - 输入 `京A12345`，点击「模拟识别」
   - **观察大屏**：P001车位立即变绿色呼吸闪烁(引导停车)
   - **1.5秒后**：P001变蓝色(占用)，事件日志新增记录
5. **再次输入同车牌**：模拟离场，地锚升起，车位恢复灰色

---

## 🎨 Canvas 车位状态配色

| 状态 | 填充色 | 边框色 | 动效 | 含义 |
|------|--------|--------|------|------|
| 空闲 | `#4b5563` 灰 | `#6b7280` | 无 | 地锚升起，可分配 |
| 引导 | `#059669` 绿 | `#34d399` | 呼吸脉冲 + 径向光晕 | 白名单进场，地锚下降中 |
| 占用 | `#2563eb` 蓝 | `#60a5fa` | 外发光阴影 | 车辆已停，地锚锁住 |

---

## 🛠️ 技术栈

| 层级 | 技术 | 选型理由 |
|------|------|---------|
| 网关层 | Go + gorilla/mux | 高并发TCP连接处理，内存占用低 |
| 业务层 | Spring Boot 3.4 + MyBatis | 生态成熟，企业级开发标准 |
| 数据库 | MySQL 8.0 | 关系型数据，事务支持 |
| 实时通信 | Spring WebSocket | 车位状态毫秒级推送 |
| 前端 | Vue3 + Pinia + Vite | 响应式状态，构建快 |
| 可视化 | HTML5 Canvas API | 动态车位渲染，60fps脉冲动画 |

---

## 🔐 扩展建议 (生产环境)

1. **Go网关**：增加断线重连机制、Redis做连接池共享、支持集群部署
2. **Java后端**：加入Spring Security鉴权、Redisson分布式锁、MySQL读写分离
3. **前端**：加入登录页、权限管理、车位拖拽调整位置后持久化坐标
4. **地锚硬件**：加入MQTT协议支持、地磁检测器辅助判断车位占用
