#!/bin/bash
# ================================================================
# TruckManager Pro — Aprovisionamiento Azure
# Automatiza: grupo de recursos + Service Bus
# Manual guiado: Azure AD B2C (requiere portal la primera vez)
# ================================================================

set -e

# ── Variables ────────────────────────────────────────────────────
PREFIJO="truckmanagercl"
GRUPO_RECURSOS="rg-${PREFIJO}"
UBICACION="eastus"
B2C_NOMBRE="${PREFIJO}"
B2C_DOMINIO="${PREFIJO}.onmicrosoft.com"
BUS_NAMESPACE="sb-${PREFIJO}"
COLA_GPS="pistas-gps"
APP_NAME="TruckManager Pro"

echo "======================================================"
echo " TruckManager Pro — Aprovisionamiento Azure"
echo "======================================================"

# ── 1. Grupo de recursos ─────────────────────────────────────────
echo ""
echo "▶ [1/4] Creando grupo de recursos..."
az group create \
  --name "$GRUPO_RECURSOS" \
  --location "$UBICACION" \
  --tags proyecto=truckmanager ambiente=produccion \
  --output none

echo "✔ Grupo de recursos: $GRUPO_RECURSOS"

# ── 2. Azure AD B2C tenant ───────────────────────────────────────
echo ""
echo "▶ [2/4] Creando tenant Azure AD B2C..."

SUBSCRIPTION_ID=$(az account show --query id --output tsv)

az rest \
  --method PUT \
  --url "https://management.azure.com/subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${GRUPO_RECURSOS}/providers/Microsoft.AzureActiveDirectory/b2cDirectories/${B2C_NOMBRE}?api-version=2021-04-01" \
  --body "{
    \"location\": \"United States\",
    \"sku\": {
      \"name\": \"PremiumP1\",
      \"tier\": \"A0\"
    },
    \"properties\": {
      \"createTenantProperties\": {
        \"displayName\": \"TruckManager\",
        \"countryCode\": \"CL\"
      }
    }
  }" --output none

echo "✔ Tenant B2C iniciado: $B2C_DOMINIO"
echo ""
echo "  ════════════════════════════════════════════════════════"
echo "  ACCIÓN MANUAL REQUERIDA — pasos 3 y 4:"
echo "  ════════════════════════════════════════════════════════"
echo ""
echo "  1. Abre https://portal.azure.com"
echo "  2. En el buscador superior escribe 'Azure AD B2C' y ábrelo"
echo "  3. Cambia al directorio B2C:"
echo "     → Ícono de persona (arriba derecha) → 'Cambiar directorio'"
echo "     → Selecciona '$B2C_DOMINIO' → Cambiar"
echo "  4. Registra la aplicación:"
echo "     → Menú izquierdo: 'Registros de aplicaciones'"
echo "     → 'Nuevo registro'"
echo "       Nombre: $APP_NAME"
echo "       Tipos de cuenta: Cuentas en cualquier proveedor de identidad"
echo "       URI de redireccionamiento (SPA): http://localhost:4200"
echo "     → Registrar"
echo "     → Agrega también esta URI: https://truckmanagercl.azurecontainerapps.io"
echo "  5. Copia el 'Id. de aplicación (cliente)' → será AZURE_CLIENT_ID"
echo "  6. Crea el secreto:"
echo "     → 'Certificados y secretos' → 'Nuevo secreto de cliente'"
echo "       Descripción: backend-prod  |  Expiración: 24 meses"
echo "     → Agregar → copia el Valor → será AZURE_CLIENT_SECRET"
echo "  7. Crea el flujo de usuario:"
echo "     → Menú izquierdo: 'Flujos de usuario'"
echo "     → 'Nuevo flujo de usuario'"
echo "       Tipo: 'Registrarse e iniciar sesión' (Recomendado)"
echo "     → Crear"
echo "       Nombre: susi  (quedará guardado como B2C_1_susi)"
echo "       Proveedores de identidad: 'Registro por correo electrónico'"
echo "     → Crear"
echo "  ════════════════════════════════════════════════════════"
echo ""
read -p "  Cuando termines TODOS los pasos del portal, escribe los valores:"
echo ""
read -p "  AZURE_CLIENT_ID (Application ID): " APP_ID
read -p "  AZURE_CLIENT_SECRET (Secret Value): " CLIENT_SECRET

# ── 3. Azure Service Bus ─────────────────────────────────────────
echo ""
echo "▶ [3/4] Creando Service Bus namespace..."
az servicebus namespace create \
  --resource-group "$GRUPO_RECURSOS" \
  --name "$BUS_NAMESPACE" \
  --location "$UBICACION" \
  --sku "Standard" \
  --tags proyecto=truckmanager \
  --output none

echo "▶ [3/4] Creando cola GPS..."
az servicebus queue create \
  --resource-group "$GRUPO_RECURSOS" \
  --namespace-name "$BUS_NAMESPACE" \
  --name "$COLA_GPS" \
  --max-size 1024 \
  --default-message-time-to-live "P1D" \
  --lock-duration "PT30S" \
  --output none

BUS_CONEXION=$(az servicebus namespace authorization-rule keys list \
  --resource-group "$GRUPO_RECURSOS" \
  --namespace-name "$BUS_NAMESPACE" \
  --name "RootManageSharedAccessKey" \
  --query "primaryConnectionString" \
  --output tsv)

echo "✔ Service Bus: $BUS_NAMESPACE / cola: $COLA_GPS"

# ── 4. Resumen .env ──────────────────────────────────────────────
echo ""
echo "▶ [4/4] Generando archivo .env..."
echo ""
echo "================================================================"
echo " ✅  Aprovisionamiento completado"
echo " Copia el siguiente bloque en tu archivo .env"
echo "================================================================"
cat <<ENV

# ── Azure B2C ─────────────────────────────────────────────────────
AZURE_B2C_TENANT=${PREFIJO}
AZURE_CLIENT_ID=${APP_ID}
AZURE_CLIENT_SECRET=${CLIENT_SECRET}
AZURE_B2C_DOMINIO=${B2C_DOMINIO}
AZURE_B2C_FLUJO=B2C_1_susi

# ── Azure Service Bus ─────────────────────────────────────────────
AZURE_BUS_SERVICIO_CONEXION=${BUS_CONEXION}

# ── Oracle Autonomous Database ────────────────────────────────────
# (valores del wallet descargado desde OCI Console)
ORACLE_TNS_NAME=truckmanager_high
ORACLE_WALLET_PATH=/ruta/a/tu/wallet
ORACLE_USUARIO=ADMIN
ORACLE_CLAVE=TU_CLAVE_ORACLE
ORACLE_ESQUEMA=ADMIN

# ── Hosts microservicios (localhost en dev) ───────────────────────
MS_VEHICULOS_HOST=localhost
MS_CONDUCTORES_HOST=localhost
MS_TALLER_HOST=localhost
MS_ALMACEN_HOST=localhost
MS_OPERACIONES_HOST=localhost
MS_REPORTES_HOST=localhost

# ── URL del frontend ──────────────────────────────────────────────
URL_FRONTEND=https://truckmanagercl.azurecontainerapps.io

ENV
echo "================================================================"
