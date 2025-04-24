package compost.bot;

import compost.model.SimpleUser;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MessageBuilder {

  public static String getHelp() {
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

  public static String wrongThreadId(SimpleUser user) {
    return "Бот может отвечать только в теме \"спам для бота\", " + mention(user);
  }

  public static String unknownCommand() {
    return "Неизвестная команда, падаван \uD83D\uDC7E! Напиши /help для списка команд.";
  }

  public static String noActiveUser() {
    return "Нет данных об активности.";
  }

  public static String noTags() {
    return "❌ Пока нет добавленных тегов.";
  }

  public static String noUsersInChat() {
    return "Пока никого не видел в чате. Напишите что-нибудь, чтобы я вас запомнил (суки)!";
  }

  public static String missingTagArg() {
    return "❗ Укажи тег после команды. Пример: /addtag #важно";
  }

  public static String missingTagToDelete() {
    return "❗ Укажи тег, который нужно удалить. Пример: /deltag #важно";
  }

  public static String invalidTagFormat() {
    return "❗ Тег должен начинаться с #. Пример: /addtag #вопрос";
  }

  public static String tagExists(String tag) {
    return "⏳ Такой тег уже есть: " + tag;
  }

  public static String tagAdded(String tag) {
    return "✅ Тег " + tag + " добавлен!";
  }

  public static String tagDeleted(String tag) {
    return "🗑️ Тег " + tag + " удалён.";
  }

  public static String tagNotFound(String tag) {
    return "⚠️ Такого тега нет: " + tag;
  }

  public static String tagList(Set<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return noTags();
    }
    StringBuilder sb = new StringBuilder("🏷️ Список тегов:\n");
    for (String tag : tags) {
      sb.append(tag).append("\n");
    }
    return sb.toString();
  }

  public static String mention(SimpleUser user) {
    if (user.getId() != null) {
      return "@" + user.getUsername();
    } else {
      String name = (user.getFirstName() != null ? user.getFirstName() : "??") +
          (user.getLastName() != null ? " " + user.getLastName() : "");
      return "<a href=\"tg://user?id=" + user.getId() + "\">" + name + "</a>";
    }
  }

  public static String topUsers(List<SimpleUser> users, Integer length) {
    StringBuilder sb = new StringBuilder("🔥 Топ активных навозников:\n");
    for (int i = 0; i < Math.min(length, users.size()); i++) {
      var u = users.get(i);
      sb.append(i + 1)
          .append(". ")
          .append(mention(u))
          .append(" — ")
          .append(u.getMessageCount())
          .append(" сообщений\n");
    }
    return sb.toString();
  }

  public static String mentionAll(Map<Long, SimpleUser> users) {
    StringBuilder sb = new StringBuilder("🔔 Призыв всех навозников:\n");
    for (SimpleUser user : users.values()) {
      sb.append(mention(user)).append(" ");
    }
    return sb.toString();
  }

}
