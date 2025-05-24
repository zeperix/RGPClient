# FireCly Android

Một client mã nguồn mở dành cho [Apollo](https://github.com/ClassicOldSong/Apollo)/[Sunshine](https://github.com/LizardByte/Sunshine).

FireCly Android cho phép bạn stream bộ sưu tập game từ PC chạy Windows đến thiết bị Android của bạn,  
dù trong nhà hay qua Internet.

Trải nghiệm mượt mà hơn với màn hình ảo khi kết hợp FireCly với [Apollo](https://github.com/ClassicOldSong/Apollo).

---

# Tính năng

1. Nút ảo tùy chỉnh, hỗ trợ xuất và nhập cấu hình.
2. [Độ phân giải tùy chỉnh](https://github.com/moonlight-stream/moonlight-android/pull/1349).
3. Tùy chỉnh bitrate.
4. [Chuyển đổi nhiều chế độ chuột](https://github.com/moonlight-stream/moonlight-android/pull/1304) (chuột thường, [đa điểm](https://github.com/moonlight-stream/moonlight-android/pull/1364), touchpad, vô hiệu hóa, chế độ con trỏ cục bộ).
5. Skin gamepad ảo được tối ưu hóa và joystick tự do.
6. Chế độ màn hình ngoài.
7. Hỗ trợ D-pad của Joycon.
8. Hiển thị thông tin hiệu năng đơn giản hóa.
9. [Menu quay lại game](https://github.com/moonlight-stream/moonlight-android/pull/1171).
10. Lệnh phím tắt tùy chỉnh.
11. Chuyển đổi bàn phím mềm dễ dàng.
12. Hỗ trợ chế độ dọc.
13. Chế độ hiển thị đè, hữu ích cho điện thoại gập.
14. [Không gian touchpad ảo và điều chỉnh độ nhạy](https://github.com/moonlight-stream/moonlight-android/issues/1348#issuecomment-2236344729) – hữu ích khi chơi game cần nhấp chuột phải để xem, như Warcraft.
15. Cưỡng chế sử dụng motor rung của thiết bị (trong trường hợp rung từ tay cầm không hiệu quả).
16. Trang gỡ lỗi gamepad hiển thị thông tin về rung và con quay hồi chuyển, cũng như phiên bản kernel của Android.
17. Hỗ trợ tap/cuộn trên touchpad.
18. Chế độ touchpad tự nhiên với màn hình cảm ứng.
19. Hỗ trợ bố cục bàn phím không phải QWERTY.
20. Phím Meta nhanh với nút BACK vật lý.
21. Sửa lỗi khóa khung hình trên một số thiết bị.
22. Chế độ thu phóng video: Fit / Fill / Stretch.
23. Hỗ trợ xoay / cuộn màn hình.
24. Xoay màn hình khi đang chơi game.
25. Thêm tùy chọn thoát ứng dụng trực tiếp.
26. Hỗ trợ cuộn chuột trong Samsung DeX.
27. Click / cuộn / nhấp phải chính xác cho touchpad trên tablet Android khi dùng con trỏ cục bộ.
28. Tích hợp Màn Hình Ảo với [Apollo](https://github.com/ClassicOldSong/Apollo).
29. Tích hợp lệnh điều khiển Server với [Apollo](https://github.com/ClassicOldSong/Apollo).
30. Đồng bộ clipboard (yêu cầu Apollo).

---

## Tải xuống

* [Tải APK trực tiếp tại đây](https://github.com/zeperix/FireCly/releases/latest)

---

## Biên dịch

1. Cài đặt Android Studio và Android NDK  
2. Chạy lệnh `git submodule update --init --recursive` trong thư mục `FireCly/`  
3. Trong thư mục `FireCly/`, tạo file `local.properties`. Thêm dòng `ndk.dir=` trỏ đến thư mục cài NDK của bạn  
4. Biên dịch APK bằng Android Studio hoặc lệnh `gradlew assembleRelease` 

