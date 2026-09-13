package py.edu.ucom.notifications.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import py.edu.ucom.notifications.domain.ApiResponse;
import py.edu.ucom.notifications.domain.ChannelMessage;
import py.edu.ucom.notifications.domain.NotificationRequest;
import py.edu.ucom.notifications.service.ChannelSplitter;
import py.edu.ucom.notifications.service.NotificationRepository;
import py.edu.ucom.notifications.service.NotificationValidator;
import py.edu.ucom.notifications.service.ProviderAdapter;
import py.edu.ucom.notifications.service.ProviderException;
import py.edu.ucom.notifications.service.ValidationException;

import java.util.UUID;

public class NotificationRoutes extends RouteBuilder {
    public static final String ID_PROPERTY = "notificationId";
    private final NotificationRepository repository;
    private final NotificationValidator validator;
    private final ChannelSplitter splitter;
    private final ProviderAdapter provider;
    private final ObjectMapper mapper;

    public NotificationRoutes(NotificationRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = new NotificationValidator();
        this.splitter = new ChannelSplitter();
        this.provider = new ProviderAdapter(mapper);
    }

    @Override
    public void configure() {
        onException(ValidationException.class)
                .handled(true)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .process(e -> e.getMessage().setBody(mapper.writeValueAsString(
                        new ApiResponse(null, null, "REJECTED", e.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class).getMessage()))));

        onException(ProviderException.class)
                .maximumRedeliveries(2)
                .redeliveryDelay(500)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .handled(true)
                .process(e -> {
                    ChannelMessage message = e.getMessage().getBody(ChannelMessage.class);
                    Exception cause = e.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    repository.markFailed(message, cause.getMessage());
                })
                .marshal().json()
                .to("jms:queue:notifications.dlq");

        onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "notifications.error", "Error inesperado: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setBody(constant("{\"status\":\"ERROR\",\"message\":\"Error interno de integración\"}"));

        from("netty-http:http://0.0.0.0:8080/api/notifications?httpMethodRestrict=POST")
                .routeId("http-notification-intake")
                .unmarshal().json(NotificationRequest.class)
                .process(e -> validator.validate(e.getMessage().getBody(NotificationRequest.class)))
                .process(e -> {
                    NotificationRequest request = e.getMessage().getBody(NotificationRequest.class);
                    UUID id = repository.create(request);
                    e.setProperty(ID_PROPERTY, id);
                    e.getMessage().setHeader("NotificationId", id.toString());
                    e.getMessage().setHeader("ExternalId", request.externalId());
                })
                .marshal().json()
                .to("jms:queue:notifications.in?exchangePattern=InOnly")
                .process(e -> e.getMessage().setBody(mapper.writeValueAsString(new ApiResponse(
                        e.getProperty(ID_PROPERTY, UUID.class), e.getMessage().getHeader("ExternalId", String.class),
                        "QUEUED", "Notificación aceptada para procesamiento"))))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(202))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"));

        from("netty-http:http://0.0.0.0:8080/api/notifications?httpMethodRestrict=GET")
                .routeId("http-notification-status")
                .process(e -> {
                    String rawId = e.getMessage().getHeader("id", String.class);
                    if (rawId == null) throw new ValidationException("El parámetro id es obligatorio");
                    String json;
                    try { json = repository.findAsJson(UUID.fromString(rawId)); }
                    catch (IllegalArgumentException ex) { throw new ValidationException("El id no es un UUID válido"); }
                    if (json == null) {
                        e.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
                        json = "{\"status\":\"NOT_FOUND\",\"message\":\"Notificación inexistente\"}";
                    }
                    e.getMessage().setBody(json);
                    e.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json");
                });

        from("jms:queue:notifications.in")
                .routeId("split-notification-by-channel")
                .unmarshal().json(NotificationRequest.class)
                .process(e -> e.setProperty(ID_PROPERTY,
                        UUID.fromString(e.getMessage().getHeader("NotificationId", String.class))))
                .split(method(splitter, "split(${body}, ${exchangeProperty.notificationId})"))
                    .parallelProcessing()
                    .process(e -> repository.registerDelivery(e.getMessage().getBody(ChannelMessage.class)))
                    .choice()
                        .when(simple("${body.channel} == 'EMAIL'"))
                            .marshal().json().to("jms:queue:notifications.email")
                        .when(simple("${body.channel} == 'SMS'"))
                            .marshal().json().to("jms:queue:notifications.sms")
                        .when(simple("${body.channel} == 'PUSH'"))
                            .marshal().json().to("jms:queue:notifications.push")
                    .end()
                .end();

        configureProvider("EMAIL", "jms:queue:notifications.email", "email-provider-adapter");
        configureProvider("SMS", "jms:queue:notifications.sms", "sms-provider-adapter");
        configureProvider("PUSH", "jms:queue:notifications.push", "push-provider-adapter");
    }

    private void configureProvider(String channel, String endpoint, String routeId) {
        from(endpoint)
                .routeId(routeId)
                .unmarshal().json(ChannelMessage.class)
                .setProperty("originalMessage", body())
                .process(e -> e.setProperty("providerPayload",
                        provider.toProviderPayload(e.getMessage().getBody(ChannelMessage.class))))
                .log("ADAPTADOR " + channel + " payload=${exchangeProperty.providerPayload}")
                .process(e -> {
                    ChannelMessage message = e.getMessage().getBody(ChannelMessage.class);
                    String providerId = provider.send(message);
                    repository.markSent(message, providerId);
                    e.getMessage().setBody(providerId);
                })
                .log("ENVÍO " + channel + " completado: ${body}");
    }
}
