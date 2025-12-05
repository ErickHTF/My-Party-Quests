package com.Sprint.Sprint.DTO.Request;

import jakarta.validation.constraints.NotBlank;

public record CreatePartyDTO(
        @NotBlank(message = "Name is mandatory")
        String partyName,

        @NotBlank(message = "Description is mandatory")
        String partyDescription
) {}