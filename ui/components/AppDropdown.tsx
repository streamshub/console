"use client";

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
} from "@/libs/patternfly/react-core";
import { HomeIcon } from "@/libs/patternfly/react-icons";
import { useRouter } from "next/navigation";
import { useRef, useState } from "react";

export type ClusterInfo = {
  projectName: string;
  clusterName: string;
  authenticationMethod: string;
  id: string;
};

export function AppDropdown({
  clusters,
  kafkaId,
}: {
  clusters: ClusterInfo[];
  kafkaId: string;
}) {
  const router = useRouter();
  const [activeItem, setActiveItem] = useState<string | undefined>(undefined);
  const [input, setInput] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const toggleRef = useRef<any>(undefined);
  const menuRef = useRef<any>(undefined);

  const filteredClusters = clusters.filter((cluster) =>
    cluster.clusterName.toLowerCase().includes(input.toLowerCase()),
  );

  const groupedClusters = filteredClusters.reduce<
    Record<string, ClusterInfo[]>
  >((acc, cluster) => {
    if (!acc[cluster.projectName]) {
      acc[cluster.projectName] = [];
    }
    acc[cluster.projectName].push(cluster);
    return acc;
  }, {});

  const onSelect = (
    _event: React.MouseEvent<Element, MouseEvent> | undefined,
    itemId: string | number | undefined,
  ) => {
    if (itemId === undefined) return;
    const id = String(itemId);
    setActiveItem(id);
    setIsOpen(false);
    if (id === kafkaId) {
      return;
    }
    const previousCluster = clusters.find((cluster) => cluster.id === kafkaId);
    if (previousCluster) {
      localStorage.setItem("PreviousClusterId", previousCluster.id);
      localStorage.setItem("PreviousClusterName", previousCluster.clusterName);
    }
    router.push(`/kafka/${id}/login`);
  };

  const handleTextInputChange = (value: string) => {
    if (!isOpen) {
      setIsOpen(true);
    }
    setInput(value);
  };

  const menuListGroups = Object.entries(groupedClusters).map(
    ([projectName, clusters]) => (
      <MenuGroup label={`Project: ${projectName}`} key={projectName}>
        <MenuList>
          {clusters.map((cluster) => {
            return (
              <MenuItem
                key={cluster.id}
                itemId={cluster.id}
                description={`Authentication: ${cluster.authenticationMethod}`}
                isSelected={kafkaId === cluster.id}
              >
                {cluster.clusterName}
              </MenuItem>
            );
          })}
        </MenuList>
      </MenuGroup>
    ),
  );

  if (input && filteredClusters.length === 0) {
    menuListGroups.push(
      <MenuGroup label="Results" key="no-results">
        <MenuList>
          <MenuItem isDisabled key="no-result" itemId={-1}>
            No results found
          </MenuItem>
        </MenuList>
      </MenuGroup>,
    );
  }

  const toggle = (
    <MenuToggle
      ref={toggleRef}
      onClick={() => setIsOpen(!isOpen)}
      isExpanded={isOpen}
    >
      <Flex
        alignItems={{ default: "alignItemsCenter" }}
        gap={{ default: "gapMd" }}
      >
        <FlexItem>Kafka Clusters</FlexItem>
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
      onSelect={onSelect}
      activeItemId={activeItem}
      isScrollable
    >
      <MenuSearch>
        <MenuSearchInput>
          <SearchInput
            value={input}
            aria-label="Search Kafka clusters"
            onChange={(_event, value) => handleTextInputChange(value)}
          />
        </MenuSearchInput>
      </MenuSearch>
      <MenuContent maxMenuHeight="300px">{menuListGroups}</MenuContent>
      <Divider />
      <MenuFooter>
        <HomeIcon /> View all Kafka clusters
      </MenuFooter>
    </Menu>
  );

  return (
    <MenuContainer
      menu={menu}
      menuRef={menuRef}
      toggle={toggle}
      toggleRef={toggleRef}
      isOpen={isOpen}
      onOpenChange={setIsOpen}
      onOpenChangeKeys={["Escape"]}
    />
  );
}
