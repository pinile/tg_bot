package compost.util;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConnection {

  @Value("${MONGO_ROOT_USERNAME}")
  private String username;

  @Value("${MONGO_ROOT_PASSWORD}")
  private String password;

  @Value("${MONGO_DATABASE_NAME}")
  private String databaseName;

  @Value("${MONGO_HOST: mongo}")
  private String host;

  @Value("${MONGO_PORT:27017}")
  private String port;

  @Bean
  public MongoClient mongoClient() {
    String connectionString =
        String.format("mongodb://%s:%s@%s:%s/%s", username, password, host, port, databaseName);
    return MongoClients.create(connectionString);
  }

  @Bean
  public MongoDatabase mongoDatabase(MongoClient mongoClient) {
    return mongoClient.getDatabase(databaseName);
  }
}
