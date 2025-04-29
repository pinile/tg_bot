package compost.util;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoConnection {

  private static MongoClient mongoClient;

  private MongoConnection() {
  }

  public static MongoDatabase getDatabase() {
    if (mongoClient == null) {
      synchronized (MongoConnection.class) {
        if (mongoClient == null) {
          mongoClient = MongoClients.create(Constants.MONGO_CONNECTION_STRING);
        }
      }
    }
    return mongoClient.getDatabase(Constants.MONGO_DATABASE_NAME);
  }
}
