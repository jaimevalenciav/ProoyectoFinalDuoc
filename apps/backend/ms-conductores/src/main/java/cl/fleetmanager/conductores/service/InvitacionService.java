package cl.fleetmanager.conductores.service;

import cl.fleetmanager.conductores.dto.InvitacionDto;
import cl.fleetmanager.conductores.dto.InvitacionResumenDto;
import cl.fleetmanager.conductores.dto.PerfilDto;
import cl.fleetmanager.conductores.entity.InvitacionPendiente;
import cl.fleetmanager.conductores.entity.UsuarioSistema;
import cl.fleetmanager.conductores.repository.InvitacionRepository;
import cl.fleetmanager.conductores.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class InvitacionService {

    private final InvitacionRepository invitacionRepo;
    private final UsuarioRepository    usuarioRepo;

    // ── Crear invitación (solo ADMIN) ─────────────────────────────

    public InvitacionResumenDto crear(String empresaId, InvitacionDto dto) {
        int dias = dto.getDiasVigencia() != null && dto.getDiasVigencia() > 0
                ? dto.getDiasVigencia() : 7;

        InvitacionPendiente inv = InvitacionPendiente.builder()
                .empresaId(empresaId)
                .rol(dto.getRol())
                .emailSugerido(dto.getEmailSugerido())
                .nota(dto.getNota())
                .expiresAt(LocalDateTime.now().plusDays(dias))
                .build();

        invitacionRepo.save(inv);
        return toDto(inv);
    }

    // ── Listar invitaciones de la empresa ──────────────────────────

    public List<InvitacionResumenDto> listar(String empresaId) {
        return invitacionRepo.findByEmpresa(empresaId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── Validar token (endpoint público, sin autenticación) ────────

    @Transactional(readOnly = true)
    public InvitacionResumenDto validar(String token) {
        InvitacionPendiente inv = invitacionRepo.findByTokenAndUsadaFalse(token)
                .orElseThrow(() -> new EntityNotFoundException("Invitación no válida o ya utilizada"));
        if (inv.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("La invitación ha expirado");
        }
        return toDto(inv);
    }

    // ── Aceptar invitación (usuario autenticado con JWT) ───────────

    public PerfilDto aceptar(String token, Jwt jwt) {
        InvitacionPendiente inv = invitacionRepo.findByTokenAndUsadaFalse(token)
                .orElseThrow(() -> new EntityNotFoundException("Invitación no válida o ya utilizada"));

        if (inv.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("La invitación ha expirado");
        }

        String azureOid = jwt.getSubject();

        // Verificar que el usuario no está ya registrado en otra empresa
        usuarioRepo.findByAzureOid(azureOid).ifPresent(u -> {
            if (!u.getEmpresaId().equals(inv.getEmpresaId())) {
                throw new IllegalStateException("El usuario ya pertenece a otra empresa");
            }
        });

        // Crear o actualizar usuario
        UsuarioSistema u = usuarioRepo.findByAzureOid(azureOid).orElseGet(() -> {
            String email  = resolverEmail(jwt);
            String nombre = jwt.getClaimAsString("name");
            if (nombre == null) nombre = email;

            return UsuarioSistema.builder()
                    .empresaId(inv.getEmpresaId())
                    .azureOid(azureOid)
                    .nombre(nombre)
                    .email(email)
                    .rol(inv.getRol())
                    .build();
        });

        // Actualizar empresa y rol según invitación
        u.setEmpresaId(inv.getEmpresaId());
        u.setRol(inv.getRol());
        usuarioRepo.save(u);

        // Marcar invitación como usada
        inv.setUsada(true);
        inv.setAceptadaPorOid(azureOid);
        invitacionRepo.save(inv);

        return new PerfilDto(u.getId(), u.getNombre(), u.getEmail(),
                u.getRol(), u.getEmpresaId(), u.getActivo());
    }

    // ── Revocar invitación ─────────────────────────────────────────

    public void revocar(String token) {
        InvitacionPendiente inv = invitacionRepo.findById(token)
                .orElseThrow(() -> new EntityNotFoundException("Invitación no encontrada"));
        inv.setUsada(true);
        invitacionRepo.save(inv);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String resolverEmail(Jwt jwt) {
        String e = jwt.getClaimAsString("preferred_username");
        if (e != null && e.contains("@")) return e;
        e = jwt.getClaimAsString("email");
        if (e != null) return e;
        Object emails = jwt.getClaim("emails");
        if (emails instanceof java.util.List<?> list && !list.isEmpty()) {
            return list.get(0).toString();
        }
        return jwt.getSubject();
    }

    private InvitacionResumenDto toDto(InvitacionPendiente inv) {
        return new InvitacionResumenDto(
                inv.getToken(), inv.getRol(), inv.getEmailSugerido(),
                inv.getNota(), inv.getExpiresAt(), inv.getUsada(), inv.getCreatedAt()
        );
    }
}
