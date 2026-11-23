#!/bin/sh
set -eu

export LOCAL_JWT_KEY_DIR="${LOCAL_JWT_KEY_DIR:-/app/var/jwt}"
mkdir -p "$LOCAL_JWT_KEY_DIR"
python3 /app/local-jwt.py > "$LOCAL_JWT_KEY_DIR/token"
chmod 600 "$LOCAL_JWT_KEY_DIR/token"

exec java -jar /app/app.jar
