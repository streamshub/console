import {
  AboutModal,
  Brand,
  Button,
  Content,
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
import { BarsIcon, InfoCircleIcon, QuestionCircleIcon } from '@patternfly/react-icons';
import { FeedbackModal } from '@patternfly/react-user-feedback';
import '@patternfly/react-user-feedback/dist/esm/Feedback/Feedback.css';
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import { useAppLayout } from '@/components/app/AppLayoutProvider';
import { ThemeSwitcher } from './ThemeSwitcher';
import { useTheme } from './ThemeProvider';
import { ClusterSwitcher } from '@/components/kafka/ClusterSwitcher';
import { KafkaCluster } from '@/api/types';
import { useMetadata } from '@/api/hooks/useMetadata';
import { UserDropdown } from './UserDropdown';
import { RefreshAllButton } from './RefreshAllButton';

export interface AppMastheadProps {
  readonly showSidebarToggle?: boolean;
  readonly clusters?: KafkaCluster[];
  readonly currentClusterId?: string;
  readonly staticRefresh?: Date; // allows fixed value to be provided for storybook testing
}

export function AppMasthead({
  showSidebarToggle = false,
  clusters,
  currentClusterId,
  staticRefresh,
}: AppMastheadProps) {
  const { t } = useTranslation();
  const { toggleSidebar } = useAppLayout();
  const { brandLogo } = useTheme();
  const { data: metadataResponse } = useMetadata();
  const [isAboutModalOpen, setIsAboutModalOpen] = useState(false);
  const [isFeedbackModalOpen, setIsFeedbackModalOpen] = useState(false);

  const metadata = metadataResponse?.data;

  const toggleAboutModal = () => {
    setIsAboutModalOpen(!isAboutModalOpen);
  };

  const openFeedbackModal = () => {
    setIsFeedbackModalOpen(true);
  };

  const closeFeedbackModal = () => {
    setIsFeedbackModalOpen(false);
  };

  return (
    <>
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
                src={brandLogo}
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
              {showSidebarToggle && clusters && currentClusterId && (
                <ToolbarItem>
                  <ClusterSwitcher
                    clusters={clusters}
                    currentClusterId={currentClusterId}
                  />
                </ToolbarItem>
              )}
              <ToolbarGroup
                variant="action-group"
                align={{ default: 'alignEnd' }}
              >
                <ToolbarItem>
                  <ThemeSwitcher />
                </ToolbarItem>
                <ToolbarItem>
                  <RefreshAllButton staticRefresh={staticRefresh} />
                </ToolbarItem>
                <ToolbarItem>
                  <Button
                    aria-label={t('feedback.buttonLabel')}
                    variant="plain"
                    icon={<QuestionCircleIcon />}
                    onClick={openFeedbackModal}
                  />
                </ToolbarItem>
                {metadata && (
                  <ToolbarItem>
                    <Button
                      aria-label={t('about.buttonLabel')}
                      variant="plain"
                      icon={<InfoCircleIcon />}
                      onClick={toggleAboutModal}
                    />
                  </ToolbarItem>
                )}
              </ToolbarGroup>
              <UserDropdown username="Anonymous" />
            </ToolbarContent>
          </Toolbar>
        </MastheadContent>
      </Masthead>

      <FeedbackModal
        onShareFeedback={t('feedback.links.share')}
        onJoinMailingList={t('feedback.links.informDirection')}
        onOpenSupportCase={t('feedback.links.supportCase')}
        onReportABug={t('feedback.links.bugReport')}
        feedbackImg="/pf_feedback.svg"
        isOpen={isFeedbackModalOpen}
        onClose={closeFeedbackModal}
      />

      {metadata && (
        <AboutModal
          isOpen={isAboutModalOpen}
          onClose={toggleAboutModal}
          brandImageSrc={brandLogo}
          brandImageAlt={t('common.title')}
          productName={t('common.title')}
        >
          <Content>
            <dl>
              <dt>{t('about.version')}</dt>
              <dd>{metadata.attributes.version}</dd>
              <dt>{t('about.platform')}</dt>
              <dd>{metadata.attributes.platform}</dd>
              <dt>{t('about.userAgent')}</dt>
              <dd>
                {typeof navigator !== 'undefined'
                  ? navigator.userAgent
                  : 'Unknown'}
              </dd>
            </dl>
          </Content>
        </AboutModal>
      )}
    </>
  );
}