"use client";
import { Tool } from "@/app/[locale]/_api/getTools";
import {
  Divider,
  Dropdown,
  DropdownGroup,
  DropdownItem,
  DropdownList,
  MenuSearch,
  MenuSearchInput,
  MenuToggle,
  SearchInput,
} from "@/libs/patternfly/react-core";
import { ThIcon } from "@/libs/patternfly/react-icons";
import { useTranslations } from "next-intl";
import { useRouter, useSelectedLayoutSegment } from "next/navigation";
import { useState } from "react";

export const ApplicationLauncher = ({ tools }: { tools: Tool[] }) => {
  const segment = useSelectedLayoutSegment();
  const t = useTranslations();
  const router = useRouter();

  const [isOpen, setIsOpen] = useState<boolean>(false);
  const [favorites, setFavorites] = useState<string[]>([]);
  const [searchText, setSearchText] = useState<string>("");

  const onToggleClick = () => {
    setIsOpen(!isOpen);
  };

  const toolToDropdownItem = ({ id, url, title }: Tool) => (
    <DropdownItem
      key={id}
      value={id}
      id={id}
      isFavorited={favorites.includes(id)}
      onClick={() => {
        void router.push(url);
        setIsOpen(false);
      }}
    >
      {t(title)}
    </DropdownItem>
  );

  const menuItems = tools
    .filter((tool) => !favorites.includes(tool.id))
    .filter(
      (tool) =>
        searchText === "" ||
        `${t(tool.title)}${t(tool.description)}`
          .toLowerCase()
          .includes(searchText.toLowerCase()),
    )
    .map(toolToDropdownItem);
  const favoriteItems = tools
    .filter((t) => favorites.includes(t.id))
    .map(toolToDropdownItem);

  const onFavorite = (event: any, value: string, actionId: string) => {
    event.stopPropagation();
    if (actionId === "fav") {
      const isFavorite = favorites.includes(value);
      if (isFavorite) {
        setFavorites(favorites.filter((fav) => fav !== value));
      } else {
        setFavorites([...favorites, value]);
      }
      setSearchText("");
    }
  };

  // show the launcher only in child pages
  const isChildPage = segment !== null;

  return (
    isChildPage && (
      <Dropdown
        isOpen={isOpen}
        onOpenChange={(isOpen) => setIsOpen(isOpen)}
        onOpenChangeKeys={["Escape"]}
        toggle={(toggleRef) => (
          <MenuToggle
            aria-label="Toggle"
            ref={toggleRef}
            variant="plain"
            onClick={onToggleClick}
            isExpanded={isOpen}
            style={{ width: "auto" }}
          >
            <ThIcon />
          </MenuToggle>
        )}
        onActionClick={onFavorite}
        onSelect={(_ev, value) => console.log("selected", value)}
      >
        <MenuSearch>
          <MenuSearchInput>
            <SearchInput
              aria-label="Filter menu items"
              value={searchText}
              onChange={(_event, value) => setSearchText(value)}
            />
          </MenuSearchInput>
        </MenuSearch>
        <Divider />
        {favorites.length > 0 && (
          <>
            <DropdownGroup key="favorites-group" label="Favorites">
              <DropdownList>{favoriteItems}</DropdownList>
            </DropdownGroup>
            <Divider key="favorites-divider" />
          </>
        )}
        {menuItems}
      </Dropdown>
    )
  );
};
