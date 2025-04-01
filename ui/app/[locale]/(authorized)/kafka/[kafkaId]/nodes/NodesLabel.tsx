import { BrokerStatus, ControllerStatus, NodeRoles } from "@/api/nodes/schema";
import { ReactNode } from "react";
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
  InProgressIcon,
  NewProcessIcon,
  PendingIcon,
} from "@/libs/patternfly/react-icons";
import { Icon, Popover } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export const getNodeRoleLabel = (): Record<NodeRoles, { label: ReactNode }> => {
  const t = useTranslations("nodes");

  return {
    broker: { label: <>{t("node_roles.broker")}</> },
    controller: { label: <>{t("node_roles.controller")}</> },
  };
};

export const getBrokerLabel = (): Record<BrokerStatus, ReactNode> => {
  const t = useTranslations("nodes.broker_status");

  return {
    Running: (
      <Popover
        aria-label={t("running.label")}
        headerContent={<div>{t("running.label")}</div>}
        bodyContent={<div>{t("running.popover_text")}</div>}
      >
        <span className="pf-v5-u-active-color-100">
          <Icon status={"success"}>
            <CheckCircleIcon />
          </Icon>
          &nbsp;{t("running.label")}
        </span>
      </Popover>
    ),
    Starting: (
      <Popover
        aria-label={t("starting.label")}
        headerContent={<div>{t("starting.label")}</div>}
        bodyContent={<div>{t("starting.popover_text")}</div>}
      >
        <span className="pf-v5-u-active-color-100">
          <Icon>
            <InProgressIcon />
          </Icon>
          &nbsp;{t("starting.label")}
        </span>
      </Popover>
    ),
    ShuttingDown: (
      <Popover
        aria-label={t("shutting_down.label")}
        headerContent={<div>{t("shutting_down.label")}</div>}
        bodyContent={<div>{t("shutting_down.popover_text")}</div>}
      >
        <span className="pf-v5-u-active-color-100">
          <Icon>
            <PendingIcon />
          </Icon>
          &nbsp;{t("shutting_down.label")}
        </span>
      </Popover>
    ),
    PendingControlledShutdown: (
      <Popover
        aria-label={t("pending_controlled_shutdown.label")}
        headerContent={<div>{t("pending_controlled_shutdown.label")}</div>}
        bodyContent={<div>{t("pending_controlled_shutdown.popover_text")}</div>}
      >
        <span className="pf-v5-u-active-color-100">
          <Icon status={"warning"}>
            <ExclamationTriangleIcon />
          </Icon>
          &nbsp;{t("pending_controlled_shutdown.label")}
        </span>
      </Popover>
    ),
    Recovery: (
      <Popover
        aria-label={t("recovery.label")}
        headerContent={<div>{t("recovery.label")}</div>}
        bodyContent={<div>{t("recovery.popover_text")}</div>}
      >
        <span className="pf-v5-u-active-color-100">
          <Icon>
            <NewProcessIcon />
          </Icon>
          &nbsp;{t("recovery.label")}
        </span>
      </Popover>
    ),
    NotRunning: (
      <Popover
        aria-label={t("not_running.label")}
        headerContent={<div>{t("not_running.label")}</div>}
        bodyContent={<div>{t("not_running.popover_text")}</div>}
      >
        <span className="pf-v5-u-active-color-100">
          <Icon status={"danger"}>
            <ExclamationCircleIcon />
          </Icon>
          &nbsp;{t("not_running.label")}
        </span>
      </Popover>
    ),
    Unknown: (
      <Popover
        aria-label={t("unknown.label")}
        headerContent={<div>{t("unknown.label")}</div>}
        bodyContent={<div>{t("unknown.popover_text")}</div>}
      >
        <span className="pf-v5-u-active-color-100">
          <Icon status={"danger"}>
            <ExclamationCircleIcon />
          </Icon>
          &nbsp;{t("unknown.label")}
        </span>
      </Popover>
    ),
  };
};

export const getControllerLabel = (): Record<ControllerStatus, ReactNode> => {
  const t = useTranslations("nodes.controller_status");

  return {
    QuorumLeader: (
      <Popover
        aria-label="Quorum Leader Info"
        headerContent={<div>{t("quorum_leader")}</div>}
        bodyContent={<div>{t("quorum_leader_popover_text")}</div>}
      >
        <span className="pf-v5-u-active-color-100">
          <Icon status={"success"}>
            <CheckCircleIcon />
          </Icon>
          &nbsp;{t("quorum_leader")}
        </span>
      </Popover>
    ),
    QuorumFollower: (
      <Popover
        aria-label="Quorum Follower Info"
        headerContent={<div>{t("quorum_follower")}</div>}
        bodyContent={<div>{t("quorum_follower_popover_text")}</div>}
      >
        <span className="pf-v5-u-active-color-100">
          <Icon status={"success"}>
            <CheckCircleIcon />
          </Icon>
          &nbsp;{t("quorum_follower")}
        </span>
      </Popover>
    ),
    QuorumFollowerLagged: (
      <Popover
        aria-label="Quorum Follower Lagged Info"
        headerContent={<div>{t("quorum_follower_lagged")}</div>}
        bodyContent={<div>{t("quorum_follower_lagged_popover_text")}</div>}
      >
        <span className="pf-v5-u-active-color-100">
          <Icon status={"warning"}>
            <ExclamationTriangleIcon />
          </Icon>
          &nbsp;{t("quorum_follower_lagged")}
        </span>
      </Popover>
    ),
    Unknown: (
      <Popover
        aria-label="Unknown Status Info"
        headerContent={<div>{t("unknown")}</div>}
        bodyContent={<div>{t("unknown_popover_text")}</div>}
      >
        <span className="pf-v5-u-active-color-100">
          <Icon status={"danger"}>
            <ExclamationCircleIcon />
          </Icon>
          &nbsp;{t("unknown")}
        </span>
      </Popover>
    ),
  };
};
