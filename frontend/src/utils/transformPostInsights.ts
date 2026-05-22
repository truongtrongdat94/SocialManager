export function sumReactions(raw: Record<string, number>): number {
  return Object.values(raw).reduce((acc, v) => acc + v, 0);
}
