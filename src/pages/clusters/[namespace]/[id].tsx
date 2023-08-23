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
  const { namespace, id } = router.query;
  const cluster = api.backend.describeCluster.useQuery({
    id,
  });
  const topics = api.backend.listTopics.useQuery({
    id,
  });

  return (
    <Layout
      title={`Kafka Cluster ${namespace}:${id}`}
      breadcrumb={
        <Breadcrumb>
          <BreadcrumbItem
            render={(props) => (
              <Link {...props} href={"/clusters"}>
                Kafka Clusters
              </Link>
            )}
          />
          <BreadcrumbItem isActive>
            {namespace} - {id}
          </BreadcrumbItem>
        </Breadcrumb>
      }
    >
      <TableView
        itemCount={topics.data?.length}
        page={1}
        onPageChange={() => {}}
        data={topics.data}
        emptyStateNoData={<div>No clusters</div>}
        emptyStateNoResults={<div>No search results</div>}
        ariaLabel={"List of topics"}
        columns={[
          "name" as const,
          "partitions" as const,
        ]}
        renderHeader={({ column, Th }) => {
          switch (column) {
            case "name":
              return <Th>Name</Th>;
              case "partitions":
                return <Th>Partitions</Th>;
            }
        }}
        renderCell={({ row, column, Td }) => {
          switch (column) {
            case "name":
              return (
                <Td>
                  <Link href={`/clusters/${namespace}/${id}/topics/${row.id}`}>
                    {row.name}
                  </Link>
                </Td>
              );
            case "partitions":
              return (
                <Td>
                  { ("meta" in row.partitions) 
                    ? (row.partitions as BackendError).message
                    : row.partitions?.length }
                </Td>
              );
            default:
              return <Td>{row[column]}</Td>;
          }
        }}
      />
      <pre>{JSON.stringify(cluster.data, null, 2)}</pre>
    </Layout>
  );
};

export default ClusterDetails;
