import { fakeApi } from "@/api/fakeApi";

function generateRandomDate(from: Date, to: Date) {
  return new Date(
    from.getTime() + Math.random() * (to.getTime() - from.getTime()),
  );
}

export async function getTopics(): Promise<Topic[]> {
  return fakeApi([
    {
      name: "access_logs",
      status: "healthy",
      partitions: 12,
      ingress: 123456,
      egress: undefined,
      lastProduced: new Date(),
    },
    {
      name: "error_logs",
      status: "healthy",
      partitions: 1,
      ingress: undefined,
      egress: undefined,
      lastProduced: generateRandomDate(new Date("2023-04-01"), new Date()),
    },
  ]);
}
