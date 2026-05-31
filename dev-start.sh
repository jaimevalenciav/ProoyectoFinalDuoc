#!/bin/bash
# ================================================================
# TruckManager Pro — Levantar entorno de desarrollo local
# ================================================================

set -e

RAIZ="$(cd "$(dirname "$0")" && pwd)"
BACKEND="$RAIZ/apps/backend"
FRONTEND="$RAIZ/apps/web"
ENV_FILE="$RAIZ/.env"
LOG_DIR="$RAIZ/logs"

# ── Colores ──────────────────────────────────────────────────────
VERDE='\033[0;32m'
AMARILLO='\033[1;33m'
ROJO='\033[0;31m'
NC='\033[0m'

ok()   { echo -e "${VERDE}✔ $1${NC}"; }
warn() { echo -e "${AMARILLO}⚠  $1${NC}"; }
err()  { echo -e "${ROJO}✖ $1${NC}"; exit 1; }

echo ""
echo "======================================================"
echo " TruckManager Pro — Iniciando entorno local"
echo "======================================================"
echo ""

# ── Forzar Java 17 para compilación backend ──────────────────────
# Lombok no es compatible con Java 21+ sin configuración extra;
# si existe Java 17 se usa preferentemente.
if [ -d "/usr/local/Cellar/openjdk@17" ]; then
  JAVA17=$(ls -d /usr/local/Cellar/openjdk@17/*/libexec/openjdk.jdk/Contents/Home 2>/dev/null | tail -1)
  [ -n "$JAVA17" ] && export JAVA_HOME="$JAVA17"
elif [ -d "/Library/Java/JavaVirtualMachines" ]; then
  JAVA17=$(/usr/libexec/java_home -v 17 2>/dev/null || true)
  [ -n "$JAVA17" ] && export JAVA_HOME="$JAVA17"
fi

if java -version 2>&1 | grep -q '"17'; then
  ok "Java 17 activo: $(java -version 2>&1 | head -1)"
else
  warn "No se encontró Java 17 — la compilación puede fallar con Lombok"
fi

# ── Verificaciones previas ───────────────────────────────────────
[ ! -f "$ENV_FILE" ]          && err "No se encontró .env en $RAIZ"
command -v mvn  &>/dev/null   || err "Maven no instalado (brew install maven)"
command -v node &>/dev/null   || err "Node.js no instalado (brew install node)"
command -v ng   &>/dev/null   || warn "Angular CLI no encontrado globalmente (se usará npx ng)"

ok "Dependencias verificadas"

# ── Cargar variables de entorno ──────────────────────────────────
set -a
source "$ENV_FILE"
set +a
ok "Variables de entorno cargadas"

# ── Crear directorio de logs ─────────────────────────────────────
mkdir -p "$LOG_DIR"

# ── Compilar backend (skip tests para arranque rápido) ───────────
echo ""
echo "▶ Compilando backend..."
cd "$BACKEND"
mvn clean package -DskipTests -q 2>&1 | tail -5
ok "Backend compilado"

# ── Función para arrancar un microservicio en background ─────────
arrancar_ms() {
  local NOMBRE=$1
  local LOG="$LOG_DIR/${NOMBRE}.log"
  echo -e "  Arrancando ${AMARILLO}${NOMBRE}${NC}..."
  cd "$BACKEND"
  mvn -pl "$NOMBRE" spring-boot:run > "$LOG" 2>&1 &
  echo $! > "$LOG_DIR/${NOMBRE}.pid"
}

# ── Levantar microservicios ──────────────────────────────────────
echo ""
echo "▶ Levantando microservicios..."
arrancar_ms ms-vehiculos
arrancar_ms ms-conductores
arrancar_ms ms-taller
arrancar_ms ms-almacen
arrancar_ms ms-operaciones
arrancar_ms ms-reportes

# ── Esperar a que los microservicios estén listos ────────────────
echo ""
echo "▶ Esperando que los microservicios levanten (30s)..."
sleep 30

# ── Levantar BFF Web ─────────────────────────────────────────────
echo "▶ Levantando BFF Web (gateway)..."
cd "$BACKEND"
mvn -pl bff-web spring-boot:run > "$LOG_DIR/bff-web.log" 2>&1 &
echo $! > "$LOG_DIR/bff-web.pid"
sleep 10

# ── Levantar frontend Angular ────────────────────────────────────
echo "▶ Levantando frontend Angular..."
cd "$FRONTEND"
(command -v ng &>/dev/null && ng serve || npx ng serve) > "$LOG_DIR/frontend.log" 2>&1 &
echo $! > "$LOG_DIR/frontend.pid"

# ── Resumen ──────────────────────────────────────────────────────
echo ""
echo "======================================================"
ok "Todos los servicios en marcha"
echo "======================================================"
echo ""
echo "  Frontend       → http://localhost:4200"
echo "  BFF / Gateway  → http://localhost:8080"
echo "  ms-vehiculos   → http://localhost:8082"
echo "  ms-conductores → http://localhost:8083"
echo "  ms-taller      → http://localhost:8084"
echo "  ms-almacen     → http://localhost:8085"
echo "  ms-operaciones → http://localhost:8086"
echo "  ms-reportes    → http://localhost:8087"
echo ""
echo "  Logs en: $LOG_DIR/"
echo ""
echo "  Para detener todo:  ./dev-stop.sh"
echo "======================================================"
