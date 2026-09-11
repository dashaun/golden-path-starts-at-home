SHELL := /bin/bash
SCRIPTS := platform/scripts

.DEFAULT_GOAL := help

help: ## Show this help
	@grep -hE '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
	  awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[1;32m%-18s\033[0m %s\n", $$1, $$2}'

platform-init: ## Generate platform/.env with fresh credentials
	@$(SCRIPTS)/00-platform-init.sh

cluster-up: ## Stand up Cloud Foundry on a local kind cluster and log in
	@$(SCRIPTS)/05-cluster-up.sh

org: ## Create the golden org and dev space
	@$(SCRIPTS)/10-org-space.sh

vault: ## Push Vault and seed the demo secrets
	@$(SCRIPTS)/20-vault.sh

config-server: ## Build and push the config server
	@$(SCRIPTS)/30-config-server.sh

service: ## Create the golden-config service instance
	@$(SCRIPTS)/40-bind-service.sh

apps: ## Build and push the three consuming applications
	@$(SCRIPTS)/50-apps.sh

up: platform-init cluster-up org vault config-server service apps ## The whole demo, from nothing
	@echo
	@echo "The platform is up. Bind your laptop with: bin/dev-bind.sh"

bind: ## Materialize the binding for local development
	@bin/dev-bind.sh

unbind: ## Remove every credential from this machine
	@bin/dev-unbind.sh

build: ## Build every module
	@./mvnw -q -B -DskipTests install

test: ## Run the tests
	@./mvnw -B test

ask: ## Ask the agent a question (make ask Q="...")
	@$(SCRIPTS)/70-ask.sh "$(Q)"

rotate: ## Change the secret in Vault and refresh the running applications
	@$(SCRIPTS)/60-rotate.sh

down: ## Delete everything this demo created, leaving the cluster alone
	@$(SCRIPTS)/90-down.sh

slides: ## Serve the presentation at http://localhost:8000
	@source bin/lib-java.sh && golden::use_pinned_java "$(CURDIR)" && \
	  echo "Presentation at http://localhost:8000  (ctrl-c to stop)" && \
	  jwebserver -d "$(CURDIR)/docs" -p 8000

.PHONY: help platform-init cluster-up org vault config-server service apps up bind unbind build test ask rotate down slides
