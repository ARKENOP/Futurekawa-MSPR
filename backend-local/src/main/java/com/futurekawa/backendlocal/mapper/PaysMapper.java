package com.futurekawa.backendlocal.mapper;

import org.mapstruct.Mapper;

import com.futurekawa.backendlocal.dto.response.PaysResponse;
import com.futurekawa.backendlocal.model.Pays;

/**
 * MapStruct mapper for Pays entities and DTOs.
 */
@Mapper
public interface PaysMapper {

    PaysResponse toResponse(Pays pays);
}
