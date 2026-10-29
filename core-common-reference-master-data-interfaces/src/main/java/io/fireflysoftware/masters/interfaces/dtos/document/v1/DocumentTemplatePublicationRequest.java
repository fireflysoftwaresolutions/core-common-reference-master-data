package io.fireflysoftware.masters.interfaces.dtos.document.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentTemplatePublicationRequest(
        @NotBlank @Size(max = 20) String version,
        @NotBlank @Size(max = 50) String localeCode) {
}
