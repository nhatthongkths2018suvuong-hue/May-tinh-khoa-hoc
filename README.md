# Máy tính khoa học (bản nhẹ)

Máy tính khoa học viết lại từ đầu: **không quảng cáo, không thông báo, không animation, không quyền Internet**.
Không dùng thư viện ngoài (không AndroidX), nên APK rất nhỏ (thường dưới 100 KB).

## Tính năng
- `+ − × ÷ ^`, ngoặc (tự đóng ngoặc thiếu), `%`, `x!`, `x²`, `x³`, `√`, `∛`
- `sin cos tan ln log` và nghịch đảo (`INV`: sin⁻¹, cos⁻¹, tan⁻¹, eˣ, 10ˣ)
- Hằng số `π`, `e`; `Ans` (kết quả trước); chuyển `DEG`/`RAD`
- Nhân ngầm: `2π`, `3(4+1)`; kết quả xem trước ngay khi gõ

## Build APK bằng GitHub
1. Tạo repo mới, đẩy toàn bộ thư mục này lên nhánh `main`.
2. Vào tab **Actions** → workflow **Build APK** sẽ tự chạy (hoặc bấm *Run workflow*).
3. Chạy xong, tải **MayTinhKhoaHoc-apk** ở cuối trang run → giải nén lấy `app-release.apk`.

APK được ký bằng khóa debug nên cài thẳng được. Nếu muốn phát hành lên Google Play, hãy tạo keystore riêng.

## Build trên máy
Cần JDK 17 + Android SDK + Gradle 8.9: `gradle assembleRelease`

## Cấu trúc
- `Evaluator.java` – bộ phân tích/tính biểu thức
- `MainActivity.java` – giao diện & xử lý phím
