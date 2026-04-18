ifneq (,$(wildcard .env))
    include .env
    export
endif

DC = docker compose

API_MODULE = heatmap-api
GEN_MODULE = data-generator

.PHONY: up down dev-api dev-gen infra db run logs build migrate

# ===== Продакшн: всё в докере =====
up:
	$(DC) --profile prod up -d --build

down:
	$(DC) --profile prod down

# ===== Локальная разработка =====
# Запуск API локально (нужна поднятая БД)
dev-api: infra
	./mvnw spring-boot:run -pl $(API_MODULE) -Dspring-boot.run.profiles=local

# Запуск Генератора локально
dev-gen: infra
	./mvnw spring-boot:run -pl $(GEN_MODULE) -Dspring-boot.run.profiles=local

# Поднять инфраструктуру (БД + Kafka)
infra:
	$(DC) up -d db kafka kafka-init

# Поднять только БД
db:
	$(DC) up -d db

# Логи приложения
logs:
	$(DC) logs -f app

# Собрать jar
build:
	./mvnw clean package -DskipTests

# ===== Миграции (только для heatmap-api) =====
migrate:
	@if [ -z "$(name)" ]; then echo "Использование: make migrate name=description"; exit 1; fi
	$(eval MIGRATION_DIR := $(API_MODULE)/src/main/resources/db/migration)
	$(eval VERSION := $(shell ls $(MIGRATION_DIR)/V*.sql 2>/dev/null | sed 's/.*V\([0-9]*\)__.*/\1/' | sort -n | tail -1))
	$(eval NEXT_NUM := $(shell echo $$(( $(if $(VERSION),$(VERSION),0) + 1 ))))
	$(eval NEXT_VERSION := $(shell printf "%03d" $(NEXT_NUM)))
	@touch $(MIGRATION_DIR)/V$(NEXT_VERSION)__$(name).sql
	@echo "Создан файл миграции: $(MIGRATION_DIR)/V$(NEXT_VERSION)__$(name).sql"

.PHONY: lint lint-checkstyle lint-pmd

# Запустить все линтеры сразу
lint:
	./mvnw checkstyle:check pmd:check --no-transfer-progress

# Только Checkstyle
lint-checkstyle:
	./mvnw checkstyle:check --no-transfer-progress

# Только PMD
lint-pmd:
	./mvnw pmd:check --no-transfer-progress

.PHONY: test
test:
	./mvnw test --no-transfer-progress