ASADMIN ?= $(HOME)/.local/share/itmo-lab/glassfish7/bin/asadmin

.PHONY: build deploy start

build:
	./gradlew war --no-daemon

deploy:
	"$(ASADMIN)" --port 12048 deploy --force=true --name lab --contextroot lab build/libs/1stLab-1.0-SNAPSHOT.war

start: build
	$(MAKE) deploy
