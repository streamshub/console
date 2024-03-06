import { LimitSelector } from "@/components/MessagesTable/components/LimitSelector";
import { Dropdown, DropdownItem, MenuToggle } from "@patternfly/react-core";
import { useState } from "react";

type Category = "limit" | "live";
export type UntilGroupProps = {
  limit?: number | "continuously";
  onLimitChange: (value: number | undefined) => void;
  onLive: (enabled: boolean) => void;
};

export function UntilGroup({
  limit = 50,
  onLimitChange,
  onLive,
}: UntilGroupProps) {
  const labels: { [K in Category]: string } = {
    limit: "Number of messages",
    live: "Continuously",
  };
  const [category, setCategory] = useState<Category>(
    limit === "continuously" ? "live" : "limit",
  );
  const [isCategoryMenuOpen, setIsCategoryMenuOpen] = useState(false);
  const [isLimitMenuOpen, setIsLimitMenuOpen] = useState(false);

  function handleLimit() {
    setCategory("limit");
    setIsCategoryMenuOpen(false);
    onLimitChange(50);
    onLive(false);
  }

  function handleLive() {
    setCategory("live");
    setIsCategoryMenuOpen(false);
    onLimitChange(undefined);
    onLive(true);
  }

  return (
    <>
      <Dropdown
        data-testid={"until-group"}
        toggle={(toggleRef) => (
          <MenuToggle
            onClick={() => {
              setIsCategoryMenuOpen(true);
            }}
            isExpanded={isCategoryMenuOpen}
            data-testid={"until-group-toggle"}
            ref={toggleRef}
          >
            {labels[category]}
          </MenuToggle>
        )}
        isOpen={isCategoryMenuOpen}
        onOpenChange={() => {
          setIsCategoryMenuOpen((v) => !v);
        }}
      >
        <DropdownItem onClick={handleLimit}>{labels["limit"]}</DropdownItem>
        <DropdownItem onClick={handleLive}>{labels["live"]}</DropdownItem>
      </Dropdown>
      {category === "limit" && (
        <LimitSelector
          value={limit !== "continuously" ? limit : 50}
          onChange={onLimitChange}
        />
      )}
    </>
  );
}
