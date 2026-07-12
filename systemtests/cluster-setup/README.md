# Cluster setup (macOS-safe local Kubernetes for systemtests)

Provisions a local Kubernetes cluster for running the `console-operator`
systemtests outside CI, on macOS where `systemtests/scripts/setup-minikube.sh`
doesn't work reliably: Docker Desktop / Podman Desktop / Colima all run the
container engine inside a hidden VM, so a minikube node's IP (used by CI's
`$(minikube ip).nip.io`) isn't routable from the host.

Two options, kept fully independent of each other (`kind/` doesn't touch or
depend on `minikube/`'s scripts, or vice versa):

- **`kind/` — recommended.** Reliable, few moving parts, portless URLs out
  of the box, no sudo ever needed.
- **`minikube/`** — works too, but needs either a `:8443`-suffixed URL or an
  extra sudo-gated step for portless access, and hit a real podman PID-limit
  bug under a full Kafka-heavy stack (documented below, fixed, but a real
  wrinkle kind never had).

**Recommendation: default to `kind/` for day-to-day work.** Minikube's only
edge is closer parity with CI's tooling, which doesn't matter much for local
dev/Playwright testing since `common/`'s scripts are pure kubectl/helm and
don't care which cluster distro they're talking to (proven — see below).
Keep `minikube/` around for when you specifically need it.

Full narrative — every dead end, root-caused with evidence (the Strimzi API
version mismatch, the OLM `Route` CRD quirk, the SSL-passthrough crash, the
loopback-collision bug, the finalizer race, the podman PID limit) — is in
`~/claude-docs/streamshub-local-sts/README.md`.

## Layout

```
cluster-setup/
  kind/                    # recommended: kind-based cluster
    create-cluster.sh      # create/reuse cluster + ingress-nginx, verify with a smoke test
    delete-cluster.sh      # teardown (full, or --keep-cluster)
    lib/env.sh             # shared config + podman-machine helpers
  minikube/                # alternative: minikube-based cluster
    create-cluster.sh      # create/reuse cluster + ingress addon, verify with a smoke test
    delete-cluster.sh      # teardown (full, or --keep-cluster)
    enable-tunnel.sh       # switch to portless access (needs sudo once)
    lib/env.sh             # shared config + podman-machine helpers
  common/                  # cluster-agnostic — works against either
    setup-catalogsource.sh       # install OLM + a CatalogSource
    deploy-example-console.sh    # Strimzi + Kafka + Console operator + Console instance
```

`common/` scripts have zero kind/minikube-specific logic (pure
kubectl/helm/envsubst against whatever cluster context is currently active)
— confirmed by running them unmodified against both cluster types.

## Quick start

### kind

```sh
./kind/create-cluster.sh
source kind/.cluster-env
../common/setup-catalogsource.sh --image <catalog-image> [--namespace olm] [--name strimzi-source]
../common/deploy-example-console.sh --catalog-image <catalog-image>
```
(run the `common/` scripts from inside `kind/`, or adjust the relative path —
they just need to be invoked with a cluster context already active)

Ends with a working, portless Console UI at
`https://example-console.127.0.0.1.nip.io/`, backed by a real Kafka cluster.

Teardown: `./kind/delete-cluster.sh` (or `--keep-cluster`).

### minikube

```sh
./minikube/create-cluster.sh
source minikube/.cluster-env
../common/setup-catalogsource.sh --image <catalog-image> [--namespace olm] [--name strimzi-source]
../common/deploy-example-console.sh --catalog-image <catalog-image>
```

By default, access URLs need the port suffix printed by `create-cluster.sh`
(e.g. `https://example-console.127.0.0.1.nip.io:8443/`). For portless URLs
(e.g. OIDC/auth flows that assume no port in the redirect URI), run
`./minikube/enable-tunnel.sh` instead (prompts for your sudo password once).

Teardown: `./minikube/delete-cluster.sh` (or `--keep-cluster`).

All scripts in both directories are idempotent — safe to re-run.

## `kind/create-cluster.sh`

Creates (or reuses) a kind cluster with ports 80/443 mapped to the host at
cluster-creation time (`extraPortMappings`), installs ingress-nginx bound
via `hostPort` on the node, patches in `--enable-ssl-passthrough` (required
once a Kafka cluster's TLS-terminating `secure` listener shows up —
otherwise ingress-nginx's worker processes crash: `exited with fatal code 2
and cannot be respawned`), then **verifies the whole exposure path with a
smoke test** (deploys a throwaway echo-server behind an Ingress, curls it
over HTTP/HTTPS, cleans up) before declaring the cluster ready. If the
smoke test fails, the script exits non-zero rather than claiming success.

Cluster domain is `127.0.0.1.nip.io` (public wildcard DNS), so any
`<name>.127.0.0.1.nip.io` hostname resolves to `127.0.0.1` on this machine
and reaches whatever ingress-nginx routes it to by Host header/SNI —
Console, Kafka's TLS-passthrough listener, Apicurio, all through the same
two exposed ports, portlessly, no sudo ever required.

Env vars (all optional, see `kind/lib/env.sh`):

- `CONTAINER_ENGINE` — `podman` (default) or `docker`
- `CLUSTER_NAME` — default `console-local`
- `INGRESS_HTTP_PORT` / `INGRESS_HTTPS_PORT` — default `80`/`443`
- `CONSOLE_CLUSTER_DOMAIN` — default `127.0.0.1.nip.io`
- `PODMAN_MACHINE_CPUS` / `PODMAN_MACHINE_MEMORY` (MB) — podman only;
  default to (host CPUs − 1) / (host memory − 4GB headroom), auto-detected
  via `sysctl`. **Only applied when a podman machine is created for the
  first time** — if one already exists with different sizing, the script
  warns and prints the manual resize command rather than restarting your
  machine (a resize requires stop/start, which would kill anything already
  running in it, including the cluster).

## `minikube/create-cluster.sh`

Creates (or reuses) a minikube cluster with the `ingress` addon, patches in
`--enable-ssl-passthrough` (same reason as kind), then exposes it and
**verifies with the same smoke test pattern** before declaring success.

Why not the same hostPort approach as kind? Confirmed empirically:

- minikube's `ingress` addon creates a **`NodePort`** Service, not
  hostPort-bound like kind's. Direct NodePort access from macOS to the
  minikube node IP (e.g. `curl 192.168.49.2:30674`) times out — reproduces
  a long-standing upstream minikube/docker-driver-on-macOS issue, confirmed
  on this actual machine, not just taken from GitHub issues on faith.
- `minikube tunnel` **can** give portless (`127.0.0.1`) access, but only
  after patching the Service to `LoadBalancer` (tunnel only manages
  LoadBalancer services, not NodePort — patching alone does nothing:
  without a running tunnel, the Service just reports `EXTERNAL-IP:
  127.0.0.1` cosmetically while nothing is actually listening). Binding
  ports 80/443 additionally needs an **interactive sudo password** —
  minikube's own tunnel process prints "sudo permission will be asked for
  it" for privileged ports.

So by default, `create-cluster.sh` exposes ingress-nginx via a persistent
background `kubectl port-forward` on non-privileged local ports (default
`8080`/`8443`, override via `LOCAL_HTTP_PORT`/`LOCAL_HTTPS_PORT`) — no sudo
needed, fully scriptable, but URLs need the port suffix. If the
port-forward process dies (less durable than kind's container-level port
mapping over long sessions), just re-run `create-cluster.sh` — it detects
the dead PID and restarts it.

Same `CONTAINER_ENGINE`/`PODMAN_MACHINE_CPUS`/`PODMAN_MACHINE_MEMORY` env
vars and podman-machine auto-sizing as kind (duplicated in `lib/env.sh`,
not shared, so `kind/` and `minikube/` stay fully independent).

### `minikube/enable-tunnel.sh` — portless access

Switches from the port-forward to `minikube tunnel`: stops the
port-forward, patches `ingress-nginx-controller` to `LoadBalancer`, then
runs the tunnel itself as a background process it manages (PID tracked in
`.tunnel.pid`, log in `.tunnel.log`) — not something you run in a separate
terminal.

The one thing it can't avoid: binding ports 80/443 needs `sudo`. The script
calls `sudo -v` right before starting the tunnel — **run it directly**
(`./enable-tunnel.sh`, not `sudo ./enable-tunnel.sh`), and you'll be
prompted for your password exactly once, right there. Everything after
that (starting the tunnel, waiting for it to actually bind, confirming
`EXTERNAL-IP` becomes `127.0.0.1`) runs unattended. Confirmed working.

```sh
./minikube/enable-tunnel.sh
# prompts for sudo password once, then:
# "Portless access ready: https://<name>.127.0.0.1.nip.io/"
```

To switch back to the port-forward approach: stop the tunnel
(`sudo kill $(cat minikube/.tunnel.pid)`) and re-run `create-cluster.sh` —
it detects no running tunnel and starts a fresh port-forward instead.
`delete-cluster.sh` stops whichever of the two (port-forward or tunnel) is
currently running automatically, including the `sudo -n kill` needed for a
root-owned tunnel process.

### Prerequisite for minikube: raise podman's PID limit (Kafka-heavy workloads)

minikube's docker/podman driver runs the **entire K8s node** (kubelet,
containerd, and every pod's processes) as a single podman container.
podman's default per-container PID limit is **2048**, which a full
Strimzi + multi-broker Kafka + Console + Prometheus stack can exhaust —
hit this in testing: the Console pod failed to start with `runc create
failed: ... fork/exec: resource temporarily unavailable` while
`podman inspect <container> --format '{{.HostConfig.PidsLimit}}'` showed
exactly `2048` and `podman stats` showed 2040 PIDs already in use. This
looks like a networking/LoadBalancer problem at first glance (a pod not
coming up) but isn't — check `podman inspect`/`podman stats` first if you
hit unexplained container start failures on minikube.

Fix (one-time, requires `sudo` inside the podman machine VM, so not
automated by these scripts):

```sh
podman machine ssh -- 'sudo mkdir -p /etc/containers && printf "[containers]\npids_limit = 8192\n" | sudo tee /etc/containers/containers.conf'
podman machine stop && podman machine start   # required — the running podman service caches config at boot
```

Two gotchas: editing `~/.config/containers/containers.conf` on the **macOS
host** does nothing (the real container runtime is inside the VM); and the
VM's default `/usr/share/containers/containers.conf` shouldn't be edited
directly — create the override at `/etc/containers/containers.conf`
instead. Verify with `podman inspect <minikube-container> --format
'{{.HostConfig.PidsLimit}}'` after recreating the cluster. kind hasn't hit
this (different container architecture per node), but there's no
fundamental reason it couldn't under a big enough workload — keep it in
mind either way.

## `common/setup-catalogsource.sh --image <image> [--namespace olm] [--name strimzi-source]`

Installs OLM v0.45.0 (same version/hash CI pins), then applies
`install/operator/olm/010-CatalogSource-console-operator-catalog.yaml`
patched with your `--name`/`--image`, and waits for it to report `READY`.

The "already installed" check goes beyond just checking a CRD exists — it
verifies `olm-operator`/`catalog-operator` are actually `Available`. A CRD
(or the `olm` namespace) can be left behind by a previous install that got
interrupted or crash-looped; skipping reinstall based on presence alone
would leave a broken OLM that fails confusingly much later (Subscriptions
never resolving, CatalogSources never reaching `READY`). If it detects a
prior install exists but isn't healthy, it uninstalls first (upstream's
`install.sh` refuses to touch an existing install, so a repair needs an
uninstall+reinstall, not just a re-run) — via `operator-sdk olm uninstall`
if available, otherwise it errors out with instructions (install
`operator-sdk`, or run `delete-cluster.sh` for a fresh cluster).

Tested this by deliberately breaking OLM (scaling `olm-operator` to 0
replicas): detection correctly triggered repair, and when the repair itself
timed out (a fully-dead `olm-operator` can't clear its own `CSV`
finalizers, so even `operator-sdk olm uninstall` can get stuck) the script
gave a clear "run delete-cluster.sh" message instead of falling through to
upstream's confusing "OLM is already installed... Exiting" error.

Note: the `--image` must be pullable by the cluster's containerd — a public
registry image works out of the box; a locally-built image needs
`kind load docker-image`/`kind load image-archive` (kind) or `minikube
image load` (minikube) first (not yet scripted here).

## `common/deploy-example-console.sh --catalog-image <image> [options]`

Deploys everything else in one shot: Strimzi (Helm), the Console operator
(OperatorGroup + Subscription against the CatalogSource from
`setup-catalogsource.sh`), a Kafka cluster, and a Console instance — using
the project's own `examples/kafka/*.yaml` and
`examples/console/010-Console-example.yaml` quickstart manifests (applied
the same documented `envsubst` way the repo README describes), patched
with a local-dev-only fix (see below).

Options (all have sensible defaults):
```
--catalog-image        (required)
--catalog-namespace    default: olm
--catalog-name         default: strimzi-source
--channel              default: alpha
--operator-namespace   default: co-namespace   (Strimzi + Console operator)
--strimzi-version      default: 0.51.0         (must match your console-operator build — see below)
--kafka-namespace      default: kafka          (Kafka cluster + Console instance)
--listener             default: scramplain     (internal listener Console uses)
```

**Version note:** Strimzi's version must match whatever console-operator
build you're using — `0.51.0` is what `console-operator-catalog:0.12.0` was
actually built against (confirmed by inspecting its bundled
`io.strimzi.api-0.51.0.jar`). A newer Strimzi (e.g. `1.0.0`, which GA'd the
Kafka `v1` CRD and dropped `v1beta2`) with an older console-operator build
causes a silent "No such Kafka resource" failure even though the Kafka CR
exists and is healthy — see the claude-docs writeup's "Layer 1" section.

**Why it patches listeners:** the example manifests only define the
`secure` (external, ingress+TLS) Kafka listener — correct for a real
cluster with a real routable domain, but broken on this local nip.io setup:
the Console **API pod runs inside the cluster**, and that listener's
external hostname resolves to `127.0.0.1` from inside a pod too — which is
the pod's own loopback, not the ingress controller. That produces
`Connection refused` → Kafka admin client timeouts → the UI showing "Timed
out waiting for backend service" for every data call. The script adds an
internal `scramplain` listener (SCRAM-SHA-512, no TLS, in-cluster Service
DNS) and points Console at it instead — a local-dev-only necessity, not a
product bug (CI's `$(minikube ip)`-based `nip.io` doesn't hit this loopback
collision).

## `kind/delete-cluster.sh` / `minikube/delete-cluster.sh` `[--keep-cluster]`

- Default: full delete (`kind delete cluster` / `minikube delete`) —
  deletes everything in one shot.
- `--keep-cluster`: removes just the deployed resources (Console/Kafka,
  Console operator, Strimzi, OLM) but leaves the cluster + ingress-nginx
  running, so a re-run of the `common/` scripts skips cluster creation and
  ingress image pulls. Requires `operator-sdk` to cleanly uninstall OLM
  (only for this mode — full teardown doesn't need it).

  Deletes the Strimzi/Console custom resources (`Console`, `KafkaTopic`,
  `KafkaUser`, `Kafka`, `KafkaNodePool`) *before* the namespace, because
  deleting the namespace first can tear down the entity-operator pod before
  it finishes clearing a `KafkaTopic`'s `strimzi.io/topic-operator`
  finalizer — a real race hit during testing that leaves the namespace
  stuck in `Terminating` forever. Falls back to force-clearing finalizers
  if the namespace still doesn't terminate in time.

  `minikube/delete-cluster.sh` also always stops the background
  port-forward or tunnel first (the tunnel is root-owned via sudo, so a
  plain `kill` can't touch it — tries `sudo -n kill` with cached
  credentials, falls back to telling you to do it manually).

## ⚠️ Known issue: the *real* systemtests always need the listener patch too

`common/deploy-example-console.sh` patches its own example Console CR to
use an internal listener (see above for why). **The real systemtests hit
the exact same problem, every run** — their `ConsoleInstanceSetup.java`
always creates the Console CR pointed at the external `secure` listener by
default, and there's currently no env var to change that. Since the
Console API pod runs inside the cluster, that produces the same
`Connection refused` → Kafka admin client timeout → UI shows "Timed out
waiting for backend service" for every data call, even though the UI
itself loads fine (it's served statically; only the backend API calls that
need Kafka fail).

This is **not fixed by these scripts** — it has to be done manually, every
time the real tests create a fresh Console instance (new hash-suffixed
name each run):

```sh
kubectl -n <kafka-namespace> get kafka <kafka-name> -o jsonpath='{.status.listeners}'  # find the internal SCRAM listener name (e.g. scramplain)
kubectl -n <kafka-namespace> patch console <console-name> --type='json' \
  -p='[{"op": "replace", "path": "/spec/kafkaClusters/0/listener", "value": "<internal-listener-name>"}]'
```

Confirmed this is independent of the exposure mechanism — hit it and fixed
it identically on both the port-forward and the tunnel/LoadBalancer paths.
A watcher script that auto-patches any new Console CR as soon as it
appears would remove the need to do this by hand; not written yet.

## ⚠️ Known issue: orphaned `minikube tunnel` process across cluster recreations

If `delete-cluster.sh` can't stop a running tunnel (no cached sudo
credentials for the non-interactive `sudo -n kill`), it now **deliberately
leaves `.tunnel.pid` in place** rather than silently dropping the only
record of it (an earlier version of this script removed the file
regardless — don't do that again if editing this). A root-owned tunnel
process left running across a cluster delete+recreate cycle keeps holding
ports 80/443 on the host while routing to a cluster that no longer
exists, which:

- blocks anything else (e.g. `kind/create-cluster.sh`) from binding those
  same ports (`address already in use`)
- can make a *freshly recreated* minikube cluster's Console instance look
  broken from the browser/Playwright side, when the real cause is a stale
  tunnel serving nothing useful — this happened once already and looked
  exactly like a networking regression until traced back to `ps aux | grep
  minikube tunnel` showing a process from hours earlier.

If you ever see `create-cluster.sh` or `enable-tunnel.sh` behaving oddly
right after a full teardown+recreate, check for stray tunnel processes
first: `ps aux | grep "[m]inikube tunnel"`, then `sudo kill` any with a
suspiciously old start time before re-running anything.

## Status

Full stack confirmed working end-to-end (2026-07-12), via scripts alone, on
both cluster types: cluster → ingress → OLM → Strimzi → Console operator →
Kafka → Console instance → UI reachable and functional (shows live Kafka
nodes/topics, confirmed via clean `console-api` logs with no timeout
errors). All scripts are idempotent, and all teardown paths (full,
`--keep-cluster`, the finalizer-race fallback) are verified working on
both.

Minikube's `enable-tunnel.sh` portless path is **confirmed working** by the
user in practice, including against a real systemtests run (Kafka +
Console instance created by the actual test framework, not
`deploy-example-console.sh`) — reachable at
`https://<console-name>.127.0.0.1.nip.io/` with no port suffix, once the
listener patch above is applied and no orphaned tunnel is interfering.

Not yet scripted / open items: Apicurio Registry, OIDC/Keycloak variants,
loading a locally-built (not published-registry) console/operator image
into either cluster type, wiring up Playwright to consume
`CONSOLE_CLUSTER_DOMAIN`/the printed Console URL automatically, and a
watcher to auto-patch new Console CRs to an internal listener (see known
issue above).
