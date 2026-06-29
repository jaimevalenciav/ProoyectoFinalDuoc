package cl.truckmanager.reportes.repository;

import cl.truckmanager.reportes.entity.VehiculoKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehiculoKpiRepository extends JpaRepository<VehiculoKpi, String> {

    @Query("SELECT COUNT(v) FROM VehiculoKpi v WHERE v.empresaId = :emp")
    long countVehiculos(@Param("emp") String emp);

    @Query("SELECT COUNT(v) FROM VehiculoKpi v WHERE v.empresaId = :emp AND v.estado = 'OPERATIVO'")
    long countOperativos(@Param("emp") String emp);

    @Query("SELECT COUNT(v) FROM VehiculoKpi v WHERE v.empresaId = :emp AND v.estado = 'EN_TALLER'")
    long countEnTaller(@Param("emp") String emp);
}
