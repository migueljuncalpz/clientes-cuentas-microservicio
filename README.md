# Clientes y cuentas bancarias

Microservicio REST del ejercicio, implementado con Java 21, Spring Boot 3.5.16,
Maven y H2 en memoria. Expone consultas de clientes, alta de cuentas y sustitución
del saldo, con arquitectura hexagonal y contrato OpenAPI.

## Decisiones y alcance

- Un servicio posee las tablas de clientes y cuentas. La creación de un cliente
  y su primera cuenta comparte una transacción. No hay otros servicios ni
  comunicaciones de red de negocio; Kafka, outbox, circuit breakers y reintentos
  de red se incorporarían cuando existiera una integración que los necesitara.
- Dominio y aplicación son Java puro. Spring, JPA, validación HTTP y observabilidad
  se concentran en infraestructura. ArchUnit comprueba estas dependencias.
- Los importes usan `BigDecimal` y `DECIMAL(19,2)`, evitando los errores de `Double`.
  Se admite desde cero hasta `99999999999999999.99`; no se redondean importes con
  decimales significativos adicionales. Los clientes que consuman la API deben
  usar también un tipo decimal para conservar la precisión.
- Los filtros se ejecutan sobre modelos inmutables usando Streams. La lectura de
  clientes y cuentas se resuelve con un `JOIN FETCH`, sin N+1 ni Open Session in View.
  La API devuelve todos los resultados para ajustarse al ejercicio; para grandes
  volúmenes convendría añadir paginación y trasladar los filtros a consultas SQL.
- Flyway controla esquema y datos iniciales. Hibernate valida el esquema y no lo
  crea. La clave foránea garantiza que una cuenta siempre tenga un titular.
- `@Version` detecta actualizaciones concurrentes incompatibles y las traduce a
  HTTP 409. Si dos altas compiten por crear el mismo cliente, la operación que
  pierde la carrera revierte su primera transacción y reintenta la cuenta en una
  transacción nueva cuando confirma que el cliente ya existe.

Se mantiene Spring Boot 3 por requisito del enunciado. La versión elegida y su
compatibilidad están documentadas en las
[notas de Spring Boot 3.5.16](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/).

## Estructura

```text
src/main/java/com/zarahome/cuentas/
├── CuentasApplication.java
├── domain/                       # Cliente, CuentaBancaria, Dni, Dinero, reglas y errores
├── application/
│   ├── ServicioClientes.java      # Consultas y filtros
│   ├── ServicioCuentas.java       # Alta y actualización atómicas
│   └── port/
│       ├── in/                   # Casos de uso públicos
│       └── out/                  # Repositorios y unidad de trabajo
└── infrastructure/
    ├── config/                   # Composición de casos de uso y reloj UTC
    ├── rest/                     # Controladores, DTO y errores RFC 9457
    ├── persistence/              # Entidades JPA, adaptadores y transacciones
    ├── observability/            # Correlation ID y logs de peticiones
    └── security/                 # JWT Resource Server, scopes y cabeceras HTTP
src/main/resources/
├── application.yml
├── application-local.yml
├── db/migration/                 # Esquema y cinco clientes/siete cuentas iniciales
└── static/openapi.yaml           # Contrato API-First
src/test/                         # Dominio, casos de uso, REST, BD y arquitectura
```

Las dependencias apuntan hacia dentro: REST invoca puertos de entrada; los casos
de uso invocan puertos de salida; JPA implementa esos puertos. Los modelos JPA y
los DTO HTTP nunca se devuelven al dominio.

## Reglas que concreta la solución

- El alta recibe únicamente `dniCliente`, `tipoCuenta` y `total`. Si el cliente no
  existe, se crea con los datos personales a `null`, sin inventar identidad ni
  fecha de nacimiento. El ejercicio no solicita completar después esos datos.
- La mayoría de edad comienza el día del cumpleaños número 18. Se utiliza UTC
  y un `Clock` inyectable. Una fecha de nacimiento desconocida excluye al cliente
  del filtro de adultos. Las pruebas fijan el reloj en el 23 de noviembre de 2026.
- El filtro por cantidad compara la **suma de todas las cuentas** con `>` estricto.
  Por ejemplo, Juan suma 170000 y aparece al filtrar por 150000, pero no por 170000.
- Se valida el formato del DNI, sus ocho dígitos y la letra de control según el
  algoritmo oficial; la letra se normaliza a mayúsculas.
- `NORMAL`, `PREMIUM` y `JUNIOR` son categorías. No se añaden restricciones de edad
  o saldo por tipo que el ejercicio no define.
- `PUT` sustituye el saldo y es idempotente. `POST` crea una cuenta en cada petición
  y el cliente no debe reintentarla automáticamente, porque cada petición aceptada
  crea una cuenta distinta. El servicio sí reintenta internamente la transacción
  si detecta que otra petición creó concurrentemente al mismo cliente.

## Ejecutar

Requisitos: JDK 21, Python 3 y OpenSSL para generar JWT locales. El wrapper
descarga la versión fijada de Maven en el primer uso y necesita acceso a Maven
Central. También se puede utilizar Maven 3.6.3 o superior.

El POM declara Lombok como procesador de anotaciones para que también se ejecute
al compilar con JDK 23 o superior. El código generado sigue siendo compatible
con Java 21. En IntelliJ, recarga el proyecto Maven después de cambiar el POM;
para la prueba técnica, selecciona JDK 21 como Project SDK y Maven Runner JRE.


El servicio está protegido con JWT Bearer también en el perfil local. Antes de
arrancarlo, genera la clave RSA local (solo la primera vez):

```bash
python3 scripts/local-jwt.py >/dev/null
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

El generador crea la clave privada y pública en
`~/.config/clientes-cuentas-microservicio/jwt/`; la privada no se guarda en el
repositorio. En otra terminal, genera un token local válido durante 15 minutos y
úsalo como Bearer:

```bash
TOKEN="$(python3 scripts/local-jwt.py)"
curl http://localhost:8080/clientes \
  -H "Authorization: Bearer ${TOKEN}"
```

Sin token válido se responde `401`. Un token válido sin el permiso necesario
recibe `403`. El generador incluye los cuatro permisos de desarrollo por defecto;
puedes limitar los scopes, por ejemplo, a solo lectura:

```bash
LOCAL_JWT_SCOPES="cuentas.read" python3 scripts/local-jwt.py
```

El token tiene los permisos configurados en la variable `LOCAL_JWT_SCOPES` al
generarlo. No compartas tokens ni claves privadas.

La aplicación escucha en `http://localhost:8080`. Los cinco clientes y las siete
cuentas del enunciado se cargan al iniciar. Los datos se pierden al detener la JVM.

Para empaquetar y ejecutar el JAR:

```bash
./mvnw clean verify
java -jar target/clientes-cuentas-microservicio-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

El perfil `local` no expone la consola H2.

### Omitir autenticación para la evaluación

Si el entrevistador quiere ejecutar y probar la API sin tokens, puede desactivar
la autenticación explícitamente mediante `APP_SECURITY_ENABLED=false`:

```bash
APP_SECURITY_ENABLED=false ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

También se puede usar con el JAR:

```bash
APP_SECURITY_ENABLED=false java -jar target/clientes-cuentas-microservicio-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=local
```

La seguridad está activada por defecto. Al desactivarla, todas las rutas,
incluidos Actuator y las operaciones de escritura, quedan accesibles sin token;
la aplicación registra un aviso al arrancar. No uses esta opción en entornos
compartidos o productivos.

## API

Contrato: [openapi.yaml](src/main/resources/static/openapi.yaml), también accesible
en [la aplicación](http://localhost:8080/openapi.yaml). Las fechas JSON usan ISO
`yyyy-MM-dd`. Las colecciones se ordenan por DNI y sus cuentas por ID.

| Método | Ruta | Resultado correcto |
| --- | --- | --- |
| GET | `/clientes` | 200, clientes con cuentas |
| GET | `/clientes/mayores-de-edad` | 200, clientes con 18 años cumplidos |
| GET | `/clientes/con-cuenta-superior-a/{cantidad}` | 200, suma de saldos mayor que la cantidad |
| GET | `/clientes/{dni}` | 200, un cliente con todas sus cuentas |
| POST | `/cuentas` | 201, cuenta creada |
| PUT | `/cuentas/{idCuenta}` | 200, cuenta con el nuevo saldo |

Ejemplos:

```bash
TOKEN="$(python3 scripts/local-jwt.py)"
curl http://localhost:8080/clientes -H "Authorization: Bearer ${TOKEN}"
curl http://localhost:8080/clientes/mayores-de-edad -H "Authorization: Bearer ${TOKEN}"
curl http://localhost:8080/clientes/con-cuenta-superior-a/150000 -H "Authorization: Bearer ${TOKEN}"
curl http://localhost:8080/clientes/11111111H -H "Authorization: Bearer ${TOKEN}"

curl -X POST http://localhost:8080/cuentas \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-ID: alta-cuenta-001' \
  -d '{"dniCliente":"11111111H","tipoCuenta":"NORMAL","total":50000}'

curl -X POST http://localhost:8080/cuentas \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"dniCliente":"66666666Q","tipoCuenta":"JUNIOR","total":300}'

curl -X PUT http://localhost:8080/cuentas/1 \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"total":180000}'
```

Errores: 400 para parámetros o cuerpos inválidos, 404 para recursos inexistentes,
409 para conflictos de escritura y 500 para errores inesperados. Se devuelven como
`application/problem+json`, con `codigo`, `correlationId` y, en validaciones de
campos, `errores`. No se exponen SQL ni trazas de pila en las respuestas. Se rechazan
campos JSON desconocidos, tipos incompatibles, categorías numéricas y JSON adicional.

## Postman

Importa estos dos archivos desde **Import** en Postman:

- [Colección de clientes y cuentas](postman/clientes-cuentas.postman_collection.json).
- [Entorno Local](postman/local.postman_environment.json).

Arranca la aplicación y selecciona el entorno **Clientes y cuentas - Local**.
Genera un JWT con `python3 scripts/local-jwt.py` y pega el resultado en la variable
secreta `accessToken` del entorno Local. `baseUrl` vale `http://localhost:8080`;
puedes cambiarlo en el entorno. La colección
incluye 21 peticiones con pruebas automáticas y ejemplos de respuestas. Ejecuta
la colección completa en orden desde **Run collection** para comprobar diagnóstico,
consultas, altas, actualización idempotente y errores esperados 400/404/405.

Las variables `dniCliente`, `cantidadMinima`, `saldoInicial` y `saldoActualizado`
se editan en las variables de la colección. El flujo captura `cuentaId`, genera
`dniNuevo` y captura `cuentaNuevoClienteId`; no definas esas variables dinámicas
en el entorno para evitar que oculten los valores capturados. Las peticiones de
actualización requieren ejecutar antes el alta del mismo flujo.

Cada ejecución crea dos cuentas y un cliente. Las actualizaciones afectan solo
a la cuenta creada por el propio flujo; las siete cuentas iniciales se conservan.
Las comprobaciones admiten ejecuciones repetidas sin exigir un número fijo de
clientes. Reiniciar la JVM restablece los datos iniciales de H2.

También se puede ejecutar con Node.js y Newman, sin instalar nada globalmente:

```bash
TOKEN="$(python3 scripts/local-jwt.py)"
npx --yes newman@6.2.1 run postman/clientes-cuentas.postman_collection.json \
  -e postman/local.postman_environment.json \
  --env-var "accessToken=${TOKEN}" \
  --bail --timeout-request 10000
```

Para otro puerto, añade `--env-var baseUrl=http://localhost:8081`. Los importes de
los ejemplos se mantienen en el rango exacto de `Number` de JavaScript. Los errores
incluidos son casos deliberados y sus códigos HTTP forman parte de las pruebas.
El formato de la colección sigue el
[esquema Postman 2.1](https://schema.postman.com/collection/json/v2.1.0/draft-04/collection.json)
y el comando utiliza las [opciones oficiales de Newman](https://learning.postman.com/docs/collections/using-newman-cli/newman-options).

## Pruebas y calidad

```bash
./mvnw test
./mvnw clean verify
```

`verify` ejecuta todas las pruebas, empaqueta el JAR y exige al menos un 80 % de
cobertura de líneas. El informe HTML está en `target/site/jacoco/index.html`.
GitHub Actions ejecuta el mismo comando con Java 21 en push a `main`/`develop`
y en pull requests.

Las pruebas cubren reglas de dinero y edad, agregación de saldos, casos de uso,
los endpoints con MockMvc y H2 real, errores y validaciones, rollback de cliente y
cuenta y dos escrituras simultáneas que deben producir un conflicto optimista.
ArchUnit impide dependencias del dominio o la aplicación hacia infraestructura.

## Observabilidad

- Logs JSON Logstash con `correlationId`, `traceId` y `spanId`. Se registra método
  HTTP y estado; no se registran cuerpos ni DNI en el log de peticiones.
- Se acepta `X-Correlation-ID` con 1 a 64 caracteres alfanuméricos, `.`, `_` o `-`;
  en otro caso se genera un UUID. Se devuelve también en errores y se limpia el MDC.
- Salud: `/actuator/health`. Métricas Prometheus: `/actuator/prometheus`.
- Ambos endpoints requieren JWT: `actuator.health.read` para salud y
  `actuator.metrics.read` para métricas. La API de clientes requiere
  `cuentas.read`; crear o actualizar cuentas requiere `cuentas.write`.
- Micrometer Tracing utiliza OpenTelemetry. La exportación OTLP está desactivada
  por defecto para permitir ejecutar el ejercicio sin un colector. Para activarla:

```bash
OTEL_EXPORT_ENABLED=true \
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://localhost:4318/v1/traces \
TRACING_SAMPLING_PROBABILITY=1.0 \
./mvnw spring-boot:run
```

La configuración y los tipos de logs siguen la
[documentación de Spring Boot](https://docs.spring.io/spring-boot/3.5/reference/actuator/tracing.html).

## Análisis de seguridad con GitHub Actions

El workflow `Security` integra **Gitleaks**, **OWASP Dependency-Check**, **CodeQL**,
**ZAP** y **SonarQube**. Dependabot propone actualizaciones de Maven y acciones
hacia `develop`. Los análisis generan informes y conservan los fallos detectados.

Se ejecutan en push, pull requests, manualmente y semanalmente. CodeQL se activa
por defecto en repositorios públicos; en privados requiere habilitación compatible.
SonarQube se activa configurando `ENABLE_SONAR`, `SONAR_TOKEN` y el proyecto real.
`NVD_API_KEY` es recomendable para Dependency-Check.

El workflow ZAP genera credenciales locales efímeras y escanea la API autenticándose.

La revisión y las mitigaciones de dependencias se documentan en
[SECURITY.md](SECURITY.md). Se mantiene Spring Boot 3 por el enunciado: las alertas
pendientes de Spring siguen bloqueando Dependency-Check. La única exclusión
corresponde a una CVE de OpenTelemetry JavaScript asociada a los artefactos Java.
Sin `NVD_API_KEY`, el workflow utiliza los feeds JSON 2.0 oficiales de NVD.


## Trabajo con Gitflow

`main` contiene versiones estables y `develop` integra funcionalidades. Este
ejercicio se desarrolla en `feature/gestion-cuentas-bancarias`. Prefijos adicionales:
`bugfix/`, `release/`, `hotfix/` y `support/`. Las ramas pueden gestionarse con Git
estándar aunque el comando `git-flow` no esté instalado.

El perfil local usa claves RSA de desarrollo. En otros entornos, configura
`JWT_ISSUER_URI`, `JWT_JWKS_URI` y `JWT_AUDIENCE` con los valores oficiales del
proveedor de identidad. El microservicio valida los tokens; no los emite. La
integración con un proveedor real requiere confirmar el formato de sus scopes y
claims. H2 continúa siendo efímera.
