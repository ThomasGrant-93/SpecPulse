package com.specpulse.version;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

public record SpecVersionDTO(
        Long id,
        Long serviceId,
        String versionHash,
        String specVersion,
        String specTitle,
        Long fileSizeBytes,
        Instant pulledAt,
        String specContent
) {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static SpecVersionDTO fromEntity(SpecVersionEntity entity) {
        String contentString = null;
        if (entity.getSpecContent() != null) {
            try {
                // JsonNode is directly serializable by Jackson
                contentString = objectMapper.writeValueAsString(entity.getSpecContent());
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                contentString = entity.getSpecContent().toString();
            }
        }

        return new SpecVersionDTO(
                entity.getId(),
                entity.getService().getId(),
                entity.getVersionHash(),
                entity.getSpecVersion(),
                entity.getSpecTitle(),
                entity.getFileSizeBytes(),
                entity.getPulledAt(),
                contentString
        );
    }

    // Overload without content for API responses (to avoid sending large content)
    public SpecVersionDTO withoutContent() {
        return new SpecVersionDTO(id, serviceId, versionHash, specVersion, specTitle, fileSizeBytes, pulledAt, null);
    }
}
