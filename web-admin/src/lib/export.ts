import type { Feedback } from "../mocks/data";
/** Create a real workbook for the supplied period, including every feedback row in that period. */
export async function exportReport(
  from: string,
  to: string,
  metrics: [string, string | number][],
  feedback: Feedback[],
) {
  const { Workbook } = await import("exceljs");
  const workbook = new Workbook();
  const analytics = workbook.addWorksheet("Analytics");
  analytics.addRows([
    ["Báo cáo demo HVP", "Dữ liệu giả lập"],
    ["Từ ngày", from],
    ["Đến ngày", to],
    ["Chỉ số", "Giá trị"],
    ...metrics,
  ]);
  const sheet = workbook.addWorksheet("Feedback");
  sheet.addRow([
    "ID",
    "Địa điểm",
    "Danh mục",
    "Nội dung",
    "Đánh giá",
    "Trạng thái",
    "Ngày tạo",
    "Cập nhật",
  ]);
  feedback.forEach((x) =>
    sheet.addRow([
      x.id,
      x.location,
      x.category,
      x.content,
      x.rating,
      x.status,
      x.created,
      x.updated || "",
    ]),
  );
  for (const s of [analytics, sheet]) {
    s.getRow(1).font = { bold: true };
    s.columns.forEach((c) => {
      c.width = 24;
    });
    s.views = [{ state: "frozen", ySplit: 1 }];
  }
  const buffer = await workbook.xlsx.writeBuffer();
  const blob = new Blob([new Uint8Array(buffer)], {
    type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `bao-cao-${from}-${to}.xlsx`;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
