import { adminRequest } from "./admin-content";
import type { ExportFeedback } from "./export";

export type FeedbackItem = ExportFeedback & {
  locationId: string | null;
  createdAt: string;
};
export type FeedbackPage = {
  items: FeedbackItem[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
  categories: string[];
};
export function feedbackQuery(
  params: URLSearchParams,
  from: string,
  to: string,
) {
  const query = new URLSearchParams({ from, to });
  for (const key of ["category", "status"]) {
    const value = params.get(key);
    if (value) query.set(key, value);
  }
  return query;
}
/** Export all matching rows, independently of the currently displayed page. */
export function feedbackForExport(
  authorization: string,
  query: URLSearchParams,
) {
  return adminRequest<FeedbackItem[]>(
    `/api/admin/feedback/export?${query}`,
    authorization,
  );
}
