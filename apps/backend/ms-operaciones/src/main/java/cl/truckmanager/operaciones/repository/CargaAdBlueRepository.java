package cl.truckmanager.operaciones.repository;

import cl.truckmanager.operaciones.entity.CargaAdBlue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CargaAdBlueRepository extends JpaRepository<CargaAdBlue, String> {

    @Query("SELECT c FROM CargaAdBlue c WHERE c.empresaId = :empresaId " +
           "AND (:vehiculoId IS NULL OR c.vehiculoId = :vehiculoId) " +
           "AND (:desde     IS NULL OR c.fechaCarga >= :desde) " +
           "AND (:hasta     IS NULL OR c.fechaCarga <= :hasta)")
    Page<CargaAdBlue> buscarPorFiltros(
        @Param("empresaId")  String empresaId,
        @Param("vehiculoId") String vehiculoId,
        @Param("desde")      LocalDate desde,
        @Param("hasta")      LocalDate hasta,
        Pageable pageable
    );

    /** Última carga del vehículo para calcular pctDiesel */
    java.util.Optional<CargaAdBlue> findTopByVehiculoIdOrderByFechaCargaDescKmVehiculoDesc(String vehiculoId);

    /** Cargas con porcentaje anómalo (>8% o <2% del diésel) */
    @Query("SELECT c FROM CargaAdBlue c WHERE c.empresaId = :empresaId " +
           "AND c.pctDiesel IS NOT NULL " +
           "AND (c.pctDiesel > 8 OR c.pctDiesel < 2)")
    List<CargaAdBlue> findAnomalias(@Param("empresaId") String empresaId);
}
