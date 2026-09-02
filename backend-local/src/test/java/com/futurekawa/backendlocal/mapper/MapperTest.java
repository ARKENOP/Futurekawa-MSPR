package com.futurekawa.backendlocal.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.futurekawa.backendlocal.dto.MqttMesurePayload;
import com.futurekawa.backendlocal.model.Alerte;
import com.futurekawa.backendlocal.model.Entrepot;
import com.futurekawa.backendlocal.model.Exploitation;
import com.futurekawa.backendlocal.model.Lot;
import com.futurekawa.backendlocal.model.MesureStockage;
import com.futurekawa.backendlocal.model.Pays;
import com.futurekawa.lib.dto.request.CreateLotRequest;
import com.futurekawa.lib.dto.response.AlerteResponse;
import com.futurekawa.lib.dto.response.EntrepotResponse;
import com.futurekawa.lib.dto.response.ExploitationResponse;
import com.futurekawa.lib.dto.response.LotResponse;
import com.futurekawa.lib.dto.response.MesureStockageResponse;
import com.futurekawa.lib.dto.response.PaysResponse;
import com.futurekawa.lib.enums.NiveauAlerte;
import com.futurekawa.lib.enums.StatutAlerte;
import com.futurekawa.lib.enums.TypeAlerte;

class MapperTest {
    private final LotMapper lotMapper = new LotMapperImpl();
    private final MesureStockageMapper mesureMapper = new MesureStockageMapperImpl();
    private final AlerteMapper alerteMapper = new AlerteMapperImpl();
    private final EntrepotMapper entrepotMapper = new EntrepotMapperImpl();
    private final ExploitationMapper exploitationMapper = new ExploitationMapperImpl();
    private final PaysMapper paysMapper = new PaysMapperImpl();

    private Pays pays() {
        Pays p = new Pays();
        p.setId(1L);
        p.setCodePays("BR");
        p.setNomPays("Brésil");
        p.setTemperatureIdealeC(new BigDecimal("29.0"));
        p.setHumiditeIdealePourcent(new BigDecimal("55.0"));
        p.setToleranceTemperatureC(new BigDecimal("3.0"));
        p.setToleranceHumiditePourcent(new BigDecimal("2.0"));
        p.setEstActif(true);
        return p;
    }

    @Test
    void paysMapperMapsAllFields() {
        PaysResponse r = paysMapper.toResponse(pays());
        assertThat(r.codePays()).isEqualTo("BR");
        assertThat(r.nomPays()).isEqualTo("Brésil");
        assertThat(r.estActif()).isTrue();
    }

    @Test
    void lotMapperMapsRelationIds() {
        Pays p = pays();
        Exploitation exp = new Exploitation();
        exp.setId(3L);
        Entrepot ent = new Entrepot();
        ent.setId(2L);
        Lot lot = new Lot();
        lot.setId(10L);
        lot.setReferenceLot("LOT-1");
        lot.setPays(p);
        lot.setExploitation(exp);
        lot.setEntrepot(ent);

        LotResponse r = lotMapper.toResponse(lot);
        assertThat(r.id()).isEqualTo(10L);
        assertThat(r.paysId()).isEqualTo(1L);
        assertThat(r.exploitationId()).isEqualTo(3L);
        assertThat(r.entrepotId()).isEqualTo(2L);
    }

    @Test
    void lotMapperToEntitySetsNestedIds() {
        CreateLotRequest req = new CreateLotRequest("LOT-2", LocalDate.now(), null, "AA", 3L, 2L);
        Lot lot = lotMapper.toEntity(req);
        assertThat(lot.getReferenceLot()).isEqualTo("LOT-2");
        assertThat(lot.getExploitation().getId()).isEqualTo(3L);
        assertThat(lot.getEntrepot().getId()).isEqualTo(2L);
    }

    @Test
    void lotMapperHandlesNull() {
        assertThat(lotMapper.toResponse(null)).isNull();
        assertThat(lotMapper.toEntity(null)).isNull();
    }

    @Test
    void mesureMapperMapsResponseAndEntity() {
        Entrepot ent = new Entrepot();
        ent.setId(2L);
        Lot lot = new Lot();
        lot.setId(4L);
        MesureStockage m = new MesureStockage();
        m.setId(5L);
        m.setIdCapteur("c1");
        m.setTemperatureC(new BigDecimal("27.7"));
        m.setHumiditePourcent(new BigDecimal("46.4"));
        m.setEntrepot(ent);
        m.setLot(lot);

        MesureStockageResponse r = mesureMapper.toResponse(m);
        assertThat(r.entrepotId()).isEqualTo(2L);
        assertThat(r.lotId()).isEqualTo(4L);

        MqttMesurePayload payload = new MqttMesurePayload("c9", new BigDecimal("20"), new BigDecimal("40"), 1L);
        MesureStockage entity = mesureMapper.toEntity(payload);
        assertThat(entity.getIdCapteur()).isEqualTo("c9");
        assertThat(entity.getTemperatureC()).isEqualByComparingTo("20");
        assertThat(entity.getDateHeureMesure()).isNull();
    }

    @Test
    void alerteMapperMapsRelationsAndClosureDate() {
        Pays p = pays();
        Entrepot ent = new Entrepot();
        ent.setId(2L);
        ent.setPays(p);
        Lot lot = new Lot();
        lot.setId(4L);
        Alerte a = new Alerte();
        a.setId(8L);
        a.setTypeAlerte(TypeAlerte.CONDITION_NON_IDEALE);
        a.setNiveau(NiveauAlerte.CRITIQUE);
        a.setStatutAlerte(StatutAlerte.OUVERTE);
        a.setDateHeureCreation(LocalDateTime.now());
        a.setDateCloture(LocalDateTime.now());
        a.setEntrepot(ent);
        a.setLotConcerne(lot);

        AlerteResponse r = alerteMapper.toResponse(a);
        assertThat(r.entrepotId()).isEqualTo(2L);
        assertThat(r.paysId()).isEqualTo(1L);
        assertThat(r.lotId()).isEqualTo(4L);
        assertThat(r.dateHeureCloture()).isNotNull();
    }

    @Test
    void entrepotAndExploitationMappers() {
        Pays p = pays();
        Exploitation exp = new Exploitation();
        exp.setId(3L);
        exp.setNomExploitation("Exp");
        exp.setEstActive(true);
        exp.setPays(p);

        ExploitationResponse er = exploitationMapper.toResponse(exp);
        assertThat(er.paysId()).isEqualTo(1L);
        assertThat(er.codePays()).isEqualTo("BR");

        Entrepot ent = new Entrepot();
        ent.setId(2L);
        ent.setNomEntrepot("E");
        ent.setExploitation(exp);
        ent.setPays(p);
        EntrepotResponse entR = entrepotMapper.toResponse(ent);
        assertThat(entR.exploitationId()).isEqualTo(3L);
        assertThat(entR.paysId()).isEqualTo(1L);
    }
}
