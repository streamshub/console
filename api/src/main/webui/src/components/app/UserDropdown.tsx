import {
  Avatar,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  ToolbarItem,
} from '@patternfly/react-core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

interface UserDropdownProps {
  readonly username?: string;
  readonly picture?: string;
}

export function UserDropdown({ username, picture }: UserDropdownProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);

  const displayName = username ?? t('user.anonymous');

  const onToggle = () => {
    setIsOpen(!isOpen);
  };

  const onSelect = () => {
    setIsOpen(false);
  };

  return (
    <ToolbarItem>
      <Dropdown
        isOpen={isOpen}
        onSelect={onSelect}
        onOpenChange={setIsOpen}
        popperProps={{ position: 'right' }}
        toggle={(toggleRef) => (
          <MenuToggle
            ref={toggleRef}
            onClick={onToggle}
            isFullHeight
            isExpanded={isOpen}
            icon={
              <Avatar
                src={picture ?? '/avatar_img.svg'}
                size='sm'
                alt={displayName}
              />
            }
          >
            {displayName}
          </MenuToggle>
        )}
      >
        <DropdownList>
          <DropdownItem key="logout" isDisabled>
            {t('user.logout')}
          </DropdownItem>
        </DropdownList>
      </Dropdown>
    </ToolbarItem>
  );
}