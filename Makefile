DC = docker compose

.PHONY: up down dev db run logs build migrate

# ===== Продакшн: всё в докере =====
up:
	$(DC) --profile prod up -d --build

down:
	$(DC) --profile prod down

# ===== Локальная разработка: БД в докере + приложение локально =====
dev: db
	./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Поднять только БД
db:
	$(DC) up -d db

# Логи приложения
logs:
	$(DC) logs -f app

# Собрать jar
build:
	./mvnw clean package -DskipTests

migrate:
	@if [ -z "$(name)" ]; then echo "Использование: make migrate name=create_users"; exit 1; fi
	$(eval VERSION := $(shell ls src/main/resources/db/migration/V*.sql 2>/dev/null | sed 's/.*V\([0-9]*\)__.*/\1/' | sort -n | tail -1))
	$(eval NEXT := $(shell echo $$(( $(if $(VERSION),$(VERSION),0) + 1 ))))
	touch src/main/resources/db/migration/V$(NEXT)__$(name).sql
	@echo "Создан: src/main/resources/db/migration/V$(NEXT)__$(name).sql"

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