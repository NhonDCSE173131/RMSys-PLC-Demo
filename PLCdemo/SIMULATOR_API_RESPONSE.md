# PLC Simulator - API và Response Reference

Tài liệu này mô tả **interface hiện tại** của project `PLCdemo` sau khi chỉnh theo contract backend.

Phạm vi gồm:
1. trạng thái inbound của simulator
2. outbound HTTP mà simulator gọi sang backend
3. console command và response text
4. scenario behavior và retry/send behavior

---

## 1. Tổng quan kiến trúc

Simulator hiện chạy với:

```yaml
spring:
  main:
    web-application-type: none
```

Vì vậy:
- simulator **không expose REST API inbound** cho client ngoài gọi vào
- simulator hoạt động qua:
  - **console command**
  - **HTTP client outbound** gửi telemetry sang backend

---

## 2. Inbound API của simulator

## 2.1. Trạng thái hiện tại

**Không có REST API inbound.**

Không có `@RestController` trong flow chính hiện tại.

## 2.2. `server.port`

Cấu hình hiện tại:

```yaml
server:
  port: ${SIMULATOR_PORT:8095}
```

Ý nghĩa:
- đặt port dự phòng khác `8080` để không đụng BE
- do app đang chạy `non-web`, simulator thực tế không bind endpoint HTTP inbound

---

## 3. Outbound API sang Backend

## 3.1. Endpoint đích mặc định

```text
POST http://localhost:8080/api/v1/ingest/telemetry
```

Cấu hình:

```yaml
simulator:
  backend-base-url: ${SIMULATOR_BACKEND_BASE_URL:http://localhost:8080}
  ingest-path: /api/v1/ingest/telemetry
```

## 3.2. Method và content type

```http
POST /api/v1/ingest/telemetry
Content-Type: application/json
```

---

## 4. Request body mà simulator gửi

Payload serialize từ `NormalizedTelemetryDto`.

## 4.1. JSON schema thực tế

| Field | Type | Required | Ghi chú |
|---|---|---:|---|
| `machineId` | `string` | Có | UUID cố định theo backend, ưu tiên `MC-01..MC-05`; `MC-06..MC-07` là máy mở rộng tuỳ chọn |
| `ts` | `string (ISO-8601)` | Có | source/event time tại simulator |
| `connectionStatus` | `string` | Có | `ONLINE`, `DEGRADED`, `OFFLINE` |
| `machineState` | `string` | Có | `RUNNING`, `IDLE`, `WARMUP`, `STOPPED`, `EMERGENCY_STOP` |
| `operationMode` | `string` | Có | ví dụ `AUTO`, `MANUAL`, `SETUP` |
| `programName` | `string` | Có | tên job/chương trình |
| `cycleRunning` | `boolean` | Có | có đang chạy chu kỳ hay không |
| `powerKw` | `number` | Có | công suất tức thời |
| `temperatureC` | `number` | Có | nhiệt độ chính |
| `vibrationMmS` | `number` | Có | rung |
| `runtimeHours` | `number` | Có | runtime tích lũy |
| `cycleTimeSec` | `number` | Có | chu kỳ hiện hành |
| `outputCount` | `integer` | Có | tổng sản lượng |
| `goodCount` | `integer` | Có | sản phẩm tốt |
| `rejectCount` | `integer` | Có | sản phẩm lỗi |
| `spindleSpeedRpm` | `number` | Có | rpm |
| `feedRateMmMin` | `number` | Có | feed |
| `toolCode` | `string` | Có | mã tool |
| `remainingToolLifePct` | `number` | Có | `100 - toolWearPercent` |
| `voltageV` | `number` | Có | điện áp |
| `currentA` | `number` | Có | dòng điện |
| `powerFactor` | `number` | Có | hệ số công suất |
| `frequencyHz` | `number` | Có | tần số |
| `energyKwhShift` | `number` | Có | điện năng ca |
| `energyKwhDay` | `number` | Có | điện năng ngày |
| `motorTemperatureC` | `number` | Có | nhiệt độ motor |
| `bearingTemperatureC` | `number` | Có | nhiệt độ ổ đỡ |
| `cabinetTemperatureC` | `number` | Có | nhiệt độ tủ điện |
| `servoOnHours` | `number` | Có | giờ servo on |
| `startStopCount` | `integer` | Có | số lần start/stop |
| `lubricationLevelPct` | `number` | Có | mức bôi trơn |
| `batteryLow` | `boolean` | Có | cờ cảnh báo pin |
| `metadata` | `object` | Có | metadata scenario/debug |

## 4.2. Ví dụ request

```json
{
  "machineId": "a1000000-0000-0000-0000-000000000001",
  "ts": "2026-03-27T09:15:02Z",
  "connectionStatus": "ONLINE",
  "machineState": "RUNNING",
  "operationMode": "AUTO",
  "programName": "JOB-AXLE-01",
  "cycleRunning": true,
  "powerKw": 4.8,
  "temperatureC": 48.2,
  "vibrationMmS": 1.9,
  "runtimeHours": 182.4,
  "cycleTimeSec": 42.0,
  "outputCount": 551,
  "goodCount": 545,
  "rejectCount": 6,
  "spindleSpeedRpm": 3200,
  "feedRateMmMin": 860,
  "toolCode": "T-AXLE-01",
  "remainingToolLifePct": 62.5,
  "voltageV": 380.0,
  "currentA": 8.2,
  "powerFactor": 0.91,
  "frequencyHz": 50.0,
  "energyKwhShift": 24.8,
  "energyKwhDay": 67.1,
  "motorTemperatureC": 51.0,
  "bearingTemperatureC": 44.5,
  "cabinetTemperatureC": 36.0,
  "servoOnHours": 320.0,
  "startStopCount": 127,
  "lubricationLevelPct": 81.0,
  "batteryLow": false,
  "metadata": {
    "scenario": "NORMAL",
    "simulatorNode": "SIM-LOCAL-01",
    "sendDelayMs": 0,
    "source": "SIMULATOR",
    "machineCode": "MC-01",
    "cycleCount": 568
  }
}
```

## 4.3. Các field cũ không còn là field ingest chính

Không dùng làm field nghiệp vụ chính nữa:
- `machineCode`
- `partCount`
- `cycleCount`
- `toolWearPercent`
- `energyKwhTotal`
- `maintenanceHealthScore`

Trong đó:
- `machineCode` được giữ ở `metadata.machineCode` để debug/log
- `cycleCount` có thể giữ ở `metadata.cycleCount`
- `toolWearPercent` chỉ dùng nội bộ để suy ra `remainingToolLifePct`

---

## 5. Quy tắc timestamp

Simulator phân biệt rõ 3 loại thời gian:

- `ts`: thời điểm snapshot được tạo ở nguồn
- `lastSent`: trạng thái nội bộ của simulator
- `receivedAt`: backend tự gắn nếu cần

### Quy tắc bắt buộc

1. Snapshot vừa tạo là có `ts` ngay.
2. Nếu `delayed`, packet vẫn giữ `ts` gốc.
3. Nếu `duplicate`, packet gửi lại giữ nguyên `ts` và cùng payload.
4. `LastSent` không thay cho `ts`.

---

## 6. Response backend mà simulator xử lý

Simulator **không phụ thuộc response body JSON của backend**.
Nó chủ yếu dựa vào HTTP status code.

## 6.1. Thành công

Bất kỳ mã `2xx` nào đều được coi là thành công.

Ví dụ internal result:

```text
success=true
retryable=false
message="HTTP 202"
```

## 6.2. Lỗi 4xx

Các mã `4xx` được coi là **non-retryable**.

Ví dụ:

```text
HTTP 400 from ingest for MC-01 (non-retryable)
```

Hành vi:
- không retry vô hạn
- packet bị drop
- lưu `lastError`

## 6.3. Lỗi 5xx

Các mã `5xx` được coi là **retryable**.

Hành vi:
- retry ngắn tối đa 3 lần trong client
- có backoff ngắn
- nếu vẫn fail thì packet đưa lại queue nếu queue chưa đầy

## 6.4. Lỗi mạng / timeout / connection refused

Hành vi:
- retry ngắn trong client
- sau đó mark là retryable
- packet requeue nếu queue còn chỗ

---

## 7. Queue / network behavior

## 7.1. `disconnect`

- ngừng gửi packet hoàn toàn
- connection state = `OFFLINE`
- dashboard hiện `Sending=false`

## 7.2. `slowsend`

- vẫn tick đều mỗi giây
- HTTP chỉ gửi theo chu kỳ lớn hơn, ví dụ 3 giây
- connection state thường là `DEGRADED`

## 7.3. `delayed`

- snapshot vẫn sinh đều
- snapshot đi vào delayed queue
- tới thời điểm release mới chuyển sang pending queue
- vẫn giữ nguyên `ts` gốc

## 7.4. `duplicate`

- sau khi gửi thành công, cùng payload được đưa lại queue đúng 1 lần
- dùng để test idempotency phía backend

---

## 8. Machine registry hiện tại

Registry nguồn nằm ở file `src/main/resources/machine-registry.yaml`.

| Machine Code | Machine ID | Default | Ghi chú |
|---|---|---|---|
| `MC-01` | `a1000000-0000-0000-0000-000000000001` | enabled | map chuẩn với backend |
| `MC-02` | `a1000000-0000-0000-0000-000000000002` | enabled | map chuẩn với backend |
| `MC-03` | `a1000000-0000-0000-0000-000000000003` | enabled | map chuẩn với backend |
| `MC-04` | `a1000000-0000-0000-0000-000000000004` | enabled | map chuẩn với backend |
| `MC-05` | `a1000000-0000-0000-0000-000000000005` | enabled | map chuẩn với backend |
| `MC-06` | `a1000000-0000-0000-0000-000000000006` | disabled | bật bằng `SIMULATOR_ENABLE_EXTRA_MACHINES=true` |
| `MC-07` | `a1000000-0000-0000-0000-000000000007` | disabled | bật bằng `SIMULATOR_ENABLE_EXTRA_MACHINES=true` |

Lưu ý:
- mặc định simulator vẫn chạy an toàn với backend contract 5 máy
- nếu muốn thêm 2 máy test local, bật biến môi trường `SIMULATOR_ENABLE_EXTRA_MACHINES=true`
- nếu backend chưa seed `MC-06`, `MC-07`, không nên bật 2 máy này trong môi trường tích hợp

---

## 9. Console command API

## 9.1. `help`

### Input

```text
help
cases
options
scenarios
```

### Response mẫu

```text
── PLC Simulator Commands ──────────────────────────────────────────
help | cases | options | scenarios        Show usage + supported test cases
list | status                              All machines status
pause                                      Pause HTTP send for all
resume                                     Resume HTTP send for all
exit                                       Shutdown simulator

machine <code> status                      Single machine status
machine <code> scenario <name> [durSec]    Set scenario + optional duration
machine <code> set spindle <rpm>           Override spindle speed
machine <code> set feed <mm/min>           Override feed rate
machine <code> set toolwear <0-100>        Override tool wear %
machine <code> reconnect                   Force machine back online
machine <code> reset                       Reset all state to defaults

all scenario <name> [durSec]               Apply scenario to ALL machines
all pause-send                             Pause send for all
all resume-send                            Resume send for all

script shift-start                         Run shift-start sequence
script network-chaos                       Run network chaos sequence
script maintenance-warning                 Run maintenance warning sequence

Machines: MC-01 | MC-02 | MC-03 | MC-04 | MC-05
Scenarios: normal | warmup | idle | overheat | high-vibration | tool-wear-rising | emergency-stop | disconnect | slowsend | delayed | duplicate

Invalid input tips:
- machine code is case-insensitive (ex: mc-01)
- duration must be an integer >= 0
- toolwear must stay in range 0..100
────────────────────────────────────────────────────────────────────
```

---

## 9.2. `list` / `status`

### Input

```text
list
```

### Response mẫu

```text
[MC-01] ONLINE | Sending=true | NORMAL | RUNNING | Temp=48.2C | Power=4.8kW | Vib=1.90 | Output=551 | ToolLife=62.5% | LastSent=1s ago | LastOk=1s ago | Pending=0 | Delayed=0
```

Nếu có lỗi gửi gần nhất, cuối dòng sẽ có thêm:

```text
| LastErr=HTTP 500 from ingest for MC-01
```

---

## 9.3. `machine <code> status`

### Input

```text
machine MC-01 status
```

### Response mẫu

```text
[MC-01] ONLINE | Sending=true | NORMAL | RUNNING | Temp=48.2C | Power=4.8kW | Vib=1.90 | Output=551 | ToolLife=62.5% | LastSent=1s ago | LastOk=1s ago | Pending=0 | Delayed=0
```

Nếu không có máy:

```text
Machine not found: MC-99
```

`machine code` được xử lý không phân biệt hoa thường, ví dụ `machine mc-01 status` vẫn hợp lệ.

---

## 9.4. `machine <code> scenario <name> [seconds]`

### Input

```text
machine MC-01 scenario overheat 120
```

### Response

```text
OK → MC-01 scenario=OVERHEAT for 120s
```

### Response lỗi thường gặp

```text
Unknown scenario 'abc'. Supported: normal | warmup | idle | overheat | high-vibration | tool-wear-rising | emergency-stop | disconnect | slowsend | delayed | duplicate
```

```text
Duration must be an integer >= 0.
```

---

## 9.5. `machine <code> set spindle <rpm>`

### Input

```text
machine MC-01 set spindle 3200
```

### Response

```text
OK → MC-01 spindle=3200.0
```

---

## 9.6. `machine <code> set feed <mm/min>`

### Input

```text
machine MC-01 set feed 850
```

### Response

```text
OK → MC-01 feed=850.0
```

---

## 9.7. `machine <code> set toolwear <0-100>`

### Input

```text
machine MC-01 set toolwear 40
```

### Response

```text
OK → MC-01 toolwear=40.0
```

Lưu ý:
- đây là giá trị nội bộ của simulator
- payload gửi ra sẽ là `remainingToolLifePct = 60.0`

Nếu nhập sai option:

```text
Unknown param 'speed'. Use spindle|feed|toolwear
```

```text
toolwear must be between 0 and 100.
```

---

## 9.8. `machine <code> reconnect`

### Input

```text
machine MC-03 reconnect
```

### Response

```text
OK → MC-03 is back ONLINE
```

---

## 9.9. `machine <code> reset`

### Input

```text
machine MC-04 reset
```

### Response

```text
OK → MC-04 reset to defaults
```

---

## 9.10. `all scenario <name> [durSec]`

### Input

```text
all scenario idle 60
```

### Response

```text
OK → All machines scenario=IDLE for 60s
```

---

## 9.11. `all pause-send`

### Input

```text
all pause-send
```

### Response

```text
Send paused for all machines.
```

## 9.12. `all resume-send`

### Input

```text
all resume-send
```

### Response

```text
Send resumed for all machines.
```

---

## 9.13. `script shift-start`

### Input

```text
script shift-start
```

### Response ngay lập tức

```text
Script 'shift-start' started (5 steps) in background.
```

Trong quá trình chạy sẽ có log:

```text
[SCRIPT] all scenario warmup 30
[SCRIPT] all scenario normal 60
...
```

---

## 10. State model

Simulator tách rõ 3 lớp trạng thái:

### Operational state
- `RUNNING`
- `IDLE`
- `WARMUP`
- `STOPPED`
- `EMERGENCY_STOP`

### Connection state
- `ONLINE`
- `DEGRADED`
- `OFFLINE`

### Scenario state
- `NORMAL`
- `WARMUP`
- `IDLE`
- `OVERHEAT`
- `HIGH_VIBRATION`
- `TOOL_WEAR_RISING`
- `EMERGENCY_STOP`
- `DISCONNECT`
- `SLOWSEND`
- `DELAYED`
- `DUPLICATE`

---

## 11. Dashboard nội bộ

Simulator in dashboard định kỳ với các thông tin:
- machine code
- connection state
- sending state
- scenario
- machine state
- temperature
- power
- vibration
- output count
- remaining tool life
- pending queue size
- delayed queue size
- last sent
- last success
- last error

Mục tiêu là người test nhìn console biết ngay máy nào đang:
- chạy bình thường
- bị offline/degraded
- nghẽn queue
- gửi lỗi backend

---

## 12. Giới hạn trách nhiệm của simulator

Simulator **không** là nơi tính chính thức:
- OEE
- alarm business
- downtime business
- maintenance health score chính thức
- KPI dashboard chính thức

Những phần đó phải do backend đảm nhiệm.
