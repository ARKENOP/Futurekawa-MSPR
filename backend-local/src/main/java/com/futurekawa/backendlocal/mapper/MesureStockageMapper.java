package com.futurekawa.backendlocal.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.futurekawa.backendlocal.dto.MqttMesurePayload;
import com.futurekawa.backendlocal.dto.response.MesureStockageResponse;
import com.futurekawa.backendlocal.model.MesureStockage;

/**
 * MapStruct mapper for MesureStockage entities and DTOs.
 */
@Mapper
public interface MesureStockageMapper {

    @Mapping(target = "entrepotId", source = "entrepot.id")
    @Mapping(target = "lotId", source = "lot.id")
    MesureStockageResponse toResponse(MesureStockage mesureStockage);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "entrepot", ignore = true)
    @Mapping(target = "lot", ignore = true)
    @Mapping(target = "dateHeureMesure", ignore = true) // Handled in Service by converting from timestamp
    MesureStockage toEntity(MqttMesurePayload payload);
}
