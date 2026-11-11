import benNhaRong from '../assets/locations/ben-nha-rong.webp';
import warMuseum from '../assets/locations/chung-tich-chien-tranh.webp';
import independencePalace from '../assets/locations/dinh-doc-lap.webp';
import operaHouse from '../assets/locations/nha-hat-thanh-pho.webp';
import notreDame from '../assets/locations/nha-tho-duc-ba.webp';
import saigonUniversity from '../assets/locations/dai-hoc-sai-gon.webp';
import bitexco from '../assets/locations/bitexco.webp';
import xaLoi from '../assets/locations/xa-loi.webp';

// Photos supplied by the user, associated by stable location ID, never list order.
const photos: Record<string, { src: string; width: number; height: number }> = {
  '20000000-0000-0000-0000-000000000101': { src: independencePalace, width: 1280, height: 720 },
  '20000000-0000-0000-0000-000000000102': { src: operaHouse, width: 800, height: 450 },
  '20000000-0000-0000-0000-000000000103': { src: notreDame, width: 1000, height: 357 },
  '20000000-0000-0000-0000-000000000104': { src: saigonUniversity, width: 492, height: 352 },
  '20000000-0000-0000-0000-000000000105': { src: warMuseum, width: 640, height: 480 },
  '20000000-0000-0000-0000-000000000106': { src: benNhaRong, width: 1280, height: 720 },
  '20000000-0000-0000-0000-000000000107': { src: bitexco, width: 886, height: 1600 },
  '20000000-0000-0000-0000-000000000108': { src: xaLoi, width: 800, height: 450 },
};

export const locationPhoto = (id: string, imageUrl?: string) =>
  imageUrl && /^\/api\/content-images\/[0-9a-f-]{36}\.png$/.test(imageUrl)
    ? { src: imageUrl, width: 800, height: 450 } : photos[id];

export function normalizePlaceName(value: string) {
  return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[đĐ]/g, 'd').toLocaleLowerCase('vi-VN').trim();
}
