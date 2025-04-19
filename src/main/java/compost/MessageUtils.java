package compost;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class MessageUtils {

  private final CodeCompostInspectorBot bot;

  public MessageUtils(CodeCompostInspectorBot bot) {
    this.bot = bot;
  }

  public void sendText(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    message.enableHtml(true);
    try {
      bot.execute(message);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  public static String getHelpMessage() {
    return """
        🤖 CompostInspectorBot 🤖
        
        🎯 Философия бота:
        
        Если баг нельзя воспроизвести - значит, его нет.
        Если хэштег не добавлен - значит, это не баг, а фича.
        Если все молчат - значит, пора писать /all
        
        📌 Доступные команды:
        /help - Справка... 
        /all - Поднимает всех из-под тестовых стендов (включая того, кто спит в углу). 👹💤
        /tags - Список хэштегов, которые вы все равно не используете. #опятьэтоткостыль
        /addtag #тег - Добавить хэштег, чтобы задокументировать бардак. 📌
        /deltag #тег - Удалить хештег ➖
        /top - Показать самых активных ⚔️
        /panic - Создать видимость работы
        """;
  }

  public static String enablePanic() {
    return """
        🚨 PANIC MODE ACTIVATED 🚨
        
        Создание задач в Jira...
        ✅ BUG-124: "Ничего не работает, но работает"
        ✅ TASK-923: "Выделить личного водителя"
        ✅ TASK-777: "Изучение турецкого плагина AIO Tests"
        ✅ EPIC-932: "ПМ спалил, что ты вкатун"
        ✅ TASK-031: "Притвориться, что ты в отпуске"
        ✅ TASK-032: "Созвон на 3 часа без повестки"
        ✅ TASK-034: "Открыть Notion и просто смотреть на него"
        """;
  }

  public static String unknownCommandMessage() {
    return "Неизвестная команда, падаван \uD83D\uDC7E! Напиши /help для списка команд.";
  }

  public static String noActiveUserMessage() {
    return "Нет данных об активности.";
  }

  public static String noTagsMessage() {
    return "❌ Пока нет добавленных тегов.";
  }

  public static String noUsersInChatMessage() {
    return "Пока никого не видел в чате. Напишите что-нибудь, чтобы я вас запомнил (суки)!";
  }

}
