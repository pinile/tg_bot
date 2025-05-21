package compost.util;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface TelegramSender {
  Message execute(BotApiMethod<Message> method) throws TelegramApiException;
}