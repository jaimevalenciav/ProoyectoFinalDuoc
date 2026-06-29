package cl.truckmanager.vehiculos.service;

import cl.truckmanager.vehiculos.dto.PermisoCirculacionDto;
import cl.truckmanager.vehiculos.dto.RevisionTecnicaDto;
import cl.truckmanager.vehiculos.dto.SeguroSoapDto;
import cl.truckmanager.vehiculos.entity.PermisoCirculacion;
import cl.truckmanager.vehiculos.entity.RevisionTecnica;
import cl.truckmanager.vehiculos.entity.SeguroSoap;
import cl.truckmanager.vehiculos.repository.PermisoCirculacionRepository;
import cl.truckmanager.vehiculos.repository.RevisionTecnicaRepository;
import cl.truckmanager.vehiculos.repository.SeguroSoapRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentosVehiculoService — pruebas unitarias")
class DocumentosVehiculoServiceTest {

    @Mock private PermisoCirculacionRepository permisoRepo;
    @Mock private SeguroSoapRepository         seguroRepo;
    @Mock private RevisionTecnicaRepository    revisionRepo;

    @InjectMocks
    private DocumentosVehiculoService servicio;

    private static final String EMPRESA_ID  = "EMP-001";
    private static final String VEHICULO_ID = "VEH-001";

    private PermisoCirculacion permisoEjemplo;
    private SeguroSoap         seguroEjemplo;
    private RevisionTecnica    revisionEjemplo;

    @BeforeEach
    void configurar() {
        permisoEjemplo = new PermisoCirculacion();
        permisoEjemplo.setId("PER-001");
        permisoEjemplo.setVehiculoId(VEHICULO_ID);
        permisoEjemplo.setEmpresaId(EMPRESA_ID);
        permisoEjemplo.setFechaPago(LocalDate.of(2024, 1, 10));
        permisoEjemplo.setFechaVencimiento(LocalDate.of(2025, 12, 31));

        seguroEjemplo = new SeguroSoap();
        seguroEjemplo.setId("SEG-001");
        seguroEjemplo.setVehiculoId(VEHICULO_ID);
        seguroEjemplo.setEmpresaId(EMPRESA_ID);
        seguroEjemplo.setFechaEmision(LocalDate.of(2024, 3, 1));
        seguroEjemplo.setFechaVencimiento(LocalDate.of(2025, 2, 28));

        revisionEjemplo = new RevisionTecnica();
        revisionEjemplo.setId("REV-001");
        revisionEjemplo.setVehiculoId(VEHICULO_ID);
        revisionEjemplo.setEmpresaId(EMPRESA_ID);
        revisionEjemplo.setFechaRevision(LocalDate.of(2024, 6, 15));
        revisionEjemplo.setResultado("APROBADO");
    }

    // ─── Permisos de Circulacion ──────────────────────────────────────────────

    @Test
    @DisplayName("getPermisos retorna lista del vehiculo")
    void getPermisos_retornaLista() {
        when(permisoRepo.findByVehiculoIdOrderByFechaPagoDescCreatedAtDesc(VEHICULO_ID))
            .thenReturn(List.of(permisoEjemplo));

        List<PermisoCirculacion> resultado = servicio.getPermisos(VEHICULO_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getVehiculoId()).isEqualTo(VEHICULO_ID);
    }

    @Test
    @DisplayName("createPermiso persiste con empresaId correcto")
    void createPermiso_guardaConEmpresaId() {
        when(permisoRepo.save(any(PermisoCirculacion.class))).thenReturn(permisoEjemplo);

        PermisoCirculacionDto dto = new PermisoCirculacionDto();
        dto.setVehiculoId(VEHICULO_ID);
        dto.setFechaPago(LocalDate.of(2024, 1, 10));

        PermisoCirculacion resultado = servicio.createPermiso(EMPRESA_ID, dto);

        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
        verify(permisoRepo).save(any(PermisoCirculacion.class));
    }

    @Test
    @DisplayName("deletePermiso elimina cuando existe")
    void deletePermiso_existente_elimina() {
        when(permisoRepo.existsById("PER-001")).thenReturn(true);

        servicio.deletePermiso("PER-001");

        verify(permisoRepo).deleteById("PER-001");
    }

    @Test
    @DisplayName("deletePermiso lanza EntityNotFoundException cuando no existe")
    void deletePermiso_inexistente_lanzaExcepcion() {
        when(permisoRepo.existsById("X")).thenReturn(false);

        assertThatThrownBy(() -> servicio.deletePermiso("X"))
            .isInstanceOf(EntityNotFoundException.class);
        verify(permisoRepo, never()).deleteById(any());
    }

    // ─── Seguros SOAP ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSeguros retorna lista del vehiculo")
    void getSeguros_retornaLista() {
        when(seguroRepo.findByVehiculoIdOrderByFechaEmisionDescCreatedAtDesc(VEHICULO_ID))
            .thenReturn(List.of(seguroEjemplo));

        List<SeguroSoap> resultado = servicio.getSeguros(VEHICULO_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getVehiculoId()).isEqualTo(VEHICULO_ID);
    }

    @Test
    @DisplayName("createSeguro persiste con empresaId correcto")
    void createSeguro_guardaConEmpresaId() {
        when(seguroRepo.save(any(SeguroSoap.class))).thenReturn(seguroEjemplo);

        SeguroSoapDto dto = new SeguroSoapDto();
        dto.setVehiculoId(VEHICULO_ID);
        dto.setFechaEmision(LocalDate.of(2024, 3, 1));

        SeguroSoap resultado = servicio.createSeguro(EMPRESA_ID, dto);

        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
        verify(seguroRepo).save(any(SeguroSoap.class));
    }

    @Test
    @DisplayName("deleteSeguro elimina cuando existe")
    void deleteSeguro_existente_elimina() {
        when(seguroRepo.existsById("SEG-001")).thenReturn(true);

        servicio.deleteSeguro("SEG-001");

        verify(seguroRepo).deleteById("SEG-001");
    }

    @Test
    @DisplayName("deleteSeguro lanza EntityNotFoundException cuando no existe")
    void deleteSeguro_inexistente_lanzaExcepcion() {
        when(seguroRepo.existsById("X")).thenReturn(false);

        assertThatThrownBy(() -> servicio.deleteSeguro("X"))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── Revisiones Tecnicas ──────────────────────────────────────────────────

    @Test
    @DisplayName("getRevisiones retorna lista del vehiculo")
    void getRevisiones_retornaLista() {
        when(revisionRepo.findByVehiculoIdOrderByFechaRevisionDescCreatedAtDesc(VEHICULO_ID))
            .thenReturn(List.of(revisionEjemplo));

        List<RevisionTecnica> resultado = servicio.getRevisiones(VEHICULO_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getResultado()).isEqualTo("APROBADO");
    }

    @Test
    @DisplayName("createRevision persiste con empresaId correcto")
    void createRevision_guardaConEmpresaId() {
        when(revisionRepo.save(any(RevisionTecnica.class))).thenReturn(revisionEjemplo);

        RevisionTecnicaDto dto = new RevisionTecnicaDto();
        dto.setVehiculoId(VEHICULO_ID);
        dto.setFechaRevision(LocalDate.of(2024, 6, 15));
        dto.setResultado("APROBADO");

        RevisionTecnica resultado = servicio.createRevision(EMPRESA_ID, dto);

        assertThat(resultado.getResultado()).isEqualTo("APROBADO");
        verify(revisionRepo).save(any(RevisionTecnica.class));
    }

    @Test
    @DisplayName("deleteRevision elimina cuando existe")
    void deleteRevision_existente_elimina() {
        when(revisionRepo.existsById("REV-001")).thenReturn(true);

        servicio.deleteRevision("REV-001");

        verify(revisionRepo).deleteById("REV-001");
    }

    @Test
    @DisplayName("deleteRevision lanza EntityNotFoundException cuando no existe")
    void deleteRevision_inexistente_lanzaExcepcion() {
        when(revisionRepo.existsById("X")).thenReturn(false);

        assertThatThrownBy(() -> servicio.deleteRevision("X"))
            .isInstanceOf(EntityNotFoundException.class);
    }
}
