import type { EChartsOption } from 'echarts';

function cssVar(name: string, fallback: string): string {
  if (typeof window === 'undefined') return fallback;
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
}

export interface RoastInput {
  valeur: number;
  ideale: number;
  tolerance: number;
}

/** Écart normalisé : 0 = idéal, ±1 = bord de tolérance, ±2 = seuil critique. */
export function ecartNormalise({ valeur, ideale, tolerance }: RoastInput): number {
  if (tolerance <= 0) return 0;
  return (valeur - ideale) / tolerance;
}

/** Couleur le long de la rampe de torréfaction selon l'écart. */
export function roastColor(input: RoastInput): string {
  const e = Math.abs(ecartNormalise(input));
  if (e <= 1) return cssVar('--crema-gold', '#e8c89a'); // bande idéale
  if (e <= 2) return cssVar('--roast-cinnamon', '#c9a56b'); // warning
  return cssVar('--cherry', '#d2452e'); // critique
}

/** Niveau lisible dérivé de l'écart (cohérent avec NiveauAlerte). */
export function roastNiveau(input: RoastInput): 'INFO' | 'WARNING' | 'CRITIQUE' {
  const e = Math.abs(ecartNormalise(input));
  if (e <= 1) return 'INFO';
  if (e <= 2) return 'WARNING';
  return 'CRITIQUE';
}

/** Option ECharts pour la Roast Gauge (signature visuelle). */
export function gaugeOption(input: RoastInput, unite: string, animate = true): EChartsOption {
  const { valeur, ideale, tolerance } = input;
  const min = ideale - 3 * tolerance;
  const max = ideale + 3 * tolerance;

  const green = cssVar('--roast-green', '#b7c49a');
  const gold = cssVar('--crema-gold', '#e8c89a');
  const cinnamon = cssVar('--roast-cinnamon', '#c9a56b');
  const cherry = cssVar('--cherry', '#d2452e');
  const text = cssVar('--text-crema', '#ede4da');
  const muted = cssVar('--text-muted', '#a89384');

  return {
    series: [
      {
        type: 'gauge',
        min,
        max,
        center: ['50%', '58%'],
        radius: '92%',
        startAngle: 210,
        endAngle: -30,
        animation: animate,
        progress: { show: false },
        axisLine: {
          lineStyle: {
            width: 14,
            color: [
              [0.333, green], // froid / sous-optimal
              [0.667, gold], // bande idéale "juste torréfié"
              [0.833, cinnamon], // warning
              [1, cherry], // surchauffe / critique
            ],
          },
        },
        pointer: {
          itemStyle: { color: roastColor(input) },
          width: 5,
          length: '62%',
        },
        anchor: { show: true, size: 10, itemStyle: { color: text } },
        axisTick: { distance: -14, length: 4, lineStyle: { color: muted } },
        splitLine: { distance: -14, length: 14, lineStyle: { color: muted } },
        axisLabel: { distance: 18, color: muted, fontSize: 9 },
        detail: {
          valueAnimation: animate,
          formatter: (v: number) => `${v.toFixed(1)}${unite}`,
          color: text,
          fontFamily: "'IBM Plex Mono', monospace",
          fontSize: 22,
          offsetCenter: [0, '38%'],
        },
        data: [{ value: valeur }],
      },
    ],
  };
}
