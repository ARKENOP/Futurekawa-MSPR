package com.futurekawa.backendcentral.dto.envelope;

import java.util.List;

/**
 * Miroir manuel de org.springframework.data.domain.Page<T>, utilisé à la fois pour
 * désérialiser la réponse JSON d'un backend local et pour la resérialiser vers le
 * frontend. Page<T> est une interface : Jackson ne peut pas la reconstruire sans
 * mixin, donc on porte nous-mêmes exactement les champs du contrat (§4.4/§4.5).
 */
public record PageDto<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size,
        int numberOfElements,
        boolean first,
        boolean last,
        boolean empty
) {

    public static <T> PageDto<T> empty(int size) {
        return new PageDto<>(List.of(), 0L, 0, 0, size, 0, true, true, true);
    }
}
