/**
 * Cluster Connection Context
 * 
 * Provides context for managing the cluster connection details drawer state.
 * Allows components to open/close the drawer and check its expanded state.
 */

import { createContext, useContext } from 'react';

export interface ClusterConnectionContextType {
  open: (clusterId: string) => void;
  close: () => void;
  expanded: boolean;
}

export const ClusterConnectionContext = createContext<ClusterConnectionContextType | null>(null);

export function useClusterConnectionContext() {
  const context = useContext(ClusterConnectionContext);
  if (!context) {
    throw new Error('useClusterConnectionContext must be used within ClusterConnectionProvider');
  }
  return context;
}

export function useOpenClusterConnectionPanel() {
  const { open } = useClusterConnectionContext();
  return open;
}