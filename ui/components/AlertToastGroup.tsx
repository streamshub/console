import React from "react";
import {
  AlertGroup,
  Alert,
  AlertActionCloseButton,
  AlertVariant,
} from "@patternfly/react-core";

export type GroupToastAlertProps = {
  id: string;
  title: string;
  description?: string;
  variant: "success" | "info" | "danger" | "warning";
};

export function AlertToastGroup({
  toastAlerts,
  onClickCloseAlert,
}: {
  toastAlerts: GroupToastAlertProps[];
  onClickCloseAlert: (id: string) => void;
}) {
  return (
    <AlertGroup isToast>
      {toastAlerts.map((alert) => (
        <Alert
          key={alert.id}
          isLiveRegion
          variant={AlertVariant[alert.variant]}
          title={alert.title}
          actionClose={
            <AlertActionCloseButton
              title={alert.title}
              onClose={() => onClickCloseAlert(alert.id)}
            />
          }
        >
          {alert.description}
        </Alert>
      ))}
    </AlertGroup>
  );
}
