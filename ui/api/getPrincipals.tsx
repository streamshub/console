export async function getPrincipals(): Promise<Principal[]> {
  return [
    {
      id: "a1",
      cluster: "Website data",
      name: "Admin",
      description: "Sample description of the principal",
    },
    {
      id: "a2",
      cluster: "Website data",
      name: "Website log producer",
      description: "Sample description of the principal",
    },
    {
      id: "a3",
      cluster: "Website data",
      name: "Logs developer",
      description: "Sample description of the principal",
    },
    {
      id: "a4",
      cluster: "Website data",
      name: "Website contact form producer",
      description: "Sample description of the principal",
    },
    {
      id: "a5",
      cluster: "Website data",
      name: "Website contact form developer",
      description: "Sample description of the principal",
    },
    {
      id: "b1",
      cluster: "Metrics",
      name: "Admin",
      description: "Sample description of the principal",
    },
    {
      id: "b3",
      cluster: "Metrics",
      name: "Disk usage producer",
      description: "Sample description of the principal",
    },
    {
      id: "b4",
      cluster: "Metrics",
      name: "Developer",
      description: "Sample description of the principal",
    },
  ];
}
