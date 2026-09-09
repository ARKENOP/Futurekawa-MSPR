package com.futurekawa.backendlocal.mapper;

import org.mapstruct.Mapper;

import com.futurekawa.backendlocal.model.Pays;
import com.futurekawa.lib.dto.response.PaysResponse;

@Mapper
public interface PaysMapper {
    PaysResponse toResponse(Pays pays);
}
