"use client";
import { useInterval } from "@/libs/patternfly/react-core";
import { logger } from "@/utils/loggerClient";
import { signIn, useSession } from "next-auth/react";
import { useLayoutEffect } from "react";

const log = logger.child({ module: "UI", component: "SessionRefresher" });

export function SessionRefresher() {
  const { update } = useSession();

  async function doRefresh() {
    const refreshedToken = await update();
    log.debug(refreshedToken, "Refreshed session");
    if (refreshedToken?.error === "RefreshAccessTokenError") {
      log.debug("Force sign in");
      void signIn(); // Force sign in to hopefully resolve error
    }
  }

  useInterval(() => {
    void doRefresh();
  }, 1000 * 60);

  // immediately trigger a check to deal with expired sessions when opening the app after a long time
  useLayoutEffect(() => {
    void doRefresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Make sure the effect runs only once

  return null;
}
