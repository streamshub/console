
WORKDIR := $(dir $(abspath $(firstword $(MAKEFILE_LIST))))

dev:
	docker-compose --profile plain up -d ;\
	mvn quarkus:dev -f kafka-admin/pom.xml -Dkafka.admin.bootstrap.servers=localhost:9092 -Dkafka.admin.basic.enabled=true -Dkafka.admin.oauth.enabled=false ;\
	docker-compose down
	# podman bug requires separate `docker rm`
	docker rm -f broker
.PHONY: dev

dev-tls: certgen
	docker-compose --profile tls up -d ; \
	mvn quarkus:dev -f kafka-admin/pom.xml -Dkafka.admin.bootstrap.servers=localhost:9093 -Dkafka.admin.basic.enabled=true -Dkafka.admin.oauth.enabled=false -Dkafka.admin.broker.tls.enabled=true -Dkafka.admin.broker.trusted.cert=$(shell base64 -w0 $(WORKDIR)/target/certs/ca.crt) ;\
	docker-compose down
	# podman bug requires separate `docker rm`
	docker rm -f broker
.PHONY: dev-tls

certgen:
	rm -rvf $(WORKDIR)/target/certs; \
	mkdir -p $(WORKDIR)/target/certs; \
	$(WORKDIR)/hack/gen-ca.sh $(WORKDIR)/target/certs; \
	$(WORKDIR)/hack/gen-kafka-certs.sh $(WORKDIR)/target/certs $(WORKDIR)/target/certs "password";
.PHONY: certgen
