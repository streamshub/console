# OpenShift Authentication

This directory contains resources to deploy a [dex](https://dexidp.io/) server to support authentication in the console
using OpenShift's platform identity provider. Other identity providers [supported by dex](https://dexidp.io/docs/connectors/)
may also work and require modification to the `[040-Secret-console-dex.yaml](./040-Secret-console-dex.yaml)` file.

Deploying these requires several parameters set in the environment
- `NAMESPACE` to deploy dex (may be the same namespace used for the console deployment)
- `CLUSTER_DOMAIN`: base domain used by your Kubernetes cluster. This will be used to configure dex's JWT issuer, dex's own ingress domain name, and the redirect URL to the console application. The example resources assume that a console instance is hosted at `https://example-console.${CLUSTER_DOMAIN}/api/auth/callback/oidc`.
- `CLUSTER_APISERVER`: API server end-point for the Kubernetes cluster
- `STATIC_CLIENT_SECRET`: client secret used by the console to interact with dex as an OAuth2 client

The following script is based on the [dex OpenShift documentation](https://dexidp.io/docs/connectors/openshift/).

```shell
export NAMESPACE=streams-console-dex
export CLUSTER_DOMAIN=apps-crc.testing
export CLUSTER_APISERVER=$(kubectl config view --minify=true --flatten=false -o json | jq -r .clusters[0].cluster.server)
export STATIC_CLIENT_SECRET="$(tr -dc A-Za-z0-9 </dev/urandom | head -c 24)"

# Create the namespace for dex (optional if already existing)
kubectl create namespace ${NAMESPACE}

# Create a service account for the dex server
kubectl create serviceaccount console-dex --dry-run=client -o yaml \
  | kubectl apply -n ${NAMESPACE} -f -

# Annotate the dex server's service account with the dex redirect URL
kubectl annotate serviceaccount console-dex -n ${NAMESPACE} --dry-run=client "serviceaccounts.openshift.io/oauth-redirecturi.dex=https://console-dex.${CLUSTER_DOMAIN}/callback" -o yaml \
  | kubectl apply -n ${NAMESPACE} -f -

# Set secrets used by dex (DEX_CLIENT_ID/SECRET) and the console (DEX_STATIC_CLIENT_SECRET).
# The token generated below is valid for 1 year, adjust as necessary.
kubectl create secret generic console-dex-secrets -n ${NAMESPACE} \
  --dry-run=client \
  --from-literal=DEX_CLIENT_ID="system:serviceaccount:${NAMESPACE}:console-dex" \
  --from-literal=DEX_CLIENT_SECRET="$(kubectl create token -n ${NAMESPACE} console-dex --duration=$((365*24))h)" \
  --from-literal=CONSOLE_SECURITY_OIDC_CLIENT_ID="streamshub-console" \
  --from-literal=CONSOLE_SECURITY_OIDC_CLIENT_SECRET="${STATIC_CLIENT_SECRET}" \
  -o yaml | kubectl apply -n ${NAMESPACE} -f -

cat *.yaml | envsubst | kubectl apply -f - -n ${NAMESPACE}
```

The console may then be configured to use the dex instance by adding the `security.oidc` section to the configuration YAML. The variables will be resolved provided that the previously-created `console-dex-secrets` secret is used to set the values on the console deployment. With the operator, this can be done using the `.spec.env` section of the `Console` custom resource.

```yaml
security:
  oidc:
    authServerUrl: https://console-dex.${CLUSTER_DOMAIN}
    clientId: ${console.security.oidc.client-id}
    clientSecret: ${console.security.oidc.client-secret}
    scopes: "openid email profile groups"
    roleClaimPath:
      - "groups"
# ... remainder of console configurations
```
