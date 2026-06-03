#!/bin/sh

exec java --enable-native-access=ALL-UNNAMED -Dserver.port=${PORT:-8080} -jar app.jar