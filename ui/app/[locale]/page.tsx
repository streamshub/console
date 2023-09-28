import { redirect } from "next/navigation";

export default async function resourceIndexPage({
  params,
}: {
  params: { kafkaId: string };
}) {
  redirect("/resources");
}
