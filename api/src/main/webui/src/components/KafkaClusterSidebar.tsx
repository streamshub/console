/**
 * Kafka Cluster Sidebar Navigation
 * Provides navigation for individual Kafka cluster pages
 */

import { Nav, NavItem, NavList, PageSidebar, PageSidebarBody } from '@patternfly/react-core';
import { useTranslation } from 'react-i18next';
import { NavLink, useParams, useLocation } from 'react-router-dom';
import { useAppLayout } from './AppLayoutProvider';

interface NavItemConfig {
  id: string;
  label: string;
  path: string;
}

export function KafkaClusterSidebar() {
  const { t } = useTranslation();
  const { kafkaId } = useParams<{ kafkaId: string }>();
  const { sidebarExpanded } = useAppLayout();
  const location = useLocation();

  const navItems: NavItemConfig[] = [
    {
      id: 'overview',
      label: t('kafka.clusterOverview'),
      path: `/kafka/${kafkaId}/overview`,
    },
    {
      id: 'topics',
      label: t('kafka.topics'),
      path: `/kafka/${kafkaId}/topics`,
    },
    {
      id: 'nodes',
      label: t('kafka.nodes'),
      path: `/kafka/${kafkaId}/nodes`,
    },
    {
      id: 'connect',
      label: t('kafka.connect.title'),
      path: `/kafka/${kafkaId}/connect/connectors`,
    },
    {
      id: 'users',
      label: t('kafka.users'),
      path: `/kafka/${kafkaId}/users`,
    },
    {
      id: 'groups',
      label: t('kafka.groups'),
      path: `/kafka/${kafkaId}/groups`,
    },
  ];

  const navigation = (
    <Nav aria-label={t('common.navigation')}>
      <NavList>
        {navItems.map((item) => {
          const isActive = location.pathname.startsWith(item.path);
          return (
            <NavItem key={item.id} itemId={item.id} isActive={isActive}>
              <NavLink to={item.path}>
                {item.label}
              </NavLink>
            </NavItem>
          );
        })}
      </NavList>
    </Nav>
  );

  return (
    <PageSidebar isSidebarOpen={sidebarExpanded}>
      <PageSidebarBody>{navigation}</PageSidebarBody>
    </PageSidebar>
  );
}