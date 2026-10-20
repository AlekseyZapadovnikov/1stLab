ASADMIN ?= $(HOME)/.local/share/itmo-lab/glassfish7/bin/asadmin
ADMIN_PORT ?= 12048
DOMAIN_DIR ?= $(HOME)/.local/share/itmo-lab/domains
PG_CTL ?= /usr/lib/postgresql/16/bin/pg_ctl
PG_DATA ?= $(HOME)/.local/share/itmo-lab/postgres

.PHONY: build deploy start gfish

build:
	./gradlew war --no-daemon

deploy:
	"$(ASADMIN)" --port $(ADMIN_PORT) deploy --force=true --name lab --contextroot lab build/libs/1stLab-1.0-SNAPSHOT.war

start: build
	@if ! "$(PG_CTL)" -D "$(PG_DATA)" status >/dev/null; then \
		"$(PG_CTL)" -D "$(PG_DATA)" -l "$(PG_DATA)/server.log" -w start; \
	fi
	$(MAKE) gfish
	$(MAKE) deploy

gfish:
	@domains=$$("$(ASADMIN)" --terse=true list-domains --domaindir "$(DOMAIN_DIR)") || exit $$?; \
	if printf '%s\n' "$$domains" | grep -Eq '^lab running([[:space:]]|$$)'; then \
		printf '%s\n' 'GlassFish lab is already running'; \
	else \
		"$(ASADMIN)" start-domain --domaindir "$(DOMAIN_DIR)" lab; \
	fi
