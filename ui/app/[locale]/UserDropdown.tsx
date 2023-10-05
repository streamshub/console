"use client";

import {
  Avatar,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  ToolbarItem,
  Divider,
} from "@/libs/patternfly/react-core";
import { signOut } from "next-auth/react";
import { useState } from "react";

export function UserDropdown({
  username,
  picture,
}: {
  username: string | undefined;
  picture: string | null | undefined;
}) {
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
                src={
                  picture || "https://www.patternfly.org/images/668560cd.svg"
                }
                alt={username || "User"}
              />
            }
          >
            {username || "User"}
          </MenuToggle>
        )}
      >
        <DropdownList>
          <DropdownItem>
            <a href={process.env.NEXT_PUBLIC_KEYCLOAK_URL + "/account/"}>
              Manage account
            </a>
          </DropdownItem>
          <Divider component="li" key="separator" />
          <DropdownItem onClick={() => signOut({ callbackUrl: "/" })}>
            Logout
          </DropdownItem>
        </DropdownList>
      </Dropdown>
    </ToolbarItem>
  );
}
