/** Keep dataset notes in project documentation, not in tourist-facing labels. */
export function categoryDisplayName(name: string) {
  return name.replace(/\s*\(demo\)/gi, '').trim();
}
