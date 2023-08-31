"use client";

import { Avatar, Dropdown, DropdownList, MenuToggle } from '@/libs/patternfly/react-core';
import { useState } from 'react';


export function UserDropdown({ username }: { username: string }) {
  const [isOpen, setIsOpen] = useState(false);
  return (
    <Dropdown
      isOpen={isOpen}
      onOpenChange={(isOpen: boolean) => setIsOpen(isOpen)}
      popperProps={{ position: 'right' }}
      toggle={(toggleRef) => (
        <MenuToggle
          ref={toggleRef}
          onClick={() => setIsOpen(o => !o)}
          isFullHeight
          isExpanded={isOpen}
          icon={<Avatar src={'https://www.patternfly.org/images/668560cd.svg'} alt="" />}
        >
          {username}
        </MenuToggle>
      )}
    >
      <DropdownList></DropdownList>
    </Dropdown>

  )
}
