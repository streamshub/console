import { logger } from "@/utils/loggerClient";
import { useInterval } from "@patternfly/react-core";
import { signIn, useSession } from "next-auth/react";

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
  return null;
}
