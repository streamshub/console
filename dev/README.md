# Local development environment (`dev/`)

A single script, `dev/dev.sh`, that stands up everything you need to work on the
console locally and launches whichever of the three development modes you want:

| Mode | You edit | What runs where |
| --- | --- | --- |
| **Frontend** | React/TS in `api/src/main/webui` | API+UI run on your host via `quarkus:dev` (Quinoa gives the React app hot-module reload) |
| **Backend** | Quarkus API in `api/` | API+UI run on your host via `quarkus:dev` (Java live-reload, remote debug on `5005`) |
| **Operator** | Quarkus operator in `operator/` | Operator runs on your host via `quarkus:dev` and reconciles a `Console` CR into the cluster |

In every mode a **kind** cluster hosts the backing infrastructure — Strimzi + Kafka
plus any optional profiles you enable — exposed on host ports `80`/`443` via
`ingress-nginx` with `*.127.0.0.1.nip.io` DNS. Frontend/backend modes run the console
*on your host* and reach Kafka over its TLS ingress listener; operator mode runs the
console *in the cluster*.

---

## Prerequisites

Install once (macOS/Homebrew shown; use your package manager on Linux):

```bash
brew install kind kubectl helm jq gettext   # gettext provides `envsubst`
brew link --force gettext                    # put envsubst on PATH
# JDK 21 + Maven for the quarkus:dev loops, Node for the UI build:
brew install openjdk@21 maven node
# Container engine — docker via Colima is the default (see "Container engines"):
brew install docker colima
```

Start the container engine before your first `up`:

```bash
colima start --cpus 6 --memory 16 --disk 60
```

> `envsubst` (from `gettext`) is required — the toolkit templates manifests with it.

---

## Quick start

```bash
# 1. Provision the cluster + all optional features (lean single-node Kafka):
dev/dev.sh up --profile all

# 2. Launch your mode of choice:
dev/dev.sh frontend     # or: backend / operator

# 3. See where everything lives:
dev/dev.sh urls

# 4. Tear down when done:
dev/dev.sh down                 # deletes the whole cluster
dev/dev.sh down --keep-cluster  # keeps the cluster + ingress, drops the workloads
```

`up` is idempotent — re-running it reuses an existing cluster and only fills in
what's missing. The three mode commands regenerate configuration from the live
cluster each time, so you can switch between them freely.

---

## Commands

```
dev.sh up [--full] [--profile <list>]   Create the cluster + infrastructure
dev.sh backend                          Run API+UI locally for backend dev
dev.sh frontend                         Run API+UI locally for frontend dev
dev.sh operator                         Run the operator locally against the cluster
dev.sh config                           Regenerate .gen/console-config.yaml + .gen/console-cr.yaml
dev.sh status                           Cluster + component health
dev.sh urls                             Print component URLs
dev.sh down [--keep-cluster]            Tear down
```

Run `dev.sh --help` for the full, self-documenting reference.

### `up` flags

- `--profile metrics,registry,keycloak,connect` (comma-separated, or repeat the flag)
  or `--profile all`. Selects the optional ecosystem components below.
- `--full` — deploy the repo's 3-broker `examples/kafka` topology (with Cruise Control,
  for rebalance/reassignment demos) instead of the default lean single-node Kafka.

---

## Profiles

The base stack is always Strimzi + one Kafka cluster (`console-kafka`) with demo topics.
Profiles add the surrounding ecosystem so you can exercise every console feature:

| Profile | Deploys | Unlocks in the console | Rough footprint |
| --- | --- | --- | --- |
| `metrics` | Prometheus operator + a `Prometheus` scraping Kafka | Broker/topic **metrics charts**, cluster health | ~1–1.5 GB |
| `registry` | Apicurio Registry (in-memory) | **Schema registry** integration for topics | ~0.3 GB |
| `keycloak` | Keycloak (dev mode) + a `streamshub` realm | **OIDC login** with demo users + role-based access | ~0.6 GB |
| `connect` | A Strimzi `KafkaConnect` cluster | **Kafka Connect** view (connectors/tasks) | ~0.7 GB |

Demo Keycloak users (realm `streamshub`, client `streamshub-console-client`):

| User | Password | Group → role |
| --- | --- | --- |
| `admin-user` | `admin123` | `administrators` (full access) |
| `dev-user` | `dev123` | `developers` (read-only) |

Keycloak's own admin console is `admin` / `admin`.

> **OIDC and operator mode:** the Keycloak profile is wired for **frontend/backend**
> modes (the browser and the host-run API both reach Keycloak at
> `http://keycloak.127.0.0.1.nip.io`). It is intentionally *not* wired into the
> operator-mode `Console` CR: an in-cluster console validates the token issuer against
> that same hostname, which an in-cluster pod can't resolve without extra DNS wiring.
> Use `dev.sh frontend`/`backend` to exercise the login flow.

---

## The three developer tasks

### Frontend

```bash
dev/dev.sh up --profile all
dev/dev.sh frontend            # API+UI on http://localhost:8080
```

- Edit any component under `api/src/main/webui` — Quinoa's Vite dev server hot-reloads
  it in the browser with no restart.
- Run the component tests and Storybook from the UI module:
  ```bash
  cd api/src/main/webui
  npm test
  npm run dev        # standalone Vite (optional) — needs the API on :8080, see below
  ```
- The standalone `npm run dev` server proxies `/api` to `http://localhost:8080`, so keep
  `dev.sh frontend` (or `backend`) running alongside it. For most work the integrated
  `quarkus:dev` HMR is simpler and is the recommended loop.

### Backend

```bash
dev/dev.sh up --profile all
dev/dev.sh backend             # API+UI on http://localhost:8080, debug on :5005
```

- Edit any Java under `api/` — Quarkus live-reloads on the next request.
- Attach a debugger to `localhost:5005` (the mode starts with debugging enabled).
- Handy endpoints: `http://localhost:8080/swagger-ui`, `/openapi`, `/q/health`, `/metrics`.
- The API reaches the in-cluster Kafka over its TLS ingress listener using a SCRAM
  password and cluster CA that `dev.sh` pulls straight from the live cluster into
  `dev/.gen/console-config.yaml` (regenerated every run — inspect it to see the exact wiring).

### Operator

```bash
dev/dev.sh up --profile all
dev/dev.sh operator            # operator runs on host, reconciles into the cluster
```

- `dev.sh operator` runs `mvn -am -pl operator quarkus:dev`. Once the operator registers
  its CRD, the generated `Console` CR (`dev/.gen/console-cr.yaml`) is applied automatically.
- Watch it reconcile and open the console:
  ```bash
  kubectl get console -A -w
  # console comes up at (open in a browser — `open` on macOS, `xdg-open` on Linux):
  open https://example-console.127.0.0.1.nip.io
  ```
- Edit a reconciler or dependent resource under `operator/` — Quarkus live-reloads and
  re-reconciles against the cluster.
- The operator deploys a **released** `console-api` image (the current `-SNAPSHOT` isn't
  published). Point it at a specific build with `CONSOLE_API_IMAGE=... dev/dev.sh operator`.
- Operator mode uses Kafka's internal `plain` listener and in-cluster service DNS, so no
  host credentials are involved.

---

## Container engines

`dev.sh` defaults to **docker** — via Colima on macOS, via the native daemon on Linux —
the most reliable path for the full Kafka + Console workload. **podman is fully
supported**; set `CONTAINER_ENGINE=podman`.

> **On macOS** follow the Colima / podman-machine notes below. **On Linux** docker and
> podman run natively (no VM); see the [On Linux](#on-linux) section for the two
> environment tweaks that can be needed.

### Colima (default, macOS)

```bash
colima start --cpus 6 --memory 16 --disk 60
```

Even with Colima you still need the docker *client* (`brew install docker`); `dev.sh`
checks for it and for a reachable daemon, with guidance if either is missing.

### podman (alternative)

```bash
CONTAINER_ENGINE=podman dev/dev.sh up --profile all
```

On macOS podman runs inside a VM ("machine"). Two workarounds are needed for the full
stack — apply them **before** `up`:

1. **Use a rootful machine.** The full Kafka+Console workload is unreliable on a rootless
   machine. Create the machine rootful (the toolkit's cluster bootstrap does this for a
   brand-new machine, but an *existing* rootless machine must be recreated):
   ```bash
   podman machine stop
   podman machine rm
   podman machine init --rootful --cpus 6 --memory 16384 --disk-size 60
   podman machine start
   ```

2. **Raise the container PID limit.** Under load, Kafka/Console pods crash-loop or OOM
   because the default per-container PID limit is too low. Symptom: pods dying under the
   full stack with no obvious cause. Raise it inside the machine:
   ```bash
   podman machine ssh 'printf "\n[containers]\npids_limit = 4096\n" | sudo tee -a /etc/containers/containers.conf'
   podman machine stop && podman machine start
   ```

The toolkit auto-sizes a new podman machine to *(host memory − 4 GB)*, which is too
aggressive on a 36 GB host. Cap it so the host stays responsive:

```bash
PODMAN_MACHINE_MEMORY=16000 CONTAINER_ENGINE=podman dev/dev.sh up ...
```

### On Linux

Both docker and podman run **natively** on Linux — there is no VM, so the Colima /
`podman machine` steps above don't apply (and `podman machine ...` commands don't exist).
Start the docker daemon the usual way (`sudo systemctl start docker`) and make sure your
user can reach it (be in the `docker` group, or use rootless docker). Two Linux-specific
things can trip up first-time setup:

1. **Privileged ports 80/443 with a rootless engine.** The cluster maps ingress onto host
   ports 80 and 443. The **rootful** docker daemon (the distro default) binds these
   without ceremony. A **rootless** docker/podman cannot bind ports below 1024 unless you
   lower the threshold once:
   ```bash
   sudo sysctl net.ipv4.ip_unprivileged_port_start=80
   # persist: echo 'net.ipv4.ip_unprivileged_port_start=80' | sudo tee /etc/sysctl.d/99-kind-ingress.conf
   ```
   Alternatively point ingress at high ports and adjust the domain accordingly:
   ```bash
   INGRESS_HTTP_PORT=8080 INGRESS_HTTPS_PORT=8443 dev/dev.sh up ...
   ```
   (URLs then include the port, e.g. `https://example-console.127.0.0.1.nip.io:8443`.)

2. **`nip.io` and DNS-rebind protection.** The stack relies on `*.127.0.0.1.nip.io`
   resolving to `127.0.0.1`. Some Linux resolvers (systemd-resolved, or a local dnsmasq)
   treat a public name resolving to loopback as a rebind attack and drop it. If name
   resolution fails, either disable rebind protection for `nip.io`, or add the hosts you
   use to `/etc/hosts`, e.g.:
   ```
   127.0.0.1 bootstrap.console-kafka.127.0.0.1.nip.io example-console.127.0.0.1.nip.io \
             prometheus.127.0.0.1.nip.io registry.127.0.0.1.nip.io \
             keycloak.127.0.0.1.nip.io connect.127.0.0.1.nip.io
   ```

Rootless podman on Linux additionally needs the host's `ip_tables` kernel module loaded;
`dev.sh up` fails fast with the exact fix if it's missing. The PID-limit workaround from
the podman section applies to Linux too, but you set it directly on the host
(`/etc/containers/containers.conf`) — there is no machine to `ssh` into.

---

## Resource guidance

Memory sizing depends on your engine:

| Configuration | Approx. cluster memory | macOS Colima / podman VM | Native Linux |
| --- | --- | --- | --- |
| Lean Kafka, no profiles | ~3–4 GB | `--memory 8` | no tuning |
| Lean Kafka + all four profiles | ~7–9 GB | `--memory 16` | no tuning |
| `--full` (3-broker) Kafka + all profiles | ~12–14 GB | `--memory 20` | no tuning |

On macOS the VM is a hard cap, so size it to leave ~16–28 GB for the host on a 36 GB
machine. On native Linux there is no VM — containers draw directly from host RAM — so a
36 GB host runs any of these comfortably with no sizing needed.

---

## Troubleshooting

- **`docker`/daemon not found** — on macOS install the docker client (`brew install docker`)
  and start Colima (`colima start ...`); on Linux start the daemon
  (`sudo systemctl start docker`) and ensure your user can use it. Or switch engines with
  `CONTAINER_ENGINE=podman`.
- **Cluster create fails binding port 80/443 on Linux** — you're likely on a rootless
  engine; see [On Linux](#on-linux) (privileged ports).
- **`*.127.0.0.1.nip.io` won't resolve on Linux** — DNS-rebind protection; see
  [On Linux](#on-linux).
- **Kafka never becomes Ready** — the first run pulls images and can take several minutes.
  Check progress with `dev/dev.sh status` and `kubectl -n kafka get pods`. Persistent
  crash-loops on podman usually mean the PID-limit/rootful workarounds above are needed.
- **Ingress returns 503 briefly** — normal right after a component starts or during a
  live-reload; retry once the pod is Ready.
- **TLS/hostname errors from the host-run API** — the broker cert covers the `nip.io`
  hosts, so this usually means a stale `dev/.gen/` from a previous cluster. Regenerate
  with `dev/dev.sh config` (or just re-run the mode command, which regenerates).
- **Wrong kube context** — `dev.sh` guards every cluster operation to the
  `kind-console-local` context and refuses to touch anything else.
- **Start over cleanly** — `dev/dev.sh down` deletes the cluster and the generated
  `dev/.gen/` artefacts.

---

## What's generated

Everything the toolkit generates lives in `dev/.gen/` (git-ignored):

- `console-config.yaml` — config for the host-run API (frontend/backend modes). The
  console trusts the Kafka TLS listener automatically via the discovered Strimzi cluster
  CA (thanks to `kubernetes.enabled`), so no truststore file is needed.
- `console-cr.yaml` — the `Console` CR applied in operator mode.
- `state.env` — records which topology/profiles the running environment was brought up with.

`dev/manifests/console-cr/console.yaml` is a reference document showing the full
operator-mode CR shape; it is **not** applied directly — `dev.sh` generates a
profile-accurate CR at `dev/.gen/console-cr.yaml`.
