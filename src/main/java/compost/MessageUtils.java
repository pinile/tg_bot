package compost;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class MessageUtils {

  private final CodeCompostInspectorBot bot;

  public MessageUtils(CodeCompostInspectorBot bot) {
    this.bot = bot;
  }

  public void sendText(Long chatId, String text) {
    send(chatId, text, true);
  }

  public void sendText(Long chatId, String text, boolean enableHtml) {
    send(chatId, text, enableHtml);
  }

  public void send(Long chatId, String text, boolean enableHtml) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    message.enableHtml(enableHtml);
    try {
      bot.execute(message);
    } catch (TelegramApiException e) {
      e.printStackTrace(); // можешь заменить на логгер
    }
  }
}