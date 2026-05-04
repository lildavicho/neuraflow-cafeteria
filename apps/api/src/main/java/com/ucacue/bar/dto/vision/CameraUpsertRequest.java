package com.ucacue.bar.dto.vision;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CameraUpsertRequest(
        @NotBlank
        @Size(max = 50)
        String name,

        @NotBlank
        @Size(max = 255)
        String rtspUrl,

        @Size(max = 200)
        String description,

        @Size(max = 50)
        String location,

        Boolean enabled,

        @Size(max = 50)
        String streamPath) {
}
