package py.edu.ucom.notifications.service;

import py.edu.ucom.notifications.domain.Channel;
import py.edu.ucom.notifications.domain.NotificationRequest;

public class NotificationValidator {
    public void validate(NotificationRequest request) {
        if (request == null) throw new ValidationException("El cuerpo es obligatorio");
        if (blank(request.externalId())) throw new ValidationException("external_id es obligatorio");
        if (blank(request.subject())) throw new ValidationException("subject es obligatorio");
        if (blank(request.content())) throw new ValidationException("content es obligatorio");
        if (request.recipient() == null) throw new ValidationException("recipient es obligatorio");
        if (request.channels() == null || request.channels().isEmpty())
            throw new ValidationException("Debe indicar al menos un canal");
        if (request.channels().stream().distinct().count() != request.channels().size())
            throw new ValidationException("No se permiten canales repetidos");
        for (Channel channel : request.channels()) {
            switch (channel) {
                case EMAIL -> require(request.recipient().email(), "recipient.email");
                case SMS -> require(request.recipient().phone(), "recipient.phone");
                case PUSH -> require(request.recipient().deviceToken(), "recipient.device_token");
            }
        }
    }

    private void require(String value, String field) {
        if (blank(value)) throw new ValidationException(field + " es obligatorio para el canal solicitado");
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
