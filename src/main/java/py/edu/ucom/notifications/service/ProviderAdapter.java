package py.edu.ucom.notifications.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import py.edu.ucom.notifications.domain.ChannelMessage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ProviderAdapter {
    private final ObjectMapper mapper;

    public ProviderAdapter(ObjectMapper mapper) { this.mapper = mapper; }

    public String toProviderPayload(ChannelMessage message) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("correlationId", message.notificationId());
        payload.put("to", message.destination());
        switch (message.channel()) {
            case EMAIL -> {
                payload.put("subject", message.subject());
                payload.put("html", "<p>" + escapeHtml(message.content()) + "</p>");
            }
            case SMS -> payload.put("text", truncate(message.content(), 160));
            case PUSH -> {
                payload.put("title", message.subject());
                payload.put("body", truncate(message.content(), 240));
            }
        }
        return mapper.writeValueAsString(payload);
    }

    public String send(ChannelMessage message) {
        if (message.destination().toLowerCase().contains("fail"))
            throw new ProviderException("El proveedor simulado rechazó el destino");
        return "provider-" + UUID.randomUUID();
    }

    private String truncate(String value, int size) {
        return value.length() <= size ? value : value.substring(0, size);
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
