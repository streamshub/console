export function timeSeriesMetrics(
  ranges: Record<string, { range: string[][]; nodeId?: string }[]> | undefined,
  rangeName: string,
): Record<string, TimeSeriesMetrics> {
  const series: Record<string, TimeSeriesMetrics> = {};

  if (ranges) {
    Object.values(ranges[rangeName] ?? {}).forEach((r) => {
      series[r.nodeId!] = r.range.reduce(
        (a, v) => ({ ...a, [v[0]]: parseFloat(v[1]) }),
        {} as TimeSeriesMetrics,
      );
    });
  }

  return series;
}
