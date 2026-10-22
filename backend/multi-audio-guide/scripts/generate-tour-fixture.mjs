import { createHash } from 'node:crypto';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const source = new URL('../../../docs/en/multilingual_tour_content.md', import.meta.url);
const output = new URL('../src/main/resources/db/demo/V3_3__seed_district_tour.sql', import.meta.url);
const markdown = readFileSync(source, 'utf8');
const languages = ['VI', 'EN', 'FR', 'JA', 'ZH', 'RU'];
const sections = [...markdown.matchAll(/^## (\d+)\. (.+)\r?\n([\s\S]*?)(?=^## |$(?![\s\S]))/gm)];
const quote = value => "'" + value.replaceAll("'", "''") + "'";
// Approximate demo markers only, not surveyed geofences. No nearby/GPS claim.
const points = [[106.6953,10.7770],[106.7031,10.7766],[106.6990,10.7798],
  [106.7058,10.7870],[106.6919,10.7795],[106.7069,10.7682],[106.7044,10.7716]];
const rows = [];
const locations = [];
if (sections.length !== 7) throw new Error('Expected seven source locations');
for (const [index, section] of sections.entries()) {
  const id = `20000000-0000-0000-0000-${String(101 + index).padStart(12, '0')}`;
  const name = section[2].replace(/ \(.+\)$/, '');
  const [lng, lat] = points[index];
  locations.push(`(${quote(id)}, '10000000-0000-0000-0000-000000000101', ${quote(name)}, ST_SetSRID(ST_MakePoint(${lng}, ${lat}),4326)::geography, 100, 'APPROVED')`);
  const scripts = [...section[3].matchAll(/^### .+ \(([A-Z]{2})\)\r?\n([\s\S]*?)(?=^### |$(?![\s\S]))/gm)];
  if (scripts.length !== 6) throw new Error(`Missing source language at ${name}`);
  for (const language of languages) {
    const block = scripts.find(item => item[1] === language)?.[2];
    for (const type of ['FULL', 'SHORT']) {
      const text = block?.match(new RegExp(`^\\* \\*\\*${type}:\\*\\* (.+)$`, 'm'))?.[1].replace(/\r$/, '');
      if (!text?.trim()) throw new Error(`Missing ${name}/${language}/${type}`);
      const contentId = `31000000-0000-0000-0000-${String(rows.length + 1).padStart(12, '0')}`;
      rows.push(`(${quote(contentId)}, ${quote(id)}, ${quote(language)}, ${quote(type)}, ${quote(text)}, 'APPROVED')`);
    }
  }
}
const sql = `-- Demo-only publication authorized by the content owner; NOT fact-checked travel advice.
-- Exact source: docs/en/multilingual_tour_content.md; text is preserved, including known inconsistencies.
-- SHA256: ${createHash('sha256').update(markdown).digest('hex')}
-- Seven locations, six languages, two independently authored modes = 84 scripts.
-- Coordinates/radius are approximate demo markers, NOT verified GPS/geofence data.
-- No audio is invented. Existing Android demo content/audio remains unchanged.
INSERT INTO languages(code,display_name) VALUES
('FR','Français'),('JA','日本語'),('ZH','中文'),('RU','Русский') ON CONFLICT DO NOTHING;
INSERT INTO categories(id,name) VALUES
('10000000-0000-0000-0000-000000000101','Tuyến Quận 1 và lân cận (demo)');
INSERT INTO locations(id,category_id,name,coordinates,geofence_radius_m,status) VALUES
${locations.join(',\n')};
INSERT INTO guide_content_revisions(id,location_id,language_code,script_type,script_text,status) VALUES
${rows.join(',\n')};
`;
if (process.argv.includes('--check')) {
  if (readFileSync(output, 'utf8') !== sql) throw new Error('Fixture differs from current source; use a NEW migration to revise published content');
  console.log(`PASS: ${sections.length} locations / ${rows.length} exact source scripts`);
} else {
  if (existsSync(output)) throw new Error('Migration already exists; never overwrite an applied migration');
  writeFileSync(output, sql, 'utf8');
  console.log(fileURLToPath(output));
}
