package py.edu.ucom.notifications.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse(
        UUID id,
        @JsonProperty("external_id") String externalId,
        String status,
        String message) {
}
