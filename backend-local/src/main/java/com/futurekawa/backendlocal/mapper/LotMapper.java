package com.futurekawa.backendlocal.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.futurekawa.backendlocal.model.Lot;
import com.futurekawa.lib.dto.request.CreateLotRequest;
import com.futurekawa.lib.dto.response.LotResponse;

@Mapper
public interface LotMapper {
    @Mapping(target = "paysId", source = "pays.id")
    @Mapping(target = "exploitationId", source = "exploitation.id")
    @Mapping(target = "entrepotId", source = "entrepot.id")
    @Mapping(target = "ancienneteJours", source = "dateEntreeStockage", qualifiedByName = "joursDepuis")
    LotResponse toResponse(Lot lot);

    @Named("joursDepuis")
    static Integer joursDepuis(LocalDateTime dateEntreeStockage) {
        if (dateEntreeStockage == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(dateEntreeStockage.toLocalDate(), LocalDate.now());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateEntreeStockage", ignore = true)
    @Mapping(target = "statutLot", ignore = true)
    @Mapping(target = "pays", ignore = true)
    @Mapping(target = "exploitation.id", source = "exploitationId")
    @Mapping(target = "entrepot.id", source = "entrepotId")
    Lot toEntity(CreateLotRequest request);
}
