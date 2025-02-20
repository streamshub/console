"use client";
import { TechPreviewPopover } from "@/components/TechPreviewPopover";
import {
  Button,
  Label,
  Masthead,
  MastheadContent,
  MastheadMain,
  MastheadToggle,
  PageToggleButton,
  Toolbar,
  ToolbarContent,
  ToolbarGroup,
  ToolbarItem,
} from "@/libs/patternfly/react-core";
import { BarsIcon, QuestionCircleIcon } from "@/libs/patternfly/react-icons";
import { FeedbackModal } from "@patternfly/react-user-feedback";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { useEffect, useState } from "react";
import { useAppLayout } from "./AppLayoutProvider";
import { UserDropdown } from "./UserDropdown";
import Image from "next/image";
import { clientConfig as config } from "@/utils/config";

export function AppMasthead({
  username,
  showSidebarToggle,
}: {
  username?: string;
  showSidebarToggle: boolean;
}) {
  const t = useTranslations();
  const { toggleSidebar } = useAppLayout();
  const [isFeedbackModalOpen, setIsFeedbackModalOpen] = useState(false);
  const [isTechPreview, setIsTechPreview] = useState(false);

  useEffect(() => {
    config().then(cfg => {
      setIsTechPreview(cfg.techPreview);
    })
  }, []);

  const openFeedbackModal = () => {
    setIsFeedbackModalOpen(true);
  };
  const closeFeedbackModal = () => {
    setIsFeedbackModalOpen(false);
  };
  return (
    <>
      <Masthead>
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
        <MastheadMain>
          <Link href={"/"} className={"pf-v5-c-masthead_brand"}>
            <Image
              src={"/full-logo.svg"}
              alt={t("common.title")}
              width={300}
              height={150}
            />
          </Link>
        </MastheadMain>
        <MastheadContent>
          <Toolbar
            ouiaId="masthead-toolbar"
            id={"masthead-toolbar"}
            isFullHeight
            isStatic
          >
            <ToolbarContent id={"masthead-toolbar"}>
              <ToolbarGroup
                variant="icon-button-group"
                align={{ default: "alignRight" }}
                spacer={{ default: "spacerNone", md: "spacerMd" }}
              >
                <ToolbarGroup
                  variant="icon-button-group"
                  visibility={{ default: "hidden", lg: "visible" }}
                >
                  {isTechPreview && (
                    <ToolbarItem>
                      <TechPreviewPopover>
                        <Label color={"blue"} isCompact={true}>
                          {t("AppMasthead.tech_preview_label")}
                        </Label>
                      </TechPreviewPopover>
                    </ToolbarItem>
                  )}
                  <ToolbarItem>
                    <Button
                      aria-label={t("AppMasthead.help")}
                      variant={"plain"}
                      icon={<QuestionCircleIcon />}
                      ouiaId={"help-button"}
                      onClick={openFeedbackModal}
                    />
                  </ToolbarItem>
                </ToolbarGroup>
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
    </>
  );
}
