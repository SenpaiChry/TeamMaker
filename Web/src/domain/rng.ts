/**
 * Generatore pseudocasuale deterministico (mulberry32).
 *
 * Il Java usa `new Random()` senza seme: va benissimo in produzione, ma rende
 * i test non riproducibili. Qui il seme è esplicito, così la ricerca delle
 * squadre si può testare in modo deterministico e ogni worker può partire da
 * un seme diverso.
 */

export interface Rng {
  /** Intero in [0, max). */
  nextInt(max: number): number
}

export function createRng(seed: number): Rng {
  let state = seed >>> 0

  const nextFloat = (): number => {
    state = (state + 0x6d2b79f5) >>> 0
    let t = state
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }

  return {
    nextInt(max: number): number {
      if (max <= 0) return 0
      return Math.floor(nextFloat() * max)
    },
  }
}
