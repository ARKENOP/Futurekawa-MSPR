package com.futurekawa.backendlocal.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.futurekawa.backendlocal.dto.MqttMesurePayload;
import com.futurekawa.backendlocal.model.MesureStockage;
import com.futurekawa.lib.dto.response.MesureStockageResponse;

@Mapper
public interface MesureStockageMapper {
    @Mapping(target = "entrepotId", source = "entrepot.id")
    @Mapping(target = "lotId", source = "lot.id")
    MesureStockageResponse toResponse(MesureStockage mesureStockage);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "entrepot", ignore = true)
    @Mapping(target = "lot", ignore = true)
    @Mapping(target = "dateHeureMesure", ignore = true)
    MesureStockage toEntity(MqttMesurePayload payload);
}
