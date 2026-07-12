package cl.truckmanager.reportes.repository;

import cl.truckmanager.reportes.entity.OtKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface OtKpiRepository extends JpaRepository<OtKpi, String> {

    @Query("SELECT COUNT(o) FROM OtKpi o WHERE o.empresaId = :emp AND o.eliminado = 0")
    long countOts(@Param("emp") String emp);

    @Query("SELECT COUNT(o) FROM OtKpi o WHERE o.empresaId = :emp AND o.eliminado = 0 AND o.estado = 'PENDIENTE'")
    long countOtsPendientes(@Param("emp") String emp);

    @Query("SELECT COUNT(o) FROM OtKpi o WHERE o.empresaId = :emp AND o.eliminado = 0 AND o.estado = 'EN_EJECUCION'")
    long countOtsEnEjecucion(@Param("emp") String emp);

    @Query("SELECT COUNT(o) FROM OtKpi o WHERE o.empresaId = :emp AND o.eliminado = 0 AND o.estado = 'CERRADA'")
    long countOtsCerradas(@Param("emp") String emp);

    @Query("SELECT COUNT(o) FROM OtKpi o WHERE o.empresaId = :emp AND o.eliminado = 0 AND o.estado = 'CERRADA' AND o.fechaApertura BETWEEN :desde AND :hasta")
    long countOtsCerradasPeriodo(@Param("emp") String emp, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query(value = "SELECT COALESCE(SUM(COSTO_TOTAL), 0) FROM ORDENES_TRABAJO WHERE EMPRESA_ID = :emp AND ELIMINADO = 0 AND FECHA_APERTURA BETWEEN :desde AND :hasta", nativeQuery = true)
    double sumCostoMantenimientoPeriodo(@Param("emp") String emp, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
