package in.bachatsetu.backend.admin.interfaces.rest.dto.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

@Schema(description = "Partial update: only the feature keys present in 'flags' are changed.")
public record UpdateFeatureFlagsRequest(@NotEmpty Map<String, Boolean> flags) {

    public UpdateFeatureFlagsRequest {
        flags = flags == null ? null : Map.copyOf(flags);
    }
}
