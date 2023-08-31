"use client";

import {
  Avatar,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import { signOut } from "next-auth/react";
import { useState } from "react";

export function UserDropdown({ username }: { username: string }) {
  const [isOpen, setIsOpen] = useState(false);
  return (
    <ToolbarItem>
      <Dropdown
        isOpen={isOpen}
        onOpenChange={(isOpen: boolean) => setIsOpen(isOpen)}
        popperProps={{ position: "right" }}
        toggle={(toggleRef) => (
          <MenuToggle
            ref={toggleRef}
            onClick={() => setIsOpen((o) => !o)}
            isFullHeight
            isExpanded={isOpen}
            icon={
              <Avatar
                src={"https://www.patternfly.org/images/668560cd.svg"}
                alt=""
              />
            }
          >
            {username}
          </MenuToggle>
        )}
      >
        <DropdownList>
          <DropdownItem onClick={() => signOut()}>Logout</DropdownItem>
        </DropdownList>
      </Dropdown>
    </ToolbarItem>
  );
}
