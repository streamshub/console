"use client";
import { setContextPrincipal } from "@/api/setContextPrincipal";
import {
  Button,
  Divider,
  Dropdown,
  DropdownGroup,
  DropdownItem,
  Label,
  MenuFooter,
  MenuSearch,
  MenuSearchInput,
  MenuToggle,
  SearchInput,
} from "@/libs/patternfly/react-core";
import groupBy from "lodash.groupby";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { useState } from "react";

export const PrincipalSelector = ({
  selected,
  principals,
}: {
  selected: Principal | undefined;
  principals: Principal[];
}) => {
  const t = useTranslations("principals");
  const [isOpen, setIsOpen] = useState<boolean>(false);
  const [searchText, setSearchText] = useState<string>("");

  const onToggleClick = () => {
    setIsOpen(!isOpen);
  };

  const principalToDropdownItem = (p: Principal) => (
    <DropdownItem
      key={p.id}
      value={p.id}
      id={p.id}
      onClick={() => {
        setIsOpen(false);
      }}
      description={p.description}
    >
      {p.name}
    </DropdownItem>
  );

  const groupedPrincipals = groupBy(principals, ({ cluster }) => cluster);
  const menuItems = Object.entries(groupedPrincipals).map(
    ([cluster, principals]) => (
      <DropdownGroup key={cluster} label={cluster}>
        {principals
          .filter(
            (p) =>
              searchText === "" ||
              `${p.name}${p.description}`
                .toLowerCase()
                .includes(searchText.toLowerCase()),
          )
          .map(principalToDropdownItem)}
      </DropdownGroup>
    ),
  );
  return (
    <Dropdown
      isOpen={isOpen}
      onOpenChange={(isOpen) => setIsOpen(isOpen)}
      onOpenChangeKeys={["Escape"]}
      toggle={(toggleRef) => (
        <MenuToggle
          aria-label="Toggle"
          ref={toggleRef}
          onClick={onToggleClick}
          isExpanded={isOpen}
          style={{ width: "auto" }}
          variant={"plainText"}
        >
          {selected ? (
            <>
              {selected.name}{" "}
              <Label variant={"outline"} isCompact={true}>
                {selected.cluster}
              </Label>
            </>
          ) : (
            "Select a Principal"
          )}
        </MenuToggle>
      )}
      onSelect={(_ev, value) => {
        if (typeof value === "string") {
          void setContextPrincipal(value);
        }
      }}
      selected={selected?.id}
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

      {menuItems}

      <MenuFooter>
        <Button
          variant={"link"}
          isInline={true}
          component={() => (
            <Link onClick={onToggleClick} href={"/principals"}>
              {t("manage-button")}
            </Link>
          )}
        />
      </MenuFooter>
    </Dropdown>
  );
};
