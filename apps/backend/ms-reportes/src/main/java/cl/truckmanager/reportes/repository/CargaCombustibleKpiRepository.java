package cl.truckmanager.reportes.repository;

import cl.truckmanager.reportes.entity.CargaCombustibleKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CargaCombustibleKpiRepository extends JpaRepository<CargaCombustibleKpi, String> {

    /**
     * Agrupación por vehículo con JOIN a VEHICULOS para obtener la patente.
     * Devuelve Object[] con:
     *   [0] vehiculoId, [1] patente, [2] litrosTotales, [3] kmTotales,
     *   [4] rendimientoPromedio (km/lt), [5] costoTotal
     */
    @Query(value = """
            SELECT c.VEHICULO_ID,
                   v.PATENTE,
                   SUM(c.LITROS)        AS litros_totales,
                   (MAX(c.KM_VEHICULO) - MIN(c.KM_VEHICULO)) AS km_totales,
                   CASE WHEN SUM(c.LITROS) > 0
                        THEN ROUND((MAX(c.KM_VEHICULO) - MIN(c.KM_VEHICULO)) / SUM(c.LITROS), 2)
                        ELSE 0 END      AS rendimiento,
                   SUM(c.COSTO_TOTAL)  AS costo_total
            FROM CARGAS_COMBUSTIBLE c
            JOIN VEHICULOS v ON v.ID = c.VEHICULO_ID
            WHERE c.EMPRESA_ID = :emp
              AND c.FECHA_CARGA BETWEEN :desde AND :hasta
            GROUP BY c.VEHICULO_ID, v.PATENTE
            ORDER BY costo_total DESC
            """, nativeQuery = true)
    List<Object[]> findConsumoAgrupado(
            @Param("emp") String emp,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /**
     * Lista completa de cargas para exportación Excel.
     */
    @Query(value = """
            SELECT c.ID, c.VEHICULO_ID, v.PATENTE, c.FECHA_CARGA, c.LITROS,
                   c.PRECIO_LITRO, c.COSTO_TOTAL, c.KM_VEHICULO, c.TIPO_COMBUSTIBLE,
                   c.PROVEEDOR
            FROM CARGAS_COMBUSTIBLE c
            JOIN VEHICULOS v ON v.ID = c.VEHICULO_ID
            WHERE c.EMPRESA_ID = :emp
              AND c.FECHA_CARGA BETWEEN :desde AND :hasta
            ORDER BY c.FECHA_CARGA DESC
            """, nativeQuery = true)
    List<Object[]> findCargasParaExport(
            @Param("emp") String emp,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
