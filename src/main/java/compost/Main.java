package compost;

import compost.bot.CodeCompostInspectorBot;
import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {

  public static void main(String[] args) {

    String botToken = System.getenv("BOT_TOKEN");
    if (botToken == null || botToken.isEmpty()) {
      Dotenv dotenv = Dotenv.load();
      botToken = dotenv.get("BOT_TOKEN");
    }

    try {
      TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
      botsApi.registerBot(new CodeCompostInspectorBot(botToken));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
