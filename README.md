# English Quiz

Web app luyện tập tiếng Anh theo `Category -> Level -> Quiz` với giao diện Thymeleaf, có đăng nhập/đăng ký, phân quyền admin, quản lý CRUD ngân hàng câu hỏi và lưu tiến trình làm bài.

## Tính năng chính

- Người dùng:
  - Đăng ký / đăng nhập / đăng xuất
  - Tùy chọn `Remember login`
  - Làm quiz theo category và level
  - Nộp bài, xem kết quả chi tiết từng câu
  - Khóa kết quả sau submit (không sửa lại)
  - Nút `Reset & Retry This Quiz` để xóa kết quả bài đó và làm lại
- Lưu tiến trình:
  - Tiến trình theo từng `user + category + level` được lưu trong DB
  - Restart dự án vẫn giữ dữ liệu đã làm
- Admin:
  - Trang quản trị `/admin/quizzes`
  - CRUD cho Category / Level / Question / Answer
  - Bộ lọc và dropdown phụ thuộc (Category -> Level -> Question) để thao tác nhanh

## Công nghệ sử dụng

- Java 17
- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA
- Thymeleaf
- MySQL
- Lombok
- BCrypt (`spring-security-crypto`) để hash password

## Cấu trúc chính

```text
src/main/java/EnglishQuiz
├─ controller      # Web controllers (auth, quiz, admin, category...)
├─ service         # Business logic (quiz grading, progress persistence)
├─ model           # JPA entities
├─ repository      # Spring Data repositories
└─ config          # Interceptor, initializer...

src/main/resources
├─ templates       # Thymeleaf templates
├─ static/css      # CSS
└─ application.properties
```

## Yêu cầu môi trường

- JDK 17 (không phải chỉ JRE)
- MySQL 8+
- Maven Wrapper (`mvnw.cmd`) có sẵn trong project

## Cách chạy dự án

1. Clone project
2. Cấu hình DB trong `src/main/resources/application.properties`
3. Chạy:

```bash
./mvnw spring-boot:run
```

Trên Windows (PowerShell):

```powershell
.\mvnw.cmd spring-boot:run
```

Ứng dụng chạy mặc định tại: [http://localhost:8080](http://localhost:8080)

## Cấu hình database

File `application.properties` đang dùng:

- `spring.datasource.url=jdbc:mysql://127.0.0.1:3306/english_quiz?...`
- `spring.jpa.hibernate.ddl-auto=update`

> Khuyến nghị: không commit mật khẩu DB thật lên git. Nên chuyển sang biến môi trường hoặc profile riêng.

## Auth và phân quyền

- Bảng `roles` chứa role (ví dụ: `ADMIN`, `USER`)
- Bảng `user_account` tham chiếu role bằng `role_id`
- Chỉ user có role `ADMIN` mới truy cập `/admin/**`

## Remember Login

- Khi tick `Remember login`, hệ thống tạo token lưu ở bảng `remember_login_token` + cookie HTTP-only.
- Lần sau mở lại app, nếu token còn hạn thì tự khôi phục phiên đăng nhập.
- Logout sẽ xóa token và cookie.

## Lưu tiến trình quiz

- Bảng `user_quiz_progress` lưu:
  - `username`, `category_id`, `level_id`
  - danh sách câu hỏi và đáp án đã chọn (JSON)
  - `current_index`, `submitted`
- Mỗi bài quiz được định danh theo `user + category + level`.

## Các URL quan trọng

- Trang chính: `/`
- Login: `/login`
- Register: `/register`
- Quiz: `/quiz/{categoryId}/{levelId}`
- Result: `/quiz/result`
- Admin: `/admin/quizzes`

## Gợi ý cải tiến tiếp theo

- Thêm migration chuẩn (Flyway/Liquibase) thay vì phụ thuộc `ddl-auto=update`
- Thêm quản lý user/role ngay trong admin UI
- Thêm test integration cho auth, quiz progress, reset flow
- Tách secrets khỏi `application.properties` sang env vars

