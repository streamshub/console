#!/bin/sh

set -e

OUTDIR=${1:-$(pwd)}
CADIR=${2:-$(pwd)}
STOREPASS=${3:-Z_pkTh9xgZovK4t34cGB2o6afT4zZg0L}

echo "${STOREPASS}" > ${OUTDIR}/kafka_secret.txt
echo "subjectAltName = DNS:kafka,DNS:localhost" > ${OUTDIR}/kafka-san.ext

echo "#### Generate broker keystore"
keytool -keystore ${OUTDIR}/cluster.keystore.p12 \
 -alias localhost \
 -validity 380 \
 -genkey \
 -keyalg RSA \
 -dname "CN=my-cluster-kafka,O=io.strimzi" \
 -ext "SAN=DNS:kafka,DNS:localhost" \
 -deststoretype pkcs12 \
 -storepass $STOREPASS \
 -keypass $STOREPASS

echo "#### Export the certificate from the keystore"
keytool -keystore ${OUTDIR}/cluster.keystore.p12 \
 -storetype pkcs12 \
 -alias localhost \
 -certreq \
 -dname "CN=my-cluster-kafka,O=io.strimzi" \
 -ext "SAN=DNS:kafka,DNS:localhost" \
 -file ${OUTDIR}/cert-file \
 -storepass $STOREPASS

echo "#### Sign the certificate with the CA"
openssl x509 -req \
 -CA ${CADIR}/ca.crt \
 -CAkey ${CADIR}/ca.key \
 -extfile ${OUTDIR}/kafka-san.ext \
 -in ${OUTDIR}/cert-file \
 -out ${OUTDIR}/cert-signed \
 -days 400 \
 -CAcreateserial \
 -passin pass:$STOREPASS

echo "#### Import the CA and the signed certificate into the broker keystore"
keytool -keystore ${OUTDIR}/cluster.keystore.p12 -deststoretype pkcs12 -alias CARoot -import -file ${CADIR}/ca.crt -storepass $STOREPASS -noprompt
keytool -keystore ${OUTDIR}/cluster.keystore.p12 -deststoretype pkcs12 -alias localhost -import -file ${OUTDIR}/cert-signed -storepass $STOREPASS -noprompt

echo "#### Add the CA to the brokers’ truststore"
keytool -keystore ${OUTDIR}/cluster.truststore.p12 -deststoretype pkcs12 -storepass $STOREPASS -alias CARoot -importcert -file ${CADIR}/ca.crt -noprompt
