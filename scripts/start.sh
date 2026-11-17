#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
exec java -jar dist/home-loan-backend-1.0.0.jar --spring.config.location=file:./src/main/resources/application.yml
