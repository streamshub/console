import {k8s} from "~/server/api/routers/k8s";
import {backend} from "~/server/api/routers/backend";
import {createTRPCRouter} from "~/server/api/trpc";

/**
 * This is the primary router for your server.
 *
 * All routers added in /api/routers should be manually added here.
 */
export const appRouter = createTRPCRouter({
  k8s: k8s,
  backend: backend,
});

// export type definition of API
export type AppRouter = typeof appRouter;
