export async function getTools(isAdmin: boolean) {
  return [
    {
      url: "/overview",
      title: "overview.title" as const,
      requiresAdmin: true,
      enabled: false,
    },
    {
      url: "/brokers",
      title: "brokers.title" as const,
      requiresAdmin: true,
      enabled: false,
    },
    {
      url: "/topics",
      title: "topics.title" as const,
      requiresAdmin: false,
      enabled: false,
    },
  ].filter((t) => (!isAdmin ? !t.requiresAdmin : true));
}
