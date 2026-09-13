package py.edu.ucom.notifications.service;

import org.junit.jupiter.api.Test;
import py.edu.ucom.notifications.domain.Channel;
import py.edu.ucom.notifications.domain.NotificationRequest;
import py.edu.ucom.notifications.domain.Recipient;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class ChannelSplitterTest {
    @Test void createsOneCorrelatedMessagePerChannel() {
        UUID id = UUID.randomUUID();
        var request = new NotificationRequest("order-7", "Pedido", "Entregado",
                new Recipient("a@b.com", "+595981111111", "token-x"),
                List.of(Channel.EMAIL, Channel.SMS, Channel.PUSH));

        var parts = new ChannelSplitter().split(request, id);

        assertThat(parts).hasSize(3).allMatch(m -> m.notificationId().equals(id));
        assertThat(parts).extracting("destination")
                .containsExactly("a@b.com", "+595981111111", "token-x");
    }
}
