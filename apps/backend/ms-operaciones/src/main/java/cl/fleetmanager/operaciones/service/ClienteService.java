package cl.fleetmanager.operaciones.service;

import cl.fleetmanager.operaciones.dto.ClienteDto;
import cl.fleetmanager.operaciones.entity.Cliente;
import cl.fleetmanager.operaciones.repository.ClienteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClienteService {

    private final ClienteRepository repositorio;

    public Page<Cliente> getAll(String empresaId, String search, int pagina, int tamano) {
        return repositorio.buscarPorFiltros(
            empresaId, 1,
            (search != null && !search.isBlank()) ? search : null,
            PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.ASC, "razonSocial"))
        );
    }

    public List<Cliente> getAllActivos(String empresaId) {
        return repositorio.findByEmpresaIdAndActivo(empresaId, 1);
    }

    public Cliente getById(String id) {
        return repositorio.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado: " + id));
    }

    public Cliente crear(String empresaId, ClienteDto datos) {
        Cliente cliente = Cliente.builder()
            .id(UUID.randomUUID().toString())
            .empresaId(empresaId)
            .rut(datos.getRut())
            .razonSocial(datos.getRazonSocial())
            .giro(datos.getGiro())
            .ciudad(datos.getCiudad())
            .telefono(datos.getTelefono())
            .email(datos.getEmail())
            .repLegalNombre(datos.getRepLegalNombre())
            .repLegalRut(datos.getRepLegalRut())
            .activo(datos.getActivo() != null ? datos.getActivo() : 1)
            .build();
        return repositorio.save(cliente);
    }

    public Cliente actualizar(String id, ClienteDto datos) {
        Cliente c = getById(id);
        if (datos.getRut()           != null) c.setRut(datos.getRut());
        if (datos.getRazonSocial()   != null) c.setRazonSocial(datos.getRazonSocial());
        if (datos.getGiro()          != null) c.setGiro(datos.getGiro());
        if (datos.getCiudad()        != null) c.setCiudad(datos.getCiudad());
        if (datos.getTelefono()      != null) c.setTelefono(datos.getTelefono());
        if (datos.getEmail()         != null) c.setEmail(datos.getEmail());
        if (datos.getRepLegalNombre() != null) c.setRepLegalNombre(datos.getRepLegalNombre());
        if (datos.getRepLegalRut()   != null) c.setRepLegalRut(datos.getRepLegalRut());
        if (datos.getActivo()        != null) c.setActivo(datos.getActivo());
        return repositorio.save(c);
    }
}
