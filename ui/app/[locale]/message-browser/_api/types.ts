export type Message = {
  partition?: number;
  offset?: number;
  timestamp?: DateIsoString;
  key?: string;
  value?: string;
  headers: Record<string, string>;
};

export type MessageApiResponse = {
  lastUpdated: Date;
  messages: Message[];
  partitions: number;
  offsetMin: number;
  offsetMax: number;

  filter: {
    partition: number | undefined;
    offset: number | undefined;
    timestamp: DateIsoString | undefined;
    limit: number | undefined;
    epoch: number | undefined;
  };
};
