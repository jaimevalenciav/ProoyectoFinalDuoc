package cl.truckmanager.reportes.repository;

import cl.truckmanager.reportes.entity.RepuestoKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepuestoKpiRepository extends JpaRepository<RepuestoKpi, String> {

    @Query("SELECT COUNT(r) FROM RepuestoKpi r WHERE r.empresaId = :emp AND r.eliminado = 0 AND r.stockActual <= r.stockMinimo")
    long countBajoStock(@Param("emp") String emp);
}
