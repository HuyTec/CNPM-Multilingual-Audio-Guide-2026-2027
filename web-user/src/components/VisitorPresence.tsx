import { useEffect } from 'react';
import { startVisitorPresence } from '../lib/visitor';

export function VisitorPresence() {
  useEffect(startVisitorPresence, []);
  return null;
}
