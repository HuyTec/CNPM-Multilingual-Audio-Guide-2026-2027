export type ScriptType = 'FULL' | 'SHORT';
export const supportedLanguages = ['VI', 'EN', 'FR', 'JA', 'ZH', 'RU'];
export const tourCategoryId = '10000000-0000-0000-0000-000000000101';
export type TourLocation = {
  locationId: string; name: string; categoryId: string; categoryName: string;
  latitude: number; longitude: number; geofenceRadiusM: number; names?: Record<string,string>; imageUrl?: string;
};
export type Selection = { locationId: string; language: string; scriptType: ScriptType };
export type Guide = {
  contentId: string;
  locationId: string;
  locationName: string;
  languageCode: string;
  scriptType: ScriptType;
  scriptText: string;
  audioUrl: string | null;
};

export const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export const languagePattern = /^[A-Z]{2,3}(-[A-Z]{2})?$/;

export function languageName(code: string) {
  return new Intl.DisplayNames(['vi'], { type: 'language' }).of(code.toLowerCase()) || code;
}

/** Reject mismatched or empty successful responses instead of showing another selection. */
export function parseGuide(value: unknown, selection: Selection): Guide {
  if (!value || typeof value !== 'object') throw new Error('Invalid guide');
  const guide = value as Partial<Guide>;
  if (typeof guide.contentId !== 'string' || !uuidPattern.test(guide.contentId)
    || guide.locationId !== selection.locationId || guide.scriptType !== selection.scriptType
    || typeof guide.locationName !== 'string' || !guide.locationName.trim()
    || typeof guide.languageCode !== 'string'
    || ![selection.language, 'EN', 'VI'].includes(guide.languageCode)
    || typeof guide.scriptText !== 'string' || !guide.scriptText.trim()
    || (guide.audioUrl !== null && typeof guide.audioUrl !== 'string')) {
    throw new Error('Invalid guide');
  }
  return guide as Guide;
}
