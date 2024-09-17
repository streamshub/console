import {
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  MenuToggleAction,
  MenuToggleElement,
} from "@/libs/patternfly/react-core";
import { CopyIcon, PlayIcon } from "@patternfly/react-icons";
import { useTranslations } from "next-intl";
import { useState } from "react";

export function DryrunSelect({
  openDryrun,
  cliCommand,
}: {
  openDryrun: () => void;
  cliCommand: string;
}) {
  const t = useTranslations("ConsumerGroupsTable");

  const [isOpen, setIsOpen] = useState(false);

  const onToggleClick = () => {
    setIsOpen((prevIsOpen) => !prevIsOpen);
  };

  return (
    <Dropdown
      isOpen={isOpen}
      toggle={(toggleRef: React.Ref<MenuToggleElement>) => (
        <MenuToggle
          ref={toggleRef}
          variant="secondary"
          splitButtonOptions={{
            variant: "action",
            items: [
              <MenuToggleAction
                id="split-button-action-secondary-with-toggle-button"
                key="split-action-secondary"
                aria-label={t("dry_run")}
              >
                {t("dry_run")}
              </MenuToggleAction>,
            ],
          }}
          aria-label="dryrun toggle button"
          onClick={onToggleClick}
        />
      )}
    >
      <DropdownList>
        <DropdownItem
          value={0}
          key={t("run_and_show_result")}
          onClick={openDryrun}
        >
          <PlayIcon /> {t("run_and_show_result")}
          {cliCommand}
        </DropdownItem>
        <DropdownItem
          value={1}
          key={t("copy_dry_run_command")}
          onClick={() => {
            navigator.clipboard.writeText(cliCommand);
          }}
        >
          <CopyIcon /> {t("copy_dry_run_command")}
        </DropdownItem>
      </DropdownList>
    </Dropdown>
  );
}
