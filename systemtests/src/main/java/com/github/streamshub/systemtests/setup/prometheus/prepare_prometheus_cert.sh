#!/usr/bin/env bash


PROM_TLS_DIR=/tmp/prometheus
mkdir -p $PROM_TLS_DIR
PROM_TLS_CERT=$PROM_TLS_DIR/cert.pem
PROM_TLS_KEY=$PROM_TLS_DIR/key.pem

openssl req -subj "/CN=cstm-prometheus-server.prometheusst-98da91.svc.cluster.local/O=StreamsHubPrometheus/C=US" \
  -newkey rsa:2048 -nodes -keyout $PROM_TLS_KEY -x509 -days 365 -out $PROM_TLS_CERT
