package py.edu.ucom.notifications.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record NotificationRequest(
        @JsonProperty("external_id") String externalId,
        String subject,
        String content,
        Recipient recipient,
        List<Channel> channels) {
}
