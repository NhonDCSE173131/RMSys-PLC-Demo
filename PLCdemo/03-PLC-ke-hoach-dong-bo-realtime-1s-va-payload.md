# PLC / Simulator — Kế hoạch sửa đồng bộ để phục vụ realtime 1 giây đúng chuẩn

## 1. Vai trò thống nhất của PLC

PLC hoặc simulator **không phải nơi tính KPI business**.  
PLC chỉ là nơi phát **tín hiệu gốc** đều, đúng cấu trúc, đúng nhịp.

BE mới là nơi:
- tính OEE
- tính A/P/Q
- tính health
- tính maintenance risk
- tính predicted failure window
- chuẩn hóa display state
- phát live snapshot cho UI

Vì vậy, hướng thống nhất là:

- PLC phát đều mỗi 1 giây
- payload phải giàu đủ để BE bớt `null`
- PLC không phát “KPI business giả”
- PLC phải ổn định timestamp và state để BE tính đúng

---

## 2. Kết luận hiện trạng PLC/simulator

## 2.1 Những gì đang đúng

- simulator đang có `tick-ms: 1000`
- đã phát tốt bộ telemetry lõi:
  - machine state
  - mode/program
  - power
  - temperature
  - vibration
  - runtime
  - cycle time
  - output/good/reject
  - spindle/feed
  - energy cơ bản
  - tool life
  - maintenance cơ bản

## 2.2 Những gì vẫn thiếu hoặc chưa đủ

### A. Chưa phát đủ nhóm process nâng cao mà BE đã support ingest
Theo contract BE hiện tại, để nhiều field không còn `null`, simulator nên phát thêm:
- `idealCycleTimeSec`
- `spindleLoadPct`
- `servoLoadPct`
- `cuttingSpeedMMin`
- `depthOfCutMm`
- `feedPerToothMm`
- `widthOfCutMm`
- `materialRemovalRateCm3Min`
- `weldingCurrentA` (chỉ cho loại máy có ý nghĩa)

### B. State vận hành phải nhất quán
Simulator phải dùng bộ state ổn định, ví dụ:
- `RUNNING`
- `IDLE`
- `STOPPED`
- `WARMUP`
- `EMERGENCY_STOP`
- `MAINTENANCE`

Không phát state lẫn lộn hoặc thay đổi tên theo scenario.

### C. Timestamp phải tăng đều
Nếu timestamp không tăng đều hoặc có lúc dồn cụm, BE và UI sẽ khó vẽ chart kiểu stock chart.

### D. Counter phải có logic thật
Các field đếm như:
- `outputCount`
- `goodCount`
- `rejectCount`

phải tăng theo logic nhất quán:
- không nhảy âm
- không reset bất thường
- không để `good + reject > output`
- không tăng output mà cycle/state không hợp lý

### E. `idealCycleTimeSec` là dữ liệu rất quan trọng
Nếu simulator không phát field này, BE phải fallback, và OEE/performance sẽ kém chính xác hơn.

---

## 3. Hướng thống nhất duy nhất từ giờ

Simulator phải phát **payload canonical raw telemetry** đều mỗi 1 giây, và payload này phải được coi là đầu vào cho pipeline backend.

Không phát KPI như:
- OEE business cuối cùng
- maintenanceDueDays cuối cùng
- predictedFailureWindow cuối cùng
- machineHealth cuối cùng

Nếu cần demo, có thể phát raw inputs phong phú hơn để BE tính.

---

## 4. Payload raw telemetry simulator nên phát

Payload nên duy trì ít nhất các nhóm sau:

```json
{
  "machineId": "CNC-01",
  "machineCode": "CNC-01",
  "ts": "2026-03-30T03:10:24.830Z",

  "connectionStatus": "ONLINE",
  "machineState": "RUNNING",
  "operationMode": "AUTO",
  "programName": "JOB-001",
  "cycleRunning": true,

  "powerKw": 4.7,
  "temperatureC": 52.2,
  "vibrationMmS": 1.3,
  "runtimeHours": 123.4,
  "cycleTimeSec": 31.2,
  "idealCycleTimeSec": 28.5,

  "outputCount": 1500,
  "goodCount": 1488,
  "rejectCount": 12,

  "spindleSpeedRpm": 2400,
  "feedRateMmMin": 180,
  "spindleLoadPct": 55,
  "servoLoadPct": 48,

  "cuttingSpeedMMin": 120,
  "depthOfCutMm": 1.2,
  "feedPerToothMm": 0.08,
  "widthOfCutMm": 6.0,
  "materialRemovalRateCm3Min": 14.5,

  "toolCode": "T-01",
  "remainingToolLifePct": 72,

  "voltageV": 400.2,
  "currentA": 7.8,
  "powerFactor": 0.94,
  "frequencyHz": 50.1,
  "energyKwhShift": 12.8,
  "energyKwhDay": 38.5,

  "motorTemperatureC": 54.0,
  "bearingTemperatureC": 46.2,
  "cabinetTemperatureC": 39.0,
  "servoOnHours": 540.0,
  "startStopCount": 2140,
  "lubricationLevelPct": 78.0,
  "batteryLow": false
}
```

---

## 5. PLC / Simulator cần sửa cụ thể

## P0 — Bắt buộc sửa ngay

### 5.1 Giữ nhịp phát cố định 1 giây
File liên quan:
- `application.yaml`
- scheduler / generator loop của simulator

Yêu cầu:
- `tick-ms = 1000`
- không để loop thực tế bị drift quá nhiều
- nếu có retry gửi BE thì không làm block vòng phát chính quá lâu

### 5.2 Bổ sung nhóm process nâng cao
Các field cần thêm vào payload builder:
- `idealCycleTimeSec`
- `spindleLoadPct`
- `servoLoadPct`
- `cuttingSpeedMMin`
- `depthOfCutMm`
- `feedPerToothMm`
- `widthOfCutMm`
- `materialRemovalRateCm3Min`
- `weldingCurrentA` nếu áp dụng

Đây là nhóm field giúp:
- machine detail đỡ `--`
- process chart có dữ liệu thật
- BE tính performance và analytics tốt hơn

### 5.3 Timestamp phải là source timestamp thật của từng tick
Mỗi gói phải có:
- `ts` tăng đều
- không dùng timestamp tĩnh
- không dùng cùng một timestamp cho nhiều máy trong thời gian dài nếu simulator có thể tránh

### 5.4 Counter phải nhất quán
Quy tắc:
- `outputCount` chỉ tăng khi đang sản xuất
- `goodCount + rejectCount <= outputCount`
- không reset counter giữa chừng trừ khi có scenario reset rõ ràng
- `cycleTimeSec` phải phù hợp với trạng thái máy

### 5.5 State phải nhất quán với hành vi số liệu
Ví dụ:
- `RUNNING` → power > 0, cycleRunning thường true, counters có thể tăng
- `IDLE` → power vẫn có thể > 0 nhưng thấp hơn, counters không tăng
- `STOPPED` → counters không tăng, cycleRunning false
- `EMERGENCY_STOP` → counters đứng, state rõ ràng
- `MAINTENANCE` → không phát pattern giống đang chạy sản xuất

Nếu state và số liệu không khớp, BE và UI sẽ luôn “khó hiểu”.

---

## P1 — Nên làm tiếp

### 5.6 Tạo baseline theo từng loại máy
Mỗi loại máy nên có profile riêng:
- CNC lathe
- milling
- robot arm
- drill / grinding
- welding nếu có

Không nên dùng một bộ random giống nhau cho tất cả máy.

### 5.7 Scenario phải làm biến đổi raw signal, không làm KPI giả
Ví dụ:
- overheat → tăng `temperatureC`, có thể tăng `motorTemperatureC`
- high-vibration → tăng `vibrationMmS`
- tool-wear-rising → giảm `remainingToolLifePct`, có thể làm performance xấu dần
- slowsend/delayed → dùng để test network path, không dùng cho demo realtime bình thường

### 5.8 Tool identity phải ổn định
Simulator nên dùng `toolCode` nhất quán theo machine để:
- BE match usage đúng
- UI tools page không nhảy lung tung

---

## P2 — Tối ưu thêm

### 5.9 Có thể thêm machine profile / recipe
Mỗi máy nên có:
- ideal cycle time mặc định
- công suất nền
- giới hạn nhiệt độ/rung
- ngưỡng load

BE sẽ tính KPI sát thực hơn.

### 5.10 Có thể phát metadata hỗ trợ debug
Ví dụ:
- `simulatorNode`
- `scenario`
- `sequence`
- `sourceLatencyMs`

Không dùng cho UI chính, nhưng rất hữu ích khi kiểm tra realtime.

---

## 6. Những gì PLC không nên làm

- không tự tính OEE cuối cùng
- không tự tính maintenanceDueDays cuối cùng
- không tự tính predictedFailureWindow cuối cùng
- không tự quyết định displayState cuối cùng
- không phát số liệu “đẹp để demo” nhưng không logic với state

---

## 7. Checklist nghiệm thu PLC / Simulator

- [ ] payload được gửi đúng mỗi 1 giây
- [ ] `ts` tăng đều
- [ ] có `idealCycleTimeSec`
- [ ] có `spindleLoadPct`
- [ ] có `servoLoadPct`
- [ ] có `cuttingSpeedMMin`
- [ ] có `depthOfCutMm`
- [ ] có `feedPerToothMm`
- [ ] có `widthOfCutMm`
- [ ] có `materialRemovalRateCm3Min`
- [ ] state và telemetry khớp logic
- [ ] counters tăng hợp lý
- [ ] `good + reject <= output`
- [ ] scenario không phá nhịp realtime mặc định
- [ ] `toolCode` ổn định theo machine

---

## 8. Ưu tiên triển khai

1. giữ chắc tick 1 giây
2. thêm process fields nâng cao
3. chuẩn hóa state và counters
4. làm profile machine thực tế hơn

Nếu PLC không phát đủ raw signal, BE sẽ tiếp tục phải fallback và UI vẫn sẽ còn nhiều `--`.
