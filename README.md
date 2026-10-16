# Check Sạc (v3)

App Android siêu nhẹ đo công suất sạc thực tế (W), hiện trên thanh trạng thái khi cắm sạc và màn hình bật.

## Build bằng GitHub Actions
1. Giải nén zip, đẩy TOÀN BỘ nội dung (kể cả thư mục ẩn `.github`) lên repo GitHub (nhánh `main`).
2. Vào tab **Actions** -> workflow **Build APK** (tự chạy khi push, hoặc bấm *Run workflow*).
3. Chạy xong, kéo xuống mục **Artifacts** -> tải `CheckSac-v3-apk` -> giải nén lấy file `app-debug.apk`.
4. Cài lên máy, mở app, bấm nút BẬT, cấp quyền thông báo.

## Cách hoạt động
- Công suất = |dòng sạc (CURRENT_NOW)| x điện áp pin.
- Cắm sạc + màn hình bật: đo mỗi 2 giây, hiện số "xxW" bằng icon thông báo trên thanh trạng thái.
- Rút sạc / tắt màn hình: dừng đo hoàn toàn, ẩn icon.

## Neu khong thay thu muc `.github` (bi an tren dien thoai / trinh quan ly file)
Tren GitHub (web): **Add file -> Create new file**, o ten file go dung:
`.github/workflows/build.yml`
roi dan noi dung file `WORKFLOW_COPY_vao_.github_workflows/build.yml` vao, bam Commit.
