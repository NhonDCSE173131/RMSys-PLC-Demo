# Chốt chỉnh sửa bên Simulator-PLC

## Kết luận ngắn

Đối với **phần máy ảo / PLC simulator**, đúng là lỗi chính đang nằm ở **repo Simulator-PLC** và repo này cần sửa để nói đúng ngôn ngữ với backend.

Tuy nhiên, điều đó **không có nghĩa toàn bộ hệ thống chỉ cần sửa simulator là xong**. 
- Phần **simulator ↔ BE**: chủ yếu sửa ở **Simulator-PLC**.
- Phần **UI ↔ BE**: vẫn còn các chỗ lệch riêng giữa UI và BE, không thể giải quyết chỉ bằng simulator.

File này chỉ chốt việc của **bên Simulator-PLC**.

---

## 1. Mục tiêu bắt buộc của Simulator-PLC

Simulator-PLC phải trở thành **nguồn dữ liệu giả lập chuẩn** cho backend, nghĩa là:

1. Gửi đúng endpoint ingest của BE.
2. Gửi đúng schema mà BE chấp nhận.
3. Gửi đúng định danh máy mà BE đang dùng.
4. Gửi dữ liệu có logic vận hành, không random vô nghĩa.
5. Gửi được nhiều case thực tế để test backend và UI.
6. Có thể chạy độc lập bằng console để người test điều khiển scenario.

---

## 2. Chốt vai trò của Simulator-PLC

Simulator-PLC **không phân tích KPI**.

Simulator-PLC chỉ có nhiệm vụ:
- mô phỏng hành vi máy,
- tạo dữ liệu vật lý hợp lý,
- gắn timestamp nguồn,
- gửi telemetry thô/chuẩn hóa sang backend,
- hỗ trợ test các case như disconnect, delayed, duplicate, slow send, overheat, vibration, tool wear.

Mọi thứ như:
- alarm,
- downtime,
- OEE,
- energy summary,
- health score chính thức,
- analytics,
- export,
- trend,
- dashboard KPI

phải để **backend** làm.

---

## 3. Những lỗi chính hiện tại của Simulator-PLC

### 3.1. Sai định danh máy so với backend

Backend hiện seed máy theo mã:
- `MC-01`
- `MC-02`
- `MC-03`
- `MC-04`
- `MC-05`

Trong khi simulator hiện đang mô tả và chạy theo mã:
- `CNC-01`
- `CNC-02`
- `ROB-01`
- `LATHE-01`
- `DRILL-01`

Hậu quả:
- backend không ghép đúng dữ liệu vào máy thật đang có,
- UI lấy máy từ backend sẽ không khớp với máy simulator đang bơm dữ liệu,
- rule và dashboard dễ sai hoặc không lên dữ liệu đúng machine.

### 3.2. Payload gửi lên chưa khớp contract ingest của backend

Simulator hiện đang mô tả payload kiểu:
- `machineCode`
- `partCount`
- `cycleCount`
- `toolWearPercent`
- `maintenanceHealthScore`
- `energyKwhTotal`

Trong khi backend ingest DTO hiện dùng kiểu:
- `machineId`
- `outputCount`
- `goodCount`
- `rejectCount`
- `remainingToolLifePct`
- `energyKwhShift`
- `energyKwhDay`
- cùng nhiều field khác như `operationMode`, `runtimeHours`, `cycleTimeSec`, `toolCode`, `metadata`...

Hậu quả:
- simulator gửi mà BE không map đúng,
- dữ liệu bị thiếu,
- rule engine không chạy đúng,
- dashboard tổng hợp không chuẩn.

### 3.3. Simulator đang ôm một số khái niệm backend nên tính

Ví dụ `maintenanceHealthScore` là một chỉ số tổng hợp. Nếu simulator tự gửi và backend cũng tự tính thì sẽ sinh ra 2 nguồn sự thật.

Chốt lại: simulator chỉ nên gửi tín hiệu gốc hoặc tín hiệu gần-gốc. Chỉ số tổng hợp nên do backend tính.

### 3.4. `LastSent` chỉ là trạng thái nội bộ, không phải timestamp nghiệp vụ đầy đủ

Simulator hiện có hiển thị `LastSent`, nhưng đây chỉ nên là biến nội bộ để biết vòng gửi gần nhất.

Bản tin gửi sang backend phải có ít nhất:
- `ts` hoặc `sourceTimestamp`: thời điểm dữ liệu được tạo ở simulator/nguồn,
- metadata về delay nếu cần,
- backend sẽ tự thêm `receivedAt` phía server.

---

## 4. Chốt việc Simulator-PLC phải sửa

## 4.1. Sửa machine identity theo backend

**Bắt buộc đổi simulator sang dùng cùng mã máy với backend.**

Chốt dùng:
- `MC-01`
- `MC-02`
- `MC-03`
- `MC-04`
- `MC-05`

Và mỗi máy phải map cố định sang UUID của backend:
- `MC-01` -> `a1000000-0000-0000-0000-000000000001`
- `MC-02` -> `a1000000-0000-0000-0000-000000000002`
- `MC-03` -> `a1000000-0000-0000-0000-000000000003`
- `MC-04` -> `a1000000-0000-0000-0000-000000000004`
- `MC-05` -> `a1000000-0000-0000-0000-000000000005`

### Cách làm

Tạo `machine-registry.yaml` hoặc `machines.yaml` trong simulator, ví dụ:

```yaml
simulator:
  machines:
    - machine-id: a1000000-0000-0000-0000-000000000001
      machine-code: MC-01
      display-name: CNC Lathe 01
      machine-type: CNC_MACHINE
    - machine-id: a1000000-0000-0000-0000-000000000002
      machine-code: MC-02
      display-name: CNC Milling 01
      machine-type: CNC_MACHINE
    - machine-id: a1000000-0000-0000-0000-000000000003
      machine-code: MC-03
      display-name: Robot Arm 01
      machine-type: ROBOT_ONLY
    - machine-id: a1000000-0000-0000-0000-000000000004
      machine-code: MC-04
      display-name: Robot CNC Cell
      machine-type: ROBOT_CNC_CELL
    - machine-id: a1000000-0000-0000-0000-000000000005
      machine-code: MC-05
      display-name: CNC Grinding 01
      machine-type: CNC_MACHINE
```

**Chốt:** simulator phải gửi `machineId` là khóa chính, `machineCode` chỉ để log/debug nếu muốn.

---

## 4.2. Sửa payload gửi lên đúng contract ingest của backend

### Bản tin gửi lên phải theo shape backend đang nhận

Chốt payload simulator gửi sang BE như sau:

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
    "source": "SIMULATOR"
  }
}
```

### Không gửi các field sau nữa

Không gửi lên ingest payload chính các field sau:
- `maintenanceHealthScore`
- `energyKwhTotal`
- `partCount`
- `cycleCount`
- `toolWearPercent`
- `machineCode` là field nghiệp vụ chính

### Quy đổi field

- `partCount` -> `outputCount`
- `toolWearPercent` -> `remainingToolLifePct = 100 - toolWearPercent`
- `cycleCount` nếu cần chỉ giữ nội bộ hoặc đưa vào `metadata.cycleCount`
- `energyKwhTotal` không dùng làm field chính, chỉ nội bộ hoặc suy ra `energyKwhShift`, `energyKwhDay`
- `maintenanceHealthScore` bỏ, để backend tự tính

---

## 4.3. Chốt timestamp phải gửi như thế nào

Simulator phải phân biệt rõ:

- **event time / source time**: `ts`
- **send time nội bộ**: biến riêng của simulator, không dùng thay `ts`
- **received time**: để backend tự gắn

### Quy tắc

1. Mỗi snapshot khi sinh ra phải có `ts` ngay lúc tạo.
2. Nếu scenario là `delayed`, gói vẫn giữ `ts` gốc, chỉ trì hoãn lúc gửi.
3. Nếu scenario là `duplicate`, gói gửi lại phải giữ cùng `ts` và cùng payload để test idempotency.
4. `LastSent` chỉ hiển thị console, không được dùng thay cho `ts`.

---

## 4.4. Dữ liệu phải logic, không random vô nghĩa

Simulator phải dùng mô hình state-based thay vì random đơn giản.

## Chốt mô hình dữ liệu

Mỗi máy cần có:
- baseline vật lý
- state hiện tại
- scenario hiện tại
- counters tích lũy
- network behavior
- timeline event

### Thuộc tính nền mỗi máy

- loại máy
- nominal power
- idle power
- max power
- ngưỡng nhiệt bình thường
- ngưỡng rung bình thường
- max spindle
- base cycle time
- tốc độ mòn dao theo chu kỳ
- mức suy giảm bảo trì theo runtime/start-stop

### Công thức logic tối thiểu

#### Normal
- `powerKw` ~ nominal ± nhỏ
- `temperatureC` tăng ổn định trong vùng bình thường
- `vibrationMmS` thấp và ổn định
- `outputCount` tăng theo chu kỳ
- `goodCount` tăng gần bằng `outputCount`
- `rejectCount` tăng rất ít
- `remainingToolLifePct` giảm chậm
- `energyKwhShift`, `energyKwhDay` tăng dần

#### Idle
- spindle = 0
- feed = 0
- `cycleRunning = false`
- `powerKw` ở mức idle
- nhiệt giảm dần
- rung thấp
- output không tăng

#### Warmup
- spindle tăng dần
- power tăng dần
- nhiệt tăng từ môi trường lên vùng làm việc
- output chưa tăng ngay

#### Overheat
- nhiệt tăng nhanh
- power tăng cao
- rung nhích lên
- nếu quá lâu thì reject tăng
- tool life giảm nhanh hơn

#### High vibration
- rung tăng mạnh
- chu kỳ dài hơn
- reject tăng nhẹ
- nhiệt tăng vừa
- tool life giảm nhanh hơn normal

#### Tool wear rising
- `remainingToolLifePct` giảm nhanh
- cycle time tăng từ từ
- vibration tăng nhẹ
- reject tăng dần

#### Emergency stop
- state = `EMERGENCY_STOP`
- spindle = 0
- feed = 0
- cycleRunning = false
- power giảm đột ngột về gần idle
- output không tăng

#### Disconnect
- ngừng gửi packet hoàn toàn
- state nội bộ có thể giữ nguyên, nhưng outbound bị ngắt
- console phải hiện rõ `sending=false`

#### Slow send
- máy vẫn mô phỏng bình thường mỗi tick
- nhưng HTTP chỉ gửi theo khoảng lớn hơn, ví dụ 3s hoặc 5s

#### Delayed
- snapshot vẫn sinh đều
- snapshot vào queue delay
- gửi muộn nhưng giữ nguyên `ts`

#### Duplicate
- sau khi gửi thành công, thỉnh thoảng gửi lại đúng gói cũ
- không được sinh gói mới mà gọi là duplicate

---

## 4.5. Simulator phải có state machine rõ ràng

Tách ít nhất 3 lớp trạng thái:

### A. Operational state
- `RUNNING`
- `IDLE`
- `WARMUP`
- `STOPPED`
- `EMERGENCY_STOP`

### B. Connection state
- `ONLINE`
- `DEGRADED`
- `OFFLINE`

### C. Scenario state
- `NORMAL`
- `OVERHEAT`
- `HIGH_VIBRATION`
- `TOOL_WEAR_RISING`
- `DISCONNECT`
- `SLOWSEND`
- `DELAYED`
- `DUPLICATE`

Ba lớp này phải tách nhau, không gộp lẫn.

---

## 4.6. Chốt console command phải giữ

Các lệnh sau phải giữ hoặc hoàn thiện:

### Hệ thống
- `help`
- `list`
- `pause`
- `resume`
- `exit`

### Theo máy
- `machine <code> status`
- `machine <code> scenario <name> [seconds]`
- `machine <code> set spindle <rpm>`
- `machine <code> set feed <mm/min>`
- `machine <code> set toolwear <0-100>`
- `machine <code> reconnect`
- `machine <code> reset`

### Toàn bộ
- `all scenario normal`
- `all scenario idle <seconds>`
- `all scenario overheat <seconds>`
- `all pause-send`
- `all resume-send`

### Script test
- `script shift-start`
- `script network-chaos`
- `script maintenance-warning`

---

## 4.7. Chốt kiến trúc code nên có trong repo Simulator-PLC

## Package đề xuất

```text
com.simulator.plc
├─ app
│  ├─ PlcSimulatorApplication
│  └─ StartupRunner
├─ config
│  ├─ SimulatorProperties
│  └─ MachineRegistryProperties
├─ console
│  ├─ ConsoleCommandLoop
│  ├─ CommandParser
│  └─ CommandHandlers
├─ domain
│  ├─ model
│  │  ├─ MachineRuntimeState
│  │  ├─ MachineProfile
│  │  ├─ TelemetrySnapshot
│  │  ├─ ScenarioType
│  │  ├─ MachineOperationalState
│  │  └─ ConnectionState
│  ├─ service
│  │  ├─ ScenarioEngine
│  │  ├─ TelemetryGenerationService
│  │  ├─ NetworkBehaviorService
│  │  └─ MachineStateService
├─ integration
│  ├─ BackendIngestClient
│  ├─ IngestRequestMapper
│  └─ RetryPolicy
├─ scheduler
│  ├─ TickScheduler
│  └─ DashboardPrinter
└─ util
   ├─ TimeUtils
   └─ RandomDrift
```

---

## 4.8. Chốt backend client phía simulator

Simulator phải có 1 lớp duy nhất chịu trách nhiệm gửi sang backend, ví dụ `BackendIngestClient`.

### Nhiệm vụ của lớp này
- build request body đúng contract ingest
- set header cần thiết nếu có API key
- timeout rõ ràng
- retry có giới hạn
- log success/fail theo machine
- không để logic scenario lẫn với HTTP client

### Quy tắc retry
- lỗi mạng tạm thời: retry ngắn 1-3 lần
- lỗi 4xx do payload sai: không retry vô hạn, log lỗi rõ
- lỗi 5xx: retry có backoff

---

## 4.9. Chốt hiển thị console

Dashboard console mỗi vài giây nên hiển thị:
- machine code
- connection state
- scenario
- machine state
- temp
- power
- vibration
- outputCount
- remainingToolLifePct
- queue delayed size
- last sent time
- last success / last error

Mục tiêu là người test nhìn console là biết ngay:
- máy đang chạy gì,
- có gửi được không,
- đang ở case nào,
- packet có bị tồn queue không.

---

## 5. Những thứ Simulator-PLC không được làm

Simulator-PLC không được trở thành nơi:
- tính OEE chính thức,
- tính dashboard KPI chính thức,
- tính maintenance score chính thức,
- phân loại alarm chính thức,
- quyết định downtime chính thức,
- xuất báo cáo chính thức.

Nếu cần hiển thị tạm trong console thì chỉ là debug nội bộ, không phải business output.

---

## 6. Checklist triển khai cho team Simulator-PLC

## P1 - Bắt buộc làm ngay
- [ ] Đổi mã máy simulator sang `MC-01..MC-05`
- [ ] Map đúng `machineId` UUID của backend
- [ ] Đổi request body sang đúng contract ingest của backend
- [ ] Bỏ `maintenanceHealthScore` khỏi payload chính
- [ ] Quy đổi `toolWearPercent` thành `remainingToolLifePct`
- [ ] Gửi `ts` đúng lúc snapshot được tạo
- [ ] Giữ `LastSent` chỉ là biến nội bộ/console

## P2 - Để test được case thật
- [ ] Hoàn thiện scenario `disconnect`
- [ ] Hoàn thiện scenario `slowsend`
- [ ] Hoàn thiện scenario `delayed`
- [ ] Hoàn thiện scenario `duplicate`
- [ ] Tách rõ state vận hành và state kết nối
- [ ] Có delayed queue thật
- [ ] Có retry/backoff khi gọi backend

## P3 - Để simulator dùng lâu dài
- [ ] Tách `MachineProfile` và `MachineRuntimeState`
- [ ] Tách generator dữ liệu khỏi HTTP client
- [ ] Tách command parser khỏi scenario engine
- [ ] Có script test ca sản xuất / mạng lỗi / maintenance
- [ ] Có log rõ machine-by-machine

---

## 7. Chốt cuối cùng

**Đối với phần máy ảo**, hiện tại ưu tiên sửa đúng là **bên Simulator-PLC**.

Nhưng chỉ hiểu theo nghĩa:
- muốn **simulator nói đúng với backend** -> sửa simulator là chính.

Còn nếu muốn **toàn hệ thống ăn ý hoàn toàn** thì vẫn còn việc ở:
- backend: mở đủ API và giữ vai trò source of truth,
- UI: bỏ tự phân tích, chỉ consume dữ liệu backend.

Riêng phạm vi file này, team Simulator-PLC phải chốt 3 thứ đầu tiên:
1. **Đúng machineId**
2. **Đúng ingest payload**
3. **Đúng timestamp + scenario logic**

Làm xong 3 việc đó, simulator mới thật sự trở thành nguồn dữ liệu test đáng tin cho BE và UI.
