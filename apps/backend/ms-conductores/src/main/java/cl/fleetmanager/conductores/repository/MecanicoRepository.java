package cl.fleetmanager.conductores.repository;

import cl.fleetmanager.conductores.entity.Mecanico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MecanicoRepository extends JpaRepository<Mecanico, String> {

    @Query("""
        SELECT m FROM Mecanico m
        WHERE m.empresaId = :empresaId
          AND (:busqueda IS NULL OR LOWER(m.nombre) LIKE LOWER(CONCAT('%',:busqueda,'%'))
               OR LOWER(m.rut) LIKE LOWER(CONCAT('%',:busqueda,'%')))
          AND (:activo IS NULL OR m.activo = :activo)
        ORDER BY m.nombre
        """)
    Page<Mecanico> buscarPorFiltros(
        @Param("empresaId") String empresaId,
        @Param("busqueda")  String busqueda,
        @Param("activo")    Integer activo,
        Pageable pageable
    );

    List<Mecanico> findByEmpresaIdAndActivoOrderByNombre(String empresaId, int activo);
}
