import { redirect } from "@/navigation";

export default async function resourceIndexPage({
  params,
}: {
  params: { kafkaId: string };
}) {
  redirect("/home");
}
