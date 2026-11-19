JAVA_HOME ?= /opt/homebrew/opt/openjdk@21
export JAVA_HOME
export PATH := $(JAVA_HOME)/bin:$(PATH)

.PHONY: db-up db-down db-reset db-psql run

## Levanta Postgres y espera a que esté listo
db-up:
	docker compose up -d --wait db

## Detiene Postgres (conserva los datos)
db-down:
	docker compose down

## Borra Postgres Y sus datos
db-reset:
	docker compose down -v

## Abre psql dentro del contenedor
db-psql:
	docker compose exec db psql -U javabasics -d javabasics

## Corre la app
run:
	./mvnw spring-boot:run
