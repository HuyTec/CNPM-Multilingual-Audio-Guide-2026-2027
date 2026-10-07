export const number = (value: number) =>
  new Intl.NumberFormat("vi-VN").format(value);
export const coordinate = (value: string) =>
  value.trim() && Number.isFinite(Number(value.replace(",", ".")))
    ? new Intl.NumberFormat("vi-VN", {
        maximumFractionDigits: 6,
        useGrouping: false,
      }).format(Number(value.replace(",", ".")))
    : "—";
export const percent = (value: number) =>
  new Intl.NumberFormat("vi-VN", {
    style: "percent",
    maximumFractionDigits: 1,
  }).format(value);
export const dateTime = (value: string) =>
  new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Asia/Ho_Chi_Minh",
  }).format(new Date(value));
export const date = (value: string) =>
  new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "medium",
    timeZone: "Asia/Ho_Chi_Minh",
  }).format(new Date(`${value}T12:00:00+07:00`));
export function today() {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());
  return ["year", "month", "day"]
    .map((key) => parts.find((part) => part.type === key)?.value)
    .join("-");
}
export function daysAgo(days: number) {
  const d = new Date(`${today()}T12:00:00+07:00`);
  d.setUTCDate(d.getUTCDate() - days);
  return d.toISOString().slice(0, 10);
}
