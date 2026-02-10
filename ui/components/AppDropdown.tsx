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
  Tooltip,
} from "@/libs/patternfly/react-core";
import { ArrowRightIcon, HomeIcon } from "@/libs/patternfly/react-icons";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { KroxyliciousClusterLabel } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/KroxyliciousClusterLabel";

export type ClusterInfo = {
  projectName: string;
  clusterName: string;
  authenticationMethod: string;
  id: string;
  loginRequired: boolean;
  isVirtual: boolean;
};

export function AppDropdown({
  clusters,
  kafkaId,
  isOidcEnabled,
}: Readonly<{
  readonly clusters: ClusterInfo[];
  readonly kafkaId: string;
  readonly isOidcEnabled?: boolean;
  readonly isVirtualKafkaCluster?: boolean;
}>) {
  const t = useTranslations();
  const router = useRouter();
  const [activeItem, setActiveItem] = useState<string | undefined>(undefined);
  const [searchText, setSearchText] = useState("");
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const toggleRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  const filteredClusters = clusters.filter((cluster) =>
    cluster.clusterName.toLowerCase().includes(searchText.toLowerCase()),
  );

  const groupedClusters = filteredClusters.reduce<
    Record<string, ClusterInfo[]>
  >((acc, cluster) => {
    acc[cluster.projectName] = acc[cluster.projectName] || [];
    acc[cluster.projectName].push(cluster);
    return acc;
  }, {});

  const handleClusterSelect = (
    _event: React.MouseEvent<Element, MouseEvent> | undefined,
    selectedId: string | number | undefined,
  ) => {
    if (selectedId === undefined) return;
    const id = String(selectedId);
    setActiveItem(id);
    setIsMenuOpen(false);
    if (id === kafkaId) return;

    const currentCluster = clusters.find((cluster) => cluster.id === kafkaId);
    if (currentCluster) {
      localStorage.setItem("PreviousClusterId", currentCluster.id);
      localStorage.setItem("PreviousClusterName", currentCluster.clusterName);
    }

    if (currentCluster?.loginRequired) {
      router.push(`/kafka/${id}/login`);
    } else {
      router.push(`/kafka/${id}`);
    }
  };

  const handleSearchInputChange = (value: string) => {
    if (!isMenuOpen) {
      setIsMenuOpen(true);
    }
    setSearchText(value);
  };

  const sortedProjectNames = Object.keys(groupedClusters).sort((a, b) => {
    if (a.toLowerCase() === "not provided") return 1;
    if (b.toLowerCase() === "not provided") return -1;
    if (a === "") return 1;
    if (b === "") return -1;
    return a.localeCompare(b);
  });

  const menuItems = sortedProjectNames.map((projectName) => (
    <MenuGroup
      label={`Project: ${projectName || "not provided"}`}
      key={projectName}
    >
      <MenuList>
        {groupedClusters[projectName].map((cluster) => (
          <MenuItem
            key={cluster.id}
            itemId={cluster.id}
            description={
              !isOidcEnabled
                ? `Authentication: ${cluster.authenticationMethod}`
                : undefined
            }
            isSelected={kafkaId === cluster.id}
          >
            <Flex>
              <FlexItem>
                {cluster.clusterName}
                {cluster.isVirtual && <KroxyliciousClusterLabel />}
              </FlexItem>
              <FlexItem align={{ default: "alignRight" }}>
                {cluster.id !== kafkaId && (
                  <Tooltip content={t("cluster-selector.login-to-cluster")}>
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
            {t("cluster-selector.no-results-found")}
          </MenuItem>
        </MenuList>
      </MenuGroup>,
    );
  }

  const toggle = (
    <MenuToggle
      ref={toggleRef}
      onClick={() => setIsMenuOpen(!isMenuOpen)}
      isExpanded={isMenuOpen}
    >
      <Flex
        alignItems={{ default: "alignItemsCenter" }}
        gap={{ default: "gapMd" }}
      >
        <FlexItem>{t("cluster-selector.kafka-clusters")}</FlexItem>
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
      activeItemId={activeItem}
      isScrollable
    >
      <MenuSearch>
        <MenuSearchInput>
          <SearchInput
            value={searchText}
            aria-label="Search Kafka clusters"
            onChange={(_event, value) => handleSearchInputChange(value)}
          />
        </MenuSearchInput>
      </MenuSearch>
      <MenuContent maxMenuHeight="300px">
        <MenuList>{menuItems}</MenuList>
      </MenuContent>
      <Divider />
      <MenuFooter>
        <Link href={"/"}>
          <HomeIcon /> {t("cluster-selector.view-all-kafka-clusters")}
        </Link>
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
      onOpenChangeKeys={["Escape"]}
    />
  );
}
