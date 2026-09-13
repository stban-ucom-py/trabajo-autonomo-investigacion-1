package py.edu.ucom.notifications.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import py.edu.ucom.notifications.domain.Channel;
import py.edu.ucom.notifications.domain.ChannelMessage;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderAdapterTest {
    private final ProviderAdapter adapter = new ProviderAdapter(new ObjectMapper());

    @Test void translatesSmsAndLimitsTextTo160Characters() throws Exception {
        var message = new ChannelMessage(UUID.randomUUID(), "x", Channel.SMS, "+595981",
                "Asunto", "a".repeat(200));
        String payload = adapter.toProviderPayload(message);
        assertThat(new ObjectMapper().readTree(payload).get("text").asText()).hasSize(160);
    }

    @Test void escapesEmailHtml() throws Exception {
        var message = new ChannelMessage(UUID.randomUUID(), "x", Channel.EMAIL, "a@b.com",
                "Asunto", "<script>alert(1)</script>");
        assertThat(adapter.toProviderPayload(message)).contains("&lt;script&gt;");
    }

    @Test void simulatesProviderFailureForFailDestination() {
        var message = new ChannelMessage(UUID.randomUUID(), "x", Channel.EMAIL, "fail@example.com",
                "Asunto", "Mensaje");
        assertThatThrownBy(() -> adapter.send(message)).isInstanceOf(ProviderException.class);
    }
}
