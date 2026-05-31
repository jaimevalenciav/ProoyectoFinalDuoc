package cl.fleetmanager.export;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class ExportFunction {

    @FunctionName("ExportReporte")
    public HttpResponseMessage run(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.GET},
            route = "export",
            authLevel = AuthorizationLevel.FUNCTION
        ) HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context
    ) {
        Logger log = context.getLogger();
        String tipo  = request.getQueryParameters().getOrDefault("tipo",  "combustible");
        String desde = request.getQueryParameters().getOrDefault("desde", "");
        String hasta = request.getQueryParameters().getOrDefault("hasta", "");

        try {
            byte[] csvBytes = generateCsv(tipo, desde, hasta, log);
            return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"reporte-" + tipo + ".csv\"")
                .body(csvBytes)
                .build();
        } catch (Exception e) {
            log.severe("Error generando reporte: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al generar el reporte")
                .build();
        }
    }

    private byte[] generateCsv(String tipo, String desde, String hasta, Logger log) throws Exception {
        String url  = System.getenv("ORACLE_JDBC_URL");
        String user = System.getenv("ORACLE_USER");
        String pass = System.getenv("ORACLE_PASSWORD");

        String sql = switch (tipo) {
            case "mantenimiento" -> """
                SELECT ot.NUMERO_OT, v.PATENTE, ot.TIPO, ot.ESTADO,
                       ot.FECHA_APERTURA, ot.COSTO_MANO_OBRA, ot.COSTO_REPUESTOS, ot.COSTO_TOTAL
                FROM ORDENES_TRABAJO ot JOIN VEHICULOS v ON ot.VEHICULO_ID = v.ID
                WHERE ot.FECHA_APERTURA BETWEEN TO_DATE(?,''YYYY-MM-DD'') AND TO_DATE(?,''YYYY-MM-DD'')
                ORDER BY ot.FECHA_APERTURA
                """;
            case "servicios" -> """
                SELECT s.NUMERO_SERVICIO, cl.RAZON_SOCIAL, s.ORIGEN, s.DESTINO,
                       s.FECHA_INICIO, s.FECHA_TERMINO, s.ESTADO, s.TARIFA_ACORDADA
                FROM SERVICIOS s JOIN CLIENTES cl ON s.CLIENTE_ID = cl.ID
                WHERE s.FECHA_INICIO BETWEEN TO_DATE(?,''YYYY-MM-DD'') AND TO_DATE(?,''YYYY-MM-DD'')
                ORDER BY s.FECHA_INICIO
                """;
            default -> """
                SELECT v.PATENTE, cc.FECHA_CARGA, cc.LITROS, cc.PRECIO_POR_LITRO,
                       cc.COSTO_TOTAL, cc.KM_AL_CARGAR, cc.RENDIMIENTO_KM_L
                FROM CARGAS_COMBUSTIBLE cc JOIN VEHICULOS v ON cc.VEHICULO_ID = v.ID
                WHERE cc.FECHA_CARGA BETWEEN TO_DATE(?,''YYYY-MM-DD'') AND TO_DATE(?,''YYYY-MM-DD'')
                ORDER BY cc.FECHA_CARGA
                """;
        };

        StringBuilder csv = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!desde.isBlank()) ps.setString(1, desde);
            if (!hasta.isBlank()) ps.setString(2, hasta);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                if (i > 1) csv.append(',');
                csv.append(meta.getColumnLabel(i));
            }
            csv.append('\n');
            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) csv.append(',');
                    Object val = rs.getObject(i);
                    csv.append(val != null ? val.toString() : "");
                }
                csv.append('\n');
            }
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
