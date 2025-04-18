package compost;

import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
  public static void main(String[] args) {
    Dotenv dotenv = Dotenv.load();
    String botToken = dotenv.get("BOT_TOKEN");

    try {
      TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
      botsApi.registerBot(new CodeCompostInspectorBot(botToken));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
