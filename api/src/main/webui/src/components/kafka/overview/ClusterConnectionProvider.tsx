/**
 * Cluster Connection Provider
 * 
 * Provides the cluster connection drawer state management to child components.
 * Manages the open/close state and the currently selected cluster ID.
 */

import { ReactNode, useState } from 'react';
import { ClusterConnectionContext } from './ClusterConnectionContext';

export interface ClusterConnectionProviderProps {
  children: ReactNode;
}

export function ClusterConnectionProvider({ children }: ClusterConnectionProviderProps) {
  const [expanded, setExpanded] = useState(false);

  const open = (_clusterId: string) => {
    setExpanded(true);
  };

  const close = () => {
    setExpanded(false);
  };

  return (
    <ClusterConnectionContext.Provider value={{ open, close, expanded }}>
      {children}
    </ClusterConnectionContext.Provider>
  );
}