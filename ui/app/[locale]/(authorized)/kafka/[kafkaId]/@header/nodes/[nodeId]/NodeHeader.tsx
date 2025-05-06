import { KafkaNodeParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/kafkaNode.params";
import { AppHeader } from "@/components/AppHeader";
import { Skeleton } from "@patternfly/react-core";
import { Suspense } from "react";

export async function NodeHeader({
  params: { kafkaId, nodeId },
}: {
  params: KafkaNodeParams;
}) {
  return (
    <AppHeader
      title={
        <Suspense fallback={<Skeleton width="35%" />}>Broker {nodeId}</Suspense>
      }
      navigation={
        // <PageNavigation>
        //   <Nav aria-label="Group section navigation" variant="default">
        //     <NavList>
        //       <NavItemLink
        //         url={`/kafka/${kafkaId}/nodes/${nodeId}/configuration`}
        //       >
        //         Configuration
        //       </NavItemLink>
        //     </NavList>
        //   </Nav>
        // </PageNavigation>
        undefined
      }
    />
  );
}
