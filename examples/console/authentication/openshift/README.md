# OpenShift OAuth Authentication Example

This directory demonstrates how to configure StreamsHub Console for OpenShift OAuth authentication using the operator's built-in Dex identity broker integration (`spec.security.openshift`).

## Overview

When `spec.security.openshift` is configured on an OpenShift cluster, the StreamsHub Console operator automatically:

1. Deploys and manages a local Dex identity broker instance.
2. Uses OpenShift's internal OAuth server as the upstream identity provider via projected ServiceAccount tokens.
3. Configures re-encrypt TLS for both the Console and Dex using certificates automatically provisioned and rotated by OpenShift's service CA controller.
4. Auto-wires the Console to use Dex as its OIDC provider with PKCE and OpenShift group claim propagation.

## Prerequisites

- An OpenShift cluster running version 4.19 or higher (or with the `RouteExternalCertificate` feature gate enabled).
- StreamsHub Console Operator deployed on the cluster and watching the target namespace.

## Files

- [`console.yaml`](./console.yaml): A sample `Console` Custom Resource configured with `spec.security.openshift` and role mappings for OpenShift user groups.

## Usage

### 1. Review and Apply Console Custom Resource

Review [`console.yaml`](./console.yaml) and adjust the group names and role rules to match your OpenShift environment.

Apply the resource:

```shell
oc apply -f console.yaml -n <target-namespace>
```

### 2. Verify Deployment and Routes

Check the status of the console and dex pods and routes:

```shell
oc get pods,routes -n <target-namespace>
```

OpenShift will automatically assign a route hostname if `spec.hostname` was not specified.

### 3. Log In

Open the Console URL in your browser. You will be redirected through the OpenShift OAuth login screen. After authenticating with your OpenShift credentials and approving access, you will be redirected back to StreamsHub Console with permissions determined by your OpenShift group memberships.
