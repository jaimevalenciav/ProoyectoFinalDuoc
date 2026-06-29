package cl.truckmanager.conductores.service;

import cl.truckmanager.conductores.dto.InvitacionDto;
import cl.truckmanager.conductores.dto.InvitacionResumenDto;
import cl.truckmanager.conductores.entity.InvitacionPendiente;
import cl.truckmanager.conductores.repository.InvitacionRepository;
import cl.truckmanager.conductores.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvitacionService — pruebas unitarias")
class InvitacionServiceTest {

    @Mock
    private InvitacionRepository invitacionRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @InjectMocks
    private InvitacionService servicio;

    private static final String EMPRESA_ID = "EMP-001";
    private static final String TOKEN      = "TOKEN-ABC-123";

    private InvitacionPendiente invitacionVigente;
    private InvitacionPendiente invitacionExpirada;

    @BeforeEach
    void configurar() {
        invitacionVigente = InvitacionPendiente.builder()
            .token(TOKEN)
            .empresaId(EMPRESA_ID)
            .rol("CONDUCTOR")
            .emailSugerido("conductor@truckmanager.cl")
            .expiresAt(LocalDateTime.now().plusDays(5))
            .usada(false)
            .build();

        invitacionExpirada = InvitacionPendiente.builder()
            .token("TOKEN-EXPIRADO")
            .empresaId(EMPRESA_ID)
            .rol("CONDUCTOR")
            .expiresAt(LocalDateTime.now().minusDays(1))
            .usada(false)
            .build();
    }

    // ─── crear ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("crear persiste invitacion con vigencia por defecto de 7 dias")
    void crear_sinDiasVigencia_usa7DiasDeDefecto() {
        when(invitacionRepo.save(any(InvitacionPendiente.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        InvitacionDto dto = new InvitacionDto();
        dto.setRol("CONDUCTOR");

        InvitacionResumenDto resultado = servicio.crear(EMPRESA_ID, dto);

        assertThat(resultado.getRol()).isEqualTo("CONDUCTOR");
        assertThat(resultado.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(6));
        verify(invitacionRepo).save(any(InvitacionPendiente.class));
    }

    @Test
    @DisplayName("crear respeta los dias de vigencia especificados en el dto")
    void crear_conDiasVigencia_usaLosDiasIndicados() {
        when(invitacionRepo.save(any(InvitacionPendiente.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        InvitacionDto dto = new InvitacionDto();
        dto.setRol("OPERADOR");
        dto.setDiasVigencia(30);

        InvitacionResumenDto resultado = servicio.crear(EMPRESA_ID, dto);

        assertThat(resultado.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(29));
    }

    // ─── listar ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("listar retorna las invitaciones de la empresa")
    void listar_conInvitaciones_retornaLista() {
        when(invitacionRepo.findByEmpresa(EMPRESA_ID)).thenReturn(List.of(invitacionVigente));

        List<InvitacionResumenDto> resultado = servicio.listar(EMPRESA_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getToken()).isEqualTo(TOKEN);
    }

    // ─── validar ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validar retorna el resumen cuando el token es valido y no ha expirado")
    void validar_tokenVigente_retornaResumen() {
        when(invitacionRepo.findByTokenAndUsadaFalse(TOKEN))
            .thenReturn(Optional.of(invitacionVigente));

        InvitacionResumenDto resultado = servicio.validar(TOKEN);

        assertThat(resultado.getToken()).isEqualTo(TOKEN);
        assertThat(resultado.getRol()).isEqualTo("CONDUCTOR");
    }

    @Test
    @DisplayName("validar lanza EntityNotFoundException cuando el token no existe")
    void validar_tokenInexistente_lanzaExcepcion() {
        when(invitacionRepo.findByTokenAndUsadaFalse("TOKEN-X"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.validar("TOKEN-X"))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("validar lanza IllegalStateException cuando la invitacion ha expirado")
    void validar_tokenExpirado_lanzaExcepcion() {
        when(invitacionRepo.findByTokenAndUsadaFalse("TOKEN-EXPIRADO"))
            .thenReturn(Optional.of(invitacionExpirada));

        assertThatThrownBy(() -> servicio.validar("TOKEN-EXPIRADO"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("expirado");
    }

    // ─── revocar ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("revocar marca la invitacion como usada")
    void revocar_invitacionExistente_marcaUsada() {
        when(invitacionRepo.findById(TOKEN)).thenReturn(Optional.of(invitacionVigente));

        servicio.revocar(TOKEN);

        assertThat(invitacionVigente.getUsada()).isTrue();
        verify(invitacionRepo).save(invitacionVigente);
    }

    @Test
    @DisplayName("revocar lanza EntityNotFoundException cuando el token no existe")
    void revocar_tokenInexistente_lanzaExcepcion() {
        when(invitacionRepo.findById("TOKEN-X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.revocar("TOKEN-X"))
            .isInstanceOf(EntityNotFoundException.class);
    }
}
