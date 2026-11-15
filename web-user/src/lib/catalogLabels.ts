/** Keep dataset notes in project documentation, not in tourist-facing labels. */
export function categoryDisplayName(name: string) {
  const label = name.trim();
  // Earlier signed packages may still use the retired labels.
  if (/^Tuyến Quận 1(?: và lân cận)?(?: \(demo\))?$/i.test(label)) return 'Thường';
  if (label === 'Công viên demo (trống)') return 'Công viên';
  return label;
}

const categoryOrder = ['Thường', 'Di tích', 'Bảo tàng', 'Công viên'];

export function categoryRank(name: string) {
  const index = categoryOrder.indexOf(categoryDisplayName(name));
  return index === -1 ? categoryOrder.length : index;
}
