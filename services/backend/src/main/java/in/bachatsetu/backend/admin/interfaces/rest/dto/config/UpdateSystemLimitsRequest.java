package in.bachatsetu.backend.admin.interfaces.rest.dto.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

@Schema(description = "Partial update: only the limit keys present in 'limits' are changed.")
public record UpdateSystemLimitsRequest(@NotEmpty Map<String, Long> limits) {

    public UpdateSystemLimitsRequest {
        limits = limits == null ? null : Map.copyOf(limits);
    }
}
