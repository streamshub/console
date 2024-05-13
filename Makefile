
.PHONY: container-image-api container-image-ui container-images

include *compose.env

CONSOLE_API_IMAGE ?= localhost/eyefloaters/console-api
CONSOLE_API_ACCOUNT ?= streams-console-service-account
CONSOLE_API_NS ?= default
CONSOLE_UI_IMAGE ?= localhost/eyefloaters/console-ui
CONSOLE_UI_NEXTAUTH_SECRET ?= $(shell openssl rand -base64 32)

container-image-api:
	mvn package -f api/pom.xml -Pdocker -DskipTests -Dquarkus.container-image.image=$(CONSOLE_API_IMAGE)

container-image-ui:
	docker build -t $(CONSOLE_UI_IMAGE) ./ui -f ./ui/Dockerfile

container-images: container-image-api container-image-ui

compose-up: container-images
	> compose-runtime.env
	echo "CONSOLE_API_IMAGE=$(CONSOLE_API_IMAGE)" >> compose-runtime.env
	echo "CONSOLE_API_SERVICE_ACCOUNT_TOKEN=$(CONSOLE_API_SERVICE_ACCOUNT_TOKEN)" >> compose-runtime.env
	echo "CONSOLE_API_KUBERNETES_API_SERVER_URL=$(CONSOLE_API_KUBERNETES_API_SERVER_URL)" >> compose-runtime.env 
	echo "CONSOLE_UI_IMAGE=$(CONSOLE_UI_IMAGE)" >> compose-runtime.env
	echo "CONSOLE_UI_NEXTAUTH_SECRET=$(CONSOLE_UI_NEXTAUTH_SECRET)" >> compose-runtime.env
	docker compose --env-file compose-runtime.env up -d

compose-down:
	docker compose --env-file compose-runtime.env down
