"use client";
import {
  AboutModal,
  Brand,
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
} from "@/libs/patternfly/react-core";
import { BarsIcon } from "@/libs/patternfly/react-icons";
import { FeedbackModal } from "@patternfly/react-user-feedback";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { useAppLayout } from "./AppLayoutProvider";
import { UserDropdown } from "./UserDropdown";
import { useColorTheme } from "@/app/[locale]/useColorTheme";
import { AppDropdown, ClusterInfo } from "./AppDropdown";
import { MetadataResponse } from "@/api/meta/schema";
import { ThemeSelector } from "@/app/[locale]/ThemeSelector";

export function AppMasthead({
  username,
  showSidebarToggle,
  clusterInfoList,
  kafkaId,
  metadata,
  isOidcEnabled,
}: {
  readonly username?: string;
  readonly showSidebarToggle: boolean;
  readonly clusterInfoList: ClusterInfo[];
  readonly kafkaId: string;
  readonly metadata?: MetadataResponse;
  readonly isOidcEnabled?: boolean;
}) {
  const t = useTranslations();
  const { toggleSidebar } = useAppLayout();
  const { isDarkMode } = useColorTheme();

  const [isFeedbackModalOpen, setIsFeedbackModalOpen] = useState(false);
  const [isAboutModalOpen, setIsAboutModalOpen] = useState(false);

  const openFeedbackModal = () => {
    setIsFeedbackModalOpen(true);
  };
  const closeFeedbackModal = () => {
    setIsFeedbackModalOpen(false);
  };
  const toggleAboutModal = (
    _event: React.MouseEvent<Element, MouseEvent> | KeyboardEvent | MouseEvent,
  ) => {
    setIsAboutModalOpen(!isAboutModalOpen);
  };

  return (
    <>
      <Masthead>
        <MastheadMain>
          {showSidebarToggle && (
            <MastheadToggle>
              <PageToggleButton
                variant="plain"
                aria-label={t("AppMasthead.global_navigation")}
                onClick={toggleSidebar}
              >
                <BarsIcon />
              </PageToggleButton>
            </MastheadToggle>
          )}
          <MastheadBrand>
            <MastheadLogo href="/" target="_blank">
              <Brand
                src={
                  isDarkMode
                    ? "/full_logo_hori_reverse.svg"
                    : "/full_logo_hori_default.svg"
                }
                alt={t("common.title")}
                heights={{ default: "56px" }}
              />
            </MastheadLogo>
          </MastheadBrand>
        </MastheadMain>
        <MastheadContent>
          <Toolbar
            ouiaId="masthead-toolbar"
            id={"masthead-toolbar"}
            isFullHeight
            isStatic
          >
            <ToolbarContent id={"masthead-toolbar"}>
              {showSidebarToggle && (
                <ToolbarItem className={"pf-v6-u-py-sm"}>
                  <AppDropdown
                    clusters={clusterInfoList}
                    kafkaId={kafkaId}
                    isOidcEnabled={isOidcEnabled}
                  />
                </ToolbarItem>
              )}
              <ToolbarGroup
                variant="action-group"
                align={{ default: "alignEnd" }}
              >
                <ThemeSelector />
              </ToolbarGroup>
              {username && (
                <UserDropdown username={username} picture={undefined} />
              )}
            </ToolbarContent>
          </Toolbar>
        </MastheadContent>
      </Masthead>

      <FeedbackModal
        onShareFeedback={t("feedback.links.share")}
        onJoinMailingList={t("feedback.links.informDirection")}
        onOpenSupportCase={t("feedback.links.supportCase")}
        onReportABug={t("feedback.links.bugReport")}
        feedbackImg={"/pf_feedback.svg"}
        isOpen={isFeedbackModalOpen}
        onClose={closeFeedbackModal}
      />

      <AboutModal
        isOpen={isAboutModalOpen}
        onClose={(
          e: React.MouseEvent<Element, MouseEvent> | KeyboardEvent | MouseEvent,
        ) => toggleAboutModal(e)}
        brandImageSrc={
          isDarkMode
            ? "/full_logo_hori_reverse.svg"
            : "/full_logo_hori_default.svg"
        }
        brandImageAlt="Brand Logo"
        productName={t("common.title")}
      >
        <Content>
          <dl>
            <dt>Version</dt>
            <dd>{metadata?.attributes.version}</dd>
            <dt>Platform</dt>
            <dd>{metadata?.attributes.platform}</dd>
            <dt>User agent</dt>
            <dd>{navigator.userAgent}</dd>
          </dl>
        </Content>
      </AboutModal>
    </>
  );
}
