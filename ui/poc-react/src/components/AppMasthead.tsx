import {
  Brand,
  Masthead,
  MastheadBrand,
  MastheadContent,
  MastheadLogo,
  MastheadMain,
  MastheadToggle,
  PageToggleButton,
  Toolbar,
  ToolbarContent,
  ToolbarGroup,
  ToolbarItem,
} from '@patternfly/react-core';
import { BarsIcon } from '@patternfly/react-icons';
import { useTranslation } from 'react-i18next';
import { useAppLayout } from './AppLayoutProvider';
import { ThemeSwitcher } from './ThemeSwitcher';
import { useTheme } from './ThemeProvider';

export function AppMasthead({
  showSidebarToggle = false,
}: {
  readonly showSidebarToggle?: boolean;
}) {
  const { t } = useTranslation();
  const { toggleSidebar } = useAppLayout();
  const { isDarkMode } = useTheme();

  return (
    <Masthead>
      <MastheadMain>
        {showSidebarToggle && (
          <MastheadToggle>
            <PageToggleButton
              variant="plain"
              aria-label={t('common.navigation')}
              onClick={toggleSidebar}
            >
              <BarsIcon />
            </PageToggleButton>
          </MastheadToggle>
        )}
        <MastheadBrand>
          <MastheadLogo href="/" target="_self">
            <Brand
              src={isDarkMode ? '/full_logo_hori_reverse.svg' : '/full_logo_hori_default.svg'}
              alt={t('common.title')}
              heights={{ default: '56px' }}
            />
          </MastheadLogo>
        </MastheadBrand>
      </MastheadMain>
      <MastheadContent>
        <Toolbar
          ouiaId="masthead-toolbar"
          id="masthead-toolbar"
          isFullHeight
          isStatic
        >
          <ToolbarContent id="masthead-toolbar">
            <ToolbarGroup
              variant="action-group"
              align={{ default: 'alignEnd' }}
            >
              <ToolbarItem>
                <ThemeSwitcher />
              </ToolbarItem>
            </ToolbarGroup>
          </ToolbarContent>
        </Toolbar>
      </MastheadContent>
    </Masthead>
  );
}