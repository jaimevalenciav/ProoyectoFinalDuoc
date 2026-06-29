package cl.truckmanager.operaciones.repository;

import cl.truckmanager.operaciones.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, String> {

    @Query("SELECT c FROM Cliente c WHERE c.empresaId = :empresaId AND c.activo = :activo " +
           "AND (:search IS NULL OR UPPER(c.razonSocial) LIKE UPPER(CONCAT('%', CONCAT(:search, '%'))) " +
           "     OR UPPER(c.rut) LIKE UPPER(CONCAT('%', CONCAT(:search, '%'))))")
    Page<Cliente> buscarPorFiltros(
        @Param("empresaId") String empresaId,
        @Param("activo")    Integer activo,
        @Param("search")    String search,
        Pageable pageable
    );

    List<Cliente> findByEmpresaIdAndActivo(String empresaId, Integer activo);

    List<Cliente> findByEmpresaId(String empresaId);
}
