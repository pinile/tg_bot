package compost.util;

import java.util.regex.Pattern;

public class Constants {

  public static final int ALLOWED_THREAD_ID = 14282;
  public static final String MONGO_CONNECTION_STRING = System.getenv("MONGO_CONNECTION_STRING");
  public static final String MONGO_DATABASE_NAME = System.getenv("MONGO_DATABASE_NAME");
  public static final String BOT_TOKEN = System.getenv("BOT_TOKEN");
  public static final Pattern TAG_PATTERN = Pattern.compile("#[\\p{L}0-9\\-/]{2,20}");

  public enum TagOperationResult {
    SUCCESS,
    ALREADY_EXISTS,
    INVALID_FORMAT,
    MISSING_TAG,
    TAG_NOT_FOUND
  }


}