# English Quiz

Web app luyện tập tiếng Anh theo luồng **Category → Level → Quiz** với giao diện Thymeleaf, đăng nhập/đăng ký, phân quyền Admin, quản lý ngân hàng câu hỏi (có hỗ trợ **audio listening**), lưu tiến trình làm bài và trang thống kê tiến độ cá nhân.

---

## Tính năng

### Người dùng
| Tính năng | Mô tả |
|-----------|-------|
| Đăng ký / Đăng nhập / Đăng xuất | Tài khoản lưu trong DB, mật khẩu hash BCrypt |
| Remember Login | Cookie HTTP-only, tự khôi phục phiên trong 30 ngày |
| Hồ sơ cá nhân (`/profile`) | Đổi tên hiển thị, email, mật khẩu |
| Làm quiz | Câu hỏi đơn / đa lựa chọn, có thể navigate qua lại |
| Hỗ trợ audio | Câu hỏi nghe (Listening) có audio player nhúng trong bài |
| Nộp bài & xem kết quả | Điểm, đánh dấu đúng/sai từng câu, giải thích |
| Reset bài | Xóa tiến trình, làm lại từ đầu |
| Tiến độ học (`/progress`) | Xem trạng thái từng level theo category (chưa học / đang làm / đã hoàn thành + điểm) |
| Chat với AI | Hỏi đáp về ngữ pháp, từ vựng, hướng dẫn làm bài (không cho đáp án trực tiếp) |
| Nâng cấp VIP | Thanh toán qua MoMo để truy cập nội dung premium (Listening) |

### Phân quyền và Tier
- **Normal (Miễn phí)**: Truy cập Grammar Basics, Vocabulary, Reading Comprehension
- **VIP (Trả phí - 199,000 VND)**: Truy cập tất cả categories bao gồm Listening, và các tính năng premium tương lai

### Admin
| Tính năng | Mô tả |
|-----------|-------|
| Trang quản trị `/admin/quizzes` | CRUD Category / Level / Question / Answer |
| Bộ lọc thông minh | Dropdown phụ thuộc Category → Level → Question |
| Thêm câu hỏi audio | Điền `media_url` = đường dẫn file `.mp3` (hoặc ảnh) |

---

## Công nghệ sử dụng

| Thành phần | Phiên bản / Chi tiết |
|------------|----------------------|
| Java | 17 |
| Spring Boot | 3.3.5 |
| Spring Data JPA + Hibernate | `ddl-auto=update` |
| Thymeleaf | Template engine |
| MySQL | 8+ |
| Lombok | Giảm boilerplate code |
| spring-security-crypto | Hash mật khẩu với BCrypt |
| Google Gemini API | Chat AI hỗ trợ học tiếng Anh |
| MoMo Payment API | Thanh toán nâng cấp VIP |
| gTTS (Python) | Tạo file mp3 mẫu cho category Listening |

---

## Cấu trúc project

```text
EnglishQuiz/
├─ src/main/java/EnglishQuiz/
│   ├─ controller/
│   │   ├─ AuthController.java        # Đăng nhập, đăng ký, đăng xuất
│   │   ├─ ProfileController.java     # Hồ sơ cá nhân
│   │   ├─ ProgressController.java    # Trang tiến độ học /progress
│   │   ├─ QuizController.java        # Làm bài, lưu đáp án, nộp bài
│   │   ├─ CategoryController.java    # Trang chủ + search
│   │   ├─ ChatController.java        # API chat với AI (Gemini)
│   │   ├─ UpgradeController.java     # Trang nâng cấp VIP, thanh toán MoMo
│   │   ├─ AdminQuizController.java   # CRUD admin
│   │   └─ GlobalModelAttributes.java # Inject loggedIn/currentUser vào mọi view
│   ├─ service/
│   │   ├─ QuizService.java           # Chấm điểm
│   │   ├─ QuizProgressService.java   # Lưu/nạp tiến trình làm bài
│   │   └─ MomoService.java           # Tích hợp thanh toán MoMo
│   ├─ model/                         # JPA entities
│   ├─ repository/                    # Spring Data repositories
│   ├─ dto/                           # QuizSession (trạng thái bài làm trong session)
│   └─ config/                        # Interceptor auth/admin, RoleInitializer
│
├─ src/main/resources/
│   ├─ templates/
│   │   ├─ _layout.html               # Navbar, footer, fragment dùng chung
│   │   ├─ categories.html            # Trang chủ
│   │   ├─ levels.html                # Chọn level
│   │   ├─ quiz.html                  # Làm bài (hỗ trợ audio player)
│   │   ├─ result.html                # Kết quả chi tiết
│   │   ├─ progress.html              # Tiến độ học của user
│   │   ├─ profile.html               # Hồ sơ cá nhân
│   │   ├─ upgrade.html               # Trang nâng cấp VIP
│   │   ├─ login.html                 # Đăng nhập
│   │   └─ register.html              # Đăng ký
│   ├─ static/
│   │   ├─ css/style.css              # Toàn bộ style
│   │   ├─ js/
│   │   │   ├─ password-toggle.js     # Toggle hiện/ẩn mật khẩu
│   │   │   └─ chat.js                # JavaScript cho chat AI
│   │   └─ audio/                     # File mp3 cho category Listening
│   └─ application.properties
│
├─ data.sql                           # Dữ liệu mẫu (grammar, vocab, reading)
├─ listening_data.sql                 # Dữ liệu mẫu category Listening (audio, VIP only)
└─ pom.xml
```

---

## Yêu cầu môi trường

- **JDK 17** (không phải JRE — cần compiler)
- **MySQL 8+**
- Maven Wrapper (`mvnw.cmd`) có sẵn trong project
- *(Tùy chọn)* Python 3 + `gtts` nếu muốn tự tạo thêm file mp3 mẫu

---

## Cài đặt và chạy

### Bước 1 — Cấu hình Database và API Keys

Mở `src/main/resources/application.properties`, điền thông tin kết nối MySQL và API keys:

```properties
# Database
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/english_quiz?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=<MẬT_KHẨU_CỦA_BẠN>

# Gemini AI Chat (từ Google AI Studio)
gemini.api.key=your_gemini_api_key_here

# MoMo Payment (test environment - đăng ký tại MoMo Developer)
momo.partner-code=your_momo_partner_code
momo.access-key=your_momo_access_key
momo.secret-key=your_momo_secret_key
momo.api-url=https://test-payment.momo.vn/v2/gateway/api/create
momo.redirect-url=http://localhost:8080/upgrade/success
momo.ipn-url=http://localhost:8080/upgrade/ipn
momo.request-type=captureWallet
```

> **Lưu ý bảo mật:** Không commit mật khẩu và API keys thật lên Git. Nên dùng biến môi trường hoặc Spring profile riêng cho production.

### Bước 2 — Khởi động ứng dụng

```powershell
# Windows (PowerShell)
.\mvnw.cmd spring-boot:run
```

```bash
# Linux / macOS
./mvnw spring-boot:run
```

Hibernate sẽ tự động tạo toàn bộ bảng lần đầu chạy (`ddl-auto=update`).

Truy cập: **[http://localhost:8080](http://localhost:8080)**

### Bước 3 — Nạp dữ liệu mẫu

Chạy 2 file SQL trong MySQL sau khi app đã khởi động:

```sql
-- Dữ liệu gốc: Grammar, Vocabulary, Reading
source data.sql;

-- Category Listening với 6 câu hỏi audio
source listening_data.sql;
```

Hoặc chạy từng file trong MySQL Workbench / DBeaver / CLI:

```bash
mysql -u root -p english_quiz < data.sql
mysql -u root -p english_quiz < listening_data.sql
```

---

## Phân quyền và tài khoản

### Tạo tài khoản

Truy cập `/register` để tạo tài khoản mới. Mọi tài khoản mới mặc định có role `USER` và tier `NORMAL` (miễn phí).

### Nâng cấp VIP

- Truy cập `/upgrade` để thanh toán 199,000 VND qua MoMo
- Sau khi thanh toán thành công, tier sẽ chuyển thành `VIP`
- VIP có quyền truy cập category "Listening" và các tính năng premium khác

### Cấp quyền Admin

Sau khi đã có tài khoản (ví dụ username `admin`), chạy SQL:

```sql
UPDATE user_account u
JOIN roles r ON r.name = 'ADMIN'
SET u.role_id = r.id
WHERE u.username = 'admin';
```

Admin có thể truy cập `/admin/quizzes` để quản lý toàn bộ ngân hàng câu hỏi.

---

## Các URL quan trọng

| URL | Mô tả | Yêu cầu |
|-----|-------|---------|
| `/` | Trang chủ — danh sách category | Công khai |
| `/register` | Đăng ký tài khoản | Công khai |
| `/login` | Đăng nhập | Công khai |
| `/logout` | Đăng xuất | — |
| `/profile` | Hồ sơ cá nhân (đổi tên, email, mật khẩu) | Đăng nhập |
| `/progress` | Trang tiến độ học cá nhân | Đăng nhập |
| `/upgrade` | Nâng cấp tài khoản VIP (thanh toán MoMo) | Đăng nhập |
| `/quiz/{categoryId}/{levelId}` | Làm bài quiz | Đăng nhập |
| `/quiz/result` | Xem kết quả bài vừa nộp | Đăng nhập |
| `/admin/quizzes` | Quản trị ngân hàng câu hỏi | Role ADMIN |
| `/api/chat` | API chat với AI (POST) | Đăng nhập |

---

## Tính năng chi tiết

### Remember Login
- Tick `Remember login` khi đăng nhập → hệ thống tạo token ngẫu nhiên lưu ở bảng `remember_login_token` + cookie HTTP-only.
- Cookie có thời hạn 30 ngày. Mỗi lần mở lại app, phiên tự khôi phục nếu token còn hạn.
- Logout xóa token trong DB và xóa cookie.

### Hồ sơ cá nhân (`/profile`)
- Thay đổi **tên hiển thị** (Full name) — tên này thay thế username trên navbar.
- Thay đổi **email** (không bắt buộc).
- Đổi **mật khẩu** bằng cách nhập mật khẩu hiện tại + mật khẩu mới. Bỏ trống cả 3 ô thì giữ nguyên mật khẩu.

### Trang tiến độ học (`/progress`)
- Hiển thị **3 thống kê tổng quan**: số level đã hoàn thành / đang làm / chưa bắt đầu.
- Mỗi **category card** có progress bar và danh sách level với:
  - ✓ (xanh): hoàn thành — hiển thị điểm và thanh điểm màu (xanh ≥80%, tím ≥50%, đỏ <50%)
  - ⋯ (vàng): đang làm dở — hiển thị số câu đã trả lời / tổng
  - ○ (xám): chưa bắt đầu
- Nút **Start / Continue / View result** dẫn thẳng vào bài hoặc trang kết quả.

### Category Listening (Audio Quiz)
- File mp3 được tạo bằng **Google Text-to-Speech** (`gTTS`) lưu tại `static/audio/`.
- Khi câu hỏi có `media_url` kết thúc bằng `.mp3` / `.ogg` / `.wav`, trang quiz tự động hiển thị **audio player** thay vì ảnh.
- Audio player cũng xuất hiện trong trang kết quả khi review đáp án.
- Level 1 (Beginner): tình huống hàng ngày — giờ tàu, chỉ đường, gọi đồ uống.
- Level 2 (Intermediate): thông báo, dự báo thời tiết, đặt bàn nhà hàng, thư viện.

Cấu trúc câu hỏi audio trong DB:
```sql
INSERT INTO question (id, level_id, title, explaination, type, media_url) VALUES
  (4001, 401, 'Listen to the audio. What time does the train leave?',
   'The speaker says "The train to London leaves at seven thirty..."',
   'S', '/audio/listen_401_q1.mp3');
```

Để thêm câu hỏi audio mới:
1. Đặt file mp3 vào `src/main/resources/static/audio/`
2. Thêm record vào bảng `question` với `media_url = '/audio/<tên-file>.mp3'`
3. Thêm đáp án vào bảng `answer`

---

## Tự tạo thêm file mp3 mẫu (tùy chọn)

```bash
pip install gtts
```

```python
from gtts import gTTS
tts = gTTS(text="What is the weather like today?", lang='en')
tts.save("src/main/resources/static/audio/my_question.mp3")
```

---

## Gợi ý cải tiến tiếp theo

- [ ] Thêm migration chuẩn (**Flyway** hoặc **Liquibase**) thay vì phụ thuộc `ddl-auto=update`
- [ ] Tách secrets khỏi `application.properties` sang biến môi trường hoặc Spring profile
- [ ] Trang quản lý user/role trong admin UI
- [ ] Hiển thị leaderboard / so sánh điểm giữa các user
- [ ] Giới hạn thời gian làm bài (countdown timer)
- [ ] Thêm loại câu hỏi điền vào chỗ trống (type `F`)
- [ ] Test integration cho auth, quiz progress, reset flow
