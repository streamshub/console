# Cluster setup (local Kubernetes for systemtests)

Provisions a local Kubernetes cluster for running the `console-operator`
systemtests, for local dev and CI alike, on both macOS and Linux. Originally
built as a macOS-only alternative to the project's old
`systemtests/scripts/setup-minikube.sh` (Docker Desktop / Podman Desktop /
Colima all run the container engine inside a hidden VM, so a minikube
node's IP — used by CI's `$(minikube ip).nip.io` — wasn't reachable from
the host there), it has since absorbed that script's role entirely: CI now
calls directly into these scripts too, branching internally
(`NEEDS_LOCAL_PORT_FORWARD` in `common/common.sh`) between macOS's
port-forward-based exposure and Linux's direct node-IP access.

**Container engine / driver**: auto-detected, and `kind/` and `minikube/`
pick differently:

- `kind/` only ever supports `docker` or `podman` — prefers `docker`,
  falls back to `podman`. Override with `CONTAINER_ENGINE=docker` or
  `CONTAINER_ENGINE=podman`.
- `minikube/` also considers VM-based drivers, per
  [minikube's own driver docs](https://minikube.sigs.k8s.io/docs/drivers/):
  `docker` → the platform's VM driver (`vfkit` on macOS, `kvm2` on Linux)
  → `podman` as a last resort (still marked experimental upstream on both
  OSes). Override with `CONTAINER_ENGINE=docker`, `vfkit`, `kvm2`, or
  `podman`.

Podman works as a fallback on both, but avoid it if you have another
option — in practice it's needed real workarounds (the `ip_tables` kernel
module requirement on Linux, PID-limit crashes under a full
Kafka+Console workload, and rootless-mode failures reported directly
against minikube). The podman-machine VM setup (`ensure_podman_machine`
in `common/common.sh`) only runs on macOS, where podman needs a VM to run
containers at all — it's a no-op on Linux, which doesn't need one.

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

**macOS reality check**: `kind/` with the `docker` driver is the path
that's actually been verified working reliably. `minikube/` with the
`vfkit` driver has hit VM-networking failures in practice that are still
being root-caused, and rootless podman is a recurring source of pain on
both cluster types (see the driver notes above) — don't reach for `podman`
or `vfkit` as your first choice on macOS. If you need `minikube/`, prefer
`docker` as the driver; only try `vfkit`/`podman` if you're specifically
debugging that path.

## Layout

```
cluster-setup/
  kind/                    # recommended
    create-cluster.sh      # create/reuse cluster + ingress-nginx, verify with a smoke test
    setup-registry.sh      # local image registry, reachable from host and cluster
    load-images.sh         # push locally-built Console images to that registry
    delete-cluster.sh      # teardown (full, or --keep-cluster)
    env.sh                 # shared config (sources common/common.sh)
  minikube/                # alternative
    create-cluster.sh      # create/reuse cluster + ingress addon, verify with a smoke test
    setup-registry.sh      # local image registry, reachable from host and cluster
    load-images.sh         # push locally-built Console images to that registry
    enable-tunnel.sh       # switch to portless access (needs sudo once)
    delete-cluster.sh      # teardown (full, or --keep-cluster)
    env.sh                 # shared config (sources common/common.sh)
  common/                  # cluster-agnostic — works against either
    common.sh                    # shared OS detection + podman-machine helpers (used by kind/minikube's env.sh)
    build-console.sh             # build Console images (API, operator, bundle, catalog) locally
    setup-catalogsource.sh       # install OLM + a CatalogSource
    deploy-example-console.sh    # Strimzi + Kafka + Console operator + Console instance
```

## Quick start

Running against your own locally-modified code, with nothing pushed to a
remote registry — only to the local one these scripts set up for you. All
commands below are run from `cluster-setup/`.

### kind

```sh
./kind/create-cluster.sh
./kind/setup-registry.sh
./common/build-console.sh
./kind/load-images.sh
source kind/.cluster-env
common/deploy-example-console.sh --catalog-image localhost:5000/streamshub/console-operator-catalog:<tag>
```

Ends with a working, portless Console UI at
`https://example-console.127.0.0.1.nip.io/`, backed by a real Kafka cluster.

Teardown: `./kind/delete-cluster.sh` (or `--keep-cluster`).

### minikube

```sh
./minikube/create-cluster.sh
./minikube/setup-registry.sh
./common/build-console.sh
./minikube/load-images.sh
./minikube/enable-tunnel.sh    # portless access — prompts for your sudo password once
source minikube/.cluster-env
common/deploy-example-console.sh --catalog-image localhost:5000/streamshub/console-operator-catalog:<tag>
```

`enable-tunnel.sh` is optional — skip it and URLs just need the port suffix
`create-cluster.sh` prints instead (e.g.
`https://example-console.127.0.0.1.nip.io:8443/`). Run it if you want
portless URLs, e.g. to match kind's behavior or for OIDC/auth flows that
assume no port in the redirect URI. (This whole port-forward dance is
macOS-only — on Linux, `minikube/create-cluster.sh` uses the minikube
node's real IP directly, no port-forward and no `enable-tunnel.sh` needed;
URLs are portless out of the box there.)

Teardown: `./minikube/delete-cluster.sh` (or `--keep-cluster`).

All scripts in both directories are idempotent — safe to re-run.

## What each script does

### `kind/create-cluster.sh`

Creates (or reuses) a kind cluster with ports 80/443 mapped to the host,
installs ingress-nginx bound via `hostPort`, and patches in
`--enable-ssl-passthrough` (needed for Kafka's TLS-terminating `secure`
listener). Also configures CoreDNS with split-horizon DNS for
`CONSOLE_CLUSTER_DOMAIN` (see "DNS inside the cluster" below) — without
it, anything connecting to Kafka's `secure` listener *from inside the
cluster* (Console, most notably) can't actually reach it. Finishes by
deploying a throwaway echo-server behind an Ingress and curling it over
HTTP/HTTPS to confirm the whole path actually works before declaring the
cluster ready — if that check fails, the script exits non-zero instead of
claiming success.

Env vars (all optional, see `kind/env.sh`): `CONTAINER_ENGINE`
(`podman`/`docker`), `CLUSTER_NAME`, `INGRESS_HTTP_PORT`/`INGRESS_HTTPS_PORT`,
`CONSOLE_CLUSTER_DOMAIN`, `PODMAN_MACHINE_CPUS`/`PODMAN_MACHINE_MEMORY`
(podman only, auto-sized from host resources on first-time machine init).

### `minikube/create-cluster.sh`

Creates (or reuses) a minikube cluster with the `ingress` addon, patches in
`--enable-ssl-passthrough`, then exposes it. On Linux, that means using the
minikube node's real IP directly — no port-forward needed, since (unlike
macOS's hidden-VM container engines) the node is directly reachable from
the host. On macOS, it exposes it via a persistent background `kubectl
port-forward` on non-privileged local ports (default `8080`/`8443`,
override via `LOCAL_HTTP_PORT`/`LOCAL_HTTPS_PORT`); if that port-forward
process dies, just re-run `create-cluster.sh` — it detects the dead PID and
restarts it. Either way, it verifies the exposure path the same way as
kind's script, and configures the same CoreDNS split-horizon DNS (see "DNS
inside the cluster" below) once `CONSOLE_CLUSTER_DOMAIN` is finalized.
Same env vars as kind's script, plus (for CI-style resource
sizing — all optional, no-op unless set): `MINIKUBE_CPU_COUNT`,
`MINIKUBE_MEMORY`, `MINIKUBE_DISK_SIZE`, `MINIKUBE_EXTRA_ADDONS`
(comma-separated, appended to the base `ingress` addon),
`MINIKUBE_INSECURE_REGISTRY`, and `MINIKUBE_EXTRA_CONFIG` (passed through
to one or more `--extra-config` flags — **semicolon**-separated, not
comma-separated, since a single `--extra-config` value like
`apiserver.authorization-mode=RBAC,Node` legitimately contains a comma).

### `kind/setup-registry.sh` / `minikube/setup-registry.sh [--registry-port 5000]`

Sets up a local image registry that's resolvable as `localhost:5000` both
from this host (to push into) and from inside the cluster (for kubelet to
pull from) — needed because OLM's `CatalogSource` controller hardcodes
`imagePullPolicy: Always` on the registry pod it creates, so simply having
the image sitting in the node's container runtime isn't enough; it has to
be genuinely pullable.

- `kind/`: follows [kind's own documented local-registry
  pattern](https://kind.sigs.k8s.io/docs/user/local-registry/) — a
  `registry:2` container on the kind network, with each node's containerd
  pointed at it via a dropped-in `hosts.toml`.
- `minikube/`: enables the built-in `registry` addon (its `registry-proxy`
  DaemonSet already makes `localhost:5000` resolve from every node) and
  exposes it to the host via a persistent `kubectl port-forward`, the same
  no-sudo approach `create-cluster.sh` uses for ingress.

### `kind/load-images.sh` / `minikube/load-images.sh [--registry localhost:5000] [--group streamshub] [--tag <tag>]`

Pushes the images `common/build-console.sh` just built to the registry
`setup-registry.sh` set up, via `skopeo copy --preserve-digests
--dest-tls-verify=false`. Two things that matter and aren't obvious:

- `--dest-tls-verify=false`, not `docker push`/`podman push`: the registry
  is plain HTTP, and getting a container engine to trust that for an
  arbitrary local hostname needs global daemon config that varies (or
  doesn't work at all) across docker/podman/Colima/Docker Desktop. skopeo's
  flag sidesteps that per-invocation.
- `--preserve-digests`: without it, skopeo re-serializes the manifest on
  copy, which changes its digest — and `modify-bundle-metadata.sh` already
  baked the *pre-push* digest into the CSV by reference. Dropping this flag
  breaks the operator/API image pulls with a confusing "not found" error.

Defaults match `build-console.sh`'s, so running both with no flags just
works. Prints the resulting catalog image reference to feed into
`setup-catalogsource.sh` / `deploy-example-console.sh`.

### `minikube/enable-tunnel.sh`

Switches minikube's exposure from the port-forward to `minikube tunnel` for
portless access: stops the port-forward, patches `ingress-nginx-controller`
to `LoadBalancer`, then runs the tunnel itself as a managed background
process. The one thing it can't avoid is the sudo prompt needed to bind
ports 80/443 — run it directly (not `sudo`-prefixed) and you'll be asked
for your password exactly once, right there; everything else runs
unattended.

### `kind/delete-cluster.sh` / `minikube/delete-cluster.sh [--keep-cluster]`

- Default: full delete (`kind delete cluster` / `minikube delete`),
  including the registry container (`kind/`) or registry port-forward
  (`minikube/`) `setup-registry.sh` set up.
- `--keep-cluster`: removes just the deployed resources (Console, Kafka,
  Console operator, Strimzi, OLM) but leaves the cluster + ingress-nginx
  (and registry) running, so a re-run of the `common/` scripts skips
  cluster creation and image pulls. Requires `operator-sdk` to cleanly
  uninstall OLM in this mode.

`minikube/delete-cluster.sh` also stops whichever ingress exposure
mechanism (port-forward or tunnel) is currently running.

### `common/build-console.sh [--registry localhost:5000] [--group streamshub] [--tag <tag>]`

Builds the Console images (`console-api`, `console-operator`,
`console-operator-bundle`, `console-operator-catalog`) from local source via
Maven + the bundle/catalog build scripts under `operator/bin/` — mirrors the
build half of the project's old `systemtests/scripts/setup-minikube.sh`.
Nothing is pushed
anywhere; images are tagged `<registry>/<group>/<image>:<tag>` and left in
the local docker/podman daemon for `kind/load-images.sh` or
`minikube/load-images.sh` to push on from there. `<tag>` defaults to the
Maven project version, lowercased. Cluster-agnostic — doesn't touch
kubectl.

On a docker CLI whose active buildx builder uses the `docker-container`
driver (common once you've set up multi-platform builds), a plain `docker
build` only populates buildx's own cache, not the local image store it
needs to end up in — this script works around it by passing
`quarkus.docker.buildx.platform` through to Quarkus's build (which then
adds the `--load` flag itself) and by passing `--load` directly on its own
`docker build` calls for the bundle/catalog images. Podman isn't affected
(no buildx involved).

### `common/setup-catalogsource.sh --image <image> [--namespace olm] [--name streamshub-console-catalog]`

Installs OLM v0.45.0 if needed, then creates a `CatalogSource` pointed at
`--image` and waits for it to report `READY`. Cluster-agnostic — works
against whatever context is currently active.

### `common/deploy-example-console.sh --catalog-image <image> [options]`

Deploys everything else in one shot: Strimzi (via an OLM Subscription,
channel derived from this repo's `strimzi-api.version` — same mechanism
`playwright-tests.yml` uses, not Helm), the Console operator (via the
CatalogSource from `setup-catalogsource.sh`), a Kafka cluster, and a
Console instance — using the project's own `examples/kafka/*.yaml` and
`examples/console/010-Console-example.yaml` quickstart manifests. Options:

```
--catalog-image        (required)
--catalog-namespace    default: olm
--catalog-name         default: streamshub-console-catalog
--channel              default: alpha
--operator-namespace   default: operators
--kafka-namespace      default: console-namespace
```

Console connects to Kafka over the example's default `secure` (ingress)
listener on every OS and cluster type — no listener workaround needed.
See "DNS inside the cluster" below for why that's safe to rely on even
when `CONSOLE_CLUSTER_DOMAIN` is a host-loopback address.

## DNS inside the cluster

`kind/create-cluster.sh` and `minikube/create-cluster.sh` both configure
CoreDNS with **split-horizon DNS** for `CONSOLE_CLUSTER_DOMAIN`
(`configure_dns_split_horizon` in `common/common.sh`): any `*.<domain>`
query that originates from *inside* the cluster (a Pod) is answered
directly with the ingress controller's ClusterIP, while the exact same
hostname still resolves the normal way (via real public nip.io DNS) for
anything outside the cluster — your browser, a test process on the host,
etc. Same hostname, different (and each correct for its own context)
answer depending on who's asking.

This matters because `CONSOLE_CLUSTER_DOMAIN` defaults to a
`127.0.0.1.nip.io`-style address on both kind (always, any host OS) and
minikube-on-macOS. `127.0.0.1` means something different to every asker:
from the host it's genuinely the host (where Docker/colima/podman forward
ports 80/443 to the node), but from inside a Pod it's that Pod's own
loopback interface — never the ingress controller. Kafka's `secure`
listener is ingress-exposed and addressed by exactly this kind of
hostname, and Console is itself a Pod: without the DNS fix, Console's own
connection to Kafka resolves to its own loopback and every Kafka API call
times out ("Timed out waiting for a node assignment"), even though both
Console and Kafka are perfectly healthy — confirmed hitting this directly
running the real systemtests suite against a local kind cluster.

minikube-on-Linux doesn't have this problem (its domain is the node's own
real, already-routable IP, reachable from both host and Pods without any
trick), so the fix is a no-op there in practice — it's applied
unconditionally rather than detected per cluster type, since doing so is
harmless and keeps the scripts from needing to special-case it.
