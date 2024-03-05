import { Dropdown, DropdownItem, MenuToggle } from "@patternfly/react-core";
import { useState } from "react";

type Category = "limit" | "live";
export type UntilGroupProps = {
  limit: number | undefined;
  live: boolean | undefined;
  onLimitChange: (value: number | undefined) => void;
  onLiveChange: (enabled: boolean) => void;
};

export function UntilGroup({
  limit = 50,
  live,
  onLimitChange,
  onLiveChange,
}: UntilGroupProps) {
  const labels: { [K in Category]: string } = {
    limit: "Number of messages",
    live: "Live mode (consume messages forever)",
  };
  const [category, setCategory] = useState<Category>(live ? "live" : "limit");
  const [isCategoryMenuOpen, setIsCategoryMenuOpen] = useState(false);
  const [isLimitMenuOpen, setIsLimitMenuOpen] = useState(false);

  function handleLimit() {
    setCategory("limit");
    setIsCategoryMenuOpen(false);
    onLimitChange(50);
    onLiveChange(false);
  }

  function handleLive() {
    setCategory("live");
    setIsCategoryMenuOpen(false);
    onLimitChange(undefined);
    onLiveChange(true);
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
        <Dropdown
          data-testid={"limit-value"}
          toggle={(toggleRef) => (
            <MenuToggle
              onClick={() => {
                setIsLimitMenuOpen(true);
              }}
              isExpanded={isLimitMenuOpen}
              data-testid={"limit-menu-toggle"}
              ref={toggleRef}
            >
              {limit}
            </MenuToggle>
          )}
          isOpen={isLimitMenuOpen}
          onOpenChange={() => {
            setIsLimitMenuOpen((v) => !v);
          }}
        >
          {[10, 20, 50, 100].map((v) => (
            <DropdownItem
              key={`limit-${v}`}
              onClick={() => {
                onLimitChange(v);
                setIsLimitMenuOpen(false);
              }}
            >
              {v}
            </DropdownItem>
          ))}
        </Dropdown>
      )}
    </>
  );
}
