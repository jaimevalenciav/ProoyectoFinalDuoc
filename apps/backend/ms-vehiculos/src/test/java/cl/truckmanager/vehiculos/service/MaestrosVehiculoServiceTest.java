package cl.truckmanager.vehiculos.service;

import cl.truckmanager.vehiculos.dto.AseguradoraDto;
import cl.truckmanager.vehiculos.dto.MunicipalidadDto;
import cl.truckmanager.vehiculos.dto.PlantaRevisionDto;
import cl.truckmanager.vehiculos.dto.SucursalDto;
import cl.truckmanager.vehiculos.entity.Aseguradora;
import cl.truckmanager.vehiculos.entity.Municipalidad;
import cl.truckmanager.vehiculos.entity.PlantaRevision;
import cl.truckmanager.vehiculos.entity.Sucursal;
import cl.truckmanager.vehiculos.repository.AseguradoraRepository;
import cl.truckmanager.vehiculos.repository.MunicipalidadRepository;
import cl.truckmanager.vehiculos.repository.PlantaRevisionRepository;
import cl.truckmanager.vehiculos.repository.SucursalRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaestrosVehiculoService — pruebas unitarias")
class MaestrosVehiculoServiceTest {

    @Mock private SucursalRepository     sucursalRepo;
    @Mock private MunicipalidadRepository municipalidadRepo;
    @Mock private AseguradoraRepository  aseguradoraRepo;
    @Mock private PlantaRevisionRepository plantaRepo;

    @InjectMocks
    private MaestrosVehiculoService servicio;

    private static final String EMPRESA_ID = "EMP-001";

    private Sucursal sucursalEjemplo;
    private Municipalidad municipalidadEjemplo;
    private Aseguradora aseguradoraEjemplo;
    private PlantaRevision plantaEjemplo;

    @BeforeEach
    void configurar() {
        sucursalEjemplo = Sucursal.builder()
            .id("SUC-001").empresaId(EMPRESA_ID)
            .nombre("Santiago Centro").ciudad("Santiago").eliminado(0).build();

        municipalidadEjemplo = Municipalidad.builder()
            .id("MUN-001").empresaId(EMPRESA_ID)
            .nombre("Las Condes").region("Metropolitana").eliminado(0).build();

        aseguradoraEjemplo = Aseguradora.builder()
            .id("ASE-001").empresaId(EMPRESA_ID)
            .nombre("Mapfre").rut("76543210-K").eliminado(0).build();

        plantaEjemplo = PlantaRevision.builder()
            .id("PLT-001").empresaId(EMPRESA_ID)
            .nombre("Revisur").eliminado(0).build();
    }

    // ─── Sucursales ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSucursales retorna lista de la empresa")
    void getSucursales_retornaLista() {
        when(sucursalRepo.findByEmpresaIdAndEliminadoOrderByNombreAsc(EMPRESA_ID, 0))
            .thenReturn(List.of(sucursalEjemplo));

        List<Sucursal> resultado = servicio.getSucursales(EMPRESA_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Santiago Centro");
    }

    @Test
    @DisplayName("createSucursal persiste con empresaId correcto")
    void createSucursal_guardaConEmpresaId() {
        when(sucursalRepo.save(any(Sucursal.class))).thenReturn(sucursalEjemplo);
        SucursalDto dto = new SucursalDto();
        dto.setNombre("Santiago Centro");
        dto.setCiudad("Santiago");

        Sucursal resultado = servicio.createSucursal(EMPRESA_ID, dto);

        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
        verify(sucursalRepo).save(any(Sucursal.class));
    }

    @Test
    @DisplayName("updateSucursal modifica nombre y ciudad")
    void updateSucursal_modificaCampos() {
        when(sucursalRepo.findById("SUC-001")).thenReturn(Optional.of(sucursalEjemplo));
        when(sucursalRepo.save(any(Sucursal.class))).thenAnswer(inv -> inv.getArgument(0));

        SucursalDto dto = new SucursalDto();
        dto.setNombre("Providencia");

        Sucursal resultado = servicio.updateSucursal("SUC-001", dto);

        assertThat(resultado.getNombre()).isEqualTo("Providencia");
    }

    @Test
    @DisplayName("updateSucursal lanza EntityNotFoundException cuando no existe")
    void updateSucursal_inexistente_lanzaExcepcion() {
        when(sucursalRepo.findById("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.updateSucursal("X", new SucursalDto()))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("deleteSucursal marca como eliminado logicamente")
    void deleteSucursal_marcaEliminado() {
        when(sucursalRepo.findById("SUC-001")).thenReturn(Optional.of(sucursalEjemplo));

        servicio.deleteSucursal("SUC-001");

        assertThat(sucursalEjemplo.getEliminado()).isEqualTo(1);
        verify(sucursalRepo).save(sucursalEjemplo);
    }

    // ─── Municipalidades ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getMunicipalidades retorna lista de la empresa")
    void getMunicipalidades_retornaLista() {
        when(municipalidadRepo.findByEmpresaIdAndEliminadoOrderByNombreAsc(EMPRESA_ID, 0))
            .thenReturn(List.of(municipalidadEjemplo));

        List<Municipalidad> resultado = servicio.getMunicipalidades(EMPRESA_ID);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("createMunicipalidad persiste la municipalidad")
    void createMunicipalidad_guardaConEmpresaId() {
        when(municipalidadRepo.save(any(Municipalidad.class))).thenReturn(municipalidadEjemplo);
        MunicipalidadDto dto = new MunicipalidadDto();
        dto.setNombre("Las Condes");
        dto.setRegion("Metropolitana");

        Municipalidad resultado = servicio.createMunicipalidad(EMPRESA_ID, dto);

        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    @DisplayName("updateMunicipalidad lanza EntityNotFoundException cuando no existe")
    void updateMunicipalidad_inexistente_lanzaExcepcion() {
        when(municipalidadRepo.findById("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.updateMunicipalidad("X", new MunicipalidadDto()))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("deleteMunicipalidad marca como eliminado")
    void deleteMunicipalidad_marcaEliminado() {
        when(municipalidadRepo.findById("MUN-001")).thenReturn(Optional.of(municipalidadEjemplo));

        servicio.deleteMunicipalidad("MUN-001");

        assertThat(municipalidadEjemplo.getEliminado()).isEqualTo(1);
        verify(municipalidadRepo).save(municipalidadEjemplo);
    }

    // ─── Aseguradoras ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAseguradoras retorna lista de la empresa")
    void getAseguradoras_retornaLista() {
        when(aseguradoraRepo.findByEmpresaIdAndEliminadoOrderByNombreAsc(EMPRESA_ID, 0))
            .thenReturn(List.of(aseguradoraEjemplo));

        assertThat(servicio.getAseguradoras(EMPRESA_ID)).hasSize(1);
    }

    @Test
    @DisplayName("createAseguradora persiste la aseguradora")
    void createAseguradora_guardaConEmpresaId() {
        when(aseguradoraRepo.save(any(Aseguradora.class))).thenReturn(aseguradoraEjemplo);
        AseguradoraDto dto = new AseguradoraDto();
        dto.setNombre("Mapfre");
        dto.setRut("76543210-K");

        Aseguradora resultado = servicio.createAseguradora(EMPRESA_ID, dto);

        assertThat(resultado.getNombre()).isEqualTo("Mapfre");
    }

    @Test
    @DisplayName("updateAseguradora modifica nombre")
    void updateAseguradora_modificaNombre() {
        when(aseguradoraRepo.findById("ASE-001")).thenReturn(Optional.of(aseguradoraEjemplo));
        when(aseguradoraRepo.save(any(Aseguradora.class))).thenAnswer(inv -> inv.getArgument(0));
        AseguradoraDto dto = new AseguradoraDto();
        dto.setNombre("HDI Seguros");

        Aseguradora resultado = servicio.updateAseguradora("ASE-001", dto);

        assertThat(resultado.getNombre()).isEqualTo("HDI Seguros");
    }

    @Test
    @DisplayName("deleteAseguradora marca como eliminado")
    void deleteAseguradora_marcaEliminado() {
        when(aseguradoraRepo.findById("ASE-001")).thenReturn(Optional.of(aseguradoraEjemplo));

        servicio.deleteAseguradora("ASE-001");

        assertThat(aseguradoraEjemplo.getEliminado()).isEqualTo(1);
    }

    // ─── Plantas de Revision ──────────────────────────────────────────────────

    @Test
    @DisplayName("getPlantasRevision retorna lista de la empresa")
    void getPlantasRevision_retornaLista() {
        when(plantaRepo.findByEmpresaIdAndEliminadoOrderByNombreAsc(EMPRESA_ID, 0))
            .thenReturn(List.of(plantaEjemplo));

        assertThat(servicio.getPlantasRevision(EMPRESA_ID)).hasSize(1);
    }

    @Test
    @DisplayName("createPlantaRevision persiste la planta")
    void createPlantaRevision_guardaConEmpresaId() {
        when(plantaRepo.save(any(PlantaRevision.class))).thenReturn(plantaEjemplo);
        PlantaRevisionDto dto = new PlantaRevisionDto();
        dto.setNombre("Revisur");

        PlantaRevision resultado = servicio.createPlantaRevision(EMPRESA_ID, dto);

        assertThat(resultado.getEmpresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    @DisplayName("updatePlantaRevision lanza EntityNotFoundException cuando no existe")
    void updatePlantaRevision_inexistente_lanzaExcepcion() {
        when(plantaRepo.findById("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.updatePlantaRevision("X", new PlantaRevisionDto()))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("deletePlantaRevision marca como eliminado")
    void deletePlantaRevision_marcaEliminado() {
        when(plantaRepo.findById("PLT-001")).thenReturn(Optional.of(plantaEjemplo));

        servicio.deletePlantaRevision("PLT-001");

        assertThat(plantaEjemplo.getEliminado()).isEqualTo(1);
        verify(plantaRepo).save(plantaEjemplo);
    }
}
