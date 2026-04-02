# PLC / Simulator – Phân tích tổng thể và kế hoạch sửa để đồng bộ với BE/UI, realtime 1 giây

## 1. Kết luận ngắn

Simulator hiện đã khá đúng hướng:
- tick mặc định 1000 ms
- gửi telemetry lõi tương đối đầy đủ
- có nhiều scenario test mạng / delay / duplicate / disconnect
- đã nhấn mạnh rõ: simulator **không phải nơi tính KPI business chính thức**

Nhưng để hệ thống chạy “mượt 1 giây” và bớt `null`, simulator vẫn còn 3 việc phải làm:

1. **Giữ đúng chế độ test realtime**
2. **Bổ sung thêm các field process nâng cao mà UI đang muốn hiển thị**
3. **Không gửi payload lệch contract hoặc field “di sản” gây hiểu nhầm**

---

## 2. Những gì simulator đang làm đúng

Theo README:
- backend đích mặc định: `http://localhost:8080/api/v1/ingest/telemetry`
- `tick-ms: 1000`
- có các scenario:
  - `normal`
  - `warmup`
  - `idle`
  - `overheat`
  - `high-vibration`
  - `tool-wear-rising`
  - `emergency-stop`
  - `disconnect`
  - `slowsend`
  - `delayed`
  - `duplicate`

Outbound payload chính hiện có:
- `machineId`
- `ts`
- `connectionStatus`
- `machineState`
- `operationMode`
- `programName`
- `cycleRunning`
- `powerKw`
- `temperatureC`
- `vibrationMmS`
- `runtimeHours`
- `cycleTimeSec`
- `outputCount`
- `goodCount`
- `rejectCount`
- `spindleSpeedRpm`
- `feedRateMmMin`
- `toolCode`
- `remainingToolLifePct`
- `voltageV`
- `currentA`
- `powerFactor`
- `frequencyHz`
- `energyKwhShift`
- `energyKwhDay`
- `motorTemperatureC`
- `bearingTemperatureC`
- `cabinetTemperatureC`
- `servoOnHours`
- `startStopCount`
- `lubricationLevelPct`
- `batteryLow`
- `metadata`

=> Bộ lõi này đủ để BE ingest telemetry/energy/maintenance/tool usage cơ bản.

---

## 3. Những gì simulator còn thiếu nếu muốn UI bớt rỗng

README outbound payload hiện **chưa có** nhóm process nâng cao mà UI đang muốn dùng và BE contract cũng đã chừa chỗ cho:

- `idealCycleTimeSec`
- `spindleLoadPct`
- `servoLoadPct`
- `cuttingSpeedMMin`
- `depthOfCutMm`
- `feedPerToothMm`
- `widthOfCutMm`
- `materialRemovalRateCm3Min`
- `weldingCurrentA`

Nếu bạn không gửi nhóm này, thì:
- chart process nâng cao sẽ trống
- các ô như tải spindle, tải servo, vận tốc cắt, chiều sâu cắt, MRR sẽ `--`
- UI nhìn như “thiếu dữ liệu” dù BE/UI đã support hiển thị

---

## 4. Những việc simulator không nên làm

README cũng nói rõ simulator **không phải source of truth** cho:
- OEE
- downtime chính thức
- maintenance health score chính thức
- dashboard KPI chính thức

=> Đừng cố tính sẵn OEE hoặc maintenance score ở PLC để “vá UI”.
Việc của PLC chỉ là:
- gửi tín hiệu gốc đúng nhịp
- giữ timestamp chuẩn
- mô phỏng trạng thái và tình huống lỗi đúng

Business KPI phải để BE tính.

---

## 5. File PLC / Simulator cần sửa

### 5.1 File sinh payload telemetry chính
(Tên file cụ thể tuỳ implementation trong repo, thường nằm ở service/generator/snapshot builder)

## Phải bổ sung field
```json
{
  "idealCycleTimeSec": 38.0,
  "spindleLoadPct": 62.0,
  "servoLoadPct": 48.0,
  "cuttingSpeedMMin": 180.0,
  "depthOfCutMm": 1.8,
  "feedPerToothMm": 0.12,
  "widthOfCutMm": 6.0,
  "materialRemovalRateCm3Min": 24.5,
  "weldingCurrentA": null
}
```

## Quy tắc
- CNC machine: nên có hầu hết process params
- Robot only: phần machining params có thể null
- Robot CNC cell: phần robot + machining cùng tồn tại
- Welding current chỉ có ở machine phù hợp, còn lại null

---

### 5.2 Logic scenario theo machine type
## Phải sửa
Scenario không nên chỉ đổi 2–3 field lõi.
Nó nên đổi **đúng các field liên quan**.

### Ví dụ:
#### `normal`
- RUNNING
- cycleRunning = true
- power/tốc độ/dao động ổn định
- counts tăng đều
- temperature tăng chậm, trong ngưỡng

#### `warmup`
- WARMUP
- spindle thấp hoặc 0
- power thấp đến vừa
- temperature tăng nhẹ
- outputCount không tăng

#### `idle`
- IDLE
- cycleRunning = false
- outputCount đứng
- spindle/feed gần 0
- power vẫn > 0 nhẹ

#### `overheat`
- temperature tăng
- motor/bearing/cabinet temperature tăng tương ứng
- health raw xấu dần

#### `high-vibration`
- vibration tăng
- spindleLoad có thể dao động mạnh
- quality có thể xấu dần nếu muốn mô phỏng

#### `tool-wear-rising`
- remainingToolLifePct giảm nhanh
- cycleTimeSec có thể tăng nhẹ
- quality có thể xấu dần
- spindleLoad có thể tăng nhẹ

#### `emergency-stop`
- machineState = EMERGENCY_STOP
- cycleRunning = false
- spindle/feed = 0
- power giảm mạnh nhưng không nhất thiết bằng 0 ngay

#### `disconnect`
- ngừng gửi packet thật sự

#### `slowsend`
- vẫn tạo snapshot mỗi 1 giây nhưng gửi chậm

#### `delayed`
- giữ queue rồi gửi muộn, giữ nguyên `ts`

#### `duplicate`
- gửi lại payload cũ nguyên timestamp

Nếu simulator chỉ đổi trạng thái mà không đổi các field liên quan, UI sẽ thấy số “không ăn ý”.

---

### 5.3 Timestamp
## Phải giữ nguyên
- `ts` phải là thời điểm snapshot được tạo ở simulator
- `delayed` gửi muộn nhưng giữ nguyên `ts`
- `duplicate` gửi lại nguyên `ts`

## Không được làm
- không được tự set `ts = now()` khi flush packet delayed
- không được làm `ts` nhảy lung tung lệch nhịp 1 giây

Vì chart kiểu stock cực nhạy với timestamp.

---

### 5.4 Metadata
## Nên bổ sung có kiểm soát
Hiện metadata đã có:
- `scenario`
- `simulatorNode`
- `sendDelayMs`
- `source`
- `machineCode`
- `cycleCount`

## Có thể thêm
- `machineCategory`
- `sampleIntervalMs`
- `qualityHint`
- `scenarioPhase`

Nhưng metadata chỉ để debug/phụ trợ, không thay cho field nghiệp vụ chính.

---

### 5.5 Tắt các scenario gây nhiễu khi đang test realtime
Khi test “mọi thứ phải nhảy từng giây”, chỉ dùng:
- `all scenario normal`
hoặc
- từng máy `scenario normal`

## Không dùng khi test độ mượt
- `disconnect`
- `slowsend`
- `delayed`
- `duplicate`

Vì các scenario đó sinh ra hiệu ứng đúng nghĩa “không realtime”.

---

## 6. Chuẩn dữ liệu simulator nên chốt theo machine type

### CNC machine
Bắt buộc nên có:
- power
- temp
- vibration
- cycleTime
- output/good/reject
- spindle speed
- feed rate
- ideal cycle
- spindle load
- servo load
- cutting speed
- depth of cut
- feed per tooth
- width of cut
- MRR
- tool life
- energy
- maintenance signals

### Robot only
Nên có:
- power
- temp
- vibration
- operation mode
- program
- servo load
- runtime
- energy
- maintenance signals

Không bắt buộc:
- spindle speed
- cutting speed
- depth of cut
- feed per tooth
- width of cut
- MRR

### Robot CNC cell
Nên có cả 2 lớp:
- robot zone signals
- machining zone signals

Đây là loại máy dễ làm UI “lệch logic” nhất nếu chỉ gửi nửa dữ liệu.

---

## 7. Những lỗi simulator có thể đang góp phần làm hệ thống khó sửa mãi

1. **Simulator docs và BE contract nhìn có vẻ khớp, nhưng thực tế BE ingest path chưa dùng contract đầy đủ**
   => không phải mọi field simulator gửi đều được BE lưu thật.

2. **Scenario thay state nhưng không kéo theo hệ field liên quan**
   => số liệu nhìn không logic.

3. **Thiếu process params nâng cao**
   => UI nhiều ô trống dù máy đang chạy.

4. **Khi test realtime lại bật scenario network-chaos**
   => cảm giác “BE/UI chậm” nhưng thật ra do payload cố tình bị chậm.

---

## 8. Thứ tự sửa PLC nên làm

### P1 – bắt buộc
- giữ tick 1000 ms
- test realtime bằng `normal`
- đảm bảo `ts` chuẩn
- đảm bảo state và field đi cùng nhau logic

### P2 – rất quan trọng
- bổ sung process params nâng cao
- chuẩn hoá dữ liệu theo machine type
- làm scenario đổi đúng toàn bộ nhóm tín hiệu liên quan

### P3 – nâng cao
- thêm quality hint / phase metadata
- thêm profile machine để ideal cycle, spindle range, load range không bị hard-code chung

---

## 9. Kết quả mong muốn sau khi sửa

- PLC phát dữ liệu đều 1 giây
- state và thông số không đá nhau
- BE có đủ tín hiệu gốc để tính KPI
- UI có đủ dữ liệu để chart process và chart raw chạy sống
- mọi màn không còn cảm giác “máy chạy mà số lại trống”
