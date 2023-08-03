import { Breadcrumb, BreadcrumbItem } from "@patternfly/react-core";
import { type NextPage } from "next";
import { useTranslations } from "next-intl";
import Link from "next/link";
import React from "react";
import Layout from "~/components/layout";
import { TableView } from "~/components/Table";

import { api } from "~/utils/api";

const Home: NextPage = () => {
  const clusters = api.k8s.getKafkaClusters.useQuery();
  const t = useTranslations("common");

  return (
    <Layout
      title={"Kafka Clusters"}
      breadcrumb={
        <Breadcrumb>
          <BreadcrumbItem
            isActive
            render={(props) => (
              <Link {...props} href={"/"}>
                {t("title")}
              </Link>
            )}
          />
        </Breadcrumb>
      }
    >
      <TableView
        itemCount={clusters.data?.length}
        page={1}
        onPageChange={() => {}}
        data={clusters.data}
        emptyStateNoData={<div>No clusters</div>}
        emptyStateNoResults={<div>No search results</div>}
        ariaLabel={"List of AMQ Streams clusters"}
        columns={[
          "name" as const,
          "namespace" as const,
          "status" as const,
          "creationTimestamp" as const,
        ]}
        renderHeader={({ column, Th }) => {
          switch (column) {
            case "name":
              return <Th>Name</Th>;
            case "namespace":
              return <Th>Namespace</Th>;
            case "status":
              return <Th>Status</Th>;
            case "creationTimestamp":
              return <Th>Created at</Th>;
          }
        }}
        renderCell={({ row, column, Td }) => {
          switch (column) {
            case "name":
              return (
                <Td>
                  <Link href={`/clusters/${row.namespace}/${row.name}`}>
                    {row.name}
                  </Link>
                </Td>
              );
            case "status":
              return (
                <Td>
                  {
                    row.status?.conditions?.find((c) => c.status === "True")
                      ?.type
                  }
                </Td>
              );
            case "creationTimestamp":
              return (
                <Td>
                  {new Intl.DateTimeFormat("en-US", {
                    dateStyle: "medium",
                    timeStyle: "medium",
                  }).format(Date.parse(row.creationTimestamp))}
                </Td>
              );
            default:
              return <Td>{row[column]}</Td>;
          }
        }}
      />
    </Layout>
  );
};

export default Home;
