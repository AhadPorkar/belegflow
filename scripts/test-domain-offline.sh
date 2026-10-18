#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
mkdir -p .build/domain
find domain/src/main/java -name '*.java' > .build/domain-sources.txt
printf '%s\n' domain/src/test/java/com/belegflow/domain/DomainChecks.java >> .build/domain-sources.txt
javac --release 21 -d .build/domain @.build/domain-sources.txt
java -cp .build/domain:domain/src/test/resources com.belegflow.domain.DomainChecks
