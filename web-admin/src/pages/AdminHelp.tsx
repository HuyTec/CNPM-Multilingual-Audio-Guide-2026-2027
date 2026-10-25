import { Link } from "react-router-dom";
import {
  BookOpen,
  PenLine,
  ShieldCheck,
  Download,
  BarChart3,
  CircleHelp,
} from "lucide-react";

const sections = [
  {
    id: "start",
    icon: BookOpen,
    title: "1. Đăng nhập và chọn đúng vai trò",
    steps: [
      "Dùng tài khoản và mật khẩu do nhóm vận hành cung cấp. Biên tập viên (EDITOR) tạo/sửa nội dung; người duyệt (REVIEWER) quyết định xuất bản.",
      "Xem tên tài khoản và vai trò ở thanh điều hướng. Nếu không thấy Duyệt và xuất bản, tài khoản hiện tại không có quyền REVIEWER.",
      "Phiên đăng nhập chỉ giữ trong tab. Tải lại trang có thể yêu cầu đăng nhập lại; hãy lưu bản nháp trước khi đóng trang.",
    ],
  },
  {
    id: "create",
    icon: PenLine,
    title: "2. Tạo địa điểm, ảnh và nội dung",
    steps: [
      "Vào Địa điểm → Thêm địa điểm. Nhập tên, danh mục, vĩ độ, kinh độ và bán kính. Lưu nháp được phép thiếu thông tin; gửi duyệt phải đủ các trường bắt buộc.",
      "Chọn ảnh JPEG/PNG tối đa 5 MB và kiểm tra ảnh xem trước. Một ảnh đại diện dùng chung cho các ngôn ngữ.",
      "Nhập thủ công: chọn từng ngôn ngữ, nhập tên và bài FULL (đầy đủ) hoặc SHORT (rút gọn). Dịch tự động: nhập tên/bài ở ngôn ngữ nguồn, chọn đích rồi bấm Dịch tự động; đọc và sửa kết quả. Hiện provider demo chỉ giả lập, không phải bản dịch thật.",
      "Web/Android dùng TTS đọc văn bản khi không có audio thu sẵn. Nếu cần tải audio, mở Audio thu sẵn (tùy chọn), chọn MP3/WAV/OGG rồi nghe thử.",
    ],
  },
  {
    id: "draft",
    icon: ShieldCheck,
    title: "3. Lưu nháp, gửi và duyệt",
    steps: [
      "Bấm Lưu nháp để tiếp tục sau, hoặc Gửi duyệt khi thông tin đã sẵn sàng. Nếu có lỗi, sửa trường được đánh dấu; dữ liệu đang nhập được giữ lại.",
      "Người duyệt vào Duyệt và xuất bản, chọn bản chờ duyệt, so sánh tên/kịch bản với bản hiện có. Có thể mở Xem đầy đủ bản biên tập để xem ảnh.",
      "Từ chối phải có nhận xét. Biên tập viên mở bản Bị từ chối, sửa theo góp ý rồi gửi lại. Chấp thuận và tạo gói sẽ bắt đầu tác vụ đóng gói.",
      "Đợi tác vụ Sẵn sàng (READY). Tạo thất bại (FAILED) cần xem lỗi và bấm Thử tạo lại gói. Bản công khai cũ vẫn được giữ trong lúc sửa hoặc khi tạo gói lỗi.",
    ],
  },
  {
    id: "download",
    icon: Download,
    title: "4. Kiểm tra nội dung trên Web du khách",
    steps: [
      "Sau khi tác vụ READY, mở Web du khách → Nội dung offline → Kiểm tra cập nhật. Ứng dụng cũng kiểm tra khoảng 30 giây một lần khi đang mở.",
      "Chọn Tải gói hoặc Cập nhật gói, đọc hộp xác nhận ở giữa màn hình rồi bấm Xác nhận tải. Gói cơ bản dành cho tuyến demo; địa điểm khác nằm trong gói bổ sung.",
      "Gói được lưu trong bộ nhớ trình duyệt, không phải file trong thư mục Downloads. Đợi thông báo Đã cài nội dung được xác minh; mở Xem địa điểm trong gói để đọc bài.",
      "Dữ liệu lưu riêng theo trình duyệt và địa chỉ website. Đổi link tunnel hoặc thiết bị cần tải lại. Điểm mẫu (1,1) cách xa Việt Nam; tìm theo tên hoặc mở từ gói đã cài, không tìm trong danh sách gần bạn.",
    ],
  },
  {
    id: "reports",
    icon: BarChart3,
    title: "5. Thống kê và phản hồi",
    steps: [
      "Vào Thống kê & phản hồi, chọn Từ ngày / Đến ngày. Số lượt phát chỉ tăng khi bài thực sự bắt đầu phát, không phải khi chỉ mở địa điểm.",
      "Lọc phản hồi theo danh mục và trạng thái; mở Xem chi tiết. Chuyển lần lượt Mới → Đang xử lý → Đã giải quyết, không bỏ qua bước.",
      "Xuất báo cáo .xlsx áp dụng kỳ báo cáo và bộ lọc phản hồi đang chọn. Nếu xuất lỗi, giữ nguyên bộ lọc và bấm thử lại.",
    ],
  },
  {
    id: "trouble",
    icon: CircleHelp,
    title: "6. Khi gặp sự cố",
    steps: [
      "Không đăng nhập được: kiểm tra tên tài khoản/mật khẩu được cấp. Cần duyệt nhưng không thấy menu: đăng xuất và dùng tài khoản REVIEWER.",
      "Báo xung đột phiên bản: chép lại phần đang nhập trước khi mở lại bản mới nhất. Không lưu đè khi một người khác vừa sửa cùng địa điểm.",
      "Chưa thấy gói mới: kiểm tra trạng thái duyệt và tác vụ tạo gói, đúng địa chỉ website/backend, sau đó Kiểm tra cập nhật. Đang nghe hoặc tạm dừng bài có thể khiến việc cài gói phải chờ; hãy dừng bài.",
      "Ảnh mới hiện dùng online; chưa có ảnh tải offline hoặc UI ảnh mới trên Android. Giọng TTS ngoại tuyến cần được cài trên thiết bị. Khi báo lỗi, ghi trang đang dùng, tên địa điểm và thông báo lỗi; không gửi mật khẩu.",
    ],
  },
];

export default function AdminHelp() {
  return (
    <div className="admin-help-page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">DÀNH CHO QUẢN TRỊ VIÊN MỚI</p>
          <h1>Hướng dẫn sử dụng</h1>
          <p className="muted">
            Từ một bản thuyết minh đến gói nội dung du khách có thể tải về.
          </p>
        </div>
        <Link to="/locations" className="button">
          Về danh sách địa điểm
        </Link>
      </div>
      <div className="help-workflow" aria-label="Quy trình nội dung">
        <span>Biên tập</span>
        <span aria-hidden="true">→</span>
        <span>Lưu nháp / Gửi duyệt</span>
        <span aria-hidden="true">→</span>
        <span>Người duyệt chấp thuận</span>
        <span aria-hidden="true">→</span>
        <span>Gói sẵn sàng</span>
        <span aria-hidden="true">→</span>
        <span>Du khách xác nhận tải</span>
      </div>
      <nav className="help-contents" aria-label="Mục lục hướng dẫn">
        {sections.map((section) => (
          <a key={section.id} href={`#help-${section.id}`}>
            {section.title}
          </a>
        ))}
      </nav>
      <div className="help-grid">
        {sections.map(({ id, icon: Icon, title, steps }) => (
          <section className="card help-section" id={`help-${id}`} key={id}>
            <h2>
              <Icon size={20} aria-hidden="true" />
              {title}
            </h2>
            <ol>
              {steps.map((step) => (
                <li key={step}>{step}</li>
              ))}
            </ol>
          </section>
        ))}
      </div>
    </div>
  );
}
