/**
 * Type definitions for Overview charts
 */

export enum DurationOptions {
  Last5minutes = 5,
  Last15minutes = 15,
  Last30minutes = 30,
  Last1hour = 60,
  Last3hours = 180,
  Last6hours = 360,
  Last12hours = 720,
  Last24hours = 1440,
  Last2days = 2880,
  Last7days = 10080,
}

export type TimeSeriesMetrics = Record<string, number>;

export interface ChartDatum {
  x: number;
  y: number;
  name: string;
  value?: number;
}