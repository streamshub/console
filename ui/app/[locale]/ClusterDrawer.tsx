"use client";
import { ClusterConnectionDetails } from "@/app/[locale]/ClusterConnectionDetails";
import { useClusterDrawerContext } from "@/app/[locale]/ClusterDrawerContext";
import {
  Divider,
  Drawer,
  DrawerActions,
  DrawerCloseButton,
  DrawerContent,
  DrawerContentBody,
  DrawerHead,
  DrawerPanelContent,
  Title,
} from "@/libs/patternfly/react-core";
import { Skeleton } from "@patternfly/react-core";
import { useTranslations } from "next-intl";
import { PropsWithChildren, Suspense } from "react";

export function ClusterDrawer({
  children,
  showLearningLinks,
}: PropsWithChildren<{ showLearningLinks: boolean }>) {
  const t = useTranslations();
  const { expanded, clusterId, close } = useClusterDrawerContext();
  return (
    <Drawer isExpanded={expanded}>
      <DrawerContent
        panelContent={
          <DrawerPanelContent isResizable={true}>
            <DrawerHead>
              <Title headingLevel={"h3"}>
                {t("ClusterDrawer.cluster_connection_details")}
              </Title>
              <DrawerActions>
                <DrawerCloseButton onClick={close} />
              </DrawerActions>
            </DrawerHead>
            <Divider />
            <Suspense
              fallback={
                <div className={"pf-v5-u-p-lg"}>
                  <Skeleton width={"80%"} className="pf-v5-u-my-md" />
                  <Skeleton width={"50%"} className="pf-v5-u-my-md" />
                  <Skeleton width={"60%"} className="pf-v5-u-my-md" />
                  <Skeleton width={"30%"} className="pf-v5-u-my-md" />
                  <Skeleton width={"80%"} className="pf-v5-u-my-md" />
                  <Skeleton width={"50%"} className="pf-v5-u-my-md" />
                  <Skeleton width={"60%"} className="pf-v5-u-my-md" />
                  <Skeleton width={"30%"} className="pf-v5-u-my-md" />
                  <Skeleton width={"80%"} className="pf-v5-u-my-md" />
                </div>
              }
            >
              {clusterId && (
                <ClusterConnectionDetails
                  clusterId={clusterId}
                  showLearningLinks={showLearningLinks}
                />
              )}
            </Suspense>
          </DrawerPanelContent>
        }
      >
        <DrawerContentBody
          className={"pf-v5-u-display-flex pf-v5-u-flex-direction-column"}
          // style={{
          //   height: "100%",
          //   width: "100%",
          //   overflowY: "auto",
          //   position: "absolute",
          // }}
        >
          {children}
        </DrawerContentBody>
      </DrawerContent>
    </Drawer>
  );
}
