#!/bin/bash
# ================================================================
# TruckManager Pro — Detener entorno de desarrollo local
# ================================================================

RAIZ="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$RAIZ/logs"

VERDE='\033[0;32m'
AMARILLO='\033[1;33m'
NC='\033[0m'

echo ""
echo "======================================================"
echo " TruckManager Pro — Deteniendo servicios"
echo "======================================================"
echo ""

SERVICIOS=(ms-vehiculos ms-conductores ms-taller ms-almacen ms-operaciones ms-reportes bff-web frontend)

for NOMBRE in "${SERVICIOS[@]}"; do
  PID_FILE="$LOG_DIR/${NOMBRE}.pid"
  if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
      kill "$PID" 2>/dev/null
      echo -e "  ${AMARILLO}✖${NC} $NOMBRE detenido (PID $PID)"
    else
      echo -e "  $NOMBRE ya estaba detenido"
    fi
    rm -f "$PID_FILE"
  fi
done

# Matar cualquier proceso spring-boot que quedara huérfano
pkill -f "spring-boot:run" 2>/dev/null || true
pkill -f "ng serve"        2>/dev/null || true

echo ""
echo -e "${VERDE}✔ Todos los servicios detenidos${NC}"
echo "======================================================"
