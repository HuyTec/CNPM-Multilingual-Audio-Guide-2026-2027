import { daysAgo } from "../lib/format";
export type ContentStatus = "DRAFT" | "PENDING_REVIEW" | "APPROVED";
export type FeedbackStatus = "NEW" | "IN_REVIEW" | "RESOLVED";
export type Asset = {
  locationId: string;
  languageCode: string;
  scriptType: string;
  packageVersion: string;
  text: string;
  audioName?: string;
  audioUrl?: string;
  audioStatus?: "ready" | "error";
};
export type Location = {
  id: string;
  name: string;
  category: string;
  latitude: string;
  longitude: string;
  radius: string;
  status: ContentStatus;
  packageVersion: string;
  assets: Record<string, Asset>;
  updatedAt?: string;
  published?: { version: string; name: string; assets: Record<string, Asset> };
};
export type Feedback = {
  id: string;
  category: string;
  content: string;
  rating: number;
  status: FeedbackStatus;
  created: string;
  updated?: string;
  location: string;
};
export const languages = [
  { code: "vi", label: "Tiếng Việt" },
  { code: "en", label: "English" },
  { code: "fr", label: "Français" },
];
export const categories = ["Văn hóa", "Lịch sử", "Ẩm thực"];
export const feedbackCategories = ["Nội dung", "Audio", "Ứng dụng", "Đề xuất"];
const names = [
  "Bưu điện Trung tâm",
  "Dinh Độc Lập",
  "Chợ Bến Thành",
  "Nhà thờ Đức Bà",
  "Bảo tàng Lịch sử",
  "Phố đi bộ Nguyễn Huệ",
];
// Reference points are sourced in docs/location-list-design.md; not geofence survey data.
const coordinates = [
  ["10.77998", "106.70002"],
  ["10.77702", "106.6954"],
  ["10.77257", "106.69802"],
  ["10.77977", "106.69906"],
  ["10.78484", "106.70759"],
  ["10.774112", "106.703619"],
];
const coverage = [
  { vi: "ready", en: "text" },
  { vi: "ready", en: "ready" },
  { vi: "text" },
  { vi: "ready", en: "error", fr: "ready" },
  { vi: "ready", en: "partial", fr: "text" },
  { vi: "ready", en: "ready" },
] as const;
const seedLocations: Location[] = names.map((name, i) => {
  const id = `loc-${i + 1}`;
  const assets: Record<string, Asset> = {};
  for (const [languageCode, state] of Object.entries(coverage[i])) {
    for (const scriptType of ["FULL", "SHORT"]) {
      if (state === "partial" && scriptType === "SHORT") continue;
      const audioStatus =
        state === "error" ? "error" : state === "ready" ? "ready" : undefined;
      assets[`${languageCode}:${scriptType}`] = {
        locationId: id,
        languageCode,
        scriptType,
        packageVersion: "2-demo",
        text: `${scriptType === "FULL" ? "Bản đầy đủ" : "Bản rút gọn"} · ${name} · ${languageCode.toUpperCase()}. Nội dung biên tập mẫu, chưa phải thuyết minh được duyệt.`,
        ...(audioStatus
          ? {
              audioStatus,
              audioName:
                audioStatus === "error"
                  ? "audio-loi.wav"
                  : "audio-kiem-thu.wav",
              audioUrl:
                audioStatus === "error"
                  ? "/audio/broken.wav"
                  : "/audio/preview.wav",
            }
          : {}),
      };
    }
  }
  return {
    id,
    name,
    category: [
      "Văn hóa",
      "Lịch sử",
      "Ẩm thực",
      "Văn hóa",
      "Lịch sử",
      "Văn hóa",
    ][i],
    latitude: coordinates[i][0],
    longitude: coordinates[i][1],
    radius: "80",
    status: (["APPROVED", "PENDING_REVIEW", "DRAFT"] as const)[i % 3],
    packageVersion: "2-demo",
    assets,
    updatedAt: `${daysAgo([0, 1, 3, 2, 5, 7][i])}T${["09:40", "16:15", "11:20", "14:05", "08:30", "17:45"][i]}:00+07:00`,
    ...(i % 3 === 0
      ? {
          published: {
            version: "1-demo",
            name,
            assets: Object.fromEntries(
              Object.entries(assets).map(([key, asset]) => [
                key,
                { ...asset, packageVersion: "1-demo" },
              ]),
            ),
          },
        }
      : {}),
  };
});
const seedFeedback: Feedback[] = Array.from({ length: 64 }, (_, i) => ({
  id: `fb-${i + 1}`,
  category: feedbackCategories[i % 4],
  content: [
    "Thuyết minh rõ ràng, giúp tôi hiểu thêm về địa điểm. Mong có thêm nội dung ngắn để nghe khi di chuyển.",
    "Âm lượng bản tiếng Anh hơi nhỏ. Có thể cân bằng âm thanh giữa các ngôn ngữ không?",
    "Nội dung ngoại tuyến rất hữu ích, tôi có thể tiếp tục nghe khi mất kết nối.",
    "Đề xuất bổ sung hướng dẫn sử dụng audio dễ tìm hơn.",
  ][i % 4],
  rating: 3 + (i % 3),
  status: (["NEW", "IN_REVIEW", "RESOLVED"] as const)[i % 3],
  created: daysAgo(i % 45),
  location: names[i % names.length],
}));
function read<T>(key: string, fallback: T): T {
  try {
    return (
      JSON.parse(localStorage.getItem(key) || "null") ??
      structuredClone(fallback)
    );
  } catch {
    return structuredClone(fallback);
  }
}
export const getLocations = () =>
  read<Location[]>("hvp-demo-locations-v1", seedLocations);
export const getFeedback = () =>
  read<Feedback[]>("hvp-demo-feedback-v1", seedFeedback);
export function saveLocation(location: Location) {
  const all = getLocations();
  const index = all.findIndex((x) => x.id === location.id);
  const saved = { ...location, updatedAt: new Date().toISOString() };
  if (index < 0) all.unshift(saved);
  else all[index] = saved;
  localStorage.setItem(
    "hvp-demo-locations-v1",
    JSON.stringify(
      all.map((x) => ({
        ...x,
        assets: Object.fromEntries(
          Object.entries(x.assets).map(([k, a]) => [
            k,
            { ...a, audioUrl: undefined },
          ]),
        ),
      })),
    ),
  );
  return all;
}
/** Apply exactly one allowed transition and persist its timestamp; RESOLVED is terminal. */
export function advanceFeedback(id: string): Feedback[] {
  const all = getFeedback();
  const record = all.find((x) => x.id === id);
  if (record && record.status !== "RESOLVED") {
    record.status = record.status === "NEW" ? "IN_REVIEW" : "RESOLVED";
    record.updated = new Date().toISOString();
  }
  localStorage.setItem("hvp-demo-feedback-v1", JSON.stringify(all));
  return all;
}
export function playbackEvents() {
  return Array.from({ length: 90 }, (_, day) =>
    names.map((location, i) => ({
      date: daysAgo(day),
      location,
      language: languages[i % 3].label,
      offline: (day + i) % 3 === 0,
      count: 32 + ((day * 7 + i * 13) % 80),
    })),
  ).flat();
}
export function longLocations() {
  return Array.from({ length: 64 }, (_, i) => ({
    ...seedLocations[i % 6],
    id: `long-${i}`,
    assets: Object.fromEntries(
      Object.entries(seedLocations[i % 6].assets).map(([key, asset]) => [
        key,
        { ...asset, locationId: `long-${i}` },
      ]),
    ),
    name: `${seedLocations[i % 6].name} — ${"Tên địa điểm dài để kiểm thử bố cục và khả năng ngắt dòng. ".repeat(4)}`,
  }));
}
