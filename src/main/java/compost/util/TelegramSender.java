package compost.util;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface TelegramSender {
  Message execute(BotApiMethod<Message> method) throws TelegramApiException;
}
