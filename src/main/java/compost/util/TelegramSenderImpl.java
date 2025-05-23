package compost.util;

import compost.bot.CodeCompostInspectorBot;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramSenderImpl implements TelegramSender {

  private final CodeCompostInspectorBot bot;

  public TelegramSenderImpl(@Lazy CodeCompostInspectorBot bot) {
    this.bot = bot;
  }

  @Override
  public Message execute(BotApiMethod<Message> method) throws TelegramApiException {
    return bot.sendMethod(method);
  }
}
