package vn.hvp.travelvoice.ui.mock

data class DemoLocation(
    val id: String, val name: String, val street: String, val category: String,
    val distanceMeters: Int, val summary: String, val fullText: String,
)

/** Editorial sample text and illustrative distances only; not a published tourism database. */
object DemoData {
    val languages = listOf("VI", "EN", "JA")
    val locations = listOf(
        DemoLocation("ngoc-son", "Đền Ngọc Sơn", "Hồ Hoàn Kiếm, Hà Nội", "Di tích", 120,
            "Khám phá không gian di tích bên hồ Hoàn Kiếm.", "Đây là kịch bản minh họa cho Đền Ngọc Sơn. Nội dung thuyết minh được trình bày thành các đoạn ngắn để thuận tiện đọc và nghe.\n\nBạn có thể chuyển ngôn ngữ, chọn bản tóm tắt hoặc mở trình nghe mô phỏng. Văn bản này không thay thế nội dung du lịch đã được kiểm duyệt."),
        DemoLocation("hoan-kiem", "Phố đi bộ Hoàn Kiếm", "Quanh hồ Hoàn Kiếm, Hà Nội", "Không gian công cộng", 280,
            "Một điểm dừng để trải nghiệm nhịp sống quanh hồ.", "Kịch bản minh họa giới thiệu không gian quanh hồ Hoàn Kiếm.\n\nHãy dùng chế độ đầy đủ để đọc toàn bộ mẫu, hoặc chọn tóm tắt để tiếp tục hành trình. Đây là dữ liệu tĩnh phục vụ kiểm tra giao diện."),
        DemoLocation("pho-co", "Phố cổ Hà Nội", "Quận Hoàn Kiếm, Hà Nội", "Di tích", 430,
            "Ghé thăm những tuyến phố và không gian đô thị đặc trưng.", "Kịch bản minh họa cho Phố cổ Hà Nội.\n\nCác thông tin trong bản demo chỉ dùng để thể hiện bố cục. Nội dung chính thức cần được biên tập và duyệt trước khi phát hành."),
        DemoLocation("lich-su", "Bảo tàng Lịch sử", "Tràng Tiền, Hà Nội", "Bảo tàng", 840,
            "Một điểm khám phá dành cho người yêu lịch sử.", "Nội dung minh họa cho màn hình bảo tàng.\n\nBản đầy đủ và tóm tắt thể hiện hai chế độ kịch bản của UC-02, không có truy vấn máy chủ."),
        DemoLocation("thang-long", "Hoàng thành Thăng Long", "Hoàng Diệu, Hà Nội", "Di tích", 1800,
            "Khám phá di sản trong hành trình tham quan thủ đô.", "Kịch bản minh họa cho Hoàng thành Thăng Long.\n\nHình ảnh trong demo là placeholder; không có dịch vụ bản đồ hoặc định vị."),
        DemoLocation("tay-ho", "Hồ Tây", "Tây Hồ, Hà Nội", "Thiên nhiên", 3200,
            "Không gian ven hồ để khám phá theo nhịp riêng.", "Kịch bản minh họa cho Hồ Tây.\n\nCác khoảng cách là số liệu giả lập, không được tính từ vị trí thiết bị."),
    )
    fun find(id: String) = locations.find { it.id == id }
}
