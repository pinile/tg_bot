package compost.util;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

public class Constants {

  public static final int ALLOWED_THREAD_ID = 14282;
  public static final Pattern TAG_PATTERN = Pattern.compile("#[\\p{L}0-9_/.,:'\\-()$*=]{2,50}");

  public enum TagOperationResult {
    SUCCESS,
    ALREADY_EXISTS,
    INVALID_FORMAT,
    MISSING_TAG,
    TAG_NOT_FOUND,
    UPDATED_DESCRIPTION,
    CLEARED_DESCRIPTION
  }

  public enum BotCommand {
    ADDTAG("/addtag"),
    DELTAG("/deltag"),
    TAGS("/tags"),
    HELP("/help"),
    ALL("/all"),
    TOP("/top"),
    PANIC("/panic");

    private final String command;

    BotCommand(String command) {
      this.command = command;
    }

    public String getCommand() {
      return command;
    }

    //для тестов
    public String getCommandWithArg(String arg) {
      return getCommand() + " " + arg;
    }

    public static Optional<BotCommand> fromString(String command) {
      return Arrays.stream(values())
          .filter(cmd -> cmd.command.equalsIgnoreCase(command))
          .findFirst();
    }
  }

  public enum CaseType {
    SUCCESS,
    INVALID_FORMAT
  }
}