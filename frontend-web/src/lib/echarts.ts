import { use, registerTheme } from 'echarts/core';
import { GaugeChart, LineChart } from 'echarts/charts';
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  MarkAreaComponent,
  MarkLineComponent,
  DataZoomComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

use([
  GaugeChart,
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  MarkAreaComponent,
  MarkLineComponent,
  DataZoomComponent,
  CanvasRenderer,
]);

const css = (name: string, fallback: string): string => {
  if (typeof window === 'undefined') return fallback;
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  return v || fallback;
};

/** Enregistre le thème ECharts "coffee" dérivé des tokens CSS. À appeler une fois au boot. */
export function registerCoffeeTheme(): void {
  registerTheme('coffee', {
    color: [
      css('--roast-green', '#b7c49a'),
      css('--roast-cinnamon', '#c9a56b'),
      css('--roast-medium', '#a56a3a'),
      css('--roast-dark', '#6e3d24'),
      css('--cherry', '#d2452e'),
    ],
    backgroundColor: 'transparent',
    textStyle: { fontFamily: "'IBM Plex Mono', monospace", color: css('--text-crema', '#ede4da') },
    title: { textStyle: { fontFamily: "'Space Grotesk', sans-serif" } },
    legend: { textStyle: { color: css('--text-muted', '#a89384') } },
    line: { symbol: 'none', smooth: true },
    categoryAxis: {
      axisLine: { lineStyle: { color: css('--border-husk', '#3a2f29') } },
      axisLabel: { color: css('--text-muted', '#a89384') },
      splitLine: { show: false },
    },
    valueAxis: {
      axisLine: { lineStyle: { color: css('--border-husk', '#3a2f29') } },
      axisLabel: { color: css('--text-muted', '#a89384') },
      splitLine: { lineStyle: { color: 'rgba(58,47,41,0.5)' } },
    },
    tooltip: {
      backgroundColor: css('--surface-raised', '#2a211d'),
      borderColor: css('--border-husk', '#3a2f29'),
      textStyle: { color: css('--text-crema', '#ede4da') },
    },
  });
}
