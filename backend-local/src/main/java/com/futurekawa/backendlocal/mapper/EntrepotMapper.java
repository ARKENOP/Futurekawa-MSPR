package com.futurekawa.backendlocal.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.futurekawa.backendlocal.dto.response.EntrepotResponse;
import com.futurekawa.backendlocal.model.Entrepot;

/**
 * MapStruct mapper for Entrepot entities and DTOs.
 */
@Mapper
public interface EntrepotMapper {

    @Mapping(target = "exploitationId", source = "exploitation.id")
    @Mapping(target = "paysId", source = "pays.id")
    EntrepotResponse toResponse(Entrepot entrepot);
}
