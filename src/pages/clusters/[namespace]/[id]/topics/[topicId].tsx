import { Breadcrumb, BreadcrumbItem } from "@patternfly/react-core";
import { type NextPage } from "next";
import Link from "next/link";
import { useRouter } from "next/router";
import React from "react";
import Layout from "~/components/layout";
import { TableView } from "~/components/Table";
import { BackendError } from "~/server/api/routers/backend";

import { api } from "~/utils/api";

const ClusterDetails: NextPage = () => {
  const router = useRouter();
  const { namespace, id, topicId } = router.query;
  const cluster = api.backend.describeCluster.useQuery({
    id,
  });
  const topic = api.backend.describeTopic.useQuery({
    id, topicId
  });

  return (
    <Layout
      title={"Kafka Topic ${topic.data?.name}"}
      breadcrumb={
        <Breadcrumb>
          <BreadcrumbItem
            render={(props) => (
              <Link {...props} href={"/clusters"}>
                Kafka Clusters
              </Link>
            )}
          />
          <BreadcrumbItem
            render={(props) => (
              <Link {...props} href={`/clusters/${namespace}/${id}`}>
                {namespace} - {id}
              </Link>
            )}
          />
          <BreadcrumbItem isActive>
            {topic.data?.name}
          </BreadcrumbItem>
        </Breadcrumb>
      }
    >
      <div>
      <dl>
        <dt>Name</dt>
        <dd>${topic.data?.name}</dd>

      </dl>
      </div>

      <pre>{JSON.stringify(topic.data, null, 2)}</pre>
    </Layout>
  );
};

export default ClusterDetails;
