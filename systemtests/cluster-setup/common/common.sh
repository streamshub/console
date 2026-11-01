#!/usr/bin/env bash
# Shared utilities for cluster-setup/{kind,minikube}. Source, don't execute.

OS_NAME="$(uname -s)"

# Can the host reach the minikube/kind node directly? Yes on Linux; no on
# macOS, where the container engine runs inside a hidden VM.
case "${OS_NAME}" in
  Darwin) NEEDS_LOCAL_PORT_FORWARD=true ;;
  Linux)  NEEDS_LOCAL_PORT_FORWARD=false ;;
  *)
    echo "Unsupported OS_NAME=${OS_NAME} (expected 'Darwin' or 'Linux')" >&2
    exit 1
    ;;
esac

# Podman machine sizing (macOS only — native Linux podman needs no VM).
# Defaults to host-1 CPU / host-4GB memory so the host isn't starved;
# override with PODMAN_MACHINE_CPUS/PODMAN_MACHINE_MEMORY (MB).
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

# macOS + CONTAINER_ENGINE=docker: installs docker/colima if missing, starts
# colima if no daemon is reachable (no-op with Docker Desktop or an
# already-running colima). Size the VM with COLIMA_CPUS/COLIMA_MEMORY (GB).
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

# Installs skopeo (used by both load-images.sh scripts to push into the
# local registry). On Linux, also applies the AppArmor tweak skopeo needs
# on Ubuntu 24.04+ runners (restricts unprivileged user namespaces).
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

# "localhost" can resolve to ::1 before 127.0.0.1 — fatal on macOS, where
# AirPlay Receiver squats on port 5000's IPv6 wildcard while the registry
# is IPv4-only, so a push there gets a baffling 403 instead of a real error.
#
# Only for load-images.sh's skopeo push URL (the one real network call).
# Image tags and the catalog-image reference must stay "localhost:<port>"
# — that's the containerd hosts.toml key the cluster resolves pulls by.
registry_host_for_push() {
  echo "${1/localhost/127.0.0.1}"
}

# Which local daemon actually holds the built images — NOT $CONTAINER_ENGINE,
# which for minikube is the *driver* (can be vfkit/kvm2, neither of which
# stores images). Docker preferred; if both are on PATH, `docker` may
# actually be podman's compat shim, so ask it rather than assume.
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

# Ensures a podman machine exists and is running (macOS only — native Linux
# podman has no VM to manage). Only sizes it on first-time init; an existing
# machine is never resized automatically, since that means stopping it.
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

# Rootless podman on Linux can't load kernel modules itself (no
# CAP_SYS_MODULE), so a missing ip_tables module surfaces later as a
# confusing ingress-nginx/kube-proxy failure. Check up front and fail fast.
# No-op on macOS and on rootful podman/docker.
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

# Configures CoreDNS with split-horizon DNS for CONSOLE_CLUSTER_DOMAIN: a
# "*.<domain>" query from inside the cluster (a Pod) is answered with the
# ingress controller's ClusterIP directly, instead of being forwarded to
# real nip.io DNS (which only knows 127.0.0.1 — see registry_host_for_push
# above for why that's ambiguous). The host keeps resolving the hostname
# normally; this only intercepts in-cluster queries, so it's genuinely
# split-horizon, not a global override.
#
# Needed because Kafka's `secure` listener is ingress-exposed and addressed
# by this same kind of hostname, and Console (itself a Pod) connects to
# Kafka through it. Without this, Console resolves to its own loopback and
# every Kafka API call times out, even though both are healthy. Applies on
# kind (any OS) and minikube-on-macOS; a harmless no-op on minikube-on-Linux
# (already a real routable IP), so it runs unconditionally rather than
# detecting the case.
#
# Idempotent: re-running replaces the previous block (BEGIN/END markers)
# instead of duplicating it.
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

  # Strip any block from a previous run first, so re-running converges
  # instead of duplicating or leaving a stale entry. sed, not awk: macOS's
  # non-GNU awk rejects multi-line -v values ("newline in string"); sed with
  # temp files behaves identically on BSD and GNU.
  stripped_file=$(mktemp)
  kubectl get configmap -n kube-system coredns -o jsonpath='{.data.Corefile}' \
    | sed "/${marker_begin}/,/${marker_end}/d" > "${stripped_file}"

  # Inserted right after the opening brace, not appended — CoreDNS evaluates
  # plugins in file order, and `forward . /etc/resolv.conf` claims the
  # catch-all "." zone, so it must run after ours, not before.
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

  # A Corefile this short is never valid — refuse to push it rather than
  # risk crash-looping every CoreDNS pod and taking cluster DNS down.
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

  # Only restart if the Corefile actually changed — this runs on every
  # create-cluster.sh invocation, and an unconditional restart briefly
  # disrupts any in-flight DNS lookup (e.g. Console's Kafka connection).
  # Restarting on a real change makes the new config live immediately,
  # instead of waiting on CoreDNS's own ~30s reload check.
  if [ "${apply_output}" = "configmap/coredns unchanged" ]; then
    return 0
  fi

  kubectl rollout restart deployment/coredns -n kube-system >/dev/null
  kubectl rollout status deployment/coredns -n kube-system --timeout=60s >/dev/null
}

# Deploys a throwaway echo-server behind an Ingress, curls it over HTTP and
# HTTPS on the given ports, then cleans up — confirms the cluster's exposure
# path actually works end to end before create-cluster.sh declares success.
# Exits non-zero (via the caller's `set -euo pipefail`) if it doesn't.
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

  # The ssl-passthrough patch callers apply just before this replaces the
  # ingress-nginx-controller pod; `rollout status` confirms it's ready but
  # not that kube-proxy has caught up yet, so an immediate apply (which goes
  # through the admission webhook) can land in that gap. Retry, don't guess
  # a delay — the apply is idempotent.
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
