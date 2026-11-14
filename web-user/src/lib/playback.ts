import { playbackActive } from './playbackGate';
import type { Guide } from './guide';

export type PlaybackState = {
  status: 'idle' | 'preparing' | 'playing' | 'paused' | 'ended' | 'error';
  source: 'audio' | 'tts' | null; message: string; position: number; total: number;
  rate: number; voice: string;
  elapsed: number;
  estimatedDuration?: number;
};

/** Preserve every source character across utterances. */
export function speechChunks(text: string, language: string) {
  const chunks: string[] = [];
  let current = '';
  for (const { segment } of new Intl.Segmenter(language.toLowerCase(), { granularity: 'sentence' }).segment(text)) {
    if (current && current.length + segment.length > 220) { chunks.push(current); current = ''; }
    for (const char of segment) {
      current += char;
      if (current.length >= 220) { chunks.push(current); current = ''; }
    }
  }
  if (current) chunks.push(current);
  return chunks;
}

/** One owned playback source. Generation guards suppress callbacks after cancel/seek/unmount. */
export class GuidePlayback {
  private state: PlaybackState = { status: 'idle', source: null, message: '', position: 0, total: 0, rate: 1, voice: '', elapsed: 0 };
  private listeners = new Set<() => void>();
  private audio: HTMLAudioElement | null = null;
  private generation = 0;
  private timer: ReturnType<typeof setTimeout> | undefined;
  private waitingVoices: (() => void) | undefined;
  private voice: SpeechSynthesisVoice | undefined;
  private chunks: string[];
  private offset = 0;
  private started = false;
  private chunkDurations: number[];
  private speechClock: ReturnType<typeof setInterval> | undefined;
  private readSpeechClock: (() => number) | undefined;
  constructor(private guide: Guide, private onStarted: () => void) {
    this.chunks = speechChunks(guide.scriptText, guide.languageCode);
    const language = guide.languageCode.toLowerCase();
    const segmenter = new Intl.Segmenter(language, { granularity: 'word' });
    this.chunkDurations = this.chunks.map(chunk => {
      if (/^(ja|zh)(-|$)/.test(language)) return Math.max(1, [...chunk.replace(/\s/g, '')].length / 5);
      const words = [...segmenter.segment(chunk)].filter(segment => segment.isWordLike).length;
      return Math.max(1, words / 2.5);
    });
  }
  subscribe = (listener: () => void) => { this.listeners.add(listener); return () => { this.listeners.delete(listener); }; };
  snapshot = () => this.state;
  private update(patch: Partial<PlaybackState>) {
    this.state = { ...this.state, ...patch }; playbackActive(this,["playing","preparing","paused"].includes(this.state.status)); this.listeners.forEach(listener => listener());
  }
  private clearTimer() { clearTimeout(this.timer); this.timer = undefined; }
  private clearVoiceWait() {
    if (this.waitingVoices) window.speechSynthesis?.removeEventListener('voiceschanged', this.waitingVoices);
    this.waitingVoices = undefined;
  }
  private cancelSpeech() {
    this.generation++; this.clearTimer(); this.clearVoiceWait();
    this.finishSpeechClock();
    window.speechSynthesis?.cancel();
  }
  private releaseAudio() {
    if (!this.audio) return;
    this.audio.onplaying = this.audio.onpause = this.audio.onended = this.audio.onerror = null;
    this.audio.ontimeupdate = this.audio.onloadedmetadata = null;
    this.audio.pause(); this.audio.removeAttribute('src'); this.audio.load(); this.audio = null;
  }
  private didStart() {
    this.clearTimer(); this.update({ status: 'playing' });
    if (!this.started) { this.started = true; this.onStarted(); }
  }
  play = () => {
    if (this.state.status === 'playing' || this.state.status === 'preparing') return;
    if (this.state.status === 'paused') {
      if (this.state.source === 'audio' && this.audio) {
        this.update({ status: 'preparing' }); this.armTimeout();
        void this.audio.play().catch(() => { if (this.state.status === 'preparing') this.startTts(); });
      } else this.speak(this.state.position, this.offset);
      return;
    }
    this.stop(); this.started = false;
    if (!this.guide.audioUrl) { this.startTts(); return; }
    let url: URL;
    try { url = new URL(this.guide.audioUrl, location.origin); if (!['http:', 'https:', 'blob:'].includes(url.protocol)) throw new Error(); }
    catch { this.startTts(); return; }
    this.update({ source: 'audio', status: 'preparing', message: 'Đang chuẩn bị audio…' });
    const audio = new Audio(url.href); this.audio = audio; audio.playbackRate = this.state.rate;
    audio.onplaying = () => { if (this.audio === audio) this.didStart(); };
    audio.onloadedmetadata = () => { if (this.audio === audio && Number.isFinite(audio.duration)) this.update({ total: audio.duration }); };
    audio.ontimeupdate = () => { if (this.audio === audio) this.update({ position: audio.currentTime }); };
    audio.onpause = () => { if (this.audio === audio && !audio.ended && this.state.status === 'playing') this.update({ status: 'paused' }); };
    audio.onended = () => { if (this.audio === audio) this.update({ status: 'ended', position: this.state.total }); };
    audio.onerror = () => { if (this.audio === audio) this.startTts(); };
    this.armTimeout();
    void audio.play().catch(() => { if (this.audio === audio && this.state.status === 'preparing') this.startTts(); });
  };
  private armTimeout() {
    this.clearTimer();
    this.timer = setTimeout(() => {
      if (this.state.source === 'audio') this.startTts();
      else this.fail('Không thể bắt đầu đọc. Hãy kiểm tra giọng đọc, kết nối mạng rồi thử lại.');
    }, 12_000);
  }
  private startTts() {
    this.releaseAudio(); this.cancelSpeech(); this.offset = 0;
    this.update({ source: 'tts', status: 'preparing', position: 0, total: this.chunks.length,
      message: 'Audio chưa khả dụng. Đang chuẩn bị giọng đọc…' });
    if (!window.speechSynthesis || !window.SpeechSynthesisUtterance) {
      this.fail('Trình duyệt chưa hỗ trợ đọc văn bản. Bạn vẫn có thể đọc bài thuyết minh.'); return;
    }
    const choose = () => {
      const voices = window.speechSynthesis.getVoices().filter(voice =>
        voice.lang.toLowerCase().split(/[-_]/)[0] === this.guide.languageCode.toLowerCase().split('-')[0]);
      this.voice = voices.find(voice => voice.localService) || voices.find(voice => voice.default) || voices[0];
      if (!this.voice) return false;
      this.clearVoiceWait(); this.clearTimer(); this.speak(0); return true;
    };
    if (choose()) return;
    this.waitingVoices = () => { if (this.state.status === 'preparing') choose(); };
    window.speechSynthesis.addEventListener('voiceschanged', this.waitingVoices);
    this.timer = setTimeout(() => this.fail(`Máy chưa có giọng ${this.guide.languageCode} phù hợp. Hãy cài/bật giọng này hoặc thử trên trình duyệt khác.`), 4_000);
  }
  private speak(index: number, offset = 0) {
    if (!this.voice) { this.startTts(); return; }
    if (!navigator.onLine && !this.voice.localService) {
      this.fail('Giọng này cần kết nối mạng. Hãy kết nối lại hoặc dùng giọng ngoại tuyến cùng ngôn ngữ.'); return;
    }
    this.cancelSpeech(); const generation = this.generation;
    this.offset = offset;
    this.update({ source: 'tts', status: 'preparing', position: index, total: this.chunks.length,
      estimatedDuration: this.estimateDuration(index, offset),
      voice: this.voice.name, message: 'Đang dùng giọng đọc văn bản (TTS).' });
    const utterance = new SpeechSynthesisUtterance(this.chunks[index].slice(offset));
    utterance.voice = this.voice; utterance.lang = this.voice.lang; utterance.rate = this.state.rate;
    utterance.onstart = () => {
      if (generation !== this.generation) return;
      const elapsed = this.state.elapsed;
      const startedAt = performance.now();
      const readClock = () => elapsed + (performance.now() - startedAt) / 1000;
      this.readSpeechClock = readClock;
      this.speechClock = setInterval(() => {
        if (generation === this.generation) this.update({ elapsed: readClock() });
      }, 250);
      this.didStart();
    };
    utterance.onboundary = event => { if (generation === this.generation) this.offset = offset + event.charIndex; };
    utterance.onend = () => {
      if (generation !== this.generation) return;
      this.finishSpeechClock();
      this.offset = 0;
      if (index + 1 < this.chunks.length) this.speak(index + 1);
      else this.update({ status: 'ended', position: index, estimatedDuration: this.state.elapsed });
    };
    utterance.onerror = () => { if (generation === this.generation) this.fail('Giọng đọc không phát được. Hãy kiểm tra giọng/kết nối và bấm Thử lại.'); };
    this.armTimeout(); window.speechSynthesis.speak(utterance);
  }
  private estimateDuration(index: number, offset: number) {
    const current = this.chunkDurations[index]! * (1 - offset / this.chunks[index]!.length);
    const remaining = this.chunkDurations.slice(index + 1).reduce((sum, duration) => sum + duration, current);
    return this.state.elapsed + remaining / this.state.rate;
  }
  private finishSpeechClock() {
    clearInterval(this.speechClock);
    this.speechClock = undefined;
    const elapsed = this.readSpeechClock?.();
    this.readSpeechClock = undefined;
    if (elapsed !== undefined) this.update({ elapsed });
  }
  pause = () => {
    if (!['playing', 'preparing'].includes(this.state.status)) return;
    if (this.state.source === 'audio') { this.clearTimer(); this.audio?.pause(); }
    else this.cancelSpeech();
    this.update({ status: 'paused' });
  };
  stop = () => {
    this.cancelSpeech(); this.releaseAudio(); this.offset = 0;
    this.update({ status: 'idle', source: null, position: 0, total: 0, message: '', voice: '', elapsed: 0, estimatedDuration: undefined });
  };
  private fail(message: string) {
    this.cancelSpeech(); this.releaseAudio(); this.update({ status: 'error', message });
  }
  seek = (position: number) => {
    if (this.state.source === 'audio' && this.audio) {
      this.audio.currentTime = Math.max(0, Math.min(position, this.state.total));
      this.update({ position: this.audio.currentTime });
    } else if (this.state.source === 'tts') {
      const index = Math.max(0, Math.min(Math.floor(position), this.chunks.length - 1));
      const resume = ['playing', 'preparing'].includes(this.state.status);
      this.cancelSpeech(); this.offset = 0;
      this.update({ position: index, estimatedDuration: this.estimateDuration(index, 0) });
      if (resume) this.speak(index);
      else this.update({ status: 'paused' });
    }
  };
  setRate = (rate: number) => {
    this.update({ rate });
    if (this.audio) this.audio.playbackRate = rate;
    else if (this.state.source === 'tts' && ['playing', 'preparing'].includes(this.state.status)) this.speak(this.state.position, this.offset);
    else if (this.state.source === 'tts' && this.state.status === 'paused') {
      this.update({ estimatedDuration: this.estimateDuration(this.state.position, this.offset) });
    }
  };
  networkLost = () => {
    if (this.state.source === 'tts' && this.voice && !this.voice.localService && ['playing', 'preparing'].includes(this.state.status))
      this.fail('Mất kết nối tới giọng đọc. Bài thuyết minh vẫn được giữ lại; hãy kết nối và thử lại.');
  };
}
