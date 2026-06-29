package cl.truckmanager.conductores.service;

import cl.truckmanager.conductores.dto.PerfilDto;
import cl.truckmanager.conductores.dto.UsuarioDto;
import cl.truckmanager.conductores.entity.UsuarioSistema;
import cl.truckmanager.conductores.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService — pruebas unitarias")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repo;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private UsuarioService servicio;

    private static final String EMPRESA_ID = "EMP-001";
    private static final String AZURE_OID  = "oid-abc-123";
    private static final String USUARIO_ID = "USR-001";

    private UsuarioSistema usuarioEjemplo;

    @BeforeEach
    void configurar() {
        usuarioEjemplo = UsuarioSistema.builder()
            .id(USUARIO_ID)
            .empresaId(EMPRESA_ID)
            .azureOid(AZURE_OID)
            .nombre("Maria Lopez")
            .email("maria@truckmanager.cl")
            .rol("ADMIN")
            .activo(1)
            .build();
    }

    // ─── perfilActual ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("perfilActual retorna perfil cuando el usuario ya existe en BD")
    void perfilActual_usuarioExistente_retornaPerfil() {
        when(jwt.getSubject()).thenReturn(AZURE_OID);
        when(jwt.getClaimAsString("name")).thenReturn("Maria Lopez");
        when(jwt.getClaimAsString("preferred_username")).thenReturn("maria@truckmanager.cl");
        when(repo.findByAzureOid(AZURE_OID)).thenReturn(Optional.of(usuarioEjemplo));

        PerfilDto resultado = servicio.perfilActual(EMPRESA_ID, jwt);

        assertThat(resultado.getNombre()).isEqualTo("Maria Lopez");
        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
        assertThat(resultado.getRol()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("perfilActual crea usuario nuevo cuando tiene empresaId en JWT y es el primero")
    void perfilActual_usuarioNuevoConEmpresaId_creaComoAdmin() {
        when(jwt.getSubject()).thenReturn(AZURE_OID);
        when(jwt.getClaimAsString("name")).thenReturn("Juan Nuevo");
        when(jwt.getClaimAsString("preferred_username")).thenReturn("juan@truckmanager.cl");
        when(repo.findByAzureOid(AZURE_OID)).thenReturn(Optional.empty());
        when(repo.findAllByEmpresa(EMPRESA_ID)).thenReturn(List.of()); // primer usuario
        when(repo.save(any(UsuarioSistema.class))).thenAnswer(inv -> {
            UsuarioSistema u = inv.getArgument(0);
            u.setId(USUARIO_ID);
            return u;
        });

        PerfilDto resultado = servicio.perfilActual(EMPRESA_ID, jwt);

        assertThat(resultado.getRol()).isEqualTo("ADMIN"); // primer usuario = ADMIN
        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    @DisplayName("perfilActual retorna perfil sin empresa cuando el JWT no tiene empresaId")
    void perfilActual_sinEmpresaEnJwt_retornaPerfilSinEmpresa() {
        when(jwt.getSubject()).thenReturn(AZURE_OID);
        when(jwt.getClaimAsString("name")).thenReturn("Sin Empresa");
        when(jwt.getClaimAsString("preferred_username")).thenReturn("sinempresa@correo.cl");
        when(repo.findByAzureOid(AZURE_OID)).thenReturn(Optional.empty());

        PerfilDto resultado = servicio.perfilActual(null, jwt);

        assertThat(resultado.getEmpresaId()).isNull();
        assertThat(resultado.getRol()).isNull();
    }

    // ─── listar ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("listar retorna todos los usuarios de la empresa")
    void listar_retornaUsuarios() {
        when(repo.findAllByEmpresa(EMPRESA_ID)).thenReturn(List.of(usuarioEjemplo));

        List<UsuarioSistema> resultado = servicio.listar(EMPRESA_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    // ─── crear ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("crear persiste usuario con activo=1 por defecto")
    void crear_sinActivo_asignaUno() {
        when(repo.save(any(UsuarioSistema.class))).thenReturn(usuarioEjemplo);

        UsuarioDto dto = new UsuarioDto();
        dto.setNombre("Maria Lopez");
        dto.setEmail("maria@truckmanager.cl");
        dto.setRol("ADMIN");

        UsuarioSistema resultado = servicio.crear(EMPRESA_ID, dto);

        assertThat(resultado.getActivo()).isEqualTo(1);
        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    // ─── actualizar ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("actualizar modifica nombre, email y rol")
    void actualizar_camposValidos_actualiza() {
        when(repo.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioEjemplo));
        when(repo.save(any(UsuarioSistema.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDto dto = new UsuarioDto();
        dto.setNombre("Maria Lopez V.");
        dto.setEmail("maria.v@truckmanager.cl");
        dto.setRol("OPERADOR");

        UsuarioSistema resultado = servicio.actualizar(USUARIO_ID, dto);

        assertThat(resultado.getNombre()).isEqualTo("Maria Lopez V.");
        assertThat(resultado.getRol()).isEqualTo("OPERADOR");
    }

    @Test
    @DisplayName("actualizar lanza EntityNotFoundException cuando el usuario no existe")
    void actualizar_inexistente_lanzaExcepcion() {
        when(repo.findById("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar("X", new UsuarioDto()))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── cambiarRol ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("cambiarRol actualiza el rol del usuario")
    void cambiarRol_rolValido_actualizaRol() {
        when(repo.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioEjemplo));

        servicio.cambiarRol(USUARIO_ID, "CONDUCTOR");

        assertThat(usuarioEjemplo.getRol()).isEqualTo("CONDUCTOR");
        verify(repo).save(usuarioEjemplo);
    }

    // ─── cambiarActivo ────────────────────────────────────────────────────────

    @Test
    @DisplayName("cambiarActivo desactiva el usuario")
    void cambiarActivo_desactivar_asignaCero() {
        when(repo.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioEjemplo));

        servicio.cambiarActivo(USUARIO_ID, false);

        assertThat(usuarioEjemplo.getActivo()).isEqualTo(0);
    }

    @Test
    @DisplayName("cambiarActivo activa el usuario")
    void cambiarActivo_activar_asignaUno() {
        usuarioEjemplo.setActivo(0);
        when(repo.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioEjemplo));

        servicio.cambiarActivo(USUARIO_ID, true);

        assertThat(usuarioEjemplo.getActivo()).isEqualTo(1);
    }

    // ─── eliminar ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar invoca deleteById en el repositorio")
    void eliminar_usuarioExistente_invocaDeleteById() {
        servicio.eliminar(USUARIO_ID);

        verify(repo).deleteById(USUARIO_ID);
    }
}
