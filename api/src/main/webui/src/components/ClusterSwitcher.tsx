/**
 * Cluster Switcher Component
 * Allows users to switch between different Kafka clusters
 */

import {
  Badge,
  Divider,
  Flex,
  FlexItem,
  Menu,
  MenuContainer,
  MenuContent,
  MenuFooter,
  MenuGroup,
  MenuItem,
  MenuList,
  MenuSearch,
  MenuSearchInput,
  MenuToggle,
  SearchInput,
  Tooltip,
} from '@patternfly/react-core';
import { ArrowRightIcon, HomeIcon } from '@patternfly/react-icons';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useRef, useState } from 'react';
import { KafkaCluster } from '../api/types';

export interface ClusterSwitcherProps {
  clusters: KafkaCluster[];
  currentClusterId: string;
}

export function ClusterSwitcher({
  clusters,
  currentClusterId,
}: ClusterSwitcherProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchText, setSearchText] = useState('');
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const toggleRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  const filteredClusters = clusters.filter((cluster) =>
    cluster.attributes.name.toLowerCase().includes(searchText.toLowerCase())
  );

  // Group clusters by namespace
  const groupedClusters = filteredClusters.reduce<
    Record<string, KafkaCluster[]>
  >((acc, cluster) => {
    const namespace = cluster.attributes.namespace || 'Not provided';
    acc[namespace] = acc[namespace] || [];
    acc[namespace].push(cluster);
    return acc;
  }, {});

  const handleClusterSelect = (
    _event: React.MouseEvent<Element, MouseEvent> | undefined,
    selectedId: string | number | undefined
  ) => {
    if (selectedId === undefined) return;
    const id = String(selectedId);
    setIsMenuOpen(false);
    
    if (id === currentClusterId) return;

    // Navigate to the selected cluster
    navigate(`/kafka/${id}`);
  };

  const handleSearchInputChange = (value: string) => {
    if (!isMenuOpen) {
      setIsMenuOpen(true);
    }
    setSearchText(value);
  };

  // Sort namespaces, putting "Not provided" at the end
  const sortedNamespaces = Object.keys(groupedClusters).sort((a, b) => {
    if (a.toLowerCase() === 'not provided') return 1;
    if (b.toLowerCase() === 'not provided') return -1;
    if (a === '') return 1;
    if (b === '') return -1;
    return a.localeCompare(b);
  });

  const menuItems = sortedNamespaces.map((namespace) => (
    <MenuGroup
      label={`${t('kafka.namespace')}: ${namespace}`}
      key={namespace}
    >
      <MenuList>
        {groupedClusters[namespace].map((cluster) => (
          <MenuItem
            key={cluster.id}
            itemId={cluster.id}
            isSelected={currentClusterId === cluster.id}
          >
            <Flex>
              <FlexItem>{cluster.attributes.name}</FlexItem>
              <FlexItem align={{ default: 'alignRight' }}>
                {cluster.id !== currentClusterId && (
                  <Tooltip content={t('kafka.switchToCluster')}>
                    <ArrowRightIcon />
                  </Tooltip>
                )}
              </FlexItem>
            </Flex>
          </MenuItem>
        ))}
      </MenuList>
    </MenuGroup>
  ));

  if (searchText && filteredClusters.length === 0) {
    menuItems.push(
      <MenuGroup label="Results" key="no-results">
        <MenuList>
          <MenuItem isDisabled key="no-result" itemId={-1}>
            {t('kafka.noResultsFound')}
          </MenuItem>
        </MenuList>
      </MenuGroup>
    );
  }

  const toggle = (
    <MenuToggle
      ref={toggleRef}
      onClick={() => setIsMenuOpen(!isMenuOpen)}
      isExpanded={isMenuOpen}
    >
      <Flex
        alignItems={{ default: 'alignItemsCenter' }}
        gap={{ default: 'gapMd' }}
      >
        <FlexItem>{t('kafka.selectCluster')}</FlexItem>
        <FlexItem>
          <Badge isRead key="badge">
            {clusters.length}
          </Badge>
        </FlexItem>
      </Flex>
    </MenuToggle>
  );

  const menu = (
    <Menu
      ref={menuRef}
      onSelect={handleClusterSelect}
      isScrollable
    >
      <MenuSearch>
        <MenuSearchInput>
          <SearchInput
            value={searchText}
            aria-label={t('kafka.filterByName')}
            onChange={(_event, value) => handleSearchInputChange(value)}
          />
        </MenuSearchInput>
      </MenuSearch>
      <MenuContent maxMenuHeight="300px">
        <MenuList>{menuItems}</MenuList>
      </MenuContent>
      <Divider />
      <MenuFooter>
        <a href="/" onClick={(e) => {
          e.preventDefault();
          navigate('/');
        }}>
          <HomeIcon /> {t('kafka.viewAllClusters')}
        </a>
      </MenuFooter>
    </Menu>
  );

  return (
    <MenuContainer
      menu={menu}
      menuRef={menuRef}
      toggle={toggle}
      toggleRef={toggleRef}
      isOpen={isMenuOpen}
      onOpenChange={setIsMenuOpen}
      onOpenChangeKeys={['Escape']}
    />
  );
}