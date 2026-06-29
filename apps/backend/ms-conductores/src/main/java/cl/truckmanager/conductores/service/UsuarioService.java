package cl.truckmanager.conductores.service;

import cl.truckmanager.conductores.dto.PerfilDto;
import cl.truckmanager.conductores.dto.UsuarioDto;
import cl.truckmanager.conductores.entity.UsuarioSistema;
import cl.truckmanager.conductores.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repo;

    // ── Perfil del usuario autenticado ─────────────────────────

    /**
     * Devuelve el perfil del usuario autenticado.
     * Si no existe en la BD, lo crea automáticamente con rol ADMIN
     * (solo el primer usuario; en producción se puede cambiar este comportamiento).
     */
    /**
     * Devuelve el perfil del usuario autenticado.
     *
     * Casos:
     *  1. Usuario ya registrado en BD → actualiza nombre/email si cambiaron.
     *  2. Usuario nuevo con empresaId en JWT (ej: admin de empresa nueva) → auto-registro.
     *  3. Usuario nuevo SIN empresaId → devuelve PerfilDto con empresaId=null.
     *     El frontend lo redirige al onboarding para ingresar un código de invitación.
     */
    public PerfilDto perfilActual(String empresaId, Jwt jwt) {
        String azureOid = jwt.getSubject();
        String email    = resolverEmail(jwt);
        String nombreRaw = jwt.getClaimAsString("name");
        final String nombre = (nombreRaw != null) ? nombreRaw : email;

        // ── Caso 1: ya existe en BD ──────────────────────────────
        java.util.Optional<UsuarioSistema> existente = repo.findByAzureOid(azureOid);
        if (existente.isPresent()) {
            UsuarioSistema u = existente.get();
            boolean dirty = false;
            if (nombre != null && !nombre.equals(u.getNombre())) { u.setNombre(nombre); dirty = true; }
            if (email  != null && !email.equals(u.getEmail()))   { u.setEmail(email);   dirty = true; }
            if (dirty) repo.save(u);
            return toDto(u);
        }

        // ── Caso 2: usuario nuevo con empresaId en JWT ───────────
        if (empresaId != null && !empresaId.isBlank()) {
            UsuarioSistema nuevo = UsuarioSistema.builder()
                    .empresaId(empresaId)
                    .azureOid(azureOid)
                    .nombre(nombre)
                    .email(email)
                    .rol(primerUsuario(empresaId) ? "ADMIN" : "OPERADOR")
                    .build();
            return toDto(repo.save(nuevo));
        }

        // ── Caso 3: usuario nuevo sin empresa → pendiente onboarding ──
        return new PerfilDto(null, nombre, email, null, null, 0);
    }

    // ── CRUD de usuarios (solo ADMIN) ───────────────────────────

    public List<UsuarioSistema> listar(String empresaId) {
        return repo.findAllByEmpresa(empresaId);
    }

    public UsuarioSistema crear(String empresaId, UsuarioDto dto) {
        UsuarioSistema u = UsuarioSistema.builder()
                .empresaId(empresaId)
                .nombre(dto.getNombre())
                .email(dto.getEmail())
                .rol(dto.getRol())
                .azureOid(dto.getAzureOid())
                .activo(dto.getActivo() != null ? dto.getActivo() : 1)
                .build();
        return repo.save(u);
    }

    public UsuarioSistema actualizar(String id, UsuarioDto dto) {
        UsuarioSistema u = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));
        u.setNombre(dto.getNombre());
        u.setEmail(dto.getEmail());
        u.setRol(dto.getRol());
        if (dto.getAzureOid() != null) u.setAzureOid(dto.getAzureOid());
        if (dto.getActivo()   != null) u.setActivo(dto.getActivo());
        return repo.save(u);
    }

    public void cambiarRol(String id, String nuevoRol) {
        UsuarioSistema u = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));
        u.setRol(nuevoRol);
        repo.save(u);
    }

    public void cambiarActivo(String id, boolean activo) {
        UsuarioSistema u = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));
        u.setActivo(activo ? 1 : 0);
        repo.save(u);
    }

    public void eliminar(String id) {
        repo.deleteById(id);
    }

    // ── Helpers ─────────────────────────────────────────────────

    private boolean primerUsuario(String empresaId) {
        return repo.findAllByEmpresa(empresaId).isEmpty();
    }

    private String resolverEmail(Jwt jwt) {
        // Azure B2C puede exponer el email en distintos claims
        String e = jwt.getClaimAsString("preferred_username");
        if (e != null && e.contains("@")) return e;
        e = jwt.getClaimAsString("email");
        if (e != null) return e;
        // claim emails es un array en B2C
        Object emails = jwt.getClaim("emails");
        if (emails instanceof java.util.List<?> list && !list.isEmpty()) {
            return list.get(0).toString();
        }
        return jwt.getSubject();
    }

    private PerfilDto toDto(UsuarioSistema u) {
        return new PerfilDto(
                u.getId(), u.getNombre(), u.getEmail(),
                u.getRol(), u.getEmpresaId(), u.getActivo()
        );
    }
}
