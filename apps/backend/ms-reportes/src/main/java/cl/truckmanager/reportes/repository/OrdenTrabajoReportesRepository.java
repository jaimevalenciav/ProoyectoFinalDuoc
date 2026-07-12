package cl.truckmanager.reportes.repository;

import cl.truckmanager.reportes.entity.OtKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrdenTrabajoReportesRepository extends JpaRepository<OtKpi, String> {

    /**
     * Costos de mantenimiento agrupados por mes (YYYY-MM).
     * Devuelve Object[] con: [0] mes, [1] costoManoObra, [2] costoRepuestos, [3] total
     */
    @Query(value = """
            SELECT TO_CHAR(FECHA_APERTURA, 'YYYY-MM') AS mes,
                   SUM(COSTO_MANO_OBRA)               AS costo_mano_obra,
                   SUM(COSTO_REPUESTOS)                AS costo_repuestos,
                   SUM(COSTO_TOTAL)                    AS total
            FROM ORDENES_TRABAJO
            WHERE EMPRESA_ID = :emp
              AND ELIMINADO = 0
              AND FECHA_APERTURA BETWEEN :desde AND :hasta
            GROUP BY TO_CHAR(FECHA_APERTURA, 'YYYY-MM')
            ORDER BY mes ASC
            """, nativeQuery = true)
    List<Object[]> findCostosMantenimientoPorMes(
            @Param("emp") String emp,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /**
     * Órdenes de trabajo para exportación Excel.
     * Devuelve Object[]: [0] numero, [1] vehiculoId, [2] tipo, [3] estado,
     *   [4] fechaApertura, [5] fechaCierreReal, [6] costoManoObra, [7] costoRepuestos, [8] costoTotal
     */
    @Query(value = """
            SELECT NUMERO, VEHICULO_ID, TIPO, ESTADO,
                   FECHA_APERTURA, FECHA_CIERRE_REAL,
                   COSTO_MANO_OBRA, COSTO_REPUESTOS, COSTO_TOTAL
            FROM ORDENES_TRABAJO
            WHERE EMPRESA_ID = :emp
              AND ELIMINADO = 0
              AND FECHA_APERTURA BETWEEN :desde AND :hasta
            ORDER BY FECHA_APERTURA DESC
            """, nativeQuery = true)
    List<Object[]> findOTsParaExport(
            @Param("emp") String emp,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
