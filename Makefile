
.PHONY: container-image-api container-image-ui container-images

include *compose.env

IMAGE_REGISTRY ?= quay.io
IMAGE_GROUP ?= streamshub
VERSION = $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout | tr '[:upper:]' '[:lower:]')

CONSOLE_API_IMAGE ?= $(IMAGE_REGISTRY)/$(IMAGE_GROUP)/console-api:$(VERSION)
CONSOLE_OPERATOR_IMAGE ?= $(IMAGE_REGISTRY)/$(IMAGE_GROUP)/console-operator:$(VERSION)
CONSOLE_OPERATOR_BUNDLE_IMAGE ?= $(IMAGE_REGISTRY)/$(IMAGE_GROUP)/console-operator-bundle:$(VERSION)
CONSOLE_OPERATOR_CATALOG_IMAGE ?= $(IMAGE_REGISTRY)/$(IMAGE_GROUP)/console-operator-catalog:$(VERSION)
CONSOLE_UI_IMAGE ?= $(IMAGE_REGISTRY)/$(IMAGE_GROUP)/console-ui:$(VERSION)

CONSOLE_UI_NEXTAUTH_SECRET ?= $(shell openssl rand -base64 32)
CONSOLE_METRICS_PROMETHEUS_URL ?= 
CONTAINER_RUNTIME ?= $(shell which podman || which docker)
BUILD_FOR_PLATFORM ?= linux/amd64

container-image-api:
	mvn package -am -pl api -Pcontainer-image -DskipTests -Dquarkus.container-image.image=$(CONSOLE_API_IMAGE)

container-image-api-push: container-image-api
	$(CONTAINER_RUNTIME) push $(CONSOLE_API_IMAGE)

container-image-operator:
	mvn package -am -pl operator -Pcontainer-image -DskipTests -Dquarkus.container-image.image=$(CONSOLE_OPERATOR_IMAGE)
	operator/bin/generate-catalog.sh $(VERSION)
	$(CONTAINER_RUNTIME) build --platform=$(BUILD_FOR_PLATFORM) -t $(CONSOLE_OPERATOR_BUNDLE_IMAGE) -f operator/target/bundle/console-operator/bundle.Dockerfile
	$(CONTAINER_RUNTIME) build --platform=$(BUILD_FOR_PLATFORM) -t $(CONSOLE_OPERATOR_CATALOG_IMAGE) -f operator/target/catalog.Dockerfile

container-image-operator-push: container-image-operator
	$(CONTAINER_RUNTIME) push $(CONSOLE_OPERATOR_IMAGE)
	$(CONTAINER_RUNTIME) push $(CONSOLE_OPERATOR_BUNDLE_IMAGE)
	$(CONTAINER_RUNTIME) push $(CONSOLE_OPERATOR_CATALOG_IMAGE)

container-image-ui:
	$(CONTAINER_RUNTIME) build -t $(CONSOLE_UI_IMAGE) ./ui -f ./ui/Dockerfile

container-image-ui-push: container-image-ui
	$(CONTAINER_RUNTIME) push $(CONSOLE_UI_IMAGE)

container-images: container-image-api container-image-ui container-image-operator

container-images-push: container-image-api-push container-image-ui-push container-image-operator-push

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
