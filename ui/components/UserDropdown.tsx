'use client'

import {
  Avatar,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  ToolbarItem,
} from '@/libs/patternfly/react-core'
import React, { useState } from 'react'
import { handleLogout } from '@/utils/logout'

function UserToggle(
  username: string | null | undefined,
  picture: string | null | undefined,
  isOpen: boolean,
  setIsOpen: React.Dispatch<React.SetStateAction<boolean>>,
  toggleRef: React.RefObject<any>,
) {
  return (
    <MenuToggle
      ouiaId={'user-dropdown-toggle'}
      ref={toggleRef}
      onClick={() => setIsOpen((o) => !o)}
      isFullHeight
      isExpanded={isOpen}
      icon={
        <Avatar src={picture ?? '/avatar_img.svg'} alt={username ?? 'User'} />
      }
    >
      {username ?? 'User'}
    </MenuToggle>
  )
}

export function UserDropdown({
  username,
  picture,
}: {
  username: string | null | undefined
  picture: string | null | undefined
}) {
  const [isOpen, setIsOpen] = useState(false)

  return (
    <ToolbarItem>
      <Dropdown
        isOpen={isOpen}
        onOpenChange={(isOpen: boolean) => setIsOpen(isOpen)}
        popperProps={{ position: 'right' }}
        toggle={(toggleRef) =>
          UserToggle(username, picture, isOpen, setIsOpen, toggleRef)
        }
      >
        <DropdownList>
          <DropdownItem onClick={handleLogout}>Logout</DropdownItem>
        </DropdownList>
      </Dropdown>
    </ToolbarItem>
  )
}
