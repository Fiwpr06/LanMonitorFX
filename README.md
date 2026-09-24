# 🛡️ LanMonitorFX — Hệ Thống Giám Sát Phòng Máy Mạng LAN (JavaFX 21 + AnyDesk Style)

Ứng dụng giám sát và quản trị phòng máy tính qua mạng LAN sử dụng giao thức **TCP/IP** theo mô hình **Client/Server**, xây dựng hoàn toàn bằng **Java 21** và **JavaFX 21**.

---

## 🌟 Tính Năng Nổi Bật

### 1. Phía Giáo Viên (Server Dashboard)
* **Giám sát trực quan (Monitoring Grid)**: Hiển thị lưới ảnh màn hình thu nhỏ (thumbnail) của toàn bộ học sinh kết nối, cập nhật định kỳ 2 giây/lần.
* **Phóng to màn hình (Zoom View)**: Nhấp đúp vào bất kỳ máy học sinh để mở cửa sổ phóng to riêng biệt theo thời gian thực.
* **Khóa / Mở khóa màn hình (Remote Lock/Unlock)**: Khóa toàn màn hình máy tính khi cần tập trung lớp học, hiển thị cảnh báo đỏ và vô hiệu hóa thoát ứng dụng.
* **Quản lý theo ngữ cảnh & Đa lựa chọn phong cách Google Drive**:
  * Tích hợp thanh công cụ ngữ cảnh nổi tự động xuất hiện khi chọn một hoặc nhiều máy tính (trên cả Bảng danh sách và Lưới thumbnail).
  * Hỗ trợ thao tác hàng loạt: Gửi thông báo nhóm, Khóa nhóm, Mở khóa nhóm, Tắt nguồn nhóm, Khởi động lại nhóm.
* **Quản lý nguồn hệ điều hành (Power Management)**:
  * Tắt nguồn từ xa (`shutdown /s /f /t 10`).
  * Khởi động lại máy (`shutdown /r /f /t 10`).
  * Đăng xuất phiên làm việc (`shutdown /l /f`).
  * Hỗ trợ thực hiện trên các máy tính được chọn hoặc menu "Quản lý cả lớp ▾" cho TOÀN BỘ học sinh.
* **Gửi thông báo (Live Notification)**:
  * Gửi thông báo tới cả lớp qua menu công cụ hoặc thanh hành động đa lựa chọn.
  * Gửi thông báo riêng cho từng máy tính qua menu ngữ cảnh (Right-click).
* **Quản trị IP & Nhật ký (Live Log Viewer)**:
  * Tự động dò IP LAN máy chủ (`172.26.x.x` hoặc `192.168.x.x`), sao chép 1-click vào Clipboard.
  * Tab Nhật Ký đồng bộ thời gian thực UTF-8, lưu trữ nhật ký theo ngày tại `logs/NhatKy_dd_MM_yyyy.txt`.

### 2. Phía Sinh Viên (Client — AnyDesk Style)
* **Giao diện Bàn làm việc (This Desk)**:
  * Hiển thị địa chỉ IP máy tính to rõ kèm nút sao chép nhanh và huy hiệu định danh.
  * Thẻ định danh sinh viên: Họ tên, MSSV, Tên thiết bị.
  * Bảng phân quyền phòng máy: Trạng thái chia sẻ màn hình, quyền khóa máy, quyền quản lý nguồn và nhận thông báo.
* **Kết nối Máy chủ (Remote Server Desk)**:
  * Form kết nối đơn giản, nhập IP Server, MSSV và Họ tên.
  * Tự động chuyển sang chế độ Phiên hoạt động khi kết nối thành công: Bộ đếm thời gian phiên (Session Uptime), tốc độ khung hình và nút ngắt kết nối.
* **Hộp thư Thông báo Giáo viên (Notification Feed)**:
  * Hiển thị danh sách tin nhắn nhận được trực tiếp từ giáo viên kèm mốc thời gian `[HH:mm:ss]`.
  * Cửa sổ thông báo nổi chuyên dụng đè lên mọi ứng dụng (`Always On Top`) kèm âm thanh cảnh báo, đảm bảo không bỏ lỡ nhắc nhở của giáo viên.

---

## 🏗️ Cấu Trúc Dự Án (Default Tree Chuẩn Học Thuật)

```text
LanMonitorFX/
├── pom.xml                             // Cấu hình Maven (Java 21, JavaFX 21.0.6, JUnit 5)
├── mvnw / mvnw.cmd / .mvn/             // Maven Wrapper chính quy
├── .gitignore                          // Bỏ qua target/, logs/, .idea/
├── README.md                           // Hướng dẫn cài đặt và sử dụng
├── run_server.bat / run_server.vbs     // Script khởi chạy Máy Giáo Viên (Console / Chạy ngầm)
├── run_client.bat / run_client.vbs     // Script khởi chạy Máy Sinh Viên (Console / Chạy ngầm)
├── build.bat                           // Script đóng gói Maven và copy dependencies
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── module-info.java        // Cấu hình Java Module System cho JavaFX
    │   │   └── com/fiwpr06/lanmonitorfx/
    │   │       ├── ServerMain.java     // Entry Point Máy Giáo Viên (Chạy trực tiếp hàm main)
    │   │       ├── ClientMain.java     // Entry Point Máy Sinh Viên (Chạy trực tiếp hàm main)
    │   │       │
    │   │       ├── server/             // Tầng Server TCP & Quản lý phiên
    │   │       │   ├── TcpServer.java
    │   │       │   ├── ClientHandler.java
    │   │       │   └── ClientRegistry.java
    │   │       │
    │   │       ├── client/             // Tầng Client TCP, Chụp màn hình & Thực thi lệnh
    │   │       │   ├── TcpClient.java
    │   │       │   ├── ScreenCaptureService.java
    │   │       │   └── CommandExecutor.java
    │   │       │
    │   │       ├── network/            // Giao thức TCP/IP chuẩn hóa
    │   │       │   ├── Protocol.java
    │   │       │   ├── Message.java
    │   │       │   └── SocketConnection.java
    │   │       │
    │   │       ├── model/              // Model dữ liệu
    │   │       │   ├── ClientSession.java
    │   │       │   └── NotificationItem.java
    │   │       │
    │   │       ├── ui/                 // Các Controller điều khiển giao diện JavaFX
    │   │       │   ├── ServerMainController.java
    │   │       │   ├── ClientCardController.java
    │   │       │   └── ClientMainController.java
    │   │       │
    │   │       └── util/               // Tiện ích dùng chung
    │   │           ├── FileLogger.java
    │   │           ├── ImageUtil.java
    │   │           └── AlertHelper.java
    │   │
    │   └── resources/com/fiwpr06/lanmonitorfx/
    │       ├── fxml/
    │       │   ├── server_main.fxml    // Dashboard giám sát giáo viên
    │       │   ├── client_card.fxml    // Thẻ thumbnail máy sinh viên
    │       │   └── client_main.fxml    // Giao diện sinh viên chuẩn AnyDesk Style
    │       └── css/
    │           └── theme.css           // Bộ stylesheet Dark Surveillance tối ưu mắt
    │
    └── test/                           // Kiểm thử tự động JUnit 5 (100% Pass)
        └── java/com/fiwpr06/lanmonitorfx/
            └── SystemIntegrationTest.java
```

---

## 🚀 Hướng Dẫn Chạy Ứng Dụng

### Cách 1: Chạy trực tiếp trong IntelliJ IDEA (1-Click Run)
1. Mở thư mục `LanMonitorFX` trong **IntelliJ IDEA** (chọn `Open as Maven Project`).
2. Mở file [`ServerMain.java`](file:///d:/Workspace/Practice/Java/LanMonitorFX/src/main/java/com/fiwpr06/lanmonitorfx/ServerMain.java) $\rightarrow$ bấm nút **Play (▶️)** xanh cạnh hàm `main(String[] args)` để chạy Máy Giáo Viên.
3. Mở file [`ClientMain.java`](file:///d:/Workspace/Practice/Java/LanMonitorFX/src/main/java/com/fiwpr06/lanmonitorfx/ClientMain.java) $\rightarrow$ bấm nút **Play (▶️)** xanh cạnh hàm `main(String[] args)` để chạy Máy Sinh Viên.
*(Nhờ file `module-info.java`, IntelliJ IDEA tự động nhận diện JavaFX module và nạp đầy đủ runtime mà không cần cấu hình thêm bất kỳ launcher hay tham số VM nào).*

### Cách 2: Chạy bằng Script Click đúp (Cho người dùng / Phòng máy)
* **Khởi động Máy Giáo viên**: Nhấp đúp vào `run_server.bat` (hoặc `run_server.vbs` để chạy ngầm).
* **Khởi động Máy Sinh viên**: Nhấp đúp vào `run_client.bat` (hoặc `run_client.vbs` để chạy ngầm).
* **Đóng gói dự án**: Nhấp đúp vào `build.bat` để biên dịch lại mã nguồn và sao chép các gói phụ thuộc JavaFX vào `target/dependency/`.
