package py.edu.ucom.notifications.domain;

import java.util.UUID;

public record ChannelMessage(
        UUID notificationId,
        String externalId,
        Channel channel,
        String destination,
        String subject,
        String content) {
}
