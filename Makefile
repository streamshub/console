
.PHONY: container-image-api container-image-ui container-images

include *compose.env

CONSOLE_API_IMAGE ?= quay.io/streamshub/console-api:latest
CONSOLE_UI_IMAGE ?= quay.io/streamshub/console-ui:latest
CONSOLE_UI_NEXTAUTH_SECRET ?= $(shell openssl rand -base64 32)
CONSOLE_METRICS_PROMETHEUS_URL ?= 
CONTAINER_RUNTIME ?= $(shell which podman || which docker)

container-image-api:
	mvn package -f api/pom.xml -Pdocker -DskipTests -Dquarkus.container-image.image=$(CONSOLE_API_IMAGE)

container-image-api-push: container-image-api
	$(CONTAINER_RUNTIME) push $(CONSOLE_API_IMAGE)

container-image-ui:
	$(CONTAINER_RUNTIME) build -t $(CONSOLE_UI_IMAGE) ./ui -f ./ui/Dockerfile

container-image-ui-push: container-image-ui
	$(CONTAINER_RUNTIME) push $(CONSOLE_UI_IMAGE)

container-images: container-image-api container-image-ui

container-images-push: container-image-api-push container-image-ui-push

compose-up:
	> compose-runtime.env
	echo "CONSOLE_API_IMAGE=$(CONSOLE_API_IMAGE)" >> compose-runtime.env
	echo "CONSOLE_API_SERVICE_ACCOUNT_TOKEN=$(CONSOLE_API_SERVICE_ACCOUNT_TOKEN)" >> compose-runtime.env
	echo "CONSOLE_API_KUBERNETES_API_SERVER_URL=$(CONSOLE_API_KUBERNETES_API_SERVER_URL)" >> compose-runtime.env 
	echo "CONSOLE_UI_IMAGE=$(CONSOLE_UI_IMAGE)" >> compose-runtime.env
	echo "CONSOLE_UI_NEXTAUTH_SECRET=$(CONSOLE_UI_NEXTAUTH_SECRET)" >> compose-runtime.env
	echo "CONSOLE_METRICS_PROMETHEUS_URL=$(CONSOLE_METRICS_PROMETHEUS_URL)" >> compose-runtime.env
	$(CONTAINER_RUNTIME) compose --env-file compose-runtime.env up -d

compose-down:
	$(CONTAINER_RUNTIME) compose --env-file compose-runtime.env down
