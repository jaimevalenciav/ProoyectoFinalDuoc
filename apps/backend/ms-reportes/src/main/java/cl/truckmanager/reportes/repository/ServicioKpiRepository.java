package cl.truckmanager.reportes.repository;

import cl.truckmanager.reportes.entity.ServicioKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ServicioKpiRepository extends JpaRepository<ServicioKpi, String> {

    /**
     * Servicios para exportación Excel.
     * Devuelve Object[]: [0] numServicio, [1] vehiculoId, [2] origen, [3] destino,
     *   [4] kmsRecorrido, [5] fechaServicio, [6] estado, [7] valorNeto, [8] valorTotal
     */
    @Query(value = """
            SELECT NUM_SERVICIO, VEHICULO_ID, ORIGEN, DESTINO,
                   KMS_RECORRIDO, FECHA_SERVICIO, ESTADO, VALOR_NETO, VALOR_TOTAL
            FROM SERVICIOS
            WHERE EMPRESA_ID = :emp
              AND ELIMINADO = 0
              AND FECHA_SERVICIO BETWEEN :desde AND :hasta
            ORDER BY FECHA_SERVICIO DESC
            """, nativeQuery = true)
    List<Object[]> findServiciosParaExport(
            @Param("emp") String emp,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
