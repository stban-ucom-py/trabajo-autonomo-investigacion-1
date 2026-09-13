package py.edu.ucom.notifications.service;

import org.junit.jupiter.api.Test;
import py.edu.ucom.notifications.domain.Channel;
import py.edu.ucom.notifications.domain.NotificationRequest;
import py.edu.ucom.notifications.domain.Recipient;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationValidatorTest {
    private final NotificationValidator validator = new NotificationValidator();
    private final Recipient recipient = new Recipient("student@example.com", "+595981000000", "device-123");

    @Test void acceptsAllCompleteChannels() {
        var request = new NotificationRequest("order-1", "Pedido", "Listo", recipient,
                List.of(Channel.EMAIL, Channel.SMS, Channel.PUSH));
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test void rejectsMissingExternalId() {
        var request = new NotificationRequest(" ", "Pedido", "Listo", recipient, List.of(Channel.EMAIL));
        assertThatThrownBy(() -> validator.validate(request)).hasMessage("external_id es obligatorio");
    }

    @Test void rejectsEmptyChannelList() {
        var request = new NotificationRequest("order-1", "Pedido", "Listo", recipient, List.of());
        assertThatThrownBy(() -> validator.validate(request)).hasMessageContaining("al menos un canal");
    }

    @Test void rejectsMissingDestinationForSelectedChannel() {
        var request = new NotificationRequest("order-1", "Pedido", "Listo",
                new Recipient(null, "+595981000000", null), List.of(Channel.EMAIL));
        assertThatThrownBy(() -> validator.validate(request)).hasMessageContaining("recipient.email");
    }

    @Test void rejectsDuplicateChannels() {
        var request = new NotificationRequest("order-1", "Pedido", "Listo", recipient,
                List.of(Channel.SMS, Channel.SMS));
        assertThatThrownBy(() -> validator.validate(request)).hasMessageContaining("repetidos");
    }
}
