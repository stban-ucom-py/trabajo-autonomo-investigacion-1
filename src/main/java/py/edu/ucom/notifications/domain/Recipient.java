package py.edu.ucom.notifications.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Recipient(
        String email,
        String phone,
        @JsonProperty("device_token") String deviceToken) {
}
