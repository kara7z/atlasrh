package com.example.atlasrh.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Completed calendar years and remaining months since hire; leftover days omitted. Future hires return zeros.")
public record SeniorityDto(int years, int months) {
}
