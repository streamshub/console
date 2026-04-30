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
  readonly username: string;
  readonly anonymous: boolean;
  readonly picture?: string;
}

export function UserDropdown({ username, anonymous, picture }: UserDropdownProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const logoutDisabled = anonymous;

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
                alt={username}
              />
            }
          >
            {username}
          </MenuToggle>
        )}
      >
        <DropdownList>
          <DropdownItem key="logout" inert={logoutDisabled} isDisabled={logoutDisabled} onClick={() => {
              window.location.href = `/api/session/logout?redirect_uri=${encodeURIComponent('/')}`;
            }}>
            {t('user.logout')}
          </DropdownItem>
        </DropdownList>
      </Dropdown>
    </ToolbarItem>
  );
}
