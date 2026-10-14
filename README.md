# Alquiler de Vehiculos API

API REST para gestionar una empresa de **alquiler de vehículos**: oficinas, flota de vehículos, clientes, alquileres y mantenimientos. Calcula automáticamente el precio de cada alquiler, mantiene sincronizado el estado de los vehículos y evita alquilar un vehículo que no está disponible o que ya tiene un alquiler en esas fechas.

> **Sin autenticación.** Esta API no implementa login: todos los endpoints son públicos.

**Autor:** Maria Fernanda Leon Diaz

---

## Tecnologías

| Tecnología | Uso |
|---|---|
| Java 21 | Lenguaje |
| Spring Boot 4.1.1 | Framework (Web MVC, Data JPA) |
| Hibernate / JPA | Persistencia |
| MySQL 8 | Base de datos (administrada con MySQL Workbench) |
| Maven (wrapper incluido) | Compilación y ejecución |
| Postman | Pruebas de los endpoints |

## Requisitos previos

- JDK 21
- MySQL 8 en ejecución
- Postman (o similar) para probar la API

## Instalación y ejecución

```bash
# 1. Clonar el repositorio
git clone https://github.com/FER202360573/API_3_Alquilar_Vehiculos.git
cd alquilar

# 2. Configurar las credenciales de MySQL (ver la sección siguiente)

# 3. Ejecutar
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
```

La API queda en `http://localhost:8080`. Para comprobarlo abre `http://localhost:8080/api/vehiculos` (devuelve `[]` si aún no hay vehículos).

No hace falta crear las tablas a mano: Hibernate las genera al arrancar (`ddl-auto=update`). La base de datos de la URL se crea sola si no existe (`createDatabaseIfNotExist=true`).

### Configurar la conexión a la base de datos

El proyecto necesita un usuario de MySQL con permiso para crear la base de datos. Hay dos formas de indicar las credenciales; elige una.

#### Opción A: editar `application.properties`

Abre `src/main/resources/application.properties` y cambia los valores de ejemplo por los tuyos:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/alquiler?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=usuario
spring.datasource.password=contraseña
```

Si quieres usar otra base de datos, cambia el nombre (`alquiler`) en la URL.

> **Importante:** no subas el archivo con tus credenciales reales a GitHub. Deja los valores de ejemplo en el repositorio y rellénalos solo en tu copia local.

#### Opción B: variables de entorno

No hay que tocar ningún archivo. Spring Boot da prioridad a estas variables sobre `application.properties`.

| Variable | Descripción |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC de MySQL |
| `SPRING_DATASOURCE_USERNAME` | Usuario de MySQL |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de MySQL |

```bash
# Linux / macOS
export SPRING_DATASOURCE_USERNAME=mi_usuario
export SPRING_DATASOURCE_PASSWORD=mi_password
./mvnw spring-boot:run
```

```powershell
# Windows (PowerShell)
$env:SPRING_DATASOURCE_USERNAME="mi_usuario"
$env:SPRING_DATASOURCE_PASSWORD="mi_password"
.\mvnw.cmd spring-boot:run
```

## Estructura del proyecto

Arquitectura de capas: los controladores reciben las peticiones, los servicios aplican las reglas de negocio y los repositorios acceden a la base de datos.

```
src/main/java/com/api/alquiler
├── config/          Seguridad (Permite el acceso sin contraseña a todos los endpoints)
├── controllers/     Controladores REST (@RestController)
├── models/          Entidades JPA (OficinaModel, VehiculoModel, ClienteModel, AlquilerModel, MantenimientoModel)
├── repositories/    Repositorios Spring Data JPA
└── services/        Reglas de negocio y validaciones

src/main/resources
└── application.properties
```

## Modelo de datos

| Entidad | Tabla | Descripción |
|---|---|---|
| `OficinaModel` | `oficinas` | Ubicación física donde se gestionan los vehículos |
| `VehiculoModel` | `vehiculos` | Unidad de la flota, asociada a una oficina |
| `ClienteModel` | `clientes` | Persona que alquila los vehículos |
| `AlquilerModel` | `alquileres` | Contrato que relaciona un vehículo con un cliente en un periodo |
| `MantenimientoModel` | `mantenimientos` | Historial de reparaciones y revisiones de un vehículo |

Relaciones: una **oficina** tiene muchos **vehículos**; un **vehículo** tiene muchos **alquileres** y muchos **mantenimientos**; un **cliente** tiene muchos **alquileres**.

Valores permitidos: `estado` del vehículo (`DISPONIBLE`, `ALQUILADO`, `MANTENIMIENTO`) y `estado` del alquiler (`PENDIENTE`, `ACTIVO`, `COMPLETADO`). La `placa` del vehículo, y el `email` y la `licencia` del cliente, no se pueden repetir.

Los `id` de todos los recursos los genera la base de datos. El `precioTotal` de un alquiler se calcula solo (días × `tarifaDiaria`, donde días = `fechaFin` − `fechaInicio`) y la `fechaFin` debe ser posterior a la `fechaInicio`.

Flujo de estados:

- Un alquiler nuevo queda `PENDIENTE`. Solo se crea si el vehículo está `DISPONIBLE` y no tiene otro alquiler pendiente o activo en esas fechas.
- **Activar** un alquiler (`PENDIENTE → ACTIVO`) deja el vehículo `ALQUILADO`. **Completarlo** (`ACTIVO → COMPLETADO`) regresa el vehículo a `DISPONIBLE`.
- Registrar un **mantenimiento** deja el vehículo en `MANTENIMIENTO` (no se permite si está `ALQUILADO`). Para liberarlo se usa `finalizar-mantenimiento`.
- El estado `ALQUILADO` solo lo controlan los alquileres, no se cambia a mano.

Formato de fecha: `yyyy-MM-dd` (por ejemplo `2026-11-01`).

## Endpoints

Base URL: `http://localhost:8080`. Todos los cuerpos van en JSON (`Content-Type: application/json`).

### Oficinas — `/api/oficinas`

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/oficinas` | Listar oficinas | 200 |
| GET | `/api/oficinas/{id}` | Obtener una oficina | 200 / 404 |
| POST | `/api/oficinas` | Crear oficina | 201 / 400 |
| PUT | `/api/oficinas/{id}` | Actualizar oficina | 200 / 400 / 404 |
| DELETE | `/api/oficinas/{id}` | Eliminar oficina (sin vehículos) | 204 / 404 / 409 |

### Vehículos — `/api/vehiculos`

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/vehiculos` | Listar vehículos. Filtros opcionales: `?estado=DISPONIBLE`, `?oficinaId=1` | 200 |
| GET | `/api/vehiculos/{id}` | Obtener un vehículo | 200 / 404 |
| POST | `/api/vehiculos` | Crear vehículo | 201 / 400 / 404 / 409 |
| PUT | `/api/vehiculos/{id}` | Actualizar vehículo | 200 / 400 / 404 / 409 |
| PUT | `/api/vehiculos/{id}/finalizar-mantenimiento` | Regresar el vehículo a `DISPONIBLE` | 200 / 404 / 409 |
| DELETE | `/api/vehiculos/{id}` | Eliminar vehículo (sin historial) | 204 / 404 / 409 |

### Clientes — `/api/clientes`

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/clientes` | Listar clientes | 200 |
| GET | `/api/clientes/{id}` | Obtener un cliente | 200 / 404 |
| POST | `/api/clientes` | Crear cliente | 201 / 400 / 409 |
| PUT | `/api/clientes/{id}` | Actualizar cliente | 200 / 400 / 404 / 409 |
| DELETE | `/api/clientes/{id}` | Eliminar cliente (sin alquileres) | 204 / 404 / 409 |

### Alquileres — `/api/alquileres`

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/alquileres` | Listar alquileres. Filtro opcional: `?estado=ACTIVO` | 200 |
| GET | `/api/alquileres/{id}` | Obtener un alquiler | 200 / 404 |
| GET | `/api/alquileres/cliente/{clienteId}` | Alquileres de un cliente | 200 / 404 |
| POST | `/api/alquileres` | Crear alquiler `PENDIENTE` | 201 / 400 / 404 / 409 |
| PUT | `/api/alquileres/{id}/activar` | `PENDIENTE → ACTIVO` | 200 / 404 / 409 |
| PUT | `/api/alquileres/{id}/completar` | `ACTIVO → COMPLETADO` | 200 / 404 / 409 |
| DELETE | `/api/alquileres/{id}` | Eliminar un alquiler `PENDIENTE` | 204 / 404 / 409 |

### Mantenimientos — `/api/mantenimientos`

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/mantenimientos` | Listar mantenimientos | 200 |
| GET | `/api/mantenimientos/{id}` | Obtener un mantenimiento | 200 / 404 |
| GET | `/api/mantenimientos/vehiculo/{vehiculoId}` | Historial de un vehículo | 200 / 404 |
| POST | `/api/mantenimientos` | Registrar mantenimiento | 201 / 400 / 404 / 409 |
| DELETE | `/api/mantenimientos/{id}` | Eliminar mantenimiento | 204 / 404 |

## Manejo de errores

Los errores de la API devuelven un JSON con el código, el mensaje y la ruta:

```json
{
  "timestamp": "2026-10-14T21:35:00.456+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "No existe un vehículo con id 99",
  "path": "/api/vehiculos/99"
}
```

| Código | Cuándo |
|---|---|
| 400 Bad Request | Datos faltantes o inválidos, `estado` no permitido, fechas incorrectas, JSON con formato incorrecto |
| 404 Not Found | La oficina, vehículo, cliente, alquiler o mantenimiento buscado no existe |
| 409 Conflict | Vehículo no disponible o con otro alquiler en esas fechas, placa, email o licencia duplicados, el estado del alquiler no permite la acción, o se intenta eliminar un registro con datos relacionados |

## Pruebas con Postman

Todos los endpoints se probaron con Postman. Para repetir las pruebas:

1. Crea una variable `baseUrl` con el valor `http://localhost:8080` y usa `{{baseUrl}}/api/vehiculos` en las URL (o escribe la URL completa).
2. En los `POST` y `PUT` ve a **Body → raw → JSON** y pega el cuerpo de los ejemplos. Postman agrega solo el encabezado `Content-Type: application/json`.
3. Sigue este orden: crear una oficina, crear un vehículo, crear un cliente, crear el alquiler, activarlo, completarlo y registrar un mantenimiento.

## Ejemplos de peticiones y respuestas

Flujo completo de uso. Los IDs dependen de tu base de datos.

### 1. Crear una oficina

`POST /api/oficinas`

```json
{
  "nombreOficina": "Oficina Centro",
  "direccion": "Av. Independencia 100",
  "ciudad": "Veracruz",
  "region": "Veracruz"
}
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "nombreOficina": "Oficina Centro",
  "direccion": "Av. Independencia 100",
  "ciudad": "Veracruz",
  "region": "Veracruz"
}
```

### 2. Crear un vehículo

`POST /api/vehiculos`

```json
{
  "marca": "Volkswagen",
  "modelo": "Vento",
  "anio": 2024,
  "placa": "JKL-345-E",
  "tarifaDiaria": 700.00,
  "oficina": { "id": 1 }
}
```

Respuesta `201 Created` (el estado por defecto es `DISPONIBLE`):

```json
{
  "id": 1,
  "marca": "Volkswagen",
  "modelo": "Vento",
  "anio": 2024,
  "placa": "JKL-345-E",
  "tarifaDiaria": 700.00,
  "estado": "DISPONIBLE",
  "oficina": {
    "id": 1,
    "nombreOficina": "Oficina Centro",
    "direccion": "Av. Independencia 100",
    "ciudad": "Veracruz",
    "region": "Veracruz"
  }
}
```

### 3. Crear un cliente

`POST /api/clientes`

```json
{
  "nombre": "María",
  "apellido": "Hernández Cruz",
  "email": "maria.hernandez@correo.com",
  "licencia": "LIC-0003",
  "telefono": "2295551122"
}
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "nombre": "María",
  "apellido": "Hernández Cruz",
  "email": "maria.hernandez@correo.com",
  "licencia": "LIC-0003",
  "telefono": "2295551122"
}
```

### 4. Crear un alquiler

Solo hace falta indicar el `id` del vehículo, el `id` del cliente y las fechas. El precio se calcula solo (4 días × 700 = 2800).

`POST /api/alquileres`

```json
{
  "vehiculo": { "id": 1 },
  "cliente": { "id": 1 },
  "fechaInicio": "2026-11-01",
  "fechaFin": "2026-11-05"
}
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "vehiculo": { "id": 1, "marca": "Volkswagen", "modelo": "Vento", "estado": "DISPONIBLE", ... },
  "cliente": { "id": 1, "nombre": "María", "apellido": "Hernández Cruz", ... },
  "fechaInicio": "2026-11-01",
  "fechaFin": "2026-11-05",
  "precioTotal": 2800.00,
  "estado": "PENDIENTE"
}
```

### 5. Activar y completar un alquiler

`PUT /api/alquileres/1/activar` — sin cuerpo.

Respuesta `200 OK`: el alquiler con `"estado": "ACTIVO"`. El vehículo pasa a `ALQUILADO`.

`PUT /api/alquileres/1/completar` — sin cuerpo.

Respuesta `200 OK`: el alquiler con `"estado": "COMPLETADO"`. El vehículo vuelve a `DISPONIBLE`.

### 6. Registrar un mantenimiento

`POST /api/mantenimientos`

```json
{
  "vehiculo": { "id": 1 },
  "descripcion": "Cambio de llantas",
  "costo": 3200.00,
  "kilometraje": 15000
}
```

Respuesta `201 Created` (si no se manda fecha, se usa la de hoy; el vehículo queda en `MANTENIMIENTO`):

```json
{
  "id": 1,
  "vehiculo": { "id": 1, "marca": "Volkswagen", "modelo": "Vento", "estado": "MANTENIMIENTO", ... },
  "fechaMantenimiento": "2026-10-14",
  "descripcion": "Cambio de llantas",
  "costo": 3200.00,
  "kilometraje": 15000
}
```

Cuando termina la reparación: `PUT /api/vehiculos/1/finalizar-mantenimiento` regresa el vehículo a `DISPONIBLE`.

### 7. Consultas

- `GET /api/vehiculos?estado=DISPONIBLE&oficinaId=1` → vehículos disponibles de esa oficina.
- `GET /api/alquileres?estado=ACTIVO` → alquileres en curso.
- `GET /api/alquileres/cliente/1` → alquileres de ese cliente.
- `GET /api/mantenimientos/vehiculo/1` → historial de mantenimientos de ese vehículo (los más recientes primero).

### Ejemplos de error

`POST /api/vehiculos` sin placa → `400 Bad Request`:

```json
{
  "timestamp": "2026-10-14T21:40:00.123+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "El campo 'placa' es obligatorio",
  "path": "/api/vehiculos"
}
```

`POST /api/vehiculos` con una placa ya registrada → `409 Conflict`:

```json
{
  "timestamp": "2026-10-14T21:41:00.123+00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Ya existe un vehículo con la placa JKL-345-E",
  "path": "/api/vehiculos"
}
```

`POST /api/alquileres` con un vehículo que está en mantenimiento → `409 Conflict`:

```json
{
  "timestamp": "2026-10-14T21:42:00.123+00:00",
  "status": 409,
  "error": "Conflict",
  "message": "El vehículo no está disponible (estado actual: MANTENIMIENTO)",
  "path": "/api/alquileres"
}
```

`PUT /api/alquileres/1/completar` sobre un alquiler que no está activo → `409 Conflict`:

```json
{
  "timestamp": "2026-10-14T21:43:00.123+00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Solo se puede completar un alquiler ACTIVO",
  "path": "/api/alquileres/1/completar"
}
```

`DELETE /api/oficinas/1` cuando tiene vehículos → `409 Conflict`:

```json
{
  "timestamp": "2026-10-14T21:44:00.123+00:00",
  "status": 409,
  "error": "Conflict",
  "message": "No se puede eliminar: la oficina tiene vehículos asignados",
  "path": "/api/oficinas/1"
}
```

`GET /api/vehiculos/99` → `404 Not Found`:

```json
{
  "timestamp": "2026-10-14T21:45:00.123+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "No existe un vehículo con id 99",
  "path": "/api/vehiculos/99"
}
```