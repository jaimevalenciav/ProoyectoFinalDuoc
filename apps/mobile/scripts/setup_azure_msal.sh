#!/usr/bin/env bash
# =============================================================================
# setup_azure_msal.sh
# Configura los valores reales de Azure AD B2C en msal_config.json
# Uso: ./scripts/setup_azure_msal.sh
# =============================================================================
set -euo pipefail

# ── Colores ──────────────────────────────────────────────────────────────────
R='\033[0;31m'; G='\033[0;32m'; Y='\033[1;33m'; B='\033[0;34m'; N='\033[0m'

MSAL_CONFIG="androidApp/src/androidMain/res/raw/msal_config.json"
KEYSTORE="$HOME/.android/debug.keystore"

echo ""
echo -e "${B}══════════════════════════════════════════════════════${N}"
echo -e "${B}   TruckManager — Configuración Azure AD B2C / MSAL  ${N}"
echo -e "${B}══════════════════════════════════════════════════════${N}"
echo ""

# ── 1. Calcular hash del certificado de firma ─────────────────────────────────
echo -e "${Y}[1/4] Calculando hash del certificado de firma (debug keystore)...${N}"

if [ ! -f "$KEYSTORE" ]; then
  echo -e "${R}ERROR: No se encontró el debug keystore en $KEYSTORE${N}"
  echo "  Genera uno con: keytool -genkey -v -keystore ~/.android/debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android"
  exit 1
fi

HASH_B64=$(keytool -exportcert -alias androiddebugkey \
  -keystore "$KEYSTORE" \
  -storepass android 2>/dev/null \
  | openssl sha1 -binary \
  | openssl base64)

# URL-encode solo el + (no el / ni el =, MSAL los acepta tal cual en el path)
HASH_ENCODED=$(python3 -c "import urllib.parse; print(urllib.parse.quote('$HASH_B64', safe='=/'))" 2>/dev/null || echo "$HASH_B64")

echo -e "   Hash Base64 (debug):  ${G}${HASH_B64}${N}"
echo ""

# ── 2. Leer valores de Azure del usuario ─────────────────────────────────────
echo -e "${Y}[2/4] Valores de Azure AD B2C${N}"
echo ""
echo -e "  Para obtenerlos, ve a: ${B}https://portal.azure.com${N}"
echo -e "  → Azure AD B2C → Registros de aplicaciones → TruckManager"
echo ""

# Detectar si ya están configurados
CURRENT=$(cat "$MSAL_CONFIG" 2>/dev/null || echo "")
if echo "$CURRENT" | grep -q "AZURE_CLIENT_ID"; then
  echo -e "  ${R}⚠  El archivo aún tiene placeholders. Introduce los valores reales:${N}"
else
  echo -e "  ${G}✓  El archivo ya tiene valores configurados.${N}"
  echo "  ¿Quieres sobrescribirlos? (s/N)"
  read -r CONFIRM
  [[ "$CONFIRM" != "s" && "$CONFIRM" != "S" ]] && echo "Cancelado." && exit 0
fi
echo ""

read -rp "  CLIENT_ID   (Application/Client ID del registro en Azure): " CLIENT_ID
read -rp "  TENANT      (nombre del tenant, ej: miempresa): " TENANT
read -rp "  POLICY      (nombre del flujo B2C, ej: B2C_1_signin) [B2C_1_signin]: " POLICY
POLICY="${POLICY:-B2C_1_signin}"

echo ""

# ── 3. Escribir msal_config.json ──────────────────────────────────────────────
echo -e "${Y}[3/4] Escribiendo $MSAL_CONFIG...${N}"

REDIRECT_URI="msauth://cl.truckmanager.android/${HASH_B64}"
AUTHORITY_URL="https://${TENANT}.b2clogin.com/${TENANT}.onmicrosoft.com/${POLICY}"
SCOPE="https://${TENANT}.onmicrosoft.com/api/read"

cat > "$MSAL_CONFIG" << EOF
{
  "client_id": "${CLIENT_ID}",
  "authorization_user_agent": "DEFAULT",
  "redirect_uri": "${REDIRECT_URI}",
  "account_mode": "SINGLE",
  "broker_redirect_uri_registered": false,
  "authorities": [
    {
      "type": "B2C",
      "authority_url": "${AUTHORITY_URL}",
      "b2c_policy": "${POLICY}",
      "is_default": true
    }
  ]
}
EOF

echo -e "   ${G}✓ msal_config.json actualizado${N}"
echo ""

# ── 4. Actualizar scopes en MsalAuthManager.kt ───────────────────────────────
echo -e "${Y}[4/4] Actualizando scopes en MsalAuthManager.kt...${N}"

AUTH_KT="androidApp/src/androidMain/kotlin/cl/truckmanager/android/auth/MsalAuthManager.kt"
if [ -f "$AUTH_KT" ]; then
  sed -i '' "s|https://TENANT\.onmicrosoft\.com/api/read|${SCOPE}|g" "$AUTH_KT"
  sed -i '' "s|https://TENANT\.b2clogin\.com/TENANT\.onmicrosoft\.com/B2C_1_signin|${AUTHORITY_URL}|g" "$AUTH_KT"
  echo -e "   ${G}✓ MsalAuthManager.kt actualizado${N}"
else
  echo -e "   ${R}No se encontró $AUTH_KT${N}"
fi

# ── Resumen ───────────────────────────────────────────────────────────────────
echo ""
echo -e "${B}══════════════════════════════════════════════════════${N}"
echo -e "${G}✓ Configuración completada${N}"
echo -e "${B}══════════════════════════════════════════════════════${N}"
echo ""
echo -e "  ${Y}Redirect URI a registrar en Azure Portal:${N}"
echo -e "  ${G}${REDIRECT_URI}${N}"
echo ""
echo -e "  ${Y}Pasos finales en Azure Portal:${N}"
echo "  1. Ve a: Registro de aplicación → Autenticación"
echo "  2. Agrega plataforma 'Android'"
echo "  3. Package name:  cl.truckmanager.android"
echo "  4. Signature hash: ${HASH_B64}"
echo "  5. Guarda y espera ~2 minutos para que propague"
echo ""
echo -e "  ${Y}Para reconstruir e instalar:${N}"
echo "  export JAVA_HOME=\$(/usr/libexec/java_home -v 17)"
echo "  ./gradlew :androidApp:assembleDebug && \\"
echo "  \$HOME/Library/Android/sdk/platform-tools/adb install -r \\"
echo "    androidApp/build/outputs/apk/debug/androidApp-debug.apk"
echo ""
