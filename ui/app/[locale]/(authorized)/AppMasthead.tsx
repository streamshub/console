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
import { useSession } from "next-auth/react";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { useState } from "react";
import { useAppLayout } from "./AppLayoutProvider";
import { UserDropdown } from "./UserDropdown";

export function AppMasthead() {
  const t = useTranslations();
  const { data } = useSession();
  const { user } = data || {};
  const { toggleSidebar } = useAppLayout();
  const [isFeedbackModalOpen, setIsFeedbackModalOpen] = useState(false);
  const openFeedbackModal = () => {
    setIsFeedbackModalOpen(true);
  };
  const closeFeedbackModal = () => {
    setIsFeedbackModalOpen(false);
  };
  return (
    <>
      <Masthead>
        <MastheadToggle>
          <PageToggleButton
            variant="plain"
            aria-label={t("AppMasthead.global_navigation")}
            onClick={toggleSidebar}
          >
            <BarsIcon />
          </PageToggleButton>
        </MastheadToggle>
        <MastheadMain>
          <Link href={"/"} className={"pf-v5-c-masthead_brand"}>
            <img
              className={"pf-v5-c-brand"}
              src={"/masthead-logo.svg"}
              alt={t("common.title")}
              style={{ height: 48 }}
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
                  <ToolbarItem>
                    <TechPreviewPopover>
                      <Label color={"blue"} isCompact={true}>
                        {t("AppMasthead.tech_preview_label")}
                      </Label>
                    </TechPreviewPopover>
                  </ToolbarItem>
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
              <UserDropdown
                username={user?.name || user?.email}
                picture={user?.picture}
              />
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
