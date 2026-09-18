package com.pawpass.pet.dto;

import com.pawpass.pet.domain.PetSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record PetRequest(
        @NotBlank String name,
        @NotBlank String species,
        @NotBlank String breed,
        @Positive Double weight,
        @PastOrPresent LocalDate birthDate, // 2026-09-18 추가, 선택 입력
        PetSize size,
        Boolean hasCarrier, // 이동장/케이지
        Boolean hasLeash, // 목줄/하네스
        Boolean hasMuzzle, // 입마개 (2026-09-13 추가)
        Boolean hasWasteBags, // 배변봉투
        Boolean hasStroller, // 유모차/웨건
        Boolean hasDiaper // 기저귀/매너벨트
) {
}
