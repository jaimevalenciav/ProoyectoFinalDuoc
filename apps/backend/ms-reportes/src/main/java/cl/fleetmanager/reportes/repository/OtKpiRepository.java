package cl.fleetmanager.reportes.repository;

import cl.fleetmanager.reportes.entity.OtKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtKpiRepository extends JpaRepository<OtKpi, String> {

    @Query("SELECT COUNT(o) FROM OtKpi o WHERE o.empresaId = :emp AND o.eliminado = 0")
    long countOts(@Param("emp") String emp);

    @Query("SELECT COUNT(o) FROM OtKpi o WHERE o.empresaId = :emp AND o.eliminado = 0 AND o.estado = 'PENDIENTE'")
    long countOtsPendientes(@Param("emp") String emp);

    @Query("SELECT COUNT(o) FROM OtKpi o WHERE o.empresaId = :emp AND o.eliminado = 0 AND o.estado = 'EN_EJECUCION'")
    long countOtsEnEjecucion(@Param("emp") String emp);

    @Query("SELECT COUNT(o) FROM OtKpi o WHERE o.empresaId = :emp AND o.eliminado = 0 AND o.estado = 'CERRADA'")
    long countOtsCerradas(@Param("emp") String emp);
}
