# PLC / Simulator cần sửa gì để BE bớt `null`

## 1. Mục tiêu

Tài liệu này chốt các việc **PLC simulator phải sửa/bổ sung** để backend nhận đủ dữ liệu thô, từ đó:
- giảm field `null` ở telemetry/overview/history
- giúp chart realtime giàu dữ liệu hơn
- làm đầu vào tốt hơn cho analytics/prediction của backend

Quan trọng:
**Simulator không phải nơi tính KPI chính thức.** Nó chỉ là nơi phát dữ liệu nguồn chuẩn để backend ingest và tự tính tiếp.

---

## 2. Chốt hiện trạng simulator hiện tại

Simulator hiện đã gửi khá đủ nhóm dữ liệu lõi:
- `machineId`, `ts`
- `connectionStatus`, `machineState`, `operationMode`, `programName`, `cycleRunning`
- `powerKw`, `temperatureC`, `vibrationMmS`, `runtimeHours`, `cycleTimeSec`
- `outputCount`, `goodCount`, `rejectCount`
- `spindleSpeedRpm`, `feedRateMmMin`
- `toolCode`, `remainingToolLifePct`
- `voltageV`, `currentA`, `powerFactor`, `frequencyHz`, `energyKwhShift`, `energyKwhDay`
- `motorTemperatureC`, `bearingTemperatureC`, `cabinetTemperatureC`
- `servoOnHours`, `startStopCount`, `lubricationLevelPct`, `batteryLow`
- `metadata`

Đây là bộ rất tốt cho monitoring lõi.

Nhưng simulator hiện **chưa gửi** nhóm process nâng cao mà BE đã sẵn contract ingest.

---

## 3. Những field simulator cần bổ sung gửi thêm ngay

Đây là nhóm field BE đã có sẵn schema ingest, đã lưu được, đã expose được ở overview/history. Nếu simulator không gửi thì BE sẽ hợp lý khi trả `null`.

| Field | Ý nghĩa |
|---|---|
| `idealCycleTimeSec` | chu kỳ lý tưởng |
| `spindleLoadPct` | tải spindle |
| `servoLoadPct` | tải servo |
| `cuttingSpeedMMin` | tốc độ cắt |
| `depthOfCutMm` | chiều sâu cắt |
| `feedPerToothMm` | lượng tiến dao trên răng |
| `widthOfCutMm` | chiều rộng cắt |
| `materialRemovalRateCm3Min` | tốc độ bóc vật liệu |
| `weldingCurrentA` | dòng hàn nếu dùng máy hàn |

### Kết luận
Nếu bạn muốn UI bớt `null` ở nhóm process telemetry, simulator phải phát thêm các field này.

---

## 4. Cách bổ sung field cho simulator

## 4.1. Ưu tiên theo loại máy

Không phải máy nào cũng cần phát mọi field.

### CNC / machining center
Nên có:
- `idealCycleTimeSec`
- `spindleLoadPct`
- `servoLoadPct`
- `cuttingSpeedMMin`
- `depthOfCutMm`
- `feedPerToothMm`
- `widthOfCutMm`
- `materialRemovalRateCm3Min`

### Press / assembly / inspection
Có thể chỉ cần một phần, ví dụ:
- `idealCycleTimeSec`
- `servoLoadPct`
- hoặc bỏ trống hợp lý nếu không có ý nghĩa vật lý

### Welding machine
Nếu có mô phỏng máy hàn thì thêm:
- `weldingCurrentA`

### Nguyên tắc
- field không áp dụng cho loại máy nào thì có thể để `null`
- nhưng nếu đã áp dụng thì nên phát ổn định, không phát lúc có lúc không vô lý

---

## 4.2. Dữ liệu phải logic, không random vô nghĩa

Simulator cần phát số liệu có quan hệ logic, ví dụ:
- `spindleLoadPct` tăng thì `powerKw` cũng có xu hướng tăng
- `cuttingSpeedMMin` tăng thì nhiệt độ/rung có thể tăng nhẹ theo scenario
- `materialRemovalRateCm3Min` nên phụ thuộc vào `depthOfCutMm`, `widthOfCutMm`, `feedRateMmMin`
- `remainingToolLifePct` giảm dần theo runtime/cycle/load
- `cycleTimeSec` thực tế không nên thấp hơn `idealCycleTimeSec` quá vô lý trong trạng thái bình thường

### Tránh
- random từng field độc lập
- số liệu nhảy mạnh nhưng không có quan hệ vật lý
- field tăng/giảm mâu thuẫn nhau

---

## 4.3. Giữ cadence 1 giây ổn định khi test realtime

Hiện simulator đang có `tick-ms: 1000`.

### Cần giữ khi test realtime
- dùng scenario `normal`
- không bật `slowsend`, `delayed`, `duplicate`, `disconnect` khi đang test cảm giác realtime chuẩn

### Vì sao
Nếu đang test chart realtime mà dùng scenario làm chậm hoặc trễ packet, UI/BE sẽ trông như lag dù pipeline bình thường.

---

## 4.4. Giữ timestamp chuẩn

Simulator hiện đã có quy tắc đúng:
- `ts` là thời điểm snapshot tạo ra
- `delayed` gửi muộn nhưng giữ nguyên `ts`
- `duplicate` gửi lại đúng payload cũ với cùng `ts`

### Cần giữ nguyên
Không được thay `ts` bằng thời điểm gửi lại, vì backend cần phân biệt:
- packet đến muộn
- packet trùng
- dữ liệu out-of-order

---

## 4.5. Không đưa KPI business vào làm source of truth

Các field sau **không nên coi là KPI chính thức từ simulator**:
- OEE
- downtime chính thức
- maintenance health score chính thức
- alarm analytics chính thức
- dashboard KPI chính thức

### Ý nghĩa
Simulator chỉ nên gửi dữ liệu nguồn.
Phần tính KPI, score, prediction để backend làm.

---

## 5. Những gì simulator không cần sửa

Đây là điểm cần chốt rõ để tránh sửa sai chỗ.

## 5.1. Không cần để simulator tính OEE chính thức
Không nên đẩy OEE sang PLC simulator.

## 5.2. Không cần để simulator tính `anomalyScore`
Score bất thường là việc của backend.

## 5.3. Không cần để simulator tính `predictedFailureWindow`
Đây là prediction backend phải làm.

## 5.4. Không cần biến `machineCode`, `cycleCount`, `toolWearPercent`, `energyKwhTotal`, `maintenanceHealthScore` thành payload nghiệp vụ chính
Theo tài liệu hiện tại, các field này không còn là field ingest nghiệp vụ chính. Chỉ giữ cho debug hoặc nội bộ nếu cần.

---

## 6. Đề xuất mô hình phát dữ liệu mới cho simulator

## 6.1. Nhóm lõi luôn phải có
- state / mode / program / cycleRunning
- power / temperature / vibration / runtime / cycle time
- output / good / reject
- spindle / feed
- tool / energy / maintenance sensor cơ bản

## 6.2. Nhóm process nâng cao theo capability máy
- ideal cycle
- spindle load
- servo load
- cutting params
- MRR
- welding current

## 6.3. Nhóm metadata chỉ để debug
- scenario
- simulatorNode
- source
- machineCode
- cycleCount nếu muốn theo dõi nội bộ

---

## 7. Đề xuất logic mô phỏng cho các field mới

## 7.1. `idealCycleTimeSec`
- gần ổn định theo từng machine/program
- thay đổi ít
- có thể map theo từng loại job

## 7.2. `spindleLoadPct`
- tăng khi `powerKw` tăng
- tăng khi `depthOfCutMm` hoặc `feedRateMmMin` tăng
- dao mòn cao thì load có thể tăng dần

## 7.3. `servoLoadPct`
- tăng khi machine chạy nhanh hoặc đổi hướng nhiều
- press/assembly có thể load khác CNC

## 7.4. `cuttingSpeedMMin`
- nên đồng bộ với `spindleSpeedRpm` và loại tool/material mô phỏng

## 7.5. `depthOfCutMm`, `feedPerToothMm`, `widthOfCutMm`
- nên nằm trong dải hợp lý theo machine/profile
- không dao động loạn từng giây

## 7.6. `materialRemovalRateCm3Min`
- nên suy từ các field process thay vì random riêng lẻ

## 7.7. `weldingCurrentA`
- chỉ phát cho machine có ý nghĩa
- với máy khác thì để `null`

---

## 8. Thứ tự ưu tiên nên làm

## P1 — làm ngay
1. Bổ sung phát 9 field process nâng cao
2. Giữ dữ liệu giữa các field có quan hệ logic
3. Giữ cadence 1 giây ổn định cho test realtime chuẩn

## P2 — làm tiếp
4. Chuẩn hoá phát field theo từng loại máy
5. Chuẩn hoá range/đơn vị cho từng field
6. Chuẩn hoá metadata scenario/debug

## P3 — nâng cấp sau
7. Mô phỏng tool wear ảnh hưởng tới load/power/temp
8. Mô phỏng material/program ảnh hưởng tới cycle/process params
9. Mô phỏng profile khác nhau theo line/machine class

---

## 9. Checklist nghiệm thu PLC / simulator

- [ ] payload đã có `idealCycleTimeSec`
- [ ] payload đã có `spindleLoadPct`
- [ ] payload đã có `servoLoadPct`
- [ ] payload đã có `cuttingSpeedMMin`
- [ ] payload đã có `depthOfCutMm`
- [ ] payload đã có `feedPerToothMm`
- [ ] payload đã có `widthOfCutMm`
- [ ] payload đã có `materialRemovalRateCm3Min`
- [ ] payload đã có `weldingCurrentA` khi phù hợp loại máy
- [ ] các field mới không random vô nghĩa, có quan hệ logic với power/feed/spindle/temp/tool wear
- [ ] test realtime chuẩn dùng `tick-ms = 1000` và scenario `normal`
- [ ] `ts` vẫn được giữ chuẩn khi delayed/duplicate

---

## 10. Kết luận ngắn

Simulator hiện đã tốt cho monitoring lõi, nhưng còn thiếu nhóm process nâng cao mà BE đã sẵn sàng nhận và trả ra.

Chốt lại:
- **PLC/simulator phải gửi thêm field process nâng cao** nếu muốn BE bớt `null`
- **PLC/simulator không phải nơi tính KPI business**
- **PLC/simulator nên tập trung phát dữ liệu nguồn đúng, đều, logic, 1 giây/nhịp**
