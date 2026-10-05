ASADMIN ?= $(HOME)/IS/glassfish7/bin/asadmin
ASADMIN_SHELL ?= /usr/local/bin/bash
export AS_JAVA := /usr/local/openjdk17
export JAVA_TOOL_OPTIONS := -XX:MaxHeapSize=1G -XX:MaxMetaspaceSize=128m

.PHONY: build deploy start

build:
	./gradlew war --no-daemon

deploy:
	"$(ASADMIN_SHELL)" "$(ASADMIN)" --port 12048 deploy --force=true --name lab --contextroot lab build/libs/1stLab-1.0-SNAPSHOT.war

start: build
	$(MAKE) deploy
