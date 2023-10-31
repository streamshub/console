import { getKafkaCluster } from "@/api/kafka";
import { getTopics } from "@/api/topics";
import { KafkaParams } from "@/app/[locale]/kafka/[kafkaId]/kafka.params";
import { AppHeader } from "@/components/AppHeader";
import { notFound } from "next/navigation";

export async function KafkaHeader({
  params: { kafkaId },
}: {
  params: KafkaParams;
}) {
  const cluster = await getKafkaCluster(kafkaId);
  if (!cluster) {
    notFound();
  }
  const topics = await getTopics(kafkaId, { pageSize: 1 });
  return (
    <AppHeader
      title={cluster.attributes.name}
      // navigation={
      //   <PageNavigation>
      //     <Nav aria-label="Group section navigation" variant="tertiary">
      //       <NavList>
      //         <NavItemLink url={`/kafka/${kafkaId}/topics`}>
      //           Topics&nbsp;
      //           <Label isCompact={true}>
      //             <Suspense fallback={<Spinner />}>
      //               {topics.meta.page.total}
      //             </Suspense>
      //           </Label>
      //         </NavItemLink>
      //         <NavItemLink url={`/kafka/${kafkaId}/consumer-groups`}>
      //           Consumer groups&nbsp;
      //           <Label isCompact={true}>0</Label>
      //         </NavItemLink>
      //         <NavItemLink url={`/kafka/${kafkaId}/nodes`}>
      //           Nodes&nbsp;
      //           <Label isCompact={true}>
      //             <Suspense fallback={<Spinner />}>
      //               {cluster.attributes.nodes.length}
      //             </Suspense>
      //           </Label>
      //         </NavItemLink>
      //         <NavItemLink url={`/kafka/${kafkaId}/schema-registry`}>
      //           Schema registry
      //         </NavItemLink>
      //       </NavList>
      //     </Nav>
      //   </PageNavigation>
      // }
    />
  );
}
