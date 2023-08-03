import { Breadcrumb, BreadcrumbItem } from "@patternfly/react-core";
import { type NextPage } from "next";
import Link from "next/link";
import { useRouter } from "next/router";
import React from "react";
import Layout from "~/components/layout";

import { api } from "~/utils/api";

const ClusterDetails: NextPage = () => {
  const router = useRouter();
  const { name, namespace } = router.query;
  const cluster = api.k8s.getKafkaCluster.useQuery({
    name,
    namespace,
  });

  return (
    <Layout
      title={`Kafka Cluster ${namespace}:${name}`}
      breadcrumb={
        <Breadcrumb>
          <BreadcrumbItem
            render={(props) => (
              <Link {...props} href={"/"}>
                AMQ Streams
              </Link>
            )}
          />
          <BreadcrumbItem isActive>
            {namespace} - {name}
          </BreadcrumbItem>
        </Breadcrumb>
      }
    >
      <pre>{JSON.stringify(cluster.data, null, 2)}</pre>
    </Layout>
  );
};

export default ClusterDetails;
