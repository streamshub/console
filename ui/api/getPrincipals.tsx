export async function getPrincipals(): Promise<Principal[]> {
  return [
    {
      id: "a1",
      cluster: "Website data",
      name: "Admin",
    },
    {
      id: "a2",
      cluster: "Website data",
      name: "Website log producer",
    },
    {
      id: "a3",
      cluster: "Website data",
      name: "Logs developer",
    },
    {
      id: "a4",
      cluster: "Website data",
      name: "Website contact form producer",
    },
    {
      id: "a5",
      cluster: "Website data",
      name: "Website contact form developer",
    },
    {
      id: "b1",
      cluster: "Metrics",
      name: "Admin",
    },
    {
      id: "b3",
      cluster: "Metrics",
      name: "Disk usage producer",
    },
    {
      id: "b4",
      cluster: "Metrics",
      name: "Developer",
    },
  ];
}
