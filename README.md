# loan-simulator-api

API REST **monolítica** para la **simulación de préstamos** (créditos):
cálculo de cuotas, intereses y planes de amortización para banca/fintech.

## Stack tecnológico

| Componente            | Tecnología                                            |
|-----------------------|--------------------------------------------------------|
| Lenguaje              | Java 21                                                |
| Framework             | Spring Boot **4.1.1** (última versión estable en Maven Central) |
| Arquitectura          | Monolito (Spring MVC, **sin Kafka**, sin mensajería)  |
| Base de datos         | PostgreSQL                                              |
| Migraciones           | **Flyway** (ver `src/main/resources/db/migration/README.md`) |
| Gestión del esquema   | Flyway es el **único** dueño del esquema: `ddl-auto=validate` (local/dev) y `none` (prod) |
| Persistencia          | Spring Data JPA + Hibernate                             |
| Documentación API     | springdoc-openapi 3.x (Swagger UI)                     |
| Clases base internas  | `com.vhre:base-project-spring-boot-starter` (GitHub Packages) |
| Mappers               | MapStruct                                              |
| Boilerplate           | Lombok                                                 |
| Testing               | JUnit 5 + Mockito + AssertJ + MockMvc                 |
| Build                 | **Maven** (ver justificación abajo)                   |

### Por qué Maven (y no Gradle)

1. **Consistencia con el ecosistema corporativo**: `hive-ingest-service` y
   `clabe-validation-api` usan Maven con wrapper (`./mvnw`); el starter interno
   se publica como artefacto Maven en GitHub Packages.
2. **Autenticación contra GitHub Packages**: ya resuelta en `~/.m2/settings.xml`
   con el servidor `github` (PAT con `read:packages`). Con Gradle habría que
   replicar esa configuración en otro formato.
3. **El parent POM de Spring Boot** gestiona las versiones (BOM) de todas las
   dependencias: menos decisiones de versionado, menos drift entre servicios.

## Estructura del proyecto

```
loan-simulator-api/
├── pom.xml
├── mvnw / mvnw.cmd                    # Maven wrapper (no requiere Maven instalado)
└── src
    ├── main
    │   ├── java/com/vhre/loansimulator
    │   │   ├── LoanSimulatorApiApplication.java   # Punto de entrada
    │   │   ├── config/                              # Configuración (OpenAPI/Swagger)
    │   │   └── modules/          # (crear) módulos funcionales: entity, dto,
    │   │                         # mapper, repository, service, controller
    │   └── resources
    │       ├── application.properties               # Perfil por defecto + Flyway + Swagger
    │       ├── application-local.properties         # BD local (NO se comitea: en .gitignore)
    │       ├── application-dev.properties           # Dev (via variables de entorno)
    │       ├── application-prod.properties          # Prod (via variables de entorno)
    │       └── db/migration/                        # MIGRACIONES FLYWAY
    │           ├── README.md                        # Guía completa de Flyway
    │           ├── V1__example_create_example_items_table.sql   # Ejemplo DDL comentado
    │           └── V2__example_insert_seed_data.sql             # Ejemplo DML comentado
    └── test
        └── java/com/vhre/loansimulator
            ├── LoanSimulatorApiApplicationTests.java  # Smoke test (@SpringBootTest)
            ├── config/OpenApiConfigTest.java          # Unit test con JUnit 5
            └── pattern/ServiceTestPatternTest.java    # Patrón de tests con Mockito
```

## Paquetes propios vs. estándar

| Paquete                          | Origen                    | Contenido |
|----------------------------------|---------------------------|-----------|
| `com.vhre.loansimulator`        | **Propio (este repo)**    | Código de la API |
| `com.vhre.loansimulator.config`  | **Propio (este repo)**    | Configuración (OpenAPI) |
| `com.vhre.loansimulator.modules` | **Propio (este repo)**    | Módulos funcionales (crear) |
| `com.vhre.base.*`                | **Propio (starter interno)** | `BaseEntity`, `BaseDTO`, `BaseMapper`, `BaseService(Impl)`, `BaseController`, `GlobalExceptionHandler` |
| `org.springframework.*`          | Estándar                  | Spring Boot / Framework |
| `jakarta.*`                      | Estándar                  | Persistence, Validation |
| `io.swagger.*`                   | Estándar                  | OpenAPI 3 |
| `org.mapstruct`                  | Estándar                  | Mappers |
| `org.flywaydb`                   | Estándar                  | Migraciones |

La convención de paquete raíz replica la de los servicios hermanos:
`clabe-validation-api` → `com.vhre.clabe`, `hive-ingest-service` →
`com.vhre.ingest`, `loan-simulator-api` → **`com.vhre.loansimulator`**.

## Prerrequisitos

1. **JDK 21 o superior** (`java -version`). El proyecto compila con `release 21`
   pero el build puede correr desde cualquier JDK moderno.
   - **Importante (Lombok + JDK)**: Lombok depende de APIs internas de `javac`
     y cada JDK nuevo requiere una versión mínima de Lombok. Este proyecto usa
     **Lombok 1.18.48**, necesario para compilar con **JDK 27** (IntelliJ suele
     descargar el JDK más nuevo como Project SDK). Si al compilar ves
     `Fatal error compiling: java.lang.ExceptionInInitializerError:
     com.sun.tools.javac.tree.EndPosTable`, el JDK del build es más nuevo de lo
     que soporta la versión de Lombok del proyecto: actualiza
     `lombok.version` en el `pom.xml`.
   - Soporte de Lombok por JDK: 1.18.32 → JDK 21/22 · 1.18.36 → JDK 23 ·
     1.18.38 → JDK 24 · 1.18.40 → JDK 25 · 1.18.46 → JDK 26 ·
     **1.18.48 → JDK 27**.
   - Spring Boot 4.1 declara soporte oficial para Java 17–26; este proyecto
     fue probado compilando y ejecutando con JDK 21, 25 y 27 sin problemas.
     Si apareciera algún fallo raro en *runtime* con un JDK recién salido,
     cambia el Project SDK de IntelliJ (File → Project Structure → Project)
     a un JDK con soporte LTS (21 o 25).
2. **PostgreSQL 14 o superior** corriendo en local (por defecto `localhost:5432`).
3. **Token de GitHub Packages** para el starter interno (una sola vez por máquina):
   el `pom.xml` declara el repositorio `https://maven.pkg.github.com/VHRE03/base-project-spring-boot`.
   En `~/.m2/settings.xml` debe existir el servidor `github` con un PAT classic
   (`ghp_...`) con permiso `read:packages`:

   ```xml
   <settings xmlns="http://maven.apache.org/SETTINGS/1.2.0" ...>
       <servers>
           <server>
               <id>github</id>
               <username>VHRE03</username>
               <password>ghp_tu_token_aqui</password>
           </server>
       </servers>
   </settings>
   ```

   El starter aporta: `BaseEntity`, `BaseDTO`, `BaseMapper`, `BaseService`,
   `BaseServiceImpl`, `BaseController` y el `GlobalExceptionHandler`.

## Configuración de la base de datos local

```sql
CREATE DATABASE loan_simulator_db;
CREATE USER loan_user WITH PASSWORD 'loan_secret_password_123';
-- IMPORTANTE (PostgreSQL 15+): el esquema public ya no permite CREATE a usuarios
-- que no son duenos de la base de datos. Sin esta linea Flyway falla con
-- "permission denied for schema public" al intentar crear sus tablas.
ALTER DATABASE loan_simulator_db OWNER TO loan_user;
```

Después crea tu `application-local.properties` (está en `.gitignore`, no se
comitea; este bloque sirve de plantilla):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/loan_simulator_db
spring.datasource.username=loan_user
spring.datasource.password=loan_secret_password_123
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
openapi.server.url=http://localhost:8080
openapi.server.description=Servidor Local
spring.jackson.mapper.sort-properties-alphabetically=false
```

## Ejecutar

```bash
./mvnw spring-boot:run
```

Al arrancar verás en el log cómo Flyway aplica las migraciones pendientes
(`Migrating schema "public" to version "1 - example create example items table"`)
y después el Tomcat en `http://localhost:8080`.

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

### Perfiles

| Perfil  | Activación                          | Uso                                        |
|---------|-------------------------------------|--------------------------------------------|
| `local` | Por defecto (`APP_PROFILE` vacío)   | Tu máquina: lee `application-local.properties` |
| `dev`   | `APP_PROFILE=dev`                   | Servidor de desarrollo (via variables de entorno `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`) |
| `prod`  | `APP_PROFILE=prod`                  | Producción (mismas variables de entorno, obligatorias sin defaults) |

```bash
APP_PROFILE=dev DB_HOST=... DB_NAME=... DB_USER=... DB_PASSWORD=... ./mvnw spring-boot:run
```

## Migraciones (Flyway)

Toda la guía de uso, convenciones y funcionamiento está en
[`src/main/resources/db/migration/README.md`](src/main/resources/db/migration/README.md).

Resumen operativo:

- Las migraciones viven en `src/main/resources/db/migration` y se aplican
  **automáticamente al arrancar** la aplicación, en orden y una sola vez.
- Para un cambio nuevo crea `V<siguiente>__<descripcion>.sql` (doble guion bajo).
- **Nunca edites ni borres una migración ya aplicada** en algún entorno: el
  checksum cambiaría y la aplicación no arrancaría.
- `V1` y `V2` son **ejemplos documentados** (DDL y DML) que puedes eliminar
  cuando agregues el primer módulo real (ver la sección 10 del README de
  migraciones).

## Testing

```bash
./mvnw test
```

- **JUnit 5 + AssertJ** y **Mockito** ya vienen integrados por los starters de
  test (`spring-boot-starter-webmvc-test` y `spring-boot-starter-data-jpa-test`).
- `LoanSimulatorApiApplicationTests` arranca el contexto completo: requiere
  PostgreSQL local accesible.
- `OpenApiConfigTest` y `ServiceTestPatternTest` son tests unitarios puros que
  corren **sin base de datos**. `ServiceTestPatternTest` documenta con ejemplos
  el patrón para testear los services de futuros módulos con Mockito
  (`@Mock`, `@InjectMocks`, `when/verify`).

## Cómo agregar un módulo funcional

El patrón obligatorio (heredando del starter interno) para una entidad `Foo`
(por ejemplo, el futuro módulo de simulaciones de crédito):

```
com.vhre.loansimulator.modules.<modulo>/
├── entity/Foo.java                 extends BaseEntity        (+ @Table plural snake_case)
├── dto/FooDTO.java                 extends BaseDTO           (+ @Schema, validaciones)
├── mapper/FooMapper.java           extends BaseMapper<Foo, FooDTO> (@Mapper MapStruct)
├── repository/FooRepository.java   extends JpaRepository<Foo, UUID>
├── service/FooService.java        extends BaseService<FooDTO, UUID>   (INTERFAZ)
├── service/FooServiceImpl.java    extends BaseServiceImpl<...>        (@Service)
├── controller/FooController.java  extends BaseController<Foo, FooDTO, UUID> (@RestController, @Tag)
└── enums/                          (si el módulo necesita enumeraciones)
```

Convenciones verificadas en `hive-ingest-service` y replicadas aquí:

- Ruta de los controllers: `@RequestMapping("/api/v1/<plural>")`
  (ej. `/api/v1/loans`) con `@Tag(name = "Foo Management")`.
- Mapper MapStruct: `@Mapper(componentModel = "spring")`.
- DTO: `@Schema(description = ...)` + `@JsonPropertyOrder({"id", ..., "createdAt",
  "updatedAt", "deleted"})` para un JSON determinista.
- Montos e importes de préstamos: **`BigDecimal`** en Java y `NUMERIC` en
  PostgreSQL — nunca `double`/`float` para dinero.

Y no olvides su migración Flyway con las columnas estándar de `BaseEntity`
(`id`, `created_at`, `updated_at`, `is_deleted`): ver la plantilla en el README
de migraciones, sección 7.
