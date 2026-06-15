#!/bin/bash
# ================================================================
# TruckManager Pro — Túnel USB para pruebas de la APK Android
#
# Qué hace:
#   1. Verifica que el dispositivo esté conectado por USB
#   2. Abre el túnel adb reverse (teléfono:8080 → Mac:8080)
#   3. Verifica que el BFF-Web esté corriendo en Mac:8080
#   4. Muestra el estado de cada microservicio backend
#   5. Queda en modo watch: renueva el túnel si el cable
#      se desconecta/reconecta (cada 15 s)
# ================================================================

ADB=~/Library/Android/sdk/platform-tools/adb
VERDE='\033[0;32m'
AMARILLO='\033[1;33m'
ROJO='\033[0;31m'
CYAN='\033[0;36m'
NEGRITA='\033[1m'
NC='\033[0m'

ok()   { echo -e "${VERDE}✔  $1${NC}"; }
warn() { echo -e "${AMARILLO}⚠  $1${NC}"; }
err()  { echo -e "${ROJO}✖  $1${NC}"; }
info() { echo -e "${CYAN}→  $1${NC}"; }

# ── Verificar adb ──────────────────────────────────────────────────
if [ ! -f "$ADB" ]; then
  err "adb no encontrado en $ADB"
  err "Instala Android SDK Platform-Tools o ajusta la ruta ADB en este script."
  exit 1
fi

# ── Verificar servicios backend ────────────────────────────────────
verificar_servicios() {
  echo ""
  echo -e "${NEGRITA}Estado de microservicios backend:${NC}"
  local SERVICIOS=(
    "BFF-Web (gateway):8080"
    "ms-vehiculos:8082"
    "ms-conductores:8083"
    "ms-taller:8084"
    "ms-almacen:8085"
    "ms-operaciones:8086"
    "ms-reportes:8087"
  )
  local TODOS_OK=true
  for entrada in "${SERVICIOS[@]}"; do
    local NOMBRE="${entrada%%:*}"
    local PUERTO="${entrada##*:}"
    if curl -s --max-time 2 "http://localhost:$PUERTO/actuator/health" \
         | grep -q '"UP"' 2>/dev/null \
       || curl -s --max-time 2 "http://localhost:$PUERTO" -o /dev/null -w "%{http_code}" \
         | grep -qE "^[2-4]"; then
      ok "  $NOMBRE  (localhost:$PUERTO)"
    else
      warn "  $NOMBRE  (localhost:$PUERTO) — no responde"
      TODOS_OK=false
    fi
  done
  if [ "$TODOS_OK" = false ]; then
    echo ""
    warn "Algunos servicios no responden. ¿Ejecutaste ./dev-start.sh?"
  fi
  echo ""
}

# ── Abrir túnel ────────────────────────────────────────────────────
abrir_tunel() {
  local RESULTADO
  RESULTADO=$("$ADB" reverse tcp:8080 tcp:8080 2>&1)
  if echo "$RESULTADO" | grep -q "^8080$\|^error: no devices"; then
    return 0   # ok
  fi
  return 1
}

# ── Encabezado ─────────────────────────────────────────────────────
clear
echo ""
echo -e "${NEGRITA}================================================${NC}"
echo -e "${NEGRITA}  TruckManager — Túnel USB para APK Android  ${NC}"
echo -e "${NEGRITA}================================================${NC}"
echo ""
info "La app usa: http://localhost:8080/api/v1  (vía túnel USB)"
info "El túnel redirige: teléfono:8080 → esta Mac:8080"
echo ""

# ── Verificar servicios ────────────────────────────────────────────
verificar_servicios

# ── Loop principal ─────────────────────────────────────────────────
DISPOSITIVO_ANTERIOR=""

echo -e "${NEGRITA}Esperando dispositivo USB…${NC}"
echo -e "(Ctrl+C para salir)\n"

while true; do
  # Obtener dispositivo conectado
  DISPOSITIVO=$("$ADB" devices 2>/dev/null | grep -v "List of" | grep "device$" | awk '{print $1}' | head -1)

  if [ -z "$DISPOSITIVO" ]; then
    if [ -n "$DISPOSITIVO_ANTERIOR" ]; then
      echo ""
      warn "Dispositivo desconectado. Esperando reconexión…"
      DISPOSITIVO_ANTERIOR=""
    fi
    printf "."
    sleep 3
    continue
  fi

  # Dispositivo recién conectado o reconectado
  if [ "$DISPOSITIVO" != "$DISPOSITIVO_ANTERIOR" ]; then
    echo ""
    echo ""
    ok "Dispositivo conectado: $DISPOSITIVO"

    # Abrir túnel
    if "$ADB" -s "$DISPOSITIVO" reverse tcp:8080 tcp:8080 &>/dev/null; then
      ok "Túnel adb abierto: teléfono:8080 → Mac:8080"
    else
      err "No se pudo abrir el túnel. ¿Permitiste la depuración USB en el teléfono?"
    fi

    # Mostrar modelo del dispositivo
    MODELO=$("$ADB" -s "$DISPOSITIVO" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
    ANDROID=$("$ADB" -s "$DISPOSITIVO" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')
    info "Dispositivo: $MODELO (Android $ANDROID)"

    echo ""
    echo -e "${VERDE}${NEGRITA}✅ Listo. Abre TruckManager en el teléfono.${NC}"
    echo ""
    echo -e "  📱 App → http://localhost:8080/api/v1  (por USB)"
    echo -e "  🌐 BFF → http://localhost:8080"
    echo ""

    DISPOSITIVO_ANTERIOR="$DISPOSITIVO"
  fi

  # Renovar túnel cada 15 s para que no expire
  "$ADB" -s "$DISPOSITIVO" reverse tcp:8080 tcp:8080 &>/dev/null

  sleep 15
done
