import { redirect } from "next/navigation";

export default function TopicPage({
  params,
}: {
  params: { resource: string; topic: string };
}) {
  redirect(`${params.topic}/overview`);
}
