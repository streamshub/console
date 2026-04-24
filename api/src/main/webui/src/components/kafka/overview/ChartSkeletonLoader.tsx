/**
 * Chart Skeleton Loader Component
 * 
 * Displays a loading skeleton for charts while data is being fetched.
 */

import { Skeleton } from '@patternfly/react-core';

export function ChartSkeletonLoader() {
  return (
    <div style={{ padding: '20px 0' }}>
      <Skeleton height="200px" width="100%" />
      <div style={{ marginTop: '10px', display: 'flex', gap: '10px', justifyContent: 'center' }}>
        <Skeleton height="20px" width="100px" />
        <Skeleton height="20px" width="100px" />
        <Skeleton height="20px" width="100px" />
      </div>
    </div>
  );
}