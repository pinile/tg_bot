package compost.util;

import compost.annotation.LoggableCommand;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Утилита для отправки сообщений через Telegram-бота. Предоставляет методы для отправки текстовых
 * сообщений с поддержкой HTML и указания потока (thread).
 */

@Component
@Log4j2
public class MessageUtils {

  private final TelegramSender sender;

  public MessageUtils(TelegramSender sender) {
    this.sender = sender;
  }

  /**
   * Отправка текстового сообщения с включенной по умолчанию поддержкой HTML.
   *
   * @param chatId   Идентификатор чата, куда будет отправлено сообщение.
   * @param threadId Идентификатор потока (topic) в чате. Может быть null, если поток не
   *                 используется.
   * @param text     Текст сообщения.
   */

  public void sendText(Long chatId, Integer threadId, String text) {
    send(chatId, threadId, text, true);
  }

  /**
   * Отправка текстового сообщения с возможностью включения/отключения HTML.
   *
   * @param chatId     Идентификатор чата, куда будет отправлено сообщение.
   * @param threadId   Идентификатор потока (topic) в чате. Может быть null, если поток не
   *                   используется.
   * @param text       Текст сообщения.
   * @param enableHtml Флаг, указывающий, следует ли использовать форматирование HTML в сообщении.
   */
  public void sendText(Long chatId, Integer threadId, String text, boolean enableHtml) {
    send(chatId, threadId, text, enableHtml);
  }

  /**
   * Универсальный метод для отправки сообщений.
   *
   * @param chatId     Идентификатор чата, куда будет отправлено сообщение.
   * @param threadId   Идентификатор потока (topic) в чате. Может быть null, если поток не
   *                   используется.
   * @param text       Текст сообщения.
   * @param enableHtml Флаг, указывающий, следует ли использовать форматирование HTML в сообщении.
   */

  @LoggableCommand
  public void send(Long chatId, Integer threadId, String text, boolean enableHtml) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    message.enableHtml(enableHtml);
    if (threadId != null) {
      message.setMessageThreadId(threadId);
    }
/*

    log.debug("Отправка сообщения: chatId={}, threadId={}, enableHtml={}, text='{}'",
        chatId, threadId, enableHtml, text.replace("\n", "\\n"));
*/

    try {
      sender.execute(message);
    } catch (TelegramApiException e) {
      log.error("Ошибка при отправке сообщения: {}", e.getMessage(), e);
    }
  }
}