#!/usr/bin/env python3
"""Genera una clave RSA local si hace falta e imprime un JWT de desarrollo de corta duración."""

import base64
import json
import os
import subprocess
import sys
import time
from pathlib import Path


def base64url(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")


def main() -> int:
    key_directory = Path(os.environ.get(
        "LOCAL_JWT_KEY_DIR",
        Path.home() / ".config" / "clientes-cuentas-microservicio" / "jwt",
    )).expanduser()
    private_key = key_directory / "private.pem"
    public_key = key_directory / "public.pem"
    key_directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    key_directory.chmod(0o700)

    if not private_key.exists() or not public_key.exists():
        mascara_anterior = os.umask(0o077)
        try:
            subprocess.run(
                ["openssl", "genpkey", "-algorithm", "RSA", "-pkeyopt", "rsa_keygen_bits:2048",
                 "-out", str(private_key)], check=True,
            )
        finally:
            os.umask(mascara_anterior)
        private_key.chmod(0o600)
        with public_key.open("wb") as output:
            subprocess.run(["openssl", "pkey", "-in", str(private_key), "-pubout"], check=True, stdout=output)
        public_key.chmod(0o644)

    issuer = os.environ.get("JWT_ISSUER_URI") or "cuentas-local"
    audience = os.environ.get("JWT_AUDIENCE") or "cuentas-api-local"
    permissions = os.environ.get(
        "LOCAL_JWT_SCOPES",
        "cuentas.read cuentas.write actuator.health.read actuator.metrics.read",
    ).split()
    now = int(time.time())
    header = base64url(json.dumps({"alg": "RS256", "typ": "JWT"}, separators=(",", ":")).encode())
    claims = {
        "iss": issuer,
        "aud": audience,
        "iat": now,
        "exp": now + 900,
        "scope": " ".join(permissions),
        "sub": "local-developer",
    }
    payload = base64url(json.dumps(claims, separators=(",", ":")).encode())
    signing_input = f"{header}.{payload}".encode("ascii")
    signature = subprocess.run(
        ["openssl", "dgst", "-sha256", "-sign", str(private_key)],
        input=signing_input, check=True, capture_output=True,
    ).stdout
    print(f"{header}.{payload}.{base64url(signature)}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, subprocess.CalledProcessError) as error:
        print(f"No se pudo generar el JWT local: {error}", file=sys.stderr)
        raise SystemExit(1) from error
