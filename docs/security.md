# Análisis de seguridad

La integración se define en `.github/workflows/security.yml` y
`.github/dependabot.yml`. Los análisis de Actions se ejecutan en push sobre ramas
Gitflow, en pull requests, manualmente y cada lunes a las 05:17 UTC. La ejecución
programada y la lectura de configuración de Dependabot requieren que los archivos
estén presentes en la rama por defecto de GitHub.

## Herramientas y criterios

| Control | Funcionamiento | Resultado que bloquea el job |
| --- | --- | --- |
| Gitleaks CLI 8.30.1 | Revisa archivos y todo el historial Git disponible, con secretos redactados. | Cualquier secreto detectado o error del escáner. |
| OWASP Dependency-Check 13.0.0 | Analiza las dependencias Maven resueltas y consulta NVD. | CVSS mayor o igual que 7, o un error del análisis. |
| CodeQL Java/Kotlin | Compila con Java 21 y ejecuta `security-extended`. | Errores de extracción/análisis; los hallazgos se publican en Code scanning. |
| ZAP API Scan | Importa `/openapi.yaml` y ejecuta un análisis activo sobre una instancia temporal con H2. | Alertas WARN/FAIL y errores del escáner. |
| SonarQube | Ejecuta tests, importa JaCoCo y espera el resultado del quality gate. | Quality gate no superado o error del servicio. |
| Dependabot | Propone actualizaciones de Maven y GitHub Actions hacia `develop`. | No es un job de CI; sus PR pasan los mismos controles. |

CodeQL requiere reglas de protección de GitHub para bloquear un merge en función
de los hallazgos: que el job termine correctamente no significa que no haya
vulnerabilidades. Configura la regla **Require code scanning results** en un
ruleset cuando la funcionalidad esté disponible. Exige también los checks de
Java CI, Gitleaks, Dependency-Check y ZAP antes de integrar cambios.

Los escáneres pueden encontrar problemas en las dependencias o configuración
actuales. Los informes deben revisarse y corregirse. No hay exclusiones globales,
CVE ignoradas ni `continue-on-error` para forzar resultados verdes. ZAP mantiene
el criterio estricto incluso para avisos, que deben evaluarse en su contexto de
API HTTP local.

## Preparación del repositorio en GitHub

1. Publica el repositorio y lleva los archivos de configuración a su rama por
   defecto. Activa GitHub Actions y permite las acciones oficiales utilizadas.
2. En **Settings → Security / Advanced Security**, activa el grafo de dependencias,
   Dependabot alerts y Dependabot security updates, si están disponibles.
3. En **Settings → Secrets and variables → Actions**, configura los valores que
   correspondan a la siguiente tabla. Nunca guardes tokens en archivos del repo.
4. Consulta **Actions → Security → Run workflow** para lanzar una ejecución manual.
5. Revisa los artefactos y los hallazgos de **Security → Code scanning**.

Los nombres de los apartados de GitHub pueden variar según el tipo de cuenta.

| Tipo | Nombre | Uso |
| --- | --- | --- |
| Secret | `NVD_API_KEY` | Recomendado para acelerar y estabilizar las actualizaciones NVD. |
| Variable | `ENABLE_CODEQL` | `true` solo si se quiere habilitar CodeQL en un repositorio privado con GitHub Code Security disponible. En públicos se ejecuta automáticamente. |
| Variable | `ENABLE_SONAR` | `true` para activar SonarQube tras configurar su proyecto. |
| Secret | `SONAR_TOKEN` | Token con permiso para analizar el proyecto SonarQube. |
| Variable | `SONAR_PROJECT_KEY` | Identificador real del proyecto en SonarQube. |
| Variable | `SONAR_ORGANIZATION` | Organización; obligatoria para SonarQube Cloud. |
| Variable | `SONAR_HOST_URL` | Opcional; por defecto `https://sonarcloud.io`. Para un servidor propio, URL accesible desde el runner. |

La ausencia de `NVD_API_KEY` no desactiva Dependency-Check: se utiliza la API
pública de NVD, con una carga inicial más lenta y sujeta a límites. El job tiene
45 minutos de timeout y cachea la base de datos por versión del escáner y día.
Una descarga o análisis fallidos no se convierten en un resultado satisfactorio.
La clave se transmite mediante una variable de entorno al plugin, sin exponerla
como argumento `-DnvdApiKey`.

El análisis usa NVD como fuente principal. OSS Index se desactiva explícitamente
para no requerir una segunda cuenta o token de Sonatype. Esto no desactiva las
comprobaciones NVD, sus actualizaciones ni el umbral CVSS.

SonarQube queda desactivado hasta definir `ENABLE_SONAR=true`; al activarlo se
comprueban el token y los identificadores antes del análisis. No se ejecuta en PR
de forks ni en PR de Dependabot, donde no deben asumirse secretos de Actions.
CodeQL en privados depende de la licencia y habilitación del repositorio.

## Activación del repositorio en GitHub

1. Publicar el repositorio y llevar estos archivos a su rama por defecto. El
   repositorio local todavía no tiene un remoto configurado.
2. Habilitar GitHub Actions, el gráfico de dependencias, Dependabot alerts y
   Dependabot security updates en la configuración de seguridad de GitHub.
3. Añadir los secretos y variables anteriores. Para CodeQL, usar configuración
   avanzada; si ya estaba activo el modo por defecto, evitar dos configuraciones
   simultáneas del mismo análisis.
4. En las reglas de `main` y `develop`, exigir los checks de CI/seguridad que
   correspondan. Para impedir merges por alertas CodeQL, configurar **Require code
   scanning results** y sus umbrales; publicar alertas no equivale por sí solo a
   bloquear la fusión. Consultar la
   [protección de merges de GitHub](https://docs.github.com/en/code-security/how-tos/find-and-fix-code-vulnerabilities/manage-your-configuration/set-merge-protection).

## Informes

| Ubicación | Contenido |
| --- | --- |
| Artefacto `java-test-reports` | Resultados JUnit e informe JaCoCo, 14 días. |
| Artefacto `gitleaks-reports` | Informes SARIF redactados de historial y archivos, 7 días. |
| Artefacto `dependency-check-reports` | HTML, JSON y SARIF de dependencias, 14 días. |
| Security → Code scanning | Alertas CodeQL por rama y pull request. |
| Artefacto `zap-api-reports` | Informes del análisis ZAP. |
| Artefacto `zap-application-logs` | Logs de la aplicación temporal, 7 días. |
| Proyecto SonarQube | Hallazgos, cobertura y estado del quality gate. |

Los informes se publican también cuando un escáner devuelve errores, siempre
que haya alcanzado a generarlos. Un fallo anterior a la generación de informes
seguirá apareciendo como fallo del job.

## Ejecución local

Con Java 21 y la variable de entorno NVD configurada cuando se disponga de clave:

```bash
./mvnw -Psecurity verify
```

Para analizar únicamente las dependencias:

```bash
./mvnw -Psecurity dependency-check:check
```

Los informes se escriben en `target/security/dependency-check/`. El perfil normal
`./mvnw verify` sigue ejecutando pruebas y cobertura sin descargar la base de
vulnerabilidades.

Con Gitleaks instalado:

```bash
gitleaks git . --log-opts='--all' --redact=100
gitleaks dir . --redact=100
```

El job ZAP de GitHub usa Docker y una aplicación nueva que se detiene al terminar,
incluso cuando el análisis falla. La consola H2 y la exportación de trazas están
desactivadas en esa instancia. El objetivo está fijado al runner local; no acepta
URLs externas mediante parámetros del workflow. El análisis activo se limita a
cinco minutos, con un minuto por regla. Puede producir altas y cambios de saldo
sobre esos datos efímeros.

## Verificación local del 23 de noviembre de 2026

- `./mvnw clean verify` con Java 21: 73 pruebas, sin fallos y con el umbral de
  cobertura cumplido. Se comprueban también las cabeceras en respuestas 200/400/404.
- `actionlint` 1.7.12 valida ambos workflows. La configuración Dependabot se
  comprueba como YAML y sus ecosistemas son Maven y GitHub Actions.
- Gitleaks 8.30.1: sin secretos detectados en historial ni archivos.
- Se resuelven los plugins Maven y se comprueba el POM efectivo de ambos perfiles,
  incluido CVSS 7, fallo ante errores y lectura de NVD_API_KEY por entorno.
- ZAP 2.17.0 ejecuta el análisis activo contra el JAR y una base H2 desechable:
  120 comprobaciones correctas, cero avisos y cero fallos; código de salida 0.
  El JSON conserva resultados informativos sobre respuestas 4xx y contenido no
  almacenable, que no son alertas de riesgo.

La primera ejecución de ZAP detectó dos alertas bajas por ausencia de
`X-Content-Type-Options` y `Cross-Origin-Resource-Policy`. Se corrigieron en el
adaptador HTTP, añadiendo además `Cache-Control: no-store`, y se repitió el análisis
sin suprimir reglas. La prueba local adapta únicamente la URL del contrato para
conectar el contenedor Docker de macOS con la instancia temporal; el workflow usa
localhost en el runner Linux.

El análisis CVE completo con NVD no se ha ejecutado localmente. CodeQL y SonarQube
no se han lanzado en GitHub: el repositorio no tiene remoto y falta la configuración
externa indicada. La validación de los perfiles no equivale a un informe sin CVE.

## Mantenimiento

Las acciones están fijadas por SHA, con la versión como comentario para permitir
actualizaciones de Dependabot. Los plugins Maven están versionados en sus perfiles.
Gitleaks se instala desde una versión fija y se verifica su SHA-256 antes de usarlo;
se emplea la CLI libre, sin depender de la licencia del wrapper Gitleaks Action.

Al actualizar Gitleaks hay que actualizar tanto la URL como el checksum. Revisa
también la versión de la imagen ZAP `ghcr.io/zaproxy/zaproxy:2.17.0`, que no es una
acción actualizable automáticamente por Dependabot. Las actualizaciones de versión
van a `develop`; las alertas y security updates de Dependabot se rigen por la rama
por defecto de GitHub. No se habilita automerge.

## Referencias oficiales

- [Dependency-Check Maven y parámetros](https://dependency-check.github.io/DependencyCheck/dependency-check-maven/check-mojo.html).
- [Dependabot y opciones de actualización](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference).
- [CodeQL y configuración avanzada](https://docs.github.com/en/code-security/how-tos/find-and-fix-code-vulnerabilities/configure-code-scanning/configuring-advanced-setup-for-code-scanning).
- [Gitleaks CLI](https://github.com/gitleaks/gitleaks).
- [ZAP API Scan](https://www.zaproxy.org/docs/docker/api-scan/).
- [SonarQube Cloud con GitHub Actions](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/ci-based-analysis/github-actions-for-sonarcloud).
