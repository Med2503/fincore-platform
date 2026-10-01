package org.fincore.wealth.position.api.dto;

import org.fincore.wealth.position.api.dto.PositionResponse;

import java.util.List;

public record PositionPageResponse(
        List<PositionResponse> items,
        int page,
        int size,
        boolean hasNext
) {
}