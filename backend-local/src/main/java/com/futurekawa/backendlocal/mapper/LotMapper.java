package com.futurekawa.backendlocal.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.futurekawa.backendlocal.model.Lot;
import com.futurekawa.lib.dto.request.CreateLotRequest;
import com.futurekawa.lib.dto.response.LotResponse;

@Mapper
public interface LotMapper {

    @Mapping(target = "paysId", source = "pays.id")
    @Mapping(target = "exploitationId", source = "exploitation.id")
    @Mapping(target = "entrepotId", source = "entrepot.id")
    LotResponse toResponse(Lot lot);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateEntreeStockage", ignore = true)
    @Mapping(target = "statutLot", ignore = true)
    @Mapping(target = "pays", ignore = true)
    @Mapping(target = "exploitation.id", source = "exploitationId")
    @Mapping(target = "entrepot.id", source = "entrepotId")
    Lot toEntity(CreateLotRequest request);
}
