import { writeFileSync } from 'node:fs';
const b = Buffer.alloc(6444);
b.write('RIFF'); b.writeUInt32LE(b.length - 8, 4); b.write('WAVEfmt ', 8); b.writeUInt32LE(16, 16); b.writeUInt16LE(1, 20); b.writeUInt16LE(1, 22); b.writeUInt32LE(8000, 24); b.writeUInt32LE(16000, 28); b.writeUInt16LE(2, 32); b.writeUInt16LE(16, 34); b.write('data', 36); b.writeUInt32LE(6400, 40);
for (let i = 0; i < 3200; i++) b.writeInt16LE(Math.round(Math.sin(i * 2 * Math.PI * 440 / 8000) * 1800), 44 + i * 2);
writeFileSync('public/audio/preview.wav', b);
writeFileSync('public/audio/broken.wav', 'Invalid WAV fixture for audio-error preview');
