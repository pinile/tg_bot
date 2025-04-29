package compost.util;

public class Constants {
  public static final int ALLOWED_THREAD_ID = 14282;
  public static final String TAGS_FILE = "tags.txt";
  public static final String MONGO_CONNECTION_STRING = System.getenv("MONGO_CONNECTION_STRING");
  public static final String MONGO_DATABASE_NAME = System.getenv("MONGO_DATABASE_NAME");
  public static final String BOT_TOKEN = System.getenv("BOT_TOKEN");
}