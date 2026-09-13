package py.edu.ucom.notifications.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import py.edu.ucom.notifications.domain.ChannelMessage;
import py.edu.ucom.notifications.domain.NotificationRequest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class NotificationRepository {
    private final DataSource dataSource;
    private final ObjectMapper mapper;

    public NotificationRepository(DataSource dataSource, ObjectMapper mapper) {
        this.dataSource = dataSource;
        this.mapper = mapper;
    }

    public UUID create(NotificationRequest request) {
        UUID id = UUID.randomUUID();
        String sql = "INSERT INTO notifications(id, external_id, subject, content, status) VALUES (?, ?, ?, ?, 'QUEUED')";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setString(2, request.externalId());
            ps.setString(3, request.subject());
            ps.setString(4, request.content());
            ps.executeUpdate();
            return id;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState()))
                throw new ValidationException("external_id ya fue procesado");
            throw new IllegalStateException("No se pudo guardar la notificación", e);
        }
    }

    public void registerDelivery(ChannelMessage message) {
        String sql = "INSERT INTO deliveries(notification_id, channel, destination, status) VALUES (?, ?, ?, 'QUEUED') " +
                "ON CONFLICT(notification_id, channel) DO NOTHING";
        execute(sql, ps -> {
            ps.setObject(1, message.notificationId());
            ps.setString(2, message.channel().name());
            ps.setString(3, message.destination());
        });
    }

    public void markSent(ChannelMessage message, String providerId) {
        updateDelivery(message, "SENT", providerId, null);
        refreshNotificationStatus(message.notificationId());
    }

    public void markFailed(ChannelMessage message, String error) {
        String sql = "UPDATE deliveries SET status='FAILED', provider_message_id=NULL, error_message=?, " +
                "attempts=3, updated_at=NOW() WHERE notification_id=? AND channel=?";
        execute(sql, ps -> {
            ps.setString(1, error);
            ps.setObject(2, message.notificationId());
            ps.setString(3, message.channel().name());
        });
        refreshNotificationStatus(message.notificationId());
    }

    private void updateDelivery(ChannelMessage message, String status, String providerId, String error) {
        String sql = "UPDATE deliveries SET status=?, provider_message_id=?, error_message=?, attempts=attempts+1, updated_at=NOW() " +
                "WHERE notification_id=? AND channel=?";
        execute(sql, ps -> {
            ps.setString(1, status);
            if (providerId == null) ps.setNull(2, Types.VARCHAR); else ps.setString(2, providerId);
            if (error == null) ps.setNull(3, Types.VARCHAR); else ps.setString(3, error);
            ps.setObject(4, message.notificationId());
            ps.setString(5, message.channel().name());
        });
    }

    private void refreshNotificationStatus(UUID id) {
        String sql = "UPDATE notifications n SET status = CASE " +
                "WHEN EXISTS (SELECT 1 FROM deliveries d WHERE d.notification_id=n.id AND d.status='FAILED') THEN 'PARTIAL_FAILURE' " +
                "WHEN NOT EXISTS (SELECT 1 FROM deliveries d WHERE d.notification_id=n.id AND d.status<>'SENT') THEN 'COMPLETED' " +
                "ELSE 'PROCESSING' END, updated_at=NOW() WHERE id=?";
        execute(sql, ps -> ps.setObject(1, id));
    }

    public String findAsJson(UUID id) {
        String sql = "SELECT n.id, n.external_id, n.subject, n.status, n.created_at, " +
                "COALESCE(json_agg(json_build_object('channel', d.channel, 'destination', d.destination, " +
                "'status', d.status, 'attempts', d.attempts, 'provider_message_id', d.provider_message_id, " +
                "'error', d.error_message) ORDER BY d.id) FILTER (WHERE d.id IS NOT NULL), '[]') deliveries " +
                "FROM notifications n LEFT JOIN deliveries d ON d.notification_id=n.id WHERE n.id=? GROUP BY n.id";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("id", rs.getObject("id"));
                result.put("external_id", rs.getString("external_id"));
                result.put("subject", rs.getString("subject"));
                result.put("status", rs.getString("status"));
                result.put("created_at", rs.getObject("created_at").toString());
                result.put("deliveries", mapper.readTree(rs.getString("deliveries")));
                return mapper.writeValueAsString(result);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo consultar la notificación", e);
        }
    }

    private void execute(String sql, SqlBinder binder) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error de persistencia", e);
        }
    }

    @FunctionalInterface
    private interface SqlBinder { void bind(PreparedStatement statement) throws SQLException; }
}
