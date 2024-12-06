"use client";
import { logger } from "@/utils/loggerClient";
import { useInterval } from "@patternfly/react-core";
import { signIn, useSession } from "next-auth/react";
import { useCallback, useLayoutEffect } from "react";

const log = logger.child({ module: "UI", component: "SessionRefresher" });

export function SessionRefresher() {
  const { update } = useSession();

  const doRefresh = useCallback(async () => {
    const refreshedToken = await update();
    log.debug(refreshedToken, "Refreshed session");
    if (refreshedToken?.error === "RefreshAccessTokenError") {
      log.debug("Force sign in");
      void signIn();
    }
  }, [update]);

  useInterval(() => {
    void doRefresh();
  }, 1000 * 60);

  useLayoutEffect(() => {
    void doRefresh();
  }, [doRefresh]);

  return null;
}
