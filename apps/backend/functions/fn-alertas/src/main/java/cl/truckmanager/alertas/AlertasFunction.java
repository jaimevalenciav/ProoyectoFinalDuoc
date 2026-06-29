package cl.truckmanager.alertas;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;

public class AlertasFunction {

    @FunctionName("VerificadorAlertas")
    public void ejecutar(
        @TimerTrigger(name = "temporizador", schedule = "0 */15 * * * *") String infoTemporizador,
        final ExecutionContext contexto
    ) {
        Logger registro = contexto.getLogger();
        registro.info("Verificando alertas — vencimientos de licencias y mantenimientos");

        try {
            verificarLicencias(registro);
            verificarMantenimientos(registro);
        } catch (Exception e) {
            registro.severe("Error en VerificadorAlertas: " + e.getMessage());
        }
    }

    private void verificarLicencias(Logger registro) throws Exception {
        String urlBd = System.getenv("ORACLE_JDBC_URL");
        String sql = """
            SELECT c.ID, c.NOMBRE, c.APELLIDO, c.EMPRESA_ID, c.LICENCIA_VENCIMIENTO
            FROM CONDUCTORES c
            WHERE c.LICENCIA_VENCIMIENTO BETWEEN SYSDATE AND SYSDATE + 30
            AND NOT EXISTS (
                SELECT 1 FROM ALERTAS a
                WHERE a.CONDUCTOR_ID = c.ID
                AND a.TIPO = 'LICENCIA_POR_VENCER'
                AND TRUNC(a.CREADO_EN) = TRUNC(SYSDATE)
            )
            """;
        try (Connection conexion = DriverManager.getConnection(urlBd,
                System.getenv("ORACLE_USUARIO"), System.getenv("ORACLE_CLAVE"));
             PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                crearAlerta(conexion,
                    rs.getString("EMPRESA_ID"),
                    rs.getString("ID"),
                    null,
                    "LICENCIA_POR_VENCER",
                    "ALTA",
                    "Licencia del conductor " + rs.getString("NOMBRE") + " " + rs.getString("APELLIDO") +
                    " vence el " + rs.getDate("LICENCIA_VENCIMIENTO"),
                    registro);
            }
        }
    }

    private void verificarMantenimientos(Logger registro) throws Exception {
        String urlBd = System.getenv("ORACLE_JDBC_URL");
        String sql = """
            SELECT v.ID, v.PATENTE, v.EMPRESA_ID, v.KM_ACTUALES
            FROM VEHICULOS v
            WHERE MOD(v.KM_ACTUALES, 10000) < 500
            AND NOT EXISTS (
                SELECT 1 FROM ALERTAS a
                WHERE a.VEHICULO_ID = v.ID
                AND a.TIPO = 'MANTENIMIENTO_PROXIMO'
                AND TRUNC(a.CREADO_EN) = TRUNC(SYSDATE)
            )
            """;
        try (Connection conexion = DriverManager.getConnection(urlBd,
                System.getenv("ORACLE_USUARIO"), System.getenv("ORACLE_CLAVE"));
             PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                crearAlerta(conexion,
                    rs.getString("EMPRESA_ID"),
                    null,
                    rs.getString("ID"),
                    "MANTENIMIENTO_PROXIMO",
                    "MEDIA",
                    "Vehículo " + rs.getString("PATENTE") + " próximo a mantenimiento (" + rs.getLong("KM_ACTUALES") + " km)",
                    registro);
            }
        }
    }

    private void crearAlerta(Connection conexion, String idEmpresa, String idConductor,
                              String idVehiculo, String tipo, String severidad,
                              String mensaje, Logger registro) throws Exception {
        String sql = "INSERT INTO ALERTAS (ID, ID_EMPRESA, ID_VEHICULO, ID_CONDUCTOR, TIPO, SEVERIDAD, MENSAJE, ESTADO) " +
                     "VALUES (?,?,?,?,?,?,?,'ACTIVA')";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, idEmpresa);
            if (idVehiculo  != null) ps.setString(3, idVehiculo);  else ps.setNull(3, Types.VARCHAR);
            if (idConductor != null) ps.setString(4, idConductor); else ps.setNull(4, Types.VARCHAR);
            ps.setString(5, tipo);
            ps.setString(6, severidad);
            ps.setString(7, mensaje);
            ps.executeUpdate();
            registro.info("Alerta creada: " + tipo + " — " + mensaje);
        }
    }
}
