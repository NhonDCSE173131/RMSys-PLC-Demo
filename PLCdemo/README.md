# PLC Simulator – Hướng dẫn sử dụng

> Simulator PLC chạy console, mặc định giả lập 5 máy backend `MC-01..MC-05`, và có thể bật thêm `MC-06`, `MC-07` khi cần test mở rộng.

Tài liệu API/response chi tiết: [`SIMULATOR_API_RESPONSE.md`](./SIMULATOR_API_RESPONSE.md)

---

## 1. Mục tiêu hiện tại

Simulator này đã được chỉnh để:
- dùng đúng mã máy backend: `MC-01..MC-05`
- map đúng `machineId` UUID cố định
- gửi đúng payload ingest theo backend
- giữ `ts` là thời điểm snapshot được tạo ở simulator
- hỗ trợ các case test: `disconnect`, `slowsend`, `delayed`, `duplicate`, `overheat`, `high-vibration`, `tool-wear-rising`

---

## 2. Port và kết nối backend

### Port simulator

Simulator đang để port dự phòng:
- `server.port=8095`

Lý do:
- backend BE của bạn đang dùng `8080`
- simulator hiện chạy `web-application-type: none`, nên thực tế không mở REST API inbound
- `8095` chỉ là port dự phòng nếu sau này bật web endpoint / actuator

### Backend đích mặc định

Simulator gửi dữ liệu sang:
- `http://localhost:8080/api/v1/ingest/telemetry`

Có thể đổi bằng biến môi trường:

```powershell
$env:SIMULATOR_BACKEND_BASE_URL="http://localhost:8080"
$env:SIMULATOR_PORT="8095"
$env:SIMULATOR_ENABLE_EXTRA_MACHINES="true"
```

---

## 3. Chạy project

### Build

```powershell
.\mvnw.cmd clean package
```

### Run file jar

```powershell
$env:SIMULATOR_BACKEND_BASE_URL="http://localhost:8080"
$env:SIMULATOR_PORT="8095"
# Bật thêm MC-06, MC-07 nếu backend/local đã seed sẵn machineId tương ứng
$env:SIMULATOR_ENABLE_EXTRA_MACHINES="true"
java -jar target\PLCdemo-0.0.1-SNAPSHOT.jar
```

### Run từ IDE

Chạy file `PlCdemoApplication.java`.

Khi startup thành công sẽ thấy:

```text
PLC Simulator ready. Type 'help', 'cases', or 'list' for commands.
```

---

## 4. Danh sách máy

| Machine Code | Machine ID | Display Name | Machine Type |
|---|---|---|---|
| `MC-01` | `a1000000-0000-0000-0000-000000000001` | `CNC Lathe 01` | `CNC_MACHINE` |
| `MC-02` | `a1000000-0000-0000-0000-000000000002` | `CNC Milling 01` | `CNC_MACHINE` |
| `MC-03` | `a1000000-0000-0000-0000-000000000003` | `Robot Arm 01` | `ROBOT_ONLY` |
| `MC-04` | `a1000000-0000-0000-0000-000000000004` | `Robot CNC Cell` | `ROBOT_CNC_CELL` |
| `MC-05` | `a1000000-0000-0000-0000-000000000005` | `CNC Grinding 01` | `CNC_MACHINE` |

### Máy mở rộng tuỳ chọn

Hai máy này đã được thêm vào registry để bạn có thể bật khi cần test mở rộng, nhưng **mặc định tắt** để không lệch contract backend hiện tại.

| Machine Code | Machine ID | Display Name | Machine Type | Mặc định |
|---|---|---|---|---|
| `MC-06` | `a1000000-0000-0000-0000-000000000006` | `Assembly Press 01` | `PRESS_MACHINE` | `disabled` |
| `MC-07` | `a1000000-0000-0000-0000-000000000007` | `Inspection Cell 01` | `INSPECTION_CELL` | `disabled` |

Muốn bật 2 máy này:

```powershell
$env:SIMULATOR_ENABLE_EXTRA_MACHINES="true"
```

Registry máy hiện nằm trong file `src/main/resources/machine-registry.yaml`.

---

## 5. Cấu hình chính

File chính: `src/main/resources/application.yaml`

Registry máy: `src/main/resources/machine-registry.yaml`

```yaml
server:
  port: ${SIMULATOR_PORT:8095}

simulator:
  backend-base-url: ${SIMULATOR_BACKEND_BASE_URL:http://localhost:8080}
  ingest-path: /api/v1/ingest/telemetry
  tick-ms: 1000
  simulator-node: SIM-LOCAL-01
  console-enabled: true
```

---

## 6. Console commands

## 6.1. Hệ thống

| Command | Ý nghĩa |
|---|---|
| `help` / `cases` / `options` / `scenarios` | Xem help + danh sách case hỗ trợ |
| `list` / `status` | Xem trạng thái tất cả máy |
| `pause` | Tạm dừng gửi HTTP cho tất cả máy |
| `resume` | Tiếp tục gửi HTTP |
| `exit` | Tắt simulator |

## 6.2. Theo máy

```text
machine <code> status
machine <code> scenario <name> [seconds]
machine <code> set spindle <rpm>
machine <code> set feed <mm/min>
machine <code> set toolwear <0-100>
machine <code> reconnect
machine <code> reset
```

Ví dụ:

```text
machine MC-01 status
machine MC-01 scenario overheat 60
machine MC-03 scenario disconnect 30
machine MC-02 set spindle 3200
machine MC-05 set toolwear 78
machine MC-03 reconnect
machine MC-04 reset
```

## 6.3. Toàn bộ máy

```text
all scenario normal
all scenario idle 60
all scenario overheat 30
all pause-send
all resume-send
```

## 6.4. Script test

```text
script shift-start
script network-chaos
script maintenance-warning
```

### Các lỗi option thường gặp

- sai mã máy → `Machine not found: MC-99`
- sai scenario → `Unknown scenario 'abc'. Supported: normal | warmup | ...`
- duration không hợp lệ → `Duration must be an integer >= 0.`
- sai param set → `Unknown param 'speed'. Use spindle|feed|toolwear`
- `toolwear` ngoài khoảng → `toolwear must be between 0 and 100.`

---

## 7. Scenario hỗ trợ

| Scenario | Mục đích test |
|---|---|
| `normal` | chạy bình thường |
| `warmup` | khởi động máy |
| `idle` | máy rảnh, không chạy chu kỳ |
| `overheat` | quá nhiệt |
| `high-vibration` | rung cao |
| `tool-wear-rising` | dao mòn nhanh |
| `emergency-stop` | dừng khẩn |
| `disconnect` | ngừng gửi packet hoàn toàn |
| `slowsend` | vẫn tick bình thường nhưng gửi HTTP chậm hơn |
| `delayed` | snapshot bị giữ trong queue rồi gửi muộn, giữ nguyên `ts` |
| `duplicate` | gửi lại đúng payload cũ để test idempotency |

---

## 8. Dashboard console hiển thị gì

Mỗi vài giây simulator in dashboard kiểu:

```text
[MC-01] Conn=ONLINE | Sending=true | Scenario=NORMAL | State=RUNNING | Temp=48.2C | Power=4.8kW | Vib=1.90 | Output=551 | ToolLife=62.5% | Q=0/0 | LastSent=1s ago | LastOk=1s ago | LastErr=-
```

Ý nghĩa nhanh:
- `Conn`: trạng thái kết nối `ONLINE/DEGRADED/OFFLINE`
- `Sending`: có đang cho phép outbound hay không
- `Q=pending/delayed`: queue chờ gửi ngay / queue delay
- `LastSent`: lần thử gửi gần nhất
- `LastOk`: lần gửi thành công gần nhất
- `LastErr`: lỗi gửi gần nhất nếu có

---

## 9. Outbound payload chính

Simulator gửi telemetry theo contract mới, ví dụ:

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

### Những field không còn là payload chính

Các field sau không còn là field nghiệp vụ chính của ingest:
- `machineCode`
- `partCount`
- `cycleCount`
- `toolWearPercent`
- `energyKwhTotal`
- `maintenanceHealthScore`

Một phần vẫn được giữ nội bộ để mô phỏng logic hoặc debug.

---

## 10. Quy tắc timestamp

- `ts` = thời điểm snapshot được tạo ở simulator
- `delayed` = gửi muộn nhưng giữ nguyên `ts`
- `duplicate` = gửi lại đúng payload cũ, giữ nguyên `ts`
- `LastSent` chỉ là trạng thái nội bộ/console, không thay cho `ts`

---

## 11. Test nhanh

Sau khi chạy app, thử lần lượt:

```text
list
machine MC-01 scenario overheat 30
machine MC-03 scenario disconnect 20
machine MC-04 scenario delayed 20
machine MC-05 scenario duplicate 20
all scenario normal
```

---

## 12. Lưu ý phạm vi của simulator

Simulator này không phải nơi tính business KPI chính thức. Nó không phải source of truth cho:
- OEE
- downtime chính thức
- maintenance health score chính thức
- alarm / analytics / dashboard KPI chính thức

Nó chỉ là nguồn dữ liệu giả lập chuẩn để backend ingest và xử lý.
