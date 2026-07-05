# TruckManager Pro — Documentación del Proyecto

## Objetivo

Sistema SaaS de gestión de flotas de transporte de carga para empresas chilenas. Permite administrar vehículos, conductores, servicios, combustible, taller, facturación y seguimiento GPS en tiempo real, con acceso desde web y app móvil.

---

## Arquitectura General

```
[Angular Web] ──► [BFF Web :8080] ──► [Microservicios] ──► [Oracle DB]
[App Android/iOS] ─► [BFF Móvil :8081] ─► [Oracle DB]
                                          [Azure Service Bus] ─► [Azure Functions]
```

**Tecnologías principales**
- Backend: Java 17 + Spring Boot 3 (microservicios)
- Frontend web: Angular 17 + Angular Material
- App móvil: Kotlin Multiplatform Mobile (Android + iOS)
- Base de datos: Oracle Autonomous Database (mTLS wallet)
- Autenticación: Azure AD B2C
- Nube: Azure Container Apps

---

## Partes del Proyecto

### Backend (`apps/backend/`)

| Componente | Puerto | Descripción |
|---|---|---|
| `bff-web` | 8080 | API Gateway para el frontend Angular. Proxy inverso + validación JWT B2C |
| `bff-mobile` | 8081 | API para la app móvil. Registra GPS directamente en Oracle |
| `ms-vehiculos` | 8082 | CRUD vehículos, documentos (permiso circulación, seguro SOAP, revisión técnica), GPS |
| `ms-conductores` | 8083 | CRUD conductores, mecánicos, usuarios del sistema, invitaciones |
| `ms-operaciones` | 8084 | Servicios de transporte, clientes, tipos de servicio, combustible, facturación |
| `ms-taller` | 8085 | Órdenes de trabajo, mantenimientos preventivos, stock de repuestos |
| `ms-almacen` | 8086 | Inventario de bodega |
| `ms-reportes` | 8087 | Exportación de reportes |

**Azure Functions** (`apps/backend/functions/`):
- `fn-gps-procesador`: procesa mensajes GPS desde Service Bus
- `fn-alertas`: disparadores de alertas programadas
- `fn-export`: exportación asíncrona de reportes

### Frontend Web (`apps/web/`)

Angular 17. Autenticación con MSAL Angular (Azure AD B2C). Comunica solo con `bff-web`.

### App Móvil (`apps/mobile/`)

Kotlin Multiplatform Mobile. Módulos:
- `androidApp/` — módulo Android
- `iosApp/` — módulo iOS (Swift/SwiftUI)
- `shared/` — lógica compartida (KMM)

### Base de Datos (`database/`)

- `create_schema.sql` — script unificado de referencia
- `oracle/` — scripts de producción separados por módulo

---

## Requisitos Previos

- Java 17
- Maven 3.9+
- Node.js 20+ / Angular CLI 17
- Docker + Docker Compose
- Oracle Wallet (`wallet/` en la raíz del proyecto)
- Cuenta Azure (AD B2C + Container Apps + Service Bus + Container Registry)

---

## Implementación Local (Desarrollo)

### 1. Variables de entorno

Copiar `.env.ejemplo` a `.env` y completar:

```env
ORACLE_TNS_NAME=BDJvalencia_high
ORACLE_WALLET_PATH=/ruta/al/wallet
ORACLE_USUARIO=ADMIN
ORACLE_CLAVE=tu_clave
ORACLE_ESQUEMA=ADMIN

AZURE_B2C_TENANT=trackmanager.onmicrosoft.com
AZURE_BUS_SERVICIO_CONEXION=Endpoint=sb://...
```

### 2. Base de datos

Ejecutar los scripts sobre Oracle Autonomous Database en este orden:

```sql
-- Desde SQL*Plus o SQL Developer conectado como ADMIN
@database/oracle/01_tablas_maestras.sql
@database/oracle/02_vehiculos.sql
@database/oracle/03_conductores.sql
@database/oracle/04_operaciones.sql
@database/oracle/05_taller.sql
@database/oracle/06_almacen.sql
@database/oracle/07_gps.sql
```

### 3. Levantar con Docker Compose

```bash
# Desde la raíz del proyecto
docker compose up --build
```

Servicios disponibles:
- Backend: `http://localhost:8080/api/v1`
- Frontend: ejecutar por separado (ver paso 4)

### 4. Frontend Angular

```bash
cd apps/web
npm install
ng serve
# Disponible en http://localhost:4200
```

### 5. Ejecutar tests

```bash
cd apps/backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test \
  -pl ms-vehiculos,ms-conductores,ms-operaciones
```

---

## Implementación en Azure

### Infraestructura requerida

| Recurso | Nombre de referencia | Uso |
|---|---|---|
| Azure Container Registry | `truckmanageracr` | Imágenes Docker |
| Azure Container Apps Environment | `truckmanager-env` | Ejecución de microservicios |
| Azure AD B2C | tenant `trackmanager` | Autenticación usuarios |
| Azure Service Bus | namespace `truckmanager-bus` | Cola GPS (`pistas-gps`) |
| Oracle Autonomous Database | `BDJvalencia` | Persistencia |

### Desplegar un microservicio

```bash
# 1. Construir y subir imagen
az acr build \
  --registry truckmanageracr \
  --image ms-vehiculos:latest \
  apps/backend/ms-vehiculos

# 2. Actualizar la revisión en Container Apps
az containerapp update \
  --name ms-vehiculos \
  --resource-group rg-truckmanager \
  --image truckmanageracr.azurecr.io/ms-vehiculos:latest \
  --revision-suffix "v$(date +%Y%m%d%H%M)"
```

Repetir para cada componente: `bff-web`, `bff-mobile`, `ms-vehiculos`, `ms-conductores`, `ms-operaciones`, etc.

### Variables de entorno en Container Apps

Configurar en cada Container App los secretos equivalentes a `.env`:
- `ORACLE_TNS_NAME`, `ORACLE_WALLET_PATH`, `ORACLE_USUARIO`, `ORACLE_CLAVE`, `ORACLE_ESQUEMA`
- `AZURE_B2C_TENANT`
- `AZURE_BUS_SERVICIO_CONEXION` (solo `bff-mobile`)

El Oracle Wallet debe montarse como volumen o estar disponible en la ruta configurada.

---

## Multitenancy y Seguridad

- Cada empresa tiene su propio `empresaId` extraído del JWT de Azure AD B2C.
- Todos los endpoints de escritura y consulta individual validan que el recurso pertenezca a la empresa del token (`findByIdAndEmpresaId`), devolviendo 404 si no corresponde (patrón IDOR-safe).

## Tests

Cobertura mínima del 70% sobre la capa de servicios, verificada con JaCoCo. Se excluyen del conteo: entidades, DTOs, repositorios, controladores (capas de binding sin lógica) y generación de PDF.

```
ms-vehiculos:    50 tests — BUILD SUCCESS
ms-conductores:  44 tests — BUILD SUCCESS
ms-operaciones:  67 tests — BUILD SUCCESS
```
