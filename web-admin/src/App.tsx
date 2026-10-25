import {
  createBrowserRouter,
  Link,
  NavLink,
  Navigate,
  Outlet,
  RouterProvider,
  useLocation,
  useSearchParams,
} from "react-router-dom";
import {
  MapPin,
  BarChart3,
  Headphones,
  FlaskConical,
  ChevronRight,
  BookOpen,
  ShieldCheck,
  LogOut,
} from "lucide-react";
import "./admin-workspace.css";
import AdminHelp from "./pages/AdminHelp";
import ReviewQueue from "./pages/ReviewQueue";
import LocationList from "./pages/LocationList";
import LocationForm from "./pages/LocationForm";
import AnalyticsFeedback from "./pages/AnalyticsFeedback";
import { AdminContentGate, AdminProvider } from "./components/AdminProvider";
import { useAdminSession } from "./lib/admin-content";
function Layout() {
  const location = useLocation();
  const [params, setParams] = useSearchParams();
  const { authorization, setAuthorization, identity } = useAdminSession();
  return (
    <div className="app-shell">
      <a className="skip-link" href="#main">
        Bỏ qua đến nội dung chính
      </a>
      <aside className="sidebar">
        <Link
          to="/locations"
          className="brand"
          aria-label="HVP Admin — về danh sách địa điểm"
        >
          <span className="brand-mark">
            <Headphones size={23} aria-hidden="true" />
          </span>
          <span translate="no">
            HVP<span className="brand-sub">CONTENT STUDIO</span>
          </span>
        </Link>
        <p className="nav-label">KHÔNG GIAN QUẢN TRỊ</p>
        <nav aria-label="Điều hướng chính">
          <NavLink to="/locations">
            <MapPin size={18} aria-hidden="true" />
            Địa điểm
          </NavLink>
          {identity?.role === "REVIEWER" && (
            <NavLink to="/reviews">
              <ShieldCheck size={18} aria-hidden="true" />
              Duyệt và xuất bản
            </NavLink>
          )}
          <NavLink to="/analytics">
            <BarChart3 size={18} aria-hidden="true" />
            Thống kê & phản hồi
          </NavLink>
          <NavLink to="/help">
            <BookOpen size={18} aria-hidden="true" />
            Hướng dẫn sử dụng
          </NavLink>
        </nav>
        <div className="sidebar-bottom">
          <div className="demo-badge">
            <FlaskConical size={15} aria-hidden="true" />
            Môi trường demo
          </div>
          <p>
            Thống kê lấy từ database.
            <br />
            Biên tập và phản hồi lưu vào DB chung.
          </p>
          <div className="profile">
            <span className="avatar">AD</span>
            <div>
              <strong>{identity?.username || "Quản trị viên"}</strong>
              <small>
                {identity?.role === "EDITOR"
                  ? "Biên tập viên"
                  : identity?.role === "REVIEWER"
                    ? "Người duyệt nội dung"
                    : "Chưa đăng nhập"}
              </small>
            </div>
          </div>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <div className="breadcrumb">
            <span>Web Admin</span>
            <ChevronRight size={14} aria-hidden="true" />
            <strong>
              {location.pathname.startsWith("/analytics")
                ? "Thống kê & phản hồi"
                : location.pathname.startsWith("/reviews")
                  ? "Duyệt và xuất bản"
                  : location.pathname.startsWith("/help")
                    ? "Hướng dẫn sử dụng"
                    : "Địa điểm"}
            </strong>
          </div>
          <div className="topbar-actions">
            <Link to="/help" aria-label="Mở hướng dẫn quản trị">
              <BookOpen size={17} aria-hidden="true" />
              <span>Hướng dẫn</span>
            </Link>
            {authorization && (
              <button
                className="button secondary"
                type="button"
                onClick={() => setAuthorization("")}
              >
                <LogOut size={15} aria-hidden="true" />
                Đăng xuất quản trị
              </button>
            )}
          </div>
        </header>
        <main id="main" tabIndex={-1}>
          <div
            className={`admin-page ${location.pathname.startsWith("/analytics") ? "admin-analytics" : ""}`}
          >
            {location.pathname.startsWith("/locations") ||
            location.pathname.startsWith("/reviews") ? (
              <AdminContentGate>
                <Outlet />
              </AdminContentGate>
            ) : (
              <Outlet />
            )}
          </div>
        </main>
        {location.pathname.startsWith("/analytics") &&
          params.get("source") === "mock" && (
            <footer className="demo-controls">
              <label htmlFor="scenario">
                <FlaskConical size={15} aria-hidden="true" />
                Tình huống kiểm thử
              </label>
              <select
                id="scenario"
                name="scenario"
                value={params.get("scenario") || "normal"}
                onChange={(e) => {
                  const next = new URLSearchParams(params);
                  next.set("scenario", e.target.value);
                  next.delete("page");
                  setParams(next);
                }}
              >
                <option value="normal">Dữ liệu thường</option>
                <option value="empty">Không có dữ liệu</option>
                <option value="error">Lỗi tải dữ liệu</option>
                <option value="long">Dữ liệu dài</option>
                {location.pathname.includes("/edit") ||
                location.pathname.endsWith("/new") ? (
                  <option value="upload-error">Lỗi upload audio</option>
                ) : location.pathname.startsWith("/analytics") ? (
                  <option value="export-error">Lỗi xuất báo cáo</option>
                ) : null}
              </select>
              <small>Chỉ dùng để kiểm tra giao diện demo</small>
            </footer>
          )}
      </div>
    </div>
  );
}
function ExistingForm() {
  const location = useLocation();
  return <LocationForm key={location.pathname} />;
}
const router = createBrowserRouter([
  {
    element: (
      <AdminProvider>
        <Layout />
      </AdminProvider>
    ),
    children: [
      { index: true, element: <Navigate replace to="/locations" /> },
      { path: "/locations", element: <LocationList /> },
      { path: "/locations/new", element: <LocationForm key="new" /> },
      { path: "/help", element: <AdminHelp /> },
      { path: "/locations/:id/edit", element: <ExistingForm /> },
      { path: "/analytics", element: <AnalyticsFeedback /> },
      { path: "/reviews", element: <ReviewQueue /> },
      {
        path: "*",
        element: (
          <div>
            <h1>Không tìm thấy trang</h1>
            <Link to="/locations">Về danh sách địa điểm</Link>
          </div>
        ),
      },
    ],
  },
]);
export default function App() {
  return <RouterProvider router={router} />;
}
