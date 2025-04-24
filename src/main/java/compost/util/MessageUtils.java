package compost.util;

import compost.bot.CodeCompostInspectorBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class MessageUtils {

  private final CodeCompostInspectorBot bot;

  public MessageUtils(CodeCompostInspectorBot bot) {
    this.bot = bot;
  }

  public void sendText(Long chatId, Integer threadId, String text) {
    send(chatId, threadId, text, true);
  }

  public void sendText(Long chatId, Integer threadId, String text, boolean enableHtml) {
    send(chatId, threadId, text, enableHtml);
  }

  public void send(Long chatId, Integer threadId, String text, boolean enableHtml) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    message.enableHtml(enableHtml);
    if (threadId != null) {
      message.setMessageThreadId(threadId);
    }
    try {
      bot.execute(message);
    } catch (TelegramApiException e) {
      e.printStackTrace(); //TODO заменить на slf4j
    }
  }
}