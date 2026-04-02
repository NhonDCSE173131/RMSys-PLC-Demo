# Project giả lập PLC bằng console cho Manufacturing Monitor

## 1. Mục tiêu của project giả lập

Project này không phải là simulator kiểu random đơn giản.
Mục tiêu của nó là:
- giả lập nhiều máy
- gửi dữ liệu liên tục về backend chính
- cho người dùng nhập lệnh trên console để đổi kịch bản ngay khi đang chạy
- tạo ra các case thực tế như mất kết nối, chậm gửi, dữ liệu trễ, nhiệt độ tăng, rung tăng, tool wear tăng, idle, emergency stop...
- dữ liệu sinh ra phải **logic**, có quan hệ giữa các thông số, không nhảy loạn

Project này sẽ giúp bạn test:
- ingest API
- realtime UI
- chart
- lịch sử
- alarm/rule engine
- các case mất kết nối hoặc degraded

---

## 2. Kiến trúc tổng thể

```text
+------------------------+
| PLC Simulator Console  |
| - nhiều máy            |
| - state machine        |
| - scenario engine      |
| - command input        |
| - sender HTTP          |
+-----------+------------+
            |
            v
+------------------------+
| Main Backend           |
| /api/v1/ingest/...     |
+-----------+------------+
            |
            v
+------------------------+
| UI / Chart / History   |
+------------------------+
```

---

## 3. Vì sao nên làm project riêng thay vì nhét vào backend chính

### Lý do 1
Bạn cần chủ động đổi case khi backend đang chạy.

### Lý do 2
Bạn cần test UI và backend như thể có nguồn dữ liệu bên ngoài thật.

### Lý do 3
Bạn cần tách phần "sinh dữ liệu" khỏi phần "xử lý dữ liệu".

### Lý do 4
Sau này project này còn dùng để test regression rất tốt.

---

## 4. Công nghệ nên dùng

Để đồng bộ với hệ sinh thái hiện tại, nên dùng:
- Java
- Maven
- app console
- HTTP client để gửi sang backend chính

Không cần UI web cho project này ở giai đoạn đầu.
Console là đủ và rất tiện để test nhanh.

---

## 5. Cấu trúc project đề xuất

```text
manufacturing-plc-simulator/
├─ src/main/java/com/rmsys/plcsim
│  ├─ SimulatorApplication
│  ├─ config
│  │  └─ SimulatorProperties
│  ├─ console
│  │  ├─ ConsoleCommandLoop
│  │  ├─ CommandParser
│  │  └─ CommandHandlers
│  ├─ domain
│  │  ├─ model
│  │  │  ├─ MachineProfile
│  │  │  ├─ MachineRuntimeState
│  │  │  ├─ MachineScenario
│  │  │  ├─ ConnectionStatus
│  │  │  └─ NormalizedTelemetryDto
│  │  ├─ scenario
│  │  │  ├─ ScenarioEngine
│  │  │  ├─ NormalProductionScenario
│  │  │  ├─ HeatingScenario
│  │  │  ├─ OverheatScenario
│  │  │  ├─ VibrationScenario
│  │  │  ├─ ToolWearScenario
│  │  │  ├─ DisconnectScenario
│  │  │  ├─ SlowSendScenario
│  │  │  ├─ DelayedMessageScenario
│  │  │  ├─ IdleScenario
│  │  │  └─ EmergencyStopScenario
│  │  └─ service
│  │     ├─ MachineSimulationService
│  │     ├─ TelemetryGenerationService
│  │     ├─ ScenarioSwitchService
│  │     └─ TelemetrySendService
│  ├─ infrastructure
│  │  ├─ http
│  │  │  └─ BackendTelemetryClient
│  │  └─ scheduler
│  │     └─ TickScheduler
│  └─ shared
└─ src/main/resources
   ├─ application.yaml
   └─ machines.yaml
```

---

## 6. Ý tưởng lõi: simulator phải chạy theo state machine chứ không random hoàn toàn

Dữ liệu logic phải đến từ:
- trạng thái máy hiện tại
- profile máy
- kịch bản đang chạy
- giá trị kỳ trước
- tốc độ biến thiên hợp lý

Không nên sinh kiểu:
- tick 1: nhiệt độ 38
- tick 2: nhiệt độ 92
- tick 3: nhiệt độ 41

Kiểu đó nhìn là giả ngay và làm chart vô nghĩa.

---

## 7. Machine profile - hồ sơ từng máy

Mỗi máy nên có profile riêng để dữ liệu không giống hệt nhau.

## 7.1. Ví dụ field của `MachineProfile`

- `machineCode`
- `displayName`
- `machineType` (CNC, LATHE, MILL, ROBOT_CELL...)
- `minIdlePowerKw`
- `nominalRunPowerKw`
- `maxPowerKw`
- `normalTempRange`
- `warningTemp`
- `dangerTemp`
- `normalVibrationRange`
- `maxSpindleSpeed`
- `normalFeedRate`
- `baseCycleTimeSec`
- `toolWearRatePerCycle`
- `maintenanceDecayRate`

### Ví dụ
Máy CNC spindle lớn sẽ:
- power cao hơn
- nhiệt tăng nhanh hơn
- vibration khi overload rõ hơn

---

## 8. Runtime state - trạng thái chạy hiện tại của từng máy

`MachineRuntimeState` nên giữ:
- `connectionStatus`
- `machineState`
- `currentTemperatureC`
- `currentVibrationMmS`
- `currentPowerKw`
- `currentSpindleSpeedRpm`
- `currentFeedRateMmMin`
- `cycleCount`
- `partCount`
- `toolWearPercent`
- `maintenanceHealthScore`
- `lastTelemetryTs`
- `currentScenario`
- `scenarioUntilTs`
- `delayedQueue`
- `duplicateMode`
- `sendPaused`

---

## 9. Kịch bản cần có

## 9.1. Normal Production

### Mô tả
Máy chạy ổn định, cycle đều, nhiệt tăng nhẹ rồi ổn định, tool wear tăng chậm.

### Dấu hiệu dữ liệu
- power nằm vùng nominal
- spindle/fed rate dao động nhẹ
- nhiệt tăng từ từ rồi giữ quanh ngưỡng vận hành
- partCount tăng theo chu kỳ

---

## 9.2. Warm-up

### Mô tả
Máy vừa bắt đầu chạy sau idle.

### Dấu hiệu
- spindle tăng dần
- power tăng dần
- nhiệt tăng chậm từ nhiệt độ phòng đến vùng vận hành
- chưa tăng partCount ngay nếu chưa đủ 1 chu kỳ

---

## 9.3. Overheat

### Mô tả
Máy quá tải hoặc làm việc liên tục.

### Dấu hiệu logic
- power cao hơn bình thường
- spindle hoặc tải cắt tăng
- nhiệt tăng nhanh hơn bình thường
- vibration có thể nhích lên
- quality/cycle time có thể giảm nhẹ

---

## 9.4. High Vibration

### Mô tả
Dao mòn, lệch trục, rung bất thường.

### Dấu hiệu logic
- vibration tăng rõ
- power có thể tăng nhẹ
- nhiệt có thể tăng nhẹ hoặc vừa
- cycle time có thể kéo dài
- toolWear tăng nhanh hơn

---

## 9.5. Tool Wear Rising

### Mô tả
Dao đang mòn dần.

### Dấu hiệu logic
- `toolWearPercent` tăng dần chứ không nhảy mạnh
- cycle time tăng từ từ
- vibration tăng từ từ
- chất lượng vận hành giảm dần
- có thể dẫn tới overheat nếu kéo dài

---

## 9.6. Idle / Waiting Material

### Mô tả
Máy online nhưng không gia công.

### Dấu hiệu logic
- spindle thấp hoặc 0
- feed rate 0
- power chỉ ở mức idle
- nhiệt giảm từ từ
- partCount không tăng

---

## 9.7. Emergency Stop

### Mô tả
Dừng khẩn cấp.

### Dấu hiệu logic
- machineState chuyển ngay sang `STOPPED` hoặc `EMERGENCY_STOP`
- spindle = 0
- feed rate = 0
- power giảm mạnh nhưng không về 0 tức thì
- nhiệt giảm dần sau đó

---

## 9.8. Disconnect

### Mô tả
Nguồn gửi dữ liệu bị đứt.

### Cách giả lập
- ngừng gửi telemetry trong N giây
- hoặc gửi event trạng thái `OFFLINE`

### Lưu ý
Case này **không được** vừa báo mất kết nối vừa vẫn đều đặn gửi telemetry như bình thường.

---

## 9.9. Slow Send

### Mô tả
Dữ liệu vẫn có nhưng gửi chậm hơn poll interval.

### Cách giả lập
- tick nội bộ vẫn chạy mỗi 1s
- nhưng sender chỉ đẩy mỗi 3s hoặc 5s
- metadata nên có `sendDelayMs`

---

## 9.10. Delayed / Out-of-order Message

### Mô tả
Một số gói được gửi muộn hoặc lẫn thứ tự.

### Cách giả lập
- đưa một số snapshot vào queue trễ 10-20s
- sau đó mới gửi
- có thể xen một gói cũ vào giữa các gói mới

### Mục đích
Test backend có xử lý stale/out-of-order tốt không.

---

## 9.11. Duplicate Packet

### Mô tả
Gửi lặp cùng một gói hoặc gần như cùng một gói.

### Mục đích
Test chống duplicate ở ingest/rule.

---

## 10. Công thức sinh dữ liệu phải logic

## 10.1. Nhiệt độ

Không random hoàn toàn. Nên có công thức tiến dần về mục tiêu.

### Ý tưởng

```text
tempNext = tempCurrent + alpha * (targetTemp - tempCurrent) + loadEffect + smallNoise
```

### Giải thích
- khi idle: target thấp, nhiệt giảm dần
- khi run bình thường: target quanh vùng vận hành
- khi overheat: target tăng cao hơn và tiến dần lên

---

## 10.2. Công suất điện

Nên phụ thuộc vào trạng thái máy.

### Ý tưởng

```text
power = idlePower
      + spindleRatio * k1
      + feedRatio * k2
      + cuttingLoad * k3
      + vibrationPenalty
```

### Ví dụ logic
- idle: 1.2 kW
- run bình thường: 3.5 - 5.2 kW
- quá tải: 5.5 - 6.8 kW

---

## 10.3. Rung

Rung nên liên quan tới:
- spindle speed
- tool wear
- scenario overload/misalignment

### Ý tưởng

```text
vibration = baseVibration + spindleFactor + wearFactor + scenarioFactor + tinyNoise
```

---

## 10.4. Tool wear

Tool wear phải tăng tích lũy theo thời gian/cycle.

### Ý tưởng

```text
toolWearNext = toolWearCurrent + wearRatePerCycle * cycleProgressFactor
```

### Quy tắc
- idle thì tăng rất ít hoặc không tăng
- cắt nặng thì wear tăng nhanh hơn
- thay dao thì reset về thấp

---

## 10.5. Part count và cycle count

### Quy tắc đúng
- `cycleCount` tăng khi hoàn tất một chu kỳ
- `partCount` chỉ tăng nếu chu kỳ tạo ra sản phẩm đạt điều kiện
- khi idle hoặc disconnect thì không tăng
- khi emergency stop giữa chu kỳ thì có thể không tăng partCount

---

## 10.6. Maintenance health score

Đây là chỉ số giảm dần chứ không được nhảy lung tung.

### Ý tưởng

```text
healthNext = healthCurrent - wearImpact - vibrationImpact - temperatureImpact + maintenanceRecovery
```

---

## 11. Tick engine - lõi của simulator

Mỗi máy nên có tick nội bộ, ví dụ mỗi 1 giây.

Mỗi tick:
1. đọc scenario hiện tại
2. cập nhật state vật lý của máy
3. quyết định có tạo cycle mới không
4. tạo snapshot telemetry
5. quyết định gửi ngay, gửi chậm, hay bỏ qua theo scenario

---

## 12. Console command cần có

Simulator phải cho người dùng nhập lệnh ngay trên console.

## 12.1. Lệnh hệ thống

- `help`
- `list`
- `status`
- `exit`
- `pause`
- `resume`

---

## 12.2. Lệnh theo máy

- `machine CNC-01 status`
- `machine CNC-01 scenario normal`
- `machine CNC-01 scenario overheat 120`
- `machine CNC-01 scenario disconnect 30`
- `machine CNC-01 scenario slowsend 60`
- `machine CNC-01 set spindle 3200`
- `machine CNC-01 set feed 850`
- `machine CNC-01 set toolwear 68`
- `machine CNC-01 reconnect`
- `machine CNC-01 reset`

---

## 12.3. Lệnh toàn cục

- `all scenario idle`
- `all scenario normal`
- `all pause-send`
- `all resume-send`

---

## 12.4. Script test nhanh

Cho phép lệnh kiểu:

- `script shift-start`
- `script overload-batch`
- `script line-disconnect`

Mục tiêu là chạy nhiều case liên tiếp mà không phải gõ tay từng bước.

---

## 13. Màn hình console nên hiển thị gì

Console không chỉ nhận lệnh, nó cũng nên in dashboard text nhỏ:

- danh sách máy
- scenario hiện tại
- trạng thái kết nối
- nhiệt độ hiện tại
- power hiện tại
- partCount hiện tại
- lần gửi thành công cuối cùng
- số gói lỗi/gói bị delay

Ví dụ:

```text
[CNC-01] ONLINE | NORMAL | Temp=48.2C | Power=4.8kW | Vib=1.9 | Part=124 | LastSent=16:20:05
[CNC-02] ONLINE | OVERHEAT | Temp=76.4C | Power=6.5kW | Vib=3.8 | Part=95  | LastSent=16:20:05
[CNC-03] OFFLINE | DISCONNECT | LastSent=16:19:21
```

---

## 14. Sender sang backend chính

Project simulator nên gửi dữ liệu vào đúng ingest API của backend chính.

## 14.1. Endpoint

- `POST /api/v1/ingest/telemetry`

## 14.2. Dữ liệu nên gửi

```json
{
  "machineCode": "CNC-01",
  "ts": "2026-03-26T16:20:05Z",
  "connectionStatus": "ONLINE",
  "machineState": "RUNNING",
  "temperatureC": 48.2,
  "spindleSpeedRpm": 3200,
  "feedRateMmMin": 860,
  "vibrationMmS": 1.9,
  "powerKw": 4.8,
  "energyKwhTotal": 1524.6,
  "cycleCount": 568,
  "partCount": 551,
  "toolWearPercent": 37.5,
  "maintenanceHealthScore": 82.0,
  "metadata": {
    "scenario": "NORMAL_PRODUCTION",
    "simulatorNode": "SIM-LOCAL-01",
    "sendDelayMs": 0
  }
}
```

---

## 15. Xử lý các case gửi dữ liệu đặc biệt

## 15.1. Disconnect

### Cách giả lập đúng
- ngừng gửi hoàn toàn trong thời gian chỉ định
- hoặc gửi 1 gói cuối cùng báo `OFFLINE`, sau đó im lặng

### Không nên làm
- cứ mỗi giây gửi `OFFLINE` mãi trừ khi bạn cố tình test kiểu đó

---

## 15.2. Slow send

### Cách giả lập đúng
- snapshot vẫn được tính theo tick thật
- sender chỉ flush một phần snapshot theo chu kỳ chậm hơn

Như vậy backend sẽ thấy dữ liệu tới thưa, chứ không phải giá trị vật lý ngừng thay đổi.

---

## 15.3. Delayed packet

### Cách giả lập đúng
- snapshot được tạo đúng lúc
- nhưng bị đưa vào queue đợi
- khi gửi phải giữ timestamp gốc

Điểm này rất quan trọng để test stale/out-of-order.

---

## 15.4. Duplicate

### Cách giả lập đúng
- gửi lại y nguyên gói cũ hoặc cùng sequence gần như giống nhau
- không sinh ngẫu nhiên gói mới rồi gọi đó là duplicate

---

## 16. Các class quan trọng nên có

## 16.1. `MachineSimulationService`

Quản lý runtime state cho tất cả máy.

### Trách nhiệm
- load machines
- chạy tick
- đổi scenario
- reset state
- trả status cho console

---

## 16.2. `TelemetryGenerationService`

Sinh snapshot logic từ state hiện tại.

### Trách nhiệm
- áp công thức vật lý đơn giản
- giữ độ mượt của dữ liệu
- tạo DTO chuẩn

---

## 16.3. `ScenarioEngine`

Mỗi scenario có logic riêng.

### Trách nhiệm
- set target temp/power/vibration
- điều chỉnh cycle time
- điều chỉnh connection/send behavior

---

## 16.4. `TelemetrySendService`

### Trách nhiệm
- gửi HTTP sang backend
- hỗ trợ delayed queue
- hỗ trợ duplicate mode
- log request lỗi

---

## 16.5. `ConsoleCommandLoop`

### Trách nhiệm
- đọc lệnh người dùng
- parse lệnh
- gọi handler tương ứng

---

## 17. Luồng vận hành đề xuất

1. app startup
2. load danh sách máy từ `machines.yaml`
3. tạo runtime state cho từng máy
4. bắt đầu tick engine mỗi 1 giây
5. mở console loop để nhận lệnh
6. mỗi tick tạo snapshot logic
7. sender quyết định gửi ngay, delay, duplicate hay bỏ gửi
8. backend chính nhận dữ liệu và xử lý như nguồn thật

---

## 18. File cấu hình mẫu

```yaml
simulator:
  backendBaseUrl: http://localhost:8080
  tickMs: 1000
  machines:
    - machineCode: CNC-01
      machineType: CNC
      minIdlePowerKw: 1.2
      nominalRunPowerKw: 4.8
      maxPowerKw: 6.8
      normalTempRange: [38, 55]
      warningTemp: 68
      dangerTemp: 82
      normalVibrationRange: [1.1, 2.2]
      maxSpindleSpeed: 6000
      normalFeedRate: 850
      baseCycleTimeSec: 42
      toolWearRatePerCycle: 0.08
      maintenanceDecayRate: 0.01
```

---

## 19. Script scenario gợi ý để test UI/backend

## 19.1. Script `shift-start`

- phút 0-2: warm-up
- phút 2-20: normal production
- phút 20-25: tool wear rising
- phút 25-27: overheat nhẹ
- phút 27-35: normal production

---

## 19.2. Script `network-chaos`

- máy 1: slow send 60s
- máy 2: disconnect 30s
- máy 3: delayed packets 45s
- máy 4: duplicate mode 20 packets

---

## 19.3. Script `maintenance-warning`

- chạy normal lâu
- tăng dần wear và vibration
- health score giảm dần
- đến ngưỡng thì sinh điều kiện cảnh báo bảo trì

---

## 20. Checklist triển khai project giả lập

- [ ] Tạo project console Maven
- [ ] Tạo model `MachineProfile`
- [ ] Tạo model `MachineRuntimeState`
- [ ] Tạo `ScenarioEngine`
- [ ] Tạo 5 scenario cơ bản trước: normal, idle, overheat, disconnect, slowsend
- [ ] Tạo `TelemetryGenerationService`
- [ ] Tạo `BackendTelemetryClient`
- [ ] Tạo console command loop
- [ ] Tạo dashboard text trong console
- [ ] Tạo delayed queue
- [ ] Tạo duplicate mode
- [ ] Tạo script scenario
- [ ] Test với UI chart và realtime

---

## 21. Thứ tự nên làm để nhanh có kết quả

## Bước 1
Làm 1 máy, 2 scenario:
- normal
- disconnect

## Bước 2
Thêm idle, overheat, slowsend

## Bước 3
Thêm delayed, duplicate, tool wear rising

## Bước 4
Thêm nhiều máy và script scenario

---

## 22. Kết luận

Project giả lập PLC nên là một **console app riêng**, không phải random generator đơn giản.

Nó cần có:
- nhiều máy
- profile riêng cho từng máy
- state machine rõ ràng
- scenario engine
- command input trên console
- sender gửi liên tục sang backend chính
- dữ liệu sinh logic, có liên hệ vật lý đơn giản giữa các thông số

Làm đúng kiểu này thì simulator của bạn sẽ rất có giá trị: vừa dùng để phát triển, vừa dùng để test, vừa dùng để demo hệ thống như đang nối với thiết bị thật.
