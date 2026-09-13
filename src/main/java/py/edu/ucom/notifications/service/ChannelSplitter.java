package py.edu.ucom.notifications.service;

import py.edu.ucom.notifications.domain.Channel;
import py.edu.ucom.notifications.domain.ChannelMessage;
import py.edu.ucom.notifications.domain.NotificationRequest;
import java.util.List;
import java.util.UUID;

public class ChannelSplitter {
    public List<ChannelMessage> split(NotificationRequest request, UUID id) {
        return request.channels().stream().map(channel -> new ChannelMessage(
                id, request.externalId(), channel, destination(request, channel),
                request.subject(), request.content())).toList();
    }

    private String destination(NotificationRequest request, Channel channel) {
        return switch (channel) {
            case EMAIL -> request.recipient().email();
            case SMS -> request.recipient().phone();
            case PUSH -> request.recipient().deviceToken();
        };
    }
}
