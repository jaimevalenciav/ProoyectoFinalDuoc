package cl.truckmanager.conductores.repository;

import cl.truckmanager.conductores.entity.InvitacionPendiente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvitacionRepository extends JpaRepository<InvitacionPendiente, String> {

    @Query("SELECT i FROM InvitacionPendiente i WHERE i.empresaId = :emp ORDER BY i.createdAt DESC")
    List<InvitacionPendiente> findByEmpresa(@Param("emp") String empresaId);

    Optional<InvitacionPendiente> findByTokenAndUsadaFalse(String token);
}
