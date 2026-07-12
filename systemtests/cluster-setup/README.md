# Cluster setup (local Kubernetes for systemtests)

Provisions a local Kubernetes cluster for running the `console-operator`
systemtests outside CI. Built primarily for macOS, where
`systemtests/scripts/setup-minikube.sh` doesn't work (Docker Desktop /
Podman Desktop / Colima all run the container engine inside a hidden VM, so
a minikube node's IP — used by CI's `$(minikube ip).nip.io` — isn't
reachable from the host), but the scripts themselves are OS-agnostic and
work on Linux too.

**Container engine**: auto-detected — prefers `podman`, falls back to
`docker` if podman isn't installed. Override explicitly with
`CONTAINER_ENGINE=podman` or `CONTAINER_ENGINE=docker`. The podman-machine
VM setup (`ensure_podman_machine` in `lib/env.sh`) only runs on macOS,
where podman needs a VM to run containers at all — it's a no-op on native
Linux, which doesn't need one.

## Use kind

**Default to `kind/`.** kind maps ports 80/443 straight to the host at
cluster-creation time and binds ingress-nginx via `hostPort` on the node —
so `https://<name>.127.0.0.1.nip.io/` just works, portlessly, with no sudo
and no background process to manage.

`minikube/` works too, but its `ingress` addon uses a `NodePort` Service
instead, which isn't reliably reachable from macOS. That means either a
`kubectl port-forward` (URLs need a `:8443` suffix) or a `minikube tunnel`
that requires a one-time sudo prompt — more moving parts either way. Use
`minikube/` only if you specifically need it (e.g. closer parity with CI's
tooling); otherwise `kind/` is simpler and more reliable.

## Layout

```
cluster-setup/
  kind/                    # recommended
    create-cluster.sh      # create/reuse cluster + ingress-nginx, verify with a smoke test
    delete-cluster.sh      # teardown (full, or --keep-cluster)
    lib/env.sh             # shared config + podman-machine helpers
  minikube/                # alternative
    create-cluster.sh      # create/reuse cluster + ingress addon, verify with a smoke test
    delete-cluster.sh      # teardown (full, or --keep-cluster)
    enable-tunnel.sh       # switch to portless access (needs sudo once)
    lib/env.sh             # shared config + podman-machine helpers
  common/                  # cluster-agnostic — works against either
    setup-catalogsource.sh       # install OLM + a CatalogSource
    deploy-example-console.sh    # Strimzi + Kafka + Console operator + Console instance
```

## Quick start

### kind

```sh
./kind/create-cluster.sh
source kind/.cluster-env
../common/setup-catalogsource.sh --image <catalog-image> [--namespace olm] [--name strimzi-source]
../common/deploy-example-console.sh --catalog-image <catalog-image>
```

Ends with a working, portless Console UI at
`https://example-console.127.0.0.1.nip.io/`, backed by a real Kafka cluster.

Teardown: `./kind/delete-cluster.sh` (or `--keep-cluster`).

### minikube

```sh
./minikube/create-cluster.sh
./minikube/enable-tunnel.sh    # portless access — prompts for your sudo password once
source minikube/.cluster-env
../common/setup-catalogsource.sh --image <catalog-image> [--namespace olm] [--name strimzi-source]
../common/deploy-example-console.sh --catalog-image <catalog-image>
```

`enable-tunnel.sh` is optional — skip it and URLs just need the port suffix
`create-cluster.sh` prints instead (e.g.
`https://example-console.127.0.0.1.nip.io:8443/`). Run it if you want
portless URLs, e.g. to match kind's behavior or for OIDC/auth flows that
assume no port in the redirect URI.

Teardown: `./minikube/delete-cluster.sh` (or `--keep-cluster`).

All scripts in both directories are idempotent — safe to re-run.

## What each script does

### `kind/create-cluster.sh`

Creates (or reuses) a kind cluster with ports 80/443 mapped to the host,
installs ingress-nginx bound via `hostPort`, and patches in
`--enable-ssl-passthrough` (needed for Kafka's TLS-terminating `secure`
listener). Finishes by deploying a throwaway echo-server behind an Ingress
and curling it over HTTP/HTTPS to confirm the whole path actually works
before declaring the cluster ready — if that check fails, the script exits
non-zero instead of claiming success.

Env vars (all optional, see `kind/lib/env.sh`): `CONTAINER_ENGINE`
(`podman`/`docker`), `CLUSTER_NAME`, `INGRESS_HTTP_PORT`/`INGRESS_HTTPS_PORT`,
`CONSOLE_CLUSTER_DOMAIN`, `PODMAN_MACHINE_CPUS`/`PODMAN_MACHINE_MEMORY`
(podman only, auto-sized from host resources on first-time machine init).

### `minikube/create-cluster.sh`

Creates (or reuses) a minikube cluster with the `ingress` addon, patches in
`--enable-ssl-passthrough`, then exposes it via a persistent background
`kubectl port-forward` on non-privileged local ports (default
`8080`/`8443`, override via `LOCAL_HTTP_PORT`/`LOCAL_HTTPS_PORT`) and
verifies it the same way as kind's script. If the port-forward process
dies, just re-run `create-cluster.sh` — it detects the dead PID and
restarts it. Same env vars as kind's script.

### `minikube/enable-tunnel.sh`

Switches minikube's exposure from the port-forward to `minikube tunnel` for
portless access: stops the port-forward, patches `ingress-nginx-controller`
to `LoadBalancer`, then runs the tunnel itself as a managed background
process. The one thing it can't avoid is the sudo prompt needed to bind
ports 80/443 — run it directly (not `sudo`-prefixed) and you'll be asked
for your password exactly once, right there; everything else runs
unattended.

### `kind/delete-cluster.sh` / `minikube/delete-cluster.sh [--keep-cluster]`

- Default: full delete (`kind delete cluster` / `minikube delete`).
- `--keep-cluster`: removes just the deployed resources (Console, Kafka,
  Console operator, Strimzi, OLM) but leaves the cluster + ingress-nginx
  running, so a re-run of the `common/` scripts skips cluster creation and
  image pulls. Requires `operator-sdk` to cleanly uninstall OLM in this
  mode.

`minikube/delete-cluster.sh` also stops whichever exposure mechanism
(port-forward or tunnel) is currently running.

### `common/setup-catalogsource.sh --image <image> [--namespace olm] [--name strimzi-source]`

Installs OLM v0.45.0 if needed, then creates a `CatalogSource` pointed at
`--image` and waits for it to report `READY`. Cluster-agnostic — works
against whatever context is currently active.

### `common/deploy-example-console.sh --catalog-image <image> [options]`

Deploys everything else in one shot: Strimzi (Helm), the Console operator
(via the CatalogSource from `setup-catalogsource.sh`), a Kafka cluster, and
a Console instance — using the project's own `examples/kafka/*.yaml` and
`examples/console/010-Console-example.yaml` quickstart manifests. Options:

```
--catalog-image        (required)
--catalog-namespace    default: olm
--catalog-name         default: strimzi-source
--channel              default: alpha
--operator-namespace   default: co-namespace
--strimzi-version      default: 0.51.0   (must match your console-operator build)
--kafka-namespace      default: kafka
--listener             default: scramplain
```
