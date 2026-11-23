# Estado de las dependencias y mitigaciones

Revisión del 23 de noviembre de 2026. Se mantiene **Spring Boot 3.5.16** por el
requisito del enunciado. Esta revisión se limita a las dependencias y al control
de autenticación DPoP; no modifica los casos de uso ni el contrato REST.

## Versiones actualizadas

Las propiedades del parent de Spring Boot mantienen coordinados los módulos de
cada familia, sin fijar versiones de JAR individuales:

| Familia | Antes | Ahora | Propiedad Maven |
| --- | --- | --- | --- |
| Tomcat | 10.1.55 | 10.1.62 | `tomcat.version` |
| Jackson | 2.21.4 | 2.21.8 | `jackson-bom.version` |
| Log4j API y puente SLF4J | 2.24.3 | 2.25.5 | `log4j2.version` |
| Kotlin JVM | 1.9.25 | 2.4.21 | `kotlin.version` |

Kotlin es una dependencia transitiva de OpenTelemetry → OkHttp/Okio. Se excluye
el antiguo JAR `kotlin-stdlib-common` que Okio 3.6.0 incorporaba como 1.9.10:
el stdlib JVM de Kotlin 2.x ya incluye las declaraciones comunes. El BOM actual
solo gestiona `kotlin-stdlib-common` de tipo POM, por lo que subir `kotlin.version`
no basta para retirar aquel JAR antiguo. La prueba `ExportacionOtlpTest` envía una
traza real por HTTP a un colector local temporal para comprobar este camino.

## Autenticación limitada a Bearer

Spring Security 6.5.11 incorpora DPoP automáticamente al configurar OAuth2 Resource
Server. La API del ejercicio utiliza JWT Bearer y no necesita DPoP.
`SoloBearerFilter`, situado antes de `SecurityContextHolderFilter`, rechaza con
401 las pruebas y el esquema DPoP antes de llegar al autenticador afectado por
[CVE-2026-41707](https://spring.io/security/cve-2026-41707/). También rechaza
combinaciones de Bearer con la cabecera `DPoP` y cabeceras Authorization múltiples
que contengan DPoP.

Esta es una mitigación de la ruta de entrada, **no un parche de Spring Security**.
La CVE se conserva en Dependency-Check. Si se requiere DPoP, hay que actualizar
primero a una versión corregida y sustituir esta política con pruebas específicas.
El modo explícito `APP_SECURITY_ENABLED=false` conserva su funcionamiento como
modo de evaluación sin autenticación.

## Exclusión documentada

[config/dependency-check-suppressions.xml](config/dependency-check-suppressions.xml)
excluye únicamente **CVE-2026-54285** para una lista explícita de los **13 JAR Java
1.49.0 presentes en el ejecutable**. Se inspeccionaron sus clases JVM y sus
coordenadas; la regla no incluye otros nombres ni versiones futuras.
Dependency-Check puede agruparlos y atribuir el mismo CPE a diferentes módulos.

Artefactos revisados del grupo `io.opentelemetry`:

- `opentelemetry-api`.
- `opentelemetry-context`.
- `opentelemetry-exporter-common`.
- `opentelemetry-exporter-otlp`.
- `opentelemetry-exporter-otlp-common`.
- `opentelemetry-exporter-sender-okhttp`.
- `opentelemetry-extension-trace-propagators`.
- `opentelemetry-sdk`.
- `opentelemetry-sdk-common`.
- `opentelemetry-sdk-extension-autoconfigure-spi`.
- `opentelemetry-sdk-logs`.
- `opentelemetry-sdk-metrics`.
- `opentelemetry-sdk-trace`.

El [aviso del mantenedor](https://github.com/open-telemetry/opentelemetry-js/security/advisories/GHSA-8988-4f7v-96qf)
afecta al paquete npm `@opentelemetry/core`, no a esos artefactos Java.

La regla identifica coordenadas y CVE, caduca el **23 de mayo de 2027** y debe
revisarse entonces. El análisis falla ante exclusiones sin uso. No se excluyen
paquetes completos, puntuaciones CVSS, los avisos de Spring ni el aviso DPoP.

## Avisos pendientes de Spring

El análisis sigue detectando **23 CVE distintas en cuatro JAR**:
`spring-core`, `spring-data-jpa`, `spring-security-core` y
`spring-security-oauth2-resource-server`. Los cinco avisos de Security se repiten
en los dos últimos JAR. Que no se observe una condición de explotación en el
código actual no elimina la dependencia afectada.

| Grupo | Comprobación en este proyecto |
| --- | --- |
| Framework: CVE-2026-47883 a CVE-2026-47893 | Sin UrlHandlerFilter, XSLT, vistas por nombre, SpEL de usuarios, WebFlux, RSocket, SSE ni Jetty reactivo. |
| Framework: CVE-2026-59280 a CVE-2026-59283 | Sin FreeMarker, EscapedErrors HTML, binding sobre listas autopoblables ni compilación SpEL. |
| Framework: CVE-2026-59313 y CVE-2026-59314 | Sin SSE funcional ni Content-Disposition/multipart saliente derivado de entradas. |
| JPA: CVE-2026-47834 | JPQL fijo parametrizado, sin consultas nativas con Sort/Pageable de usuarios. |
| Security: CVE-2026-59270 y CVE-2026-47841 | Sin LDAP embebido ni WebAuthn/sesiones distribuidas. |
| Security: CVE-2026-47842 y CVE-2026-59276 | Sin los cifradores, autenticadores Digest o servicios de tokens afectados. |
| Security: CVE-2026-41707 | DPoP bloqueado explícitamente antes de autenticación. |

Los avisos oficiales sitúan las correcciones de
[Framework 6.2.20](https://spring.io/security/cve-2026-47884/),
[Security 6.5.12](https://spring.io/security/cve-2026-41707/) y
[Data JPA 3.5.14](https://spring.io/security/cve-2026-47834/)
en soporte Enterprise. Se comprobó en Maven Central que las ramas públicas
terminan en 6.2.19, 6.5.11 y Boot 3.5.16 respectivamente.

Para retirar completamente esos avisos hay que obtener los parches comerciales
compatibles con Boot 3 o cambiar el requisito y migrar de forma coordinada a una
rama pública corregida de Boot 4. No se mezcla Spring 7 con Boot 3.
**El job de seguridad permanece bloqueado** mientras esos avisos superen CVSS 7.

## Validación de esta rama

- Java 21: **85 pruebas**, cero fallos/errores; cobertura de líneas **96,89 %**.
- Exportación OTLP real a un colector HTTP temporal: correcta con Kotlin 2.4.21.
- JAR arrancado con clave RSA temporal: seis comprobaciones HTTP correctas
  (sin token 401, Bearer válido 200 en API/salud/métricas, DPoP 401).
- YAML del workflow y sintaxis Bash del paso de dependencias: correctos.
- Dependency-Check completo con feeds oficiales: de 49 CVE distintas del reporte
  inicial a **23**, todas de Spring. Informes generados el 23 de noviembre de 2026.
  El proceso devuelve **código 1 por vulnerabilidades**, no por descarga o error
  de configuración; se mantiene el bloqueo de seguridad.

## Ejecutar las comprobaciones

Usar JDK 21, el mismo que GitHub Actions:

```bash
./mvnw clean verify
```

Con clave NVD en el entorno:

```bash
./mvnw -Psecurity dependency-check:check
```

Sin clave NVD, Dependency-Check 13.0.0 tiene una
[regresión conocida](https://github.com/dependency-check/DependencyCheck/issues/8715).
El workflow selecciona los feeds oficiales y el comando equivalente es:

```bash
./mvnw -Psecurity dependency-check:check \
  '-DnvdDatafeedUrl=https://nvd.nist.gov/feeds/json/cve/2.0/nvdcve-2.0-{0}.json.gz'
```

Esta alternativa está [documentada por OWASP](https://dependency-check.github.io/DependencyCheck/data/mirrornvd.html).
Se mantiene `failBuildOnCVSS=7` y `failOnError=true`, también si falla una descarga.
Los informes HTML, JSON y SARIF se escriben en
`target/security/dependency-check/` y se publican como artefacto del workflow
incluso cuando el resultado es fallo. Una base de datos o análisis incompletos no
equivalen a una ejecución limpia.
