# Norr Music

Ứng dụng nghe nhạc **local cho Android** — đọc nhạc trong bộ nhớ máy và thẻ SD.
Giao diện tối (OLED), không quảng cáo, không tài khoản, không cần mạng.

Android 8.0 (API 26) trở lên · Kotlin + Jetpack Compose + Media3

## Tính năng

- **Thư viện**: duyệt theo Bài hát / Album / Nghệ sĩ / Thư mục, tìm kiếm **không dấu tiếng Việt**
  ("nhac" ra "nhạc"), chọn nhiều bằng nhấn giữ
- **Phát nhạc**: lời bài hát `.lrc` khớp từng dòng, equalizer, hẹn giờ tắt, tốc độ phát,
  widget màn hình chính, Android Auto
- **Dọn thư viện**: tra thông tin qua Deezer, ghi lại tên bài / nghệ sĩ / album và nhúng ảnh
  bìa 1000×1000. Hỗ trợ MP3, FLAC, M4A, OGG
- **Tải nhạc hợp pháp**: tìm và tải từ Internet Archive và Jamendo (nguồn cho phép tải),
  chọn được thư mục lưu trên thẻ SD
- **Quét playlist YouTube**: đọc tên bài trong playlist rồi đối chiếu với nhạc đã có trong máy
  — chỉ đọc tên, **không tải âm thanh từ YouTube**

## Tự build

Cần **JDK 17** và **Android SDK** (compileSdk 35).

```bash
git clone <repo-url>
cd Norrmusic
./gradlew assembleDebug        # APK ở app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # 43 unit test
```

Android Studio: mở thư mục dự án, để Gradle tự đồng bộ. `local.properties` (đường dẫn SDK)
do máy tự sinh, không nằm trong repo.

## Giấy phép

GPL-3.0 — xem [LICENSE](LICENSE). Nếu muốn dùng giấy phép khác, thay tệp `LICENSE` là xong.

---

## Lịch sử phiên bản

## Có gì mới ở bản 3.9

**Sửa tag cho FLAC, M4A, OGG** — thêm thư viện `jaudiotagger`. Trước đây mp3agic chỉ làm
được MP3 nên 39 file M4A trong máy chỉ biết đổi tên; giờ ghi được cả tag lẫn ảnh bìa.

Cách chọn đường ghi dựa trên **nội dung thật** của file chứ không theo đuôi:

| Loại | Cách xử lý |
|---|---|
| MP3 thật | mp3agic (đường cũ, đã ổn định) |
| M4A / MP4 | đổi tên cho đúng đuôi `.m4a` rồi ghi tag bằng jaudiotagger |
| FLAC / OGG / OPUS | jaudiotagger |
| Còn lại | chỉ đổi tên file |

**Sửa lỗi hiển thị quan trọng** — `displayTitle` trước đây luôn lấy **tên file**, nên dù
tag đã được sửa thì danh sách vẫn hiện chuỗi rác trong tên file. Nay ưu tiên tiêu đề
trong tag, chỉ dùng tên file khi file không có tag. Đây là lý do tính năng dọn thư viện
ở bản 3.2–3.8 chạy mà người dùng không thấy gì thay đổi.

**Hiệu năng quét** — thêm `SIZE` và `DATE_MODIFIED` vào projection của MediaStore, nên
không phải gọi `file.length()` và `file.lastModified()` cho từng file nữa (mỗi lần là
một syscall chậm trên thẻ SD). Chỉ đọc thẳng từ file khi MediaStore chưa biết file đó.

**Test**: thêm 8 test cho phần gom nhóm Album/Nghệ sĩ/Thư mục và cho `displayTitle`.
Tổng **43 test**. Một test đã bắt được lỗi thật: tách tên thư mục bằng `/` hỏng trên
Windows, đã chuyển sang `parentFile.name`.

---

## Có gì mới ở bản 3.8

**Tuỳ chọn phát** — trong màn hình phát nhạc, menu ⋮ → *Tuỳ chọn phát*:

| Tính năng | Cách làm |
|---|---|
| **Tốc độ phát** | 0,75× · 1× · 1,25× · 1,5× · 2× (qua `PlaybackParameters` của Media3) |
| **Hẹn giờ tắt** | 10 / 20 / 30 / 60 phút, đếm ngược hiện số phút còn lại, hết giờ tự dừng |
| **Equalizer** | Bật/tắt + chọn preset có sẵn của thiết bị (`android.media.audiofx.Equalizer`) |

Equalizer gắn vào `audioSessionId` của ExoPlayer trong `PlaybackService`. Phải chờ
`onAudioSessionIdChanged` rồi mới gắn — lúc mới tạo trình phát, session id còn bằng 0.

**Hiệu năng:**
- Sắp xếp dùng `Collator(Locale("vi"))` thay cho `lowercase()` trong comparator — không
  tạo chuỗi mới ở mỗi phép so sánh, và đúng thứ tự tiếng Việt (a, ă, â, b…)
- **Bỏ giới hạn cứng 5.000 bài** — trước đây duyệt cắt ngang khi đủ 5.000 nên bài nào
  được giữ phụ thuộc thứ tự thư mục, người dùng mất bài mà không biết

**Quyền riêng tư:** tệp `norr-crash.txt` chuyển từ thư mục `Download` dùng chung sang bộ
nhớ riêng của app — stack trace có thể chứa đường dẫn và tên bài. Người dùng vẫn xem được
qua màn hình lỗi và nút Sao chép.

**Android Auto:** thêm `automotive_app_desc.xml` và khai báo trong manifest.

---

## Có gì mới ở bản 3.7

**Duyệt theo Album / Nghệ sĩ / Thư mục** — bốn chip ngay dưới ô tìm kiếm:
*Bài hát · Album · Nghệ sĩ · Thư mục*. Chọn Album thì thấy danh sách album kèm ảnh bìa và
số bài; chạm vào một album để mở danh sách bài bên trong. Nghệ sĩ và Thư mục cũng vậy.

Đây là tính năng cơ bản mà Musicolet, Poweramp, Retro Music đều có — thiếu nó thì người
có vài nghìn bài rất khó tìm.

Chi tiết triển khai:
- Nhóm tính từ danh sách **đã lọc theo ô tìm kiếm**, nên gõ "nhac" thì cả ba chế độ duyệt
  cùng thu hẹp theo
- Album lấy ảnh bìa từ bài đầu tiên có artwork; nghệ sĩ hiển thị lấy tên xuất hiện nhiều
  nhất trong album đó
- Bài không có tag được gom vào nhóm "Không rõ album" / "Không rõ nghệ sĩ" thay vì bị ẩn
- Không đổi cấu trúc DB: nhóm tính một lần bằng `remember`, với 130 bài là tức thời

**Sửa lỗi giao diện**: hàng 4 chip sắp xếp bị tràn khung trên màn hình 384dp (Samsung A035F)
— đã chuyển sang `FlowRow` để tự xuống dòng.

---

### Các bản 3.1 – 3.6 (tóm tắt)

| Bản | Thay đổi |
|---|---|
| 3.6 | Sửa tay tên bài / nghệ sĩ / album; tra cứu chuyển sang Deezer (iTunes làm dự phòng); nhận diện file M4A đặt sai đuôi `.mp3`; sửa "Phiên bản 2.2" hiển thị cứng |
| 3.5 | Cho phép kích hoạt dọn thư viện từ ngoài (adb) để kiểm thử |
| 3.4 | File M4A không ghi được tag thì đổi tên thành `Nghệ sĩ - Tên bài.m4a` |
| 3.3 | Thêm ghi log lỗi ghi tag; bỏ qua file đã sạch khi chạy lại |
| 3.2 | Tính năng dọn thư viện: sửa tên, nghệ sĩ, thêm ảnh bìa |
| 3.1 | Sửa hàng chip sắp xếp bị tràn khung |

---

## Có gì mới ở bản 3.0

**Quét playlist YouTube** — tab *Tải nhạc* → *Quét playlist YouTube*: dán link, app đọc  
toàn bộ tên bài trong playlist rồi **đối chiếu với nhạc đã có trong máy**, báo  
"đã có X bài · còn thiếu Y bài". Chạm vào một bài để mở trong YouTube.

Hai đường đọc, đều là kênh chính thức:

| Cách                    | Cần gì           | Đọc được                             |
| ----------------------- | ---------------- | ------------------------------------ |
| **RSS công khai**       | Không cần gì     | 15 bài gần nhất                      |
| **YouTube Data API v3** | API key miễn phí | Toàn bộ playlist (phân trang 50/lần) |

> **Đây không phải tính năng tải nhạc.** App chỉ đọc **tên bài**; không có phần nào  
> lấy luồng âm thanh từ YouTube. Mục đích là để bạn biết playlist còn thiếu bài nào  
> so với thư viện trong máy.

Đối chiếu tên bài có bỏ dấu và bỏ phần trong ngoặc (`(Official Video)`, `[Lyrics]`…),  
nên "Em Của Ngày Hôm Qua (Official Video)" vẫn khớp với file trong máy.

---

## Có gì mới ở bản 2.9

**Sửa chất lượng tìm kiếm** — trước đây tìm "shape of you" ra buổi diễn của Jerry Garcia,  
tìm "nhac viet" chỉ được 1 kết quả. Nguyên nhân và cách sửa, đều đã đo bằng dữ liệu thật:

| Vấn đề                                                                                  | Cách sửa                             | Kết quả đo                             |
| --------------------------------------------------------------------------------------- | ------------------------------------ | -------------------------------------- |
| Tìm khắp mọi trường nên khớp cả phần mô tả                                              | Tìm trong `title:`                   | "shape of you" → đúng 52 kết quả       |
| Không sắp xếp nên bài ít người biết lên đầu                                             | `sort=downloads desc`                | bài phổ biến lên trước                 |
| Lọc `collection:(netlabels OR audio_music)` — hai bộ sưu tập gần như không có nhạc Việt | Bỏ lọc, chỉ loại trừ audiobook/radio | "nhac viet" → 1.566 kết quả thay vì 1  |
| Từ khoá rời khớp lỏng lẻo                                                               | Cụm từ chính xác `title:("…")`       | "trinh cong son" → đúng Trịnh Công Sơn |
| Lọt sách nói, truyện đọc                                                                | Danh sách loại trừ theo tên          | bớt "sachnoi", "truyện", "pháp âm"     |

Archive tự bỏ dấu khi đánh chỉ mục, nên **"tinh yeu" và "tình yêu" cho cùng kết quả**.

Jamendo cũng thêm `order=popularity_total` để bài được nghe nhiều lên trước.

---

## Có gì mới ở bản 2.8

**Sửa lỗi app thoát ngay khi mở** (xuất hiện từ 2.6). Nguyên nhân lấy từ báo cáo lỗi của  
bản 2.7:

```
NullPointerException: MutableStateFlow.setValue(...) on a null object reference
    at AppViewModel.refreshDownloadFolder(AppViewModel.kt:484)
    at AppViewModel.<init>(AppViewModel.kt:119)
```

Lỗi **thứ tự khởi tạo thuộc tính** của Kotlin: `init { }` gọi `refreshDownloadFolder()`  
trong khi `_downloadFolder` được khai báo ở phía dưới lớp, nên lúc đó còn `null`.  
Đã chuyển khai báo lên trước `init` và khởi tạo bằng giá trị rỗng an toàn.

---

## Có gì mới ở bản 2.7

**Bộ ghi lỗi** — nếu app thoát đột ngột, lần mở kế tiếp sẽ hiện nguyên văn lỗi lên màn hình:  
tên lỗi in to ở đầu, toàn bộ stack trace ở dưới, có nút *Sao chép*. Đồng thời ghi ra  
`Download/norr-crash.txt`.

Mục đích: khi app chết trong một giây thì không có cách nào lấy log — không adb, không  
màn hình lỗi. Bộ ghi này biến "app tự nhiên tắt" thành một thông báo đọc được, chụp màn  
hình gửi đi là đủ để sửa.

Kỹ thuật: cài `Thread.setDefaultUncaughtExceptionHandler` trong `Application` (chạy trước  
mọi thứ khác), ghi bằng `commit()` đồng bộ vì lúc sắp chết `apply()` có thể chưa kịp ghi.

---

## Có gì mới ở bản 2.6

**Chọn thư mục lưu nhạc tải về** — Cài đặt → *Thư mục tải nhạc* → *Chọn thư mục*.  
Nhạc tải về sau đó tự vào đúng chỗ bạn chọn và hiện ngay trong thư viện.  
Đường dẫn hiện tại hiển thị rõ ngay trong thẻ cài đặt; có nút *Về mặc định* để quay lại  
`Music/Norr Music`.

Chọn được thư mục trên bộ nhớ máy hoặc thẻ SD. Thư mục trên dịch vụ đám mây  
(Google Drive…) sẽ bị từ chối kèm thông báo — vì thư viện của app quét theo đường dẫn  
tệp, nên chỉ nhận thư mục có đường dẫn thật.

---

## Có gì mới ở bản 2.5

**Tải nhạc từ nguồn mở** — tab mới *Tải nhạc*: chọn nguồn, gõ tên bài hoặc nghệ sĩ,  
tick chọn nhiều kết quả rồi tải một lượt. Tải song song 3 bài, có thanh tiến trình,  
xong tự quét vào thư viện và nghe offline ngay.

Hai nguồn, đều **cho phép tải**:

| Nguồn                | Cần gì               | Nội dung                                      |
| -------------------- | -------------------- | --------------------------------------------- |
| **Internet Archive** | Không cần gì         | Nhạc public domain, netlabel CC, bản thu live |
| **Jamendo**          | `client_id` miễn phí | Nhạc Creative Commons, chất lượng cao         |

File lưu vào `Music/Norr Music/`, đặt tên `Nghệ sĩ - Tên bài.mp3`, tự đánh số nếu trùng.

> **Lưu ý về quyền:** bản này **có thêm quyền Internet** (trước đây không có), chỉ dùng  
> cho việc tìm và tải nhạc. Toàn bộ phần nghe nhạc vẫn chạy offline bình thường.  
> App **không** tải nội dung từ YouTube — xem mục *Giới hạn* bên dưới.

---

## Có gì mới ở bản 2.4

**Dùng ảnh của bạn làm biểu tượng** — Cài đặt → *Biểu tượng ứng dụng* → *Chọn ảnh*.  
Ảnh được cắt vuông ở giữa, bo tròn, thu về 512 px rồi lưu vào bộ nhớ riêng của app.  
Sau đó app xin hệ thống ghim một lối tắt mang ảnh đó ra màn hình chính; bấm vào lối tắt  
là mở Norr Music.

> **Vì sao là lối tắt chứ không phải icon gốc?** Android bắt buộc icon app phải là tài  
> nguyên biên dịch sẵn trong APK — không có API nào cho phép đặt bitmap tuỳ ý làm icon  
> của gói. Đây là giới hạn của hệ điều hành. Lối tắt ghim ra màn hình chính là cách duy  
> nhất hợp lệ để có một biểu tượng mang ảnh người dùng, và nó hoạt động như icon thật.  
> Icon mặc định vẫn còn trong ngăn kéo ứng dụng.

Đổi ảnh lần nữa thì lối tắt đã ghim cũng được cập nhật (nhờ `pushDynamicShortcut`).  
Có thể xoá ảnh bất cứ lúc nào; máy nào launcher không hỗ trợ ghim thì app báo rõ.

---

## Có gì mới ở bản 2.3

**Đổi biểu tượng ứng dụng (avatar)** — Cài đặt → *Biểu tượng ứng dụng*, chọn một trong  
năm kiểu: Tím, Ngọc, San hô, Hổ phách, Đen trắng. Lựa chọn được lưu vào trạng thái  
component của hệ thống nên **giữ nguyên qua khởi động lại** cho tới khi bạn đổi.

Cách hoạt động: manifest khai báo năm `activity-alias` cùng trỏ về `MainActivity`,  
mỗi alias mang một icon riêng. Đổi icon = bật alias mới rồi mới tắt alias cũ  
(đúng thứ tự để app không biến mất khỏi màn hình chính trong tích tắc).

> **Vì sao không dùng được ảnh bất kỳ?** Android không cho đặt một ảnh tuỳ ý làm icon  
> của app — icon phải là tài nguyên khai báo trong manifest. Đây là giới hạn của hệ  
> điều hành, không phải của ứng dụng. Muốn avatar là ảnh riêng thì phải dùng launcher  
> bên thứ ba có hỗ trợ, hoặc tạo shortcut ghim ra màn hình chính.

---

## Có gì mới ở bản 2.2

**Chọn nhiều bằng nhấn giữ** — giữ một bài để vào chế độ chọn, sau đó chạm để thêm/bớt.  
Thanh dưới đổi thành ba hành động: *Thêm vào DS*, *Yêu thích*, *Phát*. Tiêu đề có  
*Chọn tất cả* và nút đóng. Mỗi dòng hiện ô vuông đánh dấu, bài đã chọn được tô nền tím nhạt.

**Giao diện đen tuyền** — đổi nền sang `#000000` thật thay vì xám đậm. Trên màn hình OLED  
điểm ảnh đen tắt hẳn nên tiết kiệm pin — có ý nghĩa thật với máy yếu. Thẻ dùng viền 0.5 dp  
thay vì đổ bóng, chữ và khoảng cách được siết lại.

**Hình nền dựng sẵn** — thêm bốn kiểu nền tối vẽ bằng gradient, không tốn bộ nhớ:  
*Đen tuyền*, *Than chì*, *Xanh đêm*, *Tím mờ*. Chọn bằng ô màu trong Cài đặt.  
Kiểu *Ảnh của bạn* vẫn giữ nguyên như cũ.

---

## Có gì mới ở bản 2.1

**Tìm kiếm không dấu** — gõ `nhac` ra `nhạc`, `tinh yeu` ra `tình yêu`, `duong` ra `Đường`.  
Dùng chuẩn hoá NFD + bỏ dấu kết hợp, xử lý riêng `đ`. Không đổi cấu trúc DB.

**Lời bài hát (.lrc)** — đặt file `.lrc` cùng tên cạnh file nhạc, app tự nhận.  
Nút lời bài hát trên màn hình phát để bật/tắt; dòng đang hát được tô màu nhấn và tự cuộn.  
Hỗ trợ một dòng nhiều mốc thời gian (`[00:10][00:40]điệp khúc`).

**Hàng đợi xem và sửa được** — nút hàng đợi trên màn hình phát mở danh sách bài sắp tới:  
nhảy tới bài bất kỳ, đổi thứ tự bằng mũi tên, bỏ khỏi hàng đợi.

**Sao lưu / khôi phục danh sách phát** — Cài đặt → xuất JSON hoặc nhập lại.  
Chỉ lưu tên danh sách và đường dẫn, không nhúng nội dung. Khi khôi phục, bài nào  
không còn trên máy sẽ được bỏ qua và báo lại số lượng. Nếu đã có danh sách trùng tên  
thì thêm bài vào danh sách đó thay vì tạo bản trùng.

**Widget màn hình chính** — ảnh bìa, tên bài, nghệ sĩ, ba nút trước/phát/tới.  
Chạm vào thân widget để mở app. Dùng RemoteViews thuần, không thêm thư viện.

**Kiểm thử** — thêm 14 unit test cho phần bỏ dấu và phân tích LRC (tổng 27 test, đều pass).

---

## Có gì mới ở bản 2.0

### 1. Mở app là thấy nhạc ngay — không quét lại

Bản 1.0 quét lại toàn bộ bộ nhớ mỗi lần mở, đọc metadata từng file bằng  
`MediaMetadataRetriever` (10–50 ms mỗi file). Với 1.000 bài là gần một phút.

Bản 2.0 lưu thư viện vào **SQLite** và quét theo ba lớp:

| Lớp                        | Khi nào dùng                          | Chi phí              |
| -------------------------- | ------------------------------------- | -------------------- |
| **DB**                     | File không đổi (cùng size + mtime)    | ~0 ms, không mở file |
| **MediaStore**             | File mới, hệ thống đã đọc sẵn thẻ ID3 | ~1 ms                |
| **MediaMetadataRetriever** | File mới mà MediaStore không có       | 10–50 ms             |

Lần mở thứ hai trở đi: hiển thị từ DB trong ~100 ms, sau đó cập nhật nền.  
Chỉ file mới hoặc vừa sửa mới bị đọc lại.

### 2. Danh sách phát

- Tạo, đổi tên, xoá danh sách
- Thêm bài từ thư viện (menu ⋮ → *Thêm vào danh sách*), bỏ bài bằng nút thùng rác
- Hai danh sách tự động: **Yêu thích** và **Nghe gần đây** (100 bài gần nhất)
- Tab **Danh sách** riêng, có nút phát tất cả

### 3. Hình nền tuỳ chọn

Vào **Cài đặt → Hình nền → Chọn ảnh**. Ảnh được **sao chép** vào bộ nhớ riêng  
của app (`filesDir/wallpaper.img`) nên vẫn còn sau khi khởi động lại, kể cả khi  
xoá ảnh gốc hoặc rút thẻ SD. Có lớp phủ tối để chữ luôn đọc được.

### 4. Tối ưu cho máy yếu

| Vấn đề bản 1.0                                                   | Cách sửa ở bản 2.0                                        |
| ---------------------------------------------------------------- | --------------------------------------------------------- |
| Cache ảnh bìa tính theo **số lượng** (48 ảnh × 1024 px ≈ 190 MB) | Tính theo **byte**, tối đa 1/8 heap và chặn trên 32 MB    |
| Giải mã ảnh bìa 1024 px cho cả hàng danh sách                    | Danh sách dùng 256 px, chỉ màn hình phát mới dùng 1024 px |
| Bài **không có** ảnh bìa bị đọc lại file mỗi lần cuộn            | Có bộ nhớ đệm riêng cho trường hợp "không có bìa"         |
| Cập nhật vị trí mỗi 500 ms **kể cả khi đã đóng trình phát**      | Chỉ cập nhật khi màn hình phát đang mở, 250 ms            |
| Lọc + sắp xếp lại trong composable mỗi lần vẽ                    | Tính sẵn một lần trong ViewModel                          |
| Danh sách bài không có `contentType`                             | Đã thêm, `LazyColumn` tái sử dụng view tốt hơn            |
| `NOT IN (?,?,…)` với 5.000 tham số — vượt giới hạn SQLite        | Dùng truy vấn con, không giới hạn số bài                  |


Thanh phát nhỏ **cố ý không** vẽ thanh tiến trình động: cập nhật mỗi giây sẽ
kéo theo vẽ lại toàn bộ cây giao diện. Vị trí chính xác chỉ hiện trong màn hình phát.

### 5. Phát nhạc nền

- Ảnh bìa hiện trên thanh thông báo — đọc trực tiếp từ file nhạc qua
  `AudioFileBitmapLoader` (không phải trích xuất trước cho cả thư viện)
- Giữ service sống khi vuốt app khỏi đa nhiệm nếu đang phát
- `WAKE_MODE_LOCAL`, xử lý audio focus, tự dừng khi rút tai nghe
- Khôi phục bài và vị trí đang nghe dở lần trước (không tự phát)

### 6. Giao diện

Ba tab (Thư viện · Danh sách · Cài đặt) với chuyển cảnh mượt, thanh phát nhỏ
gọn trên thanh điều hướng, màu nhấn lấy từ ảnh bìa (Palette API).
Không dùng blur hay đổ bóng nặng — máy yếu vẫn mượt.

---

## Quyền cần cấp

Android 11+ cần **Truy cập tất cả file** để đọc/ghi trực tiếp file nhạc
(phục vụ đổi tên và nhúng ảnh bìa). App mở đúng màn hình cấp quyền cho bạn.
Android 13+ cũng cần quyền **Thông báo** để hiện điều khiển nhạc.

---

## Build lại

```bash
./gradlew assembleDebug
```

Cần JDK 17 và Android SDK (compileSdk 35). APK ở
`app/build/outputs/apk/debug/app-debug.apk`.

Trên máy này (không có gradle wrapper) dùng:

```bash
export JAVA_HOME="$PWD/.toolchain/jdk-17.0.20.1+1"
export GRADLE_USER_HOME="$PWD/.toolchain/gradle-home"
./.toolchain/gradle-8.10.2/bin/gradle assembleDebug --offline
```

Chạy test: `gradle testDebugUnitTest`

---

## Cấu trúc mã nguồn

```
data/
  NorrDb.kt              SQLite: tracks, playlists, playlist_tracks, favorites, recent
  MusicRepository.kt     Quét ba lớp + đổi tên + ảnh bìa
  AppSettings.kt         SharedPreferences: hình nền, kiểu sắp xếp, bài đang nghe
  ArtworkLoader.kt       Cache ảnh bìa theo byte, hai mức kích thước
  Track.kt               Model + SortMode + Playlist
playback/
  PlaybackService.kt     MediaSessionService
  AudioFileBitmapLoader.kt  Đọc ảnh nhúng cho thanh thông báo
ui/
  AppRoot.kt             3 tab, điều hướng, các lớp phủ
  LibraryScreen.kt       Thư viện
  PlaylistsScreen.kt     Danh sách phát
  PlaylistDetailScreen.kt
  SettingsScreen.kt      Hình nền, quét lại, giới thiệu
  PlayerScreen.kt        Màn hình phát toàn màn hình
  AppViewModel.kt        Trạng thái + điều khiển phát
```

---

## Giới hạn đã biết

- Sửa ảnh bìa chỉ hỗ trợ **MP3** (thư viện mp3agic). Định dạng khác vẫn phát và đổi tên bình thường.
- Thư viện giới hạn 5.000 bài, độ sâu thư mục 12.
- **Không có tải nhạc từ YouTube.** Việc tải nội dung từ YouTube vi phạm điều khoản
  dịch vụ của họ và có thể xâm phạm bản quyền. App chỉ tải từ nguồn **cho phép tải**
  (Internet Archive, Jamendo). Nếu bạn có file nhạc hợp pháp, chỉ cần chép vào máy —
  app sẽ tự nhận.
