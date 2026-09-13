package py.edu.ucom.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.main.Main;
import py.edu.ucom.notifications.routes.NotificationRoutes;
import py.edu.ucom.notifications.service.NotificationRepository;

public final class NotificationApplication {
    private NotificationApplication() { }

    public static void main(String[] args) throws Exception {
        String brokerUrl = env("BROKER_URL", "tcp://localhost:61616");
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                brokerUrl, env("BROKER_USER", "admin"), env("BROKER_PASSWORD", "admin"));

        HikariConfig db = new HikariConfig();
        db.setJdbcUrl(env("DB_URL", "jdbc:postgresql://localhost:5432/notifications"));
        db.setUsername(env("DB_USER", "notifications"));
        db.setPassword(env("DB_PASSWORD", "notifications"));
        db.setMaximumPoolSize(8);
        HikariDataSource dataSource = new HikariDataSource(db);

        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
        NotificationRepository repository = new NotificationRepository(dataSource, mapper);

        Main main = new Main();
        main.bind("connectionFactory", connectionFactory);
        main.configure().addRoutesBuilder(new NotificationRoutes(repository, mapper));
        try {
            main.run(args);
        } finally {
            dataSource.close();
            connectionFactory.close();
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
