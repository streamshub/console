#!/usr/bin/env bash
# Shared utilities for cluster-setup/{kind,minikube}. Source, don't execute.

OS_NAME="$(uname -s)"

# The only OS-conditional behavior left after unifying with
# systemtests/scripts/setup-minikube.sh is local exposure (reaching the
# cluster's ingress/registry from the host): Linux can reach the
# minikube/kind node directly; macOS's container engines run inside a
# hidden VM and can't. NEEDS_LOCAL_PORT_FORWARD is that one switch.
case "${OS_NAME}" in
  Darwin) NEEDS_LOCAL_PORT_FORWARD=true ;;
  Linux)  NEEDS_LOCAL_PORT_FORWARD=false ;;
  *)
    echo "Unsupported OS_NAME=${OS_NAME} (expected 'Darwin' or 'Linux')" >&2
    exit 1
    ;;
esac

# Podman machine sizing (only relevant on macOS, where podman needs a VM to
# run containers at all — native Linux podman doesn't). Defaults to (host
# CPUs - 1) and (host memory - 4GB headroom) so the host itself isn't
# starved; override with PODMAN_MACHINE_CPUS/PODMAN_MACHINE_MEMORY (MB).
# Portable: prefers Linux-native tools (nproc, /proc/meminfo), falls back
# to macOS's sysctl.
detect_default_cpus() {
  local total
  if command -v nproc >/dev/null 2>&1; then
    total=$(nproc)
  else
    total=$(sysctl -n hw.ncpu)
  fi
  if [ "${total}" -gt 1 ]; then
    echo $(( total - 1 ))
  else
    echo "${total}"
  fi
}

detect_default_memory_mb() {
  local total_mb headroom_mb=4096
  if [ -r /proc/meminfo ]; then
    total_mb=$(( $(awk '/^MemTotal:/ {print $2}' /proc/meminfo) / 1024 ))
  else
    total_mb=$(( $(sysctl -n hw.memsize) / 1024 / 1024 ))
  fi
  if [ "${total_mb}" -gt "${headroom_mb}" ]; then
    echo $(( total_mb - headroom_mb ))
  else
    echo "${total_mb}"
  fi
}

PODMAN_MACHINE_CPUS="${PODMAN_MACHINE_CPUS:-$(detect_default_cpus)}"
PODMAN_MACHINE_MEMORY="${PODMAN_MACHINE_MEMORY:-$(detect_default_memory_mb)}"

# Checks whether a command is available in PATH; if not, runs the given
# installer function. Generic by design — callers provide their own
# tool-specific installer (see install_skopeo below, or minikube/env.sh's
# install_minikube).
ensure_installed() {
  local bin="$1" installer="$2"
  if command -v "${bin}" >/dev/null 2>&1; then
    return 0
  fi
  echo "'${bin}' not found in PATH, installing..."
  "${installer}"
}

# Installs kubectl — every script here calls it directly. GitHub's Linux
# runners ship it, macOS runners don't.
install_kubectl() {
  local arch
  case "${OS_NAME}" in
    Darwin)
      brew install kubernetes-cli
      ;;
    Linux)
      case "$(uname -m)" in
        arm64|aarch64) arch=arm64 ;;
        x86_64|amd64)  arch=amd64 ;;
        *) echo "Unsupported architecture for kubectl auto-install: $(uname -m)." >&2; exit 1 ;;
      esac
      curl -Lo /tmp/kubectl "https://dl.k8s.io/release/$(curl -Ls https://dl.k8s.io/release/stable.txt)/bin/linux/${arch}/kubectl"
      chmod +x /tmp/kubectl
      sudo mv /tmp/kubectl /usr/local/bin/kubectl
      ;;
  esac
}

# macOS has no native container runtime: docker there needs a Linux VM
# behind it. Installs colima (+ docker CLI and buildx, which
# common/build-console.sh needs for `docker build --load`) via brew.
install_docker_colima() {
  brew install docker docker-buildx colima
  mkdir -p ~/.docker/cli-plugins
  ln -sfn "$(brew --prefix)/opt/docker-buildx/bin/docker-buildx" ~/.docker/cli-plugins/docker-buildx
}

# macOS + CONTAINER_ENGINE=docker only: installs docker/colima if docker is
# missing, and starts colima if no daemon is reachable. A no-op when Docker
# Desktop (or an already-running colima) provides the daemon. The VM must
# fit the minikube node, so size it with COLIMA_CPUS / COLIMA_MEMORY (GB);
# defaults match the podman machine sizing above (host - 1 CPU, host - 4GB).
# Call after CONTAINER_ENGINE has been determined.
ensure_docker_engine() {
  if [ "${OS_NAME}" != "Darwin" ] || [ "${CONTAINER_ENGINE}" != "docker" ]; then
    return 0
  fi

  ensure_installed docker install_docker_colima

  if ! docker info >/dev/null 2>&1 && command -v colima >/dev/null 2>&1; then
    local cpus="${COLIMA_CPUS:-$(detect_default_cpus)}"
    local mem="${COLIMA_MEMORY:-$(( $(detect_default_memory_mb) / 1024 ))}"
    echo "No docker daemon reachable — starting colima with ${cpus} CPUs / ${mem}GB memory..."
    colima start --cpu "${cpus}" --memory "${mem}"
  fi
}

# Installs skopeo — needed by both kind/load-images.sh and
# minikube/load-images.sh to push built images into the local registry.
# On Linux, also applies the AppArmor tweak skopeo needs to actually work
# on Ubuntu 24.04+ runners (restricts unprivileged user namespaces, which
# skopeo uses) — folded in here rather than left as a separate CI step,
# since it's specifically a "make skopeo work" concern, not a general one.
install_skopeo() {
  case "${OS_NAME}" in
    Darwin)
      brew install skopeo
      ;;
    Linux)
      sudo sysctl -w kernel.apparmor_restrict_unprivileged_userns=0
      sudo apt-get update
      sudo apt-get install -y skopeo
      ;;
  esac
}

# "localhost" resolves to both 127.0.0.1 and ::1, and getaddrinfo ordering
# can try the IPv6 one first. On macOS that's a real problem for the local
# registry: ControlCenter's AirPlay Receiver listens on port 5000 on the
# IPv6 wildcard by default (it's a system service, on whether you've ever
# used AirPlay or not), while the registry container's port is published
# IPv4-only (kind/minikube's setup-registry.sh both bind it explicitly to
# 127.0.0.1). A push that resolves "localhost" to ::1 silently lands on
# AirPlay instead of the registry and gets back a baffling "403 Forbidden"
# with no indication it's not even talking to a container registry.
#
# Used only for the skopeo *destination* URL in kind/load-images.sh and
# minikube/load-images.sh — the one place that makes a real host-level
# network connection. Locally-tagged image names and the catalog-image
# reference handed to setup-catalogsource.sh/deploy-example-console.sh
# must stay "localhost:<port>" unchanged: that string is also the key in
# the containerd hosts.toml config the cluster uses to resolve pulls, and
# has nothing to do with how the host happens to reach the registry.
registry_host_for_push() {
  echo "${1/localhost/127.0.0.1}"
}

# Which local daemon actually holds the images build-console.sh built — NOT
# $CONTAINER_ENGINE, which for minikube is the *driver* (can be vfkit/kvm2,
# neither of which builds or stores OCI images). Prefers docker, falling
# back to podman; if both are on PATH, docker may actually be podman's
# docker-CLI-compatibility shim, so ask it. Used by build-console.sh and
# both kind/load-images.sh and minikube/load-images.sh.
detect_image_runtime() {
  if command -v podman >/dev/null 2>&1 && command -v docker >/dev/null 2>&1; then
    if docker --version | grep -q podman; then
      echo podman
    else
      echo docker
    fi
  elif command -v podman >/dev/null 2>&1; then
    echo podman
  else
    echo docker
  fi
}

# skopeo source-transport prefix for reading out of the given runtime's
# local image store (see detect_image_runtime above).
skopeo_transport_for() {
  if [ "$1" = "podman" ]; then
    echo "containers-storage:"
  else
    echo "docker-daemon:"
  fi
}

# Ensures a podman machine exists and is running — but only on macOS.
# Native Linux podman talks to the local system directly; there's no VM to
# create or manage, so this is a no-op there regardless of CONTAINER_ENGINE.
# Only sizes the machine (via PODMAN_MACHINE_CPUS/PODMAN_MACHINE_MEMORY) on
# first-time init — an existing machine is never resized automatically,
# since that requires stopping it (killing anything running inside,
# including the kind cluster).
ensure_podman_machine() {
  if [ "${OS_NAME}" != "Darwin" ]; then
    return 0
  fi

  local name state cpus mem

  name=$(podman machine list --format json | jq -r '.[0].Name // empty')

  if [ -z "${name}" ]; then
    echo "No podman machine found — initializing 'podman-machine-default' with ${PODMAN_MACHINE_CPUS} CPUs / ${PODMAN_MACHINE_MEMORY}MB memory..."
    podman machine init --rootful --cpus "${PODMAN_MACHINE_CPUS}" --memory "${PODMAN_MACHINE_MEMORY}"
    podman machine start
    return
  fi

  state=$(podman machine inspect "${name}" | jq -r '.[0].State')
  if [ "${state}" != "running" ]; then
    echo "Starting existing podman machine '${name}'..."
    podman machine start "${name}"
  fi

  cpus=$(podman machine inspect "${name}" | jq -r '.[0].Resources.CPUs')
  mem=$(podman machine inspect "${name}" | jq -r '.[0].Resources.Memory')
  if [ "${cpus}" != "${PODMAN_MACHINE_CPUS}" ] || [ "${mem}" != "${PODMAN_MACHINE_MEMORY}" ]; then
    echo "Note: podman machine '${name}' is already sized at ${cpus} CPUs / ${mem}MB memory (requested ${PODMAN_MACHINE_CPUS}/${PODMAN_MACHINE_MEMORY}MB). Not resizing an existing machine automatically." >&2
    echo "  To resize: podman machine stop ${name} && podman machine set --cpus ${PODMAN_MACHINE_CPUS} --memory ${PODMAN_MACHINE_MEMORY} ${name} && podman machine start ${name}" >&2
  fi
}

# On native Linux with rootless podman, kube-proxy/ingress-nginx need the
# host's ip_tables kernel module loaded — rootless containers can't load
# kernel modules themselves (no CAP_SYS_MODULE), so a missing module
# surfaces confusingly later as ingress-nginx/kube-proxy failing, not as an
# obvious "module missing" error. Check up front and fail fast instead.
# No-op on macOS (podman machine VM, handled by ensure_podman_machine) and
# on rootful podman/docker (which don't hit this).
check_linux_rootless_podman_ip_tables() {
  if [ "${OS_NAME}" != "Linux" ] || [ "${CONTAINER_ENGINE}" != "podman" ]; then
    return 0
  fi

  if [ "$(podman info --format json | jq -r .host.security.rootless)" != "true" ]; then
    return 0
  fi

  if [ -z "$(lsmod | grep ^ip_tables)" ]; then
    echo "podman is running rootless on Linux, but the 'ip_tables' kernel module isn't loaded." >&2
    echo "Rootless containers can't load kernel modules themselves (no CAP_SYS_MODULE), and" >&2
    echo "kube-proxy/ingress-nginx need it on the host - without it, ingress setup fails." >&2
    echo "" >&2
    echo "Fix:" >&2
    echo "  sudo modprobe ip_tables" >&2
    echo "  echo ip_tables | sudo tee /etc/modules-load.d/ip_tables.conf   # persist across reboots" >&2
    exit 1
  fi
}

# Configures CoreDNS with split-horizon DNS for the cluster's ingress domain:
# any "*.<domain>" query that originates from *inside* the cluster (a Pod) gets
# answered directly with the ingress controller's ClusterIP, instead of being
# forwarded out to the real public nip.io DNS — which only knows the domain's
# embedded IP (127.0.0.1 for both kind's and macOS-minikube's default
# CONSOLE_CLUSTER_DOMAIN), and 127.0.0.1 means something different to every
# asker: from the host it's genuinely the host (where Docker/colima/podman
# forward 80/443 to the node), but from inside a Pod it's that Pod's own
# loopback, never the ingress controller. The host keeps resolving the exact
# same hostname via real DNS, completely unaffected — this only intercepts
# queries CoreDNS itself answers, so it's genuinely split-horizon, not a
# global override.
#
# Why this matters: Kafka's `secure` listener (ingress-exposed — used by
# anything connecting to Kafka from outside the cluster network, including
# Console, which is itself a Pod) is addressed by exactly this kind of
# hostname. Without this, Console's own connection to Kafka resolves
# "bootstrap.<kafka>.<domain>" to its own loopback and every Kafka API call
# times out ("Timed out waiting for a node assignment") even though both
# Console and Kafka are perfectly healthy. Confirmed happening on kind
# (regardless of host OS — it always uses the 127.0.0.1 trick) and would
# equally affect minikube's macOS exposure path for the same reason.
# minikube-on-Linux doesn't hit this (its domain is the node's own real,
# already-routable IP), but applying this there too is harmless — same
# ingress Service, just reached one hop more directly — so it's done
# unconditionally rather than detected per cluster type.
#
# Idempotent: re-running replaces the previously-injected block (tracked by
# the BEGIN/END marker comments) rather than duplicating it, so a second
# create-cluster.sh run (or one against a changed CONSOLE_CLUSTER_DOMAIN)
# converges cleanly instead of accumulating stale template blocks.
#
# Usage: configure_dns_split_horizon <domain> <ingress-namespace> [<ingress-service>]
configure_dns_split_horizon() {
  local domain="$1" ingress_namespace="$2" ingress_service="${3:-ingress-nginx-controller}"
  local ingress_ip domain_regex marker_begin marker_end stripped_file block_file final_file line_count apply_output

  ingress_ip=$(kubectl get svc -n "${ingress_namespace}" "${ingress_service}" -o jsonpath='{.spec.clusterIP}' 2>/dev/null)
  if [ -z "${ingress_ip}" ]; then
    echo "Could not resolve ClusterIP for Service ${ingress_namespace}/${ingress_service}; skipping DNS split-horizon setup" >&2
    return 1
  fi

  domain_regex="${domain//./\\.}"
  # Deliberately plain (no parens/punctuation beyond '#' and ':') so they're
  # safe to use as literal sed addresses with zero regex-escaping below.
  marker_begin="# cluster-setup:dns-split-horizon:begin"
  marker_end="# cluster-setup:dns-split-horizon:end"

  # Strip any block from a previous run before re-inserting a fresh one —
  # covers both "nothing changed" (pure no-op re-apply) and "domain or
  # ingress IP changed since last run" (stale entry would otherwise linger
  # alongside the new one). Done with sed, not awk: macOS ships the
  # non-GNU "one true awk", which rejects multi-line -v values outright
  # ("newline in string") - sed with temp files avoids that trap entirely
  # and is identical between BSD (macOS) and GNU (Linux) for everything
  # used here.
  stripped_file=$(mktemp)
  kubectl get configmap -n kube-system coredns -o jsonpath='{.data.Corefile}' \
    | sed "/${marker_begin}/,/${marker_end}/d" > "${stripped_file}"

  # Placed right after the server block's opening brace (line 1 of the
  # Corefile) rather than appended at the end — CoreDNS evaluates plugins in
  # file order, and the existing `forward . /etc/resolv.conf` plugin claims
  # the catch-all "." zone, so it would intercept (and forward upstream)
  # every query, including ours, if it ran first.
  block_file=$(mktemp)
  cat > "${block_file}" <<EOF
    ${marker_begin}
    template IN A ${domain} {
       match "^.*\\.${domain_regex}\\.\$"
       answer "{{ .Name }} 30 IN A ${ingress_ip}"
    }
    template IN AAAA ${domain} {
       match "^.*\\.${domain_regex}\\.\$"
       rcode NOERROR
    }
    ${marker_end}
EOF

  final_file=$(mktemp)
  sed "1r ${block_file}" "${stripped_file}" > "${final_file}"
  rm -f "${stripped_file}" "${block_file}"

  # A CoreDNS Corefile this short is never valid — refuse to push something
  # that would crash-loop every CoreDNS pod and take cluster DNS down with
  # it, rather than trusting the text-munging above blindly.
  line_count=$(wc -l < "${final_file}")
  if [ "${line_count}" -lt 10 ]; then
    echo "Generated Corefile looks truncated (${line_count} lines) - refusing to apply it. Left cluster DNS untouched." >&2
    rm -f "${final_file}"
    return 1
  fi

  echo "Configuring CoreDNS split-horizon DNS: *.${domain} -> ${ingress_ip} (in-cluster queries only)..."
  apply_output=$(kubectl create configmap coredns -n kube-system --from-file="Corefile=${final_file}" --dry-run=client -o yaml | kubectl apply -f -)
  echo "${apply_output}"
  rm -f "${final_file}"

  # Only restart CoreDNS if the Corefile actually changed — this runs on
  # every create-cluster.sh invocation (it's idempotent), and restarting
  # cluster-wide DNS unconditionally on a no-op re-run causes a real,
  # if brief, disruption: anything resolving a name right then (e.g.
  # Console's own Kafka connection) stalls for several seconds waiting on
  # the old CoreDNS pods to terminate and new ones to come up. The
  # `reload` plugin already in CoreDNS's default Corefile picks up a
  # genuine change on its own within ~30s regardless, but restarting
  # when there IS a change makes it deterministic — nothing after this
  # call has to guess whether the new config is live yet.
  if [ "${apply_output}" = "configmap/coredns unchanged" ]; then
    return 0
  fi

  kubectl rollout restart deployment/coredns -n kube-system >/dev/null
  kubectl rollout status deployment/coredns -n kube-system --timeout=60s >/dev/null
}

# Deploys a throwaway echo-server behind an Ingress, curls it over HTTP and
# HTTPS on the given host ports, then cleans up — confirms the cluster's
# exposure path (kind's hostPort mapping, or minikube's node-IP / tunnel /
# port-forward) actually works end to end before kind/create-cluster.sh or
# minikube/create-cluster.sh declare success. Exits non-zero (via the
# caller's `set -euo pipefail`) if it doesn't. Requires a working kubectl
# context against the target cluster and ingress-nginx already installed.
#
# Usage: verify_ingress_exposure <cluster-domain> <http-port> <https-port>
verify_ingress_exposure() {
  local domain="$1" http_port="$2" https_port="$3"
  local namespace="default"
  local host="echo.${domain}"
  local manifest
  manifest=$(mktemp)

  cleanup_smoke_test() {
    kubectl delete -f "${manifest}" --ignore-not-found >/dev/null 2>&1 || true
    rm -f "${manifest}"
  }
  trap cleanup_smoke_test EXIT

  cat > "${manifest}" <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cluster-setup-smoke-test
  namespace: ${namespace}
spec:
  replicas: 1
  selector:
    matchLabels: {app: cluster-setup-smoke-test}
  template:
    metadata:
      labels: {app: cluster-setup-smoke-test}
    spec:
      containers:
        - name: echo-server
          image: registry.k8s.io/e2e-test-images/agnhost:2.53
          args: ["netexec", "--http-port=8080"]
          ports: [{containerPort: 8080}]
---
apiVersion: v1
kind: Service
metadata:
  name: cluster-setup-smoke-test
  namespace: ${namespace}
spec:
  selector: {app: cluster-setup-smoke-test}
  ports: [{port: 80, targetPort: 8080}]
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: cluster-setup-smoke-test
  namespace: ${namespace}
spec:
  ingressClassName: nginx
  rules:
    - host: ${host}
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: cluster-setup-smoke-test
                port: {number: 80}
EOF

  # ingress-nginx reloads its config asynchronously, so a freshly-created
  # Ingress can 404/503/refuse connections for up to ~30s before routing works.
  wait_for_http() {
    local url="$1" curl_opts="$2" attempt
    for attempt in $(seq 1 30); do
      if curl -fsS ${curl_opts} --max-time 5 "${url}" >/dev/null 2>&1; then
        return 0
      fi
      sleep 2
    done
    echo "Timed out waiting for ${url} to become reachable" >&2
    return 1
  }

  # The --enable-ssl-passthrough patch both callers apply just before this
  # replaces the ingress-nginx-controller pod. `kubectl rollout status` only
  # confirms the new pod passed its /healthz readiness probe — it says
  # nothing about kube-proxy having programmed the new pod's IP into the
  # ingress-nginx-controller-admission Service's dataplane yet. Applying this
  # Ingress (which goes through that validating webhook) can land in that
  # gap and get "connection refused". Retry instead of guessing a fixed
  # delay — the apply is idempotent.
  local attempt
  for attempt in $(seq 1 15); do
    if kubectl apply -f "${manifest}"; then
      break
    fi
    [ "${attempt}" -eq 15 ] && { echo "Failed to apply smoke test manifest after retries" >&2; exit 1; }
    sleep 2
  done
  kubectl wait --for=condition=ready pod -l app=cluster-setup-smoke-test --timeout=180s -n "${namespace}"

  echo "Checking http://${host}:${http_port}/ (ingress-nginx reload can take up to ~30s)..."
  wait_for_http "http://${host}:${http_port}/" ""
  echo "Checking https://${host}:${https_port}/ ..."
  wait_for_http "https://${host}:${https_port}/" "-k"

  cleanup_smoke_test
  trap - EXIT
}
