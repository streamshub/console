#!/bin/sh
set -e

OUTDIR=${1:-$(pwd)}

# create CA key
openssl genrsa -out ${OUTDIR}/ca.key 4096

# create CA certificate
openssl req -x509 -new -nodes -sha256 -days 3650 -subj "/CN=KafkaAdmin.io" -key ${OUTDIR}/ca.key -out ${OUTDIR}/ca.crt
