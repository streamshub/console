import { redirect } from "next/navigation";

export default function TopicPage({
  params,
}: {
  params: { authProfile: string; topic: string };
}) {
  redirect(`${params.topic}/overview`);
}
