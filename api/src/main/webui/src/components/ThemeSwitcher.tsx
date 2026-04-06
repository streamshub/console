import {
  Select,
  SelectGroup,
  SelectList,
  SelectOption,
  MenuToggle,
  Icon,
} from '@patternfly/react-core';
import { MoonIcon } from '@patternfly/react-icons';
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import { useTheme } from './ThemeProvider';

const SunIcon = (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 32 32"
    className="pf-v6-svg"
    fill="var(--pf-t--global--icon--color--regular)"
  >
    <path d="M16 25c-4.963 0-9-4.038-9-9s4.037-9 9-9 9 4.038 9 9-4.037 9-9 9Zm0-16c-3.86 0-7 3.14-7 7s3.14 7 7 7 7-3.14 7-7-3.14-7-7-7Zm0-4a1 1 0 0 1-1-1V1a1 1 0 1 1 2 0v3a1 1 0 0 1-1 1Zm0 27a1 1 0 0 1-1-1v-3a1 1 0 1 1 2 0v3a1 1 0 0 1-1 1ZM4 17H1a1 1 0 1 1 0-2h3a1 1 0 1 1 0 2Zm27 0h-3a1 1 0 1 1 0-2h3a1 1 0 1 1 0 2ZM5.394 27.606a1 1 0 0 1-.707-1.707l2.12-2.12a1 1 0 1 1 1.415 1.413L6.1 27.313a.997.997 0 0 1-.707.293ZM24.485 8.515a1 1 0 0 1-.707-1.707L25.9 4.686a1 1 0 1 1 1.415 1.415l-2.122 2.12a.997.997 0 0 1-.707.294Zm-16.97 0a.997.997 0 0 1-.707-.293L4.686 6.1a1 1 0 1 1 1.415-1.415l2.12 2.122a1 1 0 0 1-.706 1.707Zm19.091 19.091a.997.997 0 0 1-.707-.293l-2.12-2.12a1 1 0 1 1 1.413-1.415l2.122 2.121a1 1 0 0 1-.707 1.707Z"></path>
  </svg>
);

const DesktopIcon = (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 32 32"
    className="pf-v6-svg"
    fill="var(--pf-t--global--icon--color--regular)"
  >
    <path d="M23.94 16a1 1 0 0 1-.992-.876 6.957 6.957 0 0 0-6.069-6.062 1 1 0 1 1 .242-1.985 8.953 8.953 0 0 1 7.812 7.8A.999.999 0 0 1 23.94 16ZM16 5a1 1 0 0 1-1-1V1a1 1 0 1 1 2 0v3a1 1 0 0 1-1 1Zm0 27a1 1 0 0 1-1-1v-3a1 1 0 1 1 2 0v3a1 1 0 0 1-1 1ZM4 17H1a1 1 0 1 1 0-2h3a1 1 0 1 1 0 2Zm27 0h-3a1 1 0 1 1 0-2h3a1 1 0 1 1 0 2ZM5.394 27.606a1 1 0 0 1-.707-1.707l2.12-2.12a1 1 0 1 1 1.415 1.413L6.1 27.313a.997.997 0 0 1-.707.293ZM24.485 8.515a1 1 0 0 1-.707-1.707L25.9 4.686a1 1 0 1 1 1.415 1.415l-2.122 2.12a.997.997 0 0 1-.707.294Zm-16.97 0a.997.997 0 0 1-.707-.293L4.686 6.1a1 1 0 1 1 1.415-1.415l2.12 2.122a1 1 0 0 1-.706 1.707Zm19.091 19.091a.997.997 0 0 1-.707-.293l-2.12-2.12a1 1 0 1 1 1.413-1.415l2.122 2.121a1 1 0 0 1-.707 1.707ZM16 24.875c-4.894 0-8.875-3.981-8.875-8.875a8.879 8.879 0 0 1 5.227-8.088.876.876 0 0 1 1.153 1.163 6.945 6.945 0 0 0-.63 2.925A7.133 7.133 0 0 0 20 19.125a6.948 6.948 0 0 0 2.925-.63.876.876 0 0 1 1.163 1.154A8.88 8.88 0 0 1 16 24.875Zm-4.785-14.153A7.135 7.135 0 0 0 8.875 16 7.133 7.133 0 0 0 16 23.125a7.13 7.13 0 0 0 5.278-2.34c-.419.06-.845.09-1.278.09-4.894 0-8.875-3.981-8.875-8.875 0-.433.03-.86.09-1.278Z"></path>
  </svg>
);

export function ThemeSwitcher() {
  const { t } = useTranslation();
  const { mode, setMode } = useTheme();
  const [isOpen, setIsOpen] = useState(false);

  const getThemeIcon = (themeMode: string) => {
    switch (themeMode) {
      case 'light':
        return SunIcon;
      case 'dark':
        return <MoonIcon />;
      default:
        return DesktopIcon;
    }
  };

  const getThemeLabel = (themeMode: string) => {
    switch (themeMode) {
      case 'light':
        return t('theme.light', 'Light');
      case 'dark':
        return t('theme.dark', 'Dark');
      default:
        return t('theme.system', 'System');
    }
  };

  return (
    <Select
      id="theme-select"
      isOpen={isOpen}
      selected={mode}
      onSelect={(_e, selected) => {
        setMode(selected as 'system' | 'light' | 'dark');
        setIsOpen(false);
      }}
      onOpenChange={setIsOpen}
      popperProps={{
        position: 'right',
        enableFlip: true,
      }}
      toggle={(toggleRef) => (
        <MenuToggle
          ref={toggleRef}
          onClick={() => setIsOpen(!isOpen)}
          isExpanded={isOpen}
          icon={<Icon size="lg">{getThemeIcon(mode)}</Icon>}
          aria-label={`${t('theme.switchTheme', 'Theme selection')}, ${t('theme.current', 'current')}: ${getThemeLabel(mode)}`}
        />
      )}
    >
      <SelectGroup>
        <SelectList aria-label={t('theme.switchTheme', 'Theme switcher')}>
          <SelectOption
            value="system"
            icon={DesktopIcon}
            description={t('theme.systemDescription', 'Follow system preference')}
          >
            {t('theme.system', 'System')}
          </SelectOption>
          <SelectOption
            value="light"
            icon={SunIcon}
            description={t('theme.lightDescription', 'Always use light mode')}
          >
            {t('theme.light', 'Light')}
          </SelectOption>
          <SelectOption
            value="dark"
            icon={<MoonIcon />}
            description={t('theme.darkDescription', 'Always use dark mode')}
          >
            {t('theme.dark', 'Dark')}
          </SelectOption>
        </SelectList>
      </SelectGroup>
    </Select>
  );
}