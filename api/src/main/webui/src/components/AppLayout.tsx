import { Page } from '@patternfly/react-core';
import { PropsWithChildren, ReactNode } from 'react';
import { AppMasthead } from './AppMasthead';

export interface AppLayoutProps {
  showSidebar?: boolean;
  sidebar?: ReactNode;
  breadcrumb?: ReactNode;
  isBreadcrumbWidthLimited?: boolean;
}

export function AppLayout({
  children,
  showSidebar = false,
  sidebar,
  breadcrumb,
  isBreadcrumbWidthLimited,
}: PropsWithChildren<AppLayoutProps>) {
  return (
    <Page
      masthead={<AppMasthead showSidebarToggle={showSidebar} />}
      sidebar={sidebar}
      breadcrumb={breadcrumb}
      isBreadcrumbWidthLimited={isBreadcrumbWidthLimited}
    >
      {children}
    </Page>
  );
}