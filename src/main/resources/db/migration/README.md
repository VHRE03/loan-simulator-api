# Guía de Flyway — `src/main/resources/db/migration`

Esta carpeta contiene las **migraciones de base de datos** del proyecto. Todo
cambio de esquema (DDL) y todo cambio de datos (DML) que acompañe a una nueva
funcionalidad **debe vivir aquí**, nunca ejecutarse a mano contra la base de
datos.

---

## 1. ¿Qué es Flyway?

Flyway es la herramienta de este proyecto para **versionar la base de datos**.
Hace con el esquema de PostgreSQL lo que Git hace con el código fuente: cada
cambio es un archivo versionado, se aplica **en orden y exactamente una vez**, y
cualquier desarrollador puede reconstruir la base de datos completa desde cero
simplemente arrancando la aplicación.

Sin Flyway, cada desarrollador aplica cambios a mano (`ALTER TABLE` en su
cliente SQL), los entornos se desincronizan y nadie sabe qué estructura tiene
cada base de datos. Con Flyway, la historia de la base de datos está en el
repositorio y es reproducible.

## 2. ¿Cómo está integrada en este proyecto?

La integración es **automática** mediante Spring Boot:

| Pieza                                    | Ubicación                                             |
|------------------------------------------|-------------------------------------------------------|
| Dependencias (`flyway-core`, `flyway-database-postgresql`) | `pom.xml`                        |
| Habilitación y carpeta de migraciones    | `src/main/resources/application.properties` (`spring.flyway.*`) |
| Archivos de migración                    | esta carpeta (`classpath:db/migration`)               |
| Historial en la base de datos            | tabla `flyway_schema_history` (la crea Flyway solo)   |

Al arrancar la aplicación, **antes** de que JPA/Hibernate inicialice, Spring
Boot ejecuta Flyway automáticamente. No hay ningún comando extra que correr:
`./mvnw spring-boot:run` aplica las migraciones pendientes.

Como Flyway es el dueño del esquema, `spring.jpa.hibernate.ddl-auto` está en
`validate` (y `none` en producción): Hibernate **nunca** crea ni modifica
tablas; solo comprueba que las entidades coincidan con el esquema migrado.

## 3. Convención de nombres (obligatoria)

```
V<versión>__<descripción_en_snake_case>.sql
  │    │      │
  │    │      └─ doble guion bajo (OBLIGATORIO, no uno solo)
  │    └─ entero creciente: 1, 2, 3, ... 10, 11 ...
  └─ V = migración versionada (se ejecuta una sola vez, en orden)
```

Ejemplos reales de esta carpeta:

| Archivo                                        | Qué hace                                       |
|------------------------------------------------|------------------------------------------------|
| `V1__example_create_example_items_table.sql`   | DDL: crea una tabla de ejemplo                 |
| `V2__example_insert_seed_data.sql`             | DML: inserta una fila de ejemplo               |

Reglas:

- La versión es un **entero secuencial**: usa siempre `siguiente número`, sin
  saltos ni subversiones (`V3`, no `V2.5` ni `V3_beta`).
- La **descripción** es corta, en `snake_case` y en inglés
  (`create_loan_simulations`, `add_is_active_to_loans`).
- Un solo idioma y un solo propósito por archivo: no mezcles DDL y DML
  enormes en la misma migración si puedes evitarlo.
- Existen otros prefijos que **no usamos por ahora**: `R__` (repeatable, se
  re-ejecuta cuando cambia su checksum) para vistas/funciones, y `U__` (undo)
  que está desaconsejado. Con `V__` es suficiente para el 99% de los casos.

## 4. ¿Cómo funciona exactamente?

1. La primera vez que arranca la aplicación contra una base de datos vacía,
   Flyway crea la tabla `flyway_schema_history`.
2. Lee todos los archivos `V*__*.sql` de esta carpeta y los ordena por versión.
3. Compara contra `flyway_schema_history` y ejecuta los **pendientes** en
   orden, cada uno dentro de una transacción.
4. Registra para cada uno: versión, descripción, **checksum** del archivo,
   script, tiempo de ejecución y éxito/fallo.
5. Si una migración falla, la transacción hace rollback, la app **no arranca**
   y el error queda claro en el log.

Puedes inspeccionar el estado real de la base de datos en cualquier momento:

```sql
SELECT installed_rank, version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;
```

## 5. Las reglas de oro

> 1. **Nunca edites una migración que ya se aplicó** en alguna base de datos
>    (ni siquiera un comentario). El checksum cambia y Flyway aborta el arranque
>    con un error de validación. ¿Necesitas corregir algo? Escribe una
>    **migración nueva** que lo corrija.
> 2. **Solo agregues archivos nuevos.** La historia es *append-only*, igual que
>    Git.
> 3. **Migraciones hacia adelante** (*forward-only*): no escribimos `DROP` para
>    revertir en desarrollo; creamos una migración nueva que compense el cambio.
>    `V__` no tiene vuelta atrás automática.
> 4. **Un archivo nuevo = siguiente versión global.** Si dos ramas crean la
>    misma versión, quien haga merge segundo debe renumerar su migración.
>    Coordina el número de versión con el equipo.
> 5. **La migración es la única forma de cambiar la base de datos.** Nada de
>    `ALTER` manuales "rápidos": si no está en esta carpeta, no existe para el
>    resto del equipo ni para los otros entornos.
> 6. **Prueba tu migración siempre en local** contra una base de datos limpia
>    (`DROP DATABASE` + `CREATE DATABASE` + arrancar la app) para verificar que
>    la secuencia completa funciona desde cero.

## 6. Cómo añadir una migración (paso a paso)

1. Consulta `flyway_schema_history` (o el archivo con el número `V` más alto de
   esta carpeta) para saber el último número de versión aplicado.
2. Crea el archivo con el siguiente número:
   `V<siguiente>__<descripción>.sql`.
3. Escríbelo en PostgreSQL (este proyecto usa exclusivamente PostgreSQL: puedes
   usar tipos como `UUID`, `JSONB`, `TIMESTAMPTZ`, `GEN_RANDOM_UUID()`...).
   Para un módulo de simulación de préstamos, recuerda: los montos e importes
   son `NUMERIC(19, 4)` y las tasas `NUMERIC(9, 6)` — nunca `FLOAT`/`DOUBLE`
   para dinero.
4. **Incluye siempre las columnas de auditoría estándar** (ver sección 7).
5. Arranca la aplicación (o ejecuta los tests de contexto) y verifica:
   ```bash
   ./mvnw spring-boot:run
   ```
   La primera vez, en el log verás algo como:
   ```
   Flyway 11.x.x : Successfully validated 2 migrations
   Migrating schema "public" to version "3 - tu descripcion"
   Successfully applied 1 migration to schema "public"
   ```
6. Verifica el resultado con tu cliente SQL y **comitea el archivo junto con el
   código Java** que lo requiere (misma rama, mismo PR).

## 7. Columnas obligatorias: la relación con `BaseEntity`

**Todas** las entidades del proyecto extienden `BaseEntity` (del starter
interno `com.vhre:base-project-spring-boot-starter`), que aporta `id`,
`createdAt`, `updatedAt` y `deleted` (mapeada a la columna `is_deleted`).
Por eso **toda tabla nueva** debe incluir exactamente estas columnas:

| Columna       | Tipo PostgreSQL | Restricción                            | Origen                              |
|---------------|-----------------|------------------------------------------|--------------------------------------|
| `id`          | `UUID`          | `NOT NULL`, `PRIMARY KEY`                | `BaseEntity` (Hibernate lo genera)   |
| `created_at`  | `TIMESTAMP`     | `NOT NULL`                               | Spring Data auditing (`@CreatedDate`) |
| `updated_at`  | `TIMESTAMP`     | `NULL`                                   | Spring Data auditing (`@LastModifiedDate`) |
| `is_deleted`  | `BOOLEAN`       | `NOT NULL DEFAULT FALSE`                 | Soft-delete de `BaseEntity`          |

Plantilla mínima para una tabla nueva:

```sql
CREATE TABLE loan_simulations
(
    id          UUID         NOT NULL,
    -- ... columnas de negocio ...
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NULL,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_loan_simulations PRIMARY KEY (id)
);
```

Notas importantes:

- Los **nombres de tabla** siguen la convención del proyecto: **plural,
  `snake_case` y minúsculas** (`loan_simulations`, nunca `LoanSimulation` ni
  `loanSimulation`).
- `BaseEntity` aplica un filtro global `is_deleted = false`: las filas con
  `is_deleted = true` son invisibles para toda la aplicación (borrado lógico).
  La API del starter (`delete`) hace soft-delete, no `DELETE` físico.
- En migraciones de **datos** la auditoría no se rellena sola (no hay ninguna
  aplicación ejecutando el SQL), así que incluye `created_at`/`updated_at`
  explícitamente (mira el ejemplo `V2__example_insert_seed_data.sql`).

## 8. Convenciones de nombres en la base de datos

| Objeto              | Prefijo | Ejemplo                              |
|---------------------|---------|--------------------------------------|
| Tabla               | —       | `loan_simulations`                   |
| Primary key         | `pk_`   | `pk_loan_simulations`                |
| Foreign key         | `fk_`   | `fk_amortization_items_simulation_id` |
| Índice              | `ix_`   | `ix_loan_simulations_is_deleted`     |
| Unique constraint   | `uq_`   | `uq_loan_simulations_reference`      |
| Check constraint    | `ck_`   | `ck_loan_simulations_amount_positive` |

## 9. Errores frecuentes y cómo resolverlos

| Error en el arranque                                  | Causa                                                          | Solución                                                                 |
|--------------------------------------------------------|------------------------------------------------------------------|---------------------------------------------------------------------------|
| `Migration checksum mismatch for migration version 1`  | Se editó un archivo ya aplicado                                | Restaurar el archivo original. Si el cambio es necesario: migración nueva |
| `Detected applied migration not resolved locally`      | La base de datos tiene versiones que ya no existen en la carpeta | NO borrar archivos. Reponerlos desde Git                                  |
| `Validate failed: Migration description mismatch`     | Se renombró un archivo ya aplicado                             | Restaurar el nombre original                                              |
| `Found more than one migration with version 3`          | Dos ramas crearon `V3`                                        | Renumerar una de ellas (la de la rama que hizo merge segundo)              |
| La app arranca pero la tabla no aparece                | El archivo está fuera de `db/migration` o mal nombrado        | Revisar nombre: `V3__descripcion.sql` con **doble** guion bajo            |
| `FlywayException: Unable to connect to database`        | `application-local.properties` con credenciales incorrectas    | Verificar host/puerto/usuario/contraseña                                   |

**Nunca uses `flyway repair` ni borres filas de `flyway_schema_history` sin
entender lo que hacen**: enmascaran desincronizaciones en lugar de
resolverlas. Su único uso legítimo en este proyecto es en bases de datos
desechables de desarrollo.

## 10. ¿Qué hacer con `V1` y `V2`?

Los dos archivos de ejemplo (`V1__...` y `V2__...`) existen **solo para
documentar el funcionamiento** con un caso real. Cuando llegue el primer módulo
de negocio tienes dos opciones:

- **Opción A (recomendada si aún no hay ningún entorno compartido):** borra
  ambos archivos, borra la base de datos local (`DROP DATABASE` +
  `CREATE DATABASE`) y crea tu `V1__...` real desde cero (por ejemplo
  `V1__create_loan_simulations.sql`). También elimina la tabla
  `example_items` de cualquier base de datos donde ya se aplicaran.
- **Opción B (si ya existen entornos con las migraciones aplicadas):** crea
  `V3__drop_example_items.sql` con `DROP TABLE example_items;` y sigue
  numerando desde ahí. En Flyway **nunca** se borra el historial aplicado.

---

*Para dudas de arquitectura de migraciones, revisa también el `README.md`
raíz del proyecto.*
