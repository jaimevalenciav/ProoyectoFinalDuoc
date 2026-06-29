package cl.truckmanager.conductores.repository;

import cl.truckmanager.conductores.entity.UsuarioSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioSistema, String> {

    Optional<UsuarioSistema> findByAzureOid(String azureOid);

    Optional<UsuarioSistema> findByEmailIgnoreCase(String email);

    @Query("SELECT u FROM UsuarioSistema u WHERE u.empresaId = :emp ORDER BY u.nombre")
    List<UsuarioSistema> findAllByEmpresa(@Param("emp") String empresaId);
}
