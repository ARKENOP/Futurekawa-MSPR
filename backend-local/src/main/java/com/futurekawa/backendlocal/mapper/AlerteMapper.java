package com.futurekawa.backendlocal.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.futurekawa.backendlocal.model.Alerte;
import com.futurekawa.lib.dto.response.AlerteResponse;

@Mapper
public interface AlerteMapper {
    @Mapping(target = "entrepotId", source = "entrepot.id")
    @Mapping(target = "lotId", source = "lotConcerne.id")
    @Mapping(target = "paysId", source = "entrepot.pays.id")
    @Mapping(target = "dateHeureCloture", source = "dateCloture")
    AlerteResponse toResponse(Alerte alerte);
}
