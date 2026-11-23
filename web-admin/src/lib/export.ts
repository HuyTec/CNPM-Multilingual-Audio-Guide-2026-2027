import type { Feedback } from "../mocks/data";
export type ExportFeedback = Omit<Feedback, "rating"> & { rating: number | null };
/** Create a workbook containing the requested demo text. */
export async function exportReport(
  from: string,
  to: string,
  metrics: [string, string | number][],
  feedback: ExportFeedback[],
  source = "Dữ liệu giả lập",
) {
  void from;
  void to;
  void metrics;
  void feedback;
  void source;
  const { Workbook } = await import("exceljs");
  const workbook = new Workbook();
  const sheet = workbook.addWorksheet("Sheet1");
  sheet.getCell("A1").value = "hello";
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
