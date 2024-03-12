import { ExternalLink } from "@/components/Navigation/ExternalLink";
import { Popover } from "@/libs/patternfly/react-core";
import { PopoverProps } from "@patternfly/react-core";

export function TechPreviewPopover({
  children,
}: {
  children: PopoverProps["children"];
}) {
  return (
    <Popover
      triggerAction={"hover"}
      headerContent={"Technology Preview"}
      bodyContent={
        <div>
          Technology Preview features are not fully supported, may not be
          functionally complete, and are not suitable for deployment in
          production. However, these features are provided to the customer as a
          courtesy and the primary goal is for the feature to gain wider
          exposure with the goal of full support in the future.
        </div>
      }
      footerContent={
        <ExternalLink
          href={"https://redhat.com"}
          testId={"tech-preview-learn-more"}
        >
          Learn more
        </ExternalLink>
      }
    >
      {children}
    </Popover>
  );
}
