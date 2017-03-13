#!/bin/sh
mvn help:evaluate -Dexpression=settings.localRepository 2>/dev/null | grep -vE '\[(INFO|DEBUG|WARN|ERROR|MVNVM)\]' | grep repository