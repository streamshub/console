/**
 * Cluster Connection Drawer Component
 * 
 * Provides a resizable drawer panel that displays cluster connection details.
 * The drawer slides in from the right side and overlays the main content.
 */

import { ReactNode, Suspense } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Drawer,
  DrawerContent,
  DrawerContentBody,
  DrawerPanelContent,
  DrawerHead,
  DrawerActions,
  DrawerCloseButton,
  Title,
  Divider,
  Skeleton,
} from '@patternfly/react-core';
import { useClusterConnectionContext } from './ClusterConnectionContext';
import { ClusterConnectionDetails } from './ClusterConnectionDetails';
import { KafkaCluster } from '@/api/types';

export interface ClusterConnectionDrawerProps {
  children: ReactNode;
  cluster?: KafkaCluster;
  showLearning?: boolean;
}

export function ClusterConnectionDrawer({
  children,
  cluster,
  showLearning = false,
}: ClusterConnectionDrawerProps) {
  const { t } = useTranslation();
  const { expanded, close } = useClusterConnectionContext();

  return (
    <Drawer isExpanded={expanded}>
      <DrawerContent
        panelContent={
          <DrawerPanelContent isResizable>
            <DrawerHead>
              <Title headingLevel="h3">
                {t('ClusterConnectionDetails.title')}
              </Title>
              <DrawerActions>
                <DrawerCloseButton onClick={close} />
              </DrawerActions>
            </DrawerHead>
            <Divider />
            <Suspense
              fallback={
                <div className="pf-v6-u-p-lg">
                  <Skeleton width="80%" className="pf-v6-u-my-md" />
                  <Skeleton width="50%" className="pf-v6-u-my-md" />
                  <Skeleton width="60%" className="pf-v6-u-my-md" />
                  <Skeleton width="30%" className="pf-v6-u-my-md" />
                  <Skeleton width="80%" className="pf-v6-u-my-md" />
                  <Skeleton width="50%" className="pf-v6-u-my-md" />
                  <Skeleton width="60%" className="pf-v6-u-my-md" />
                  <Skeleton width="30%" className="pf-v6-u-my-md" />
                  <Skeleton width="80%" className="pf-v6-u-my-md" />
                </div>
              }
            >
              {cluster && (
                <ClusterConnectionDetails
                  cluster={cluster}
                  showLearning={showLearning}
                />
              )}
            </Suspense>
          </DrawerPanelContent>
        }
      >
        <DrawerContentBody className="pf-v6-u-display-flex pf-v6-u-flex-direction-column">
          {children}
        </DrawerContentBody>
      </DrawerContent>
    </Drawer>
  );
}