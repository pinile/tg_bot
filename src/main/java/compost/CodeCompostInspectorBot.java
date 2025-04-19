package compost;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.util.concurrent.ScheduledExecutorService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CodeCompostInspectorBot extends TelegramLongPollingBot {

  private final MessageUtils messageUtils;

  private boolean usersChanged = false; // смотрит изменения по пользователю (считает сообщения)
  private static final String STORAGE_FILE = "users.json";

  private static final String TAGS_FILE = "tags.txt";
  private final Set<String> tags = new HashSet<>();

  private final Map<Long, Map<Long, SimpleUser>> groupUsers = new HashMap<>();
  private final ObjectMapper mapper = new ObjectMapper();

  private final String botToken;

  public CodeCompostInspectorBot(String botToken) {
    this.botToken = botToken;
    loadUsersFromFile();
    loadTagsFromFile();
    this.messageUtils = new MessageUtils(this);

    // шедулер для сохранения users.json раз в минуту
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(this::saveUsersToFile, 60, 60, TimeUnit.SECONDS);
  }

  @Override
  public String getBotUsername() {
    return "codeCompostInspectorBot";
  }

  @Override
  public String getBotToken() {
    return botToken;
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage() && update.getMessage().hasText()) {
      Message message = update.getMessage();
      Long chatId = message.getChatId();

      if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
        saveUser(chatId, message.getFrom());
      }

      String fullText = message.getText().trim();
      if (fullText.startsWith("/")) {
        String command = fullText.split(" ")[0];

        switch (command) {
          case "/help":
            messageUtils.sendText(chatId, MessageUtils.getHelpMessage());
            break;
          case "/all":
            mentionAll(chatId);
            break;
          case "/tags":
            messageUtils.sendText(chatId, getTagsList());
            break;
          case "/top":
            sendTop(chatId);
            break;
          case "/addtag":
            handleAddTag(chatId, fullText);
            break;
          case "/deltag":
            handleDeleteTag(chatId, fullText);
            break;
          case "/panic":
            messageUtils.sendText(chatId, MessageUtils.enablePanic());
            break;
          default:
            messageUtils.sendText(chatId, MessageUtils.unknownCommandMessage());
        }
      }
      // если текст начинается не с / - ничего не делать, сохраняет пользователя.
    }
  }

  private void saveUser(Long chatId, User user) {
    groupUsers.putIfAbsent(chatId, new HashMap<>());
    Map<Long, SimpleUser> chatMap = groupUsers.get(chatId);

    SimpleUser su = chatMap.get(user.getId());
    if (su == null) {
      su = new SimpleUser(user);
      chatMap.put(user.getId(), su);
    }

    su.messageCount++; // счетчик сообщений
    usersChanged = true; // изменяем файл, если были обновления в течении шедулера (60 секунд)
  }

  private void mentionAll(Long chatId) {
    Map<Long, SimpleUser> users = groupUsers.get(chatId);

    if (users == null || users.isEmpty()) {
      messageUtils.sendText(chatId, messageUtils.noUsersInChatMessage());
      return;
    }

    StringBuilder sb = new StringBuilder("🔔 Призыв всех навозников:\n");

    for (SimpleUser user : users.values()) {
      sb.append(user.toMention()).append(" ");
    }

    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(sb.toString());
    message.enableHtml(true);

    try {
      execute(message);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  private String getTagsList() {
    if (tags.isEmpty()) {
      return messageUtils.noTagsMessage();
    }
    StringBuilder sb = new StringBuilder("🏷️ Список тегов:\n");
    for (String tag : tags) {
      sb.append(tag).append("\n");
    }
    return sb.toString();
  }

  private void loadUsersFromFile() {
    try {
      File file = new File(STORAGE_FILE);
      if (file.exists()) {
        Map<Long, Map<Long, SimpleUser>> data = mapper.readValue(file, new TypeReference<>() {
        });
        groupUsers.putAll(data);
      }
    } catch (IOException e) {
      System.out.println("Ошибка при загрузке навозников: " + e.getMessage());
    }
  }

  private void loadTagsFromFile() {
    File file = new File(TAGS_FILE);
    if (!file.exists()) {
      return;
    }

    try (Scanner scanner = new Scanner(file)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine().trim();
        if (!line.isEmpty()) {
          tags.add(line);
        }
      }
    } catch (IOException e) {
      System.out.println("Ошибка при чтении tags.txt: " + e.getMessage());
    }
  }

  private void saveTagsToFile() {
    try (PrintWriter writer = new PrintWriter(new File(TAGS_FILE))) {
      for (String tag : tags) {
        writer.println(tag);
      }
    } catch (IOException e) {
      System.out.println("Ошибка при записи tags.txt: " + e.getMessage());
    }
  }


  private void handleAddTag(Long chatId, String fullText) {
    String[] parts = fullText.split(" ");
    if (parts.length < 2) {
      messageUtils.sendText(chatId, "❗ Укажи тег после команды. Пример: /addtag #важно");
      return;
    }

    String tag = parts[1].trim();
    if (!tag.startsWith("#")) {
      messageUtils.sendText(chatId, "❗ Тег должен начинаться с #. Пример: /addtag #вопрос");
      return;
    }

    if (tags.contains(tag)) {
      messageUtils.sendText(chatId, "⏳ Такой тег уже есть.");
    } else {
      tags.add(tag);
      saveTagsToFile();
      messageUtils.sendText(chatId, "✅ Тег " + tag + " добавлен!");
    }
  }

  private void handleDeleteTag(Long chatId, String fullText) {
    String[] parts = fullText.split(" ");
    if (parts.length < 2) {
      messageUtils.sendText(chatId, "❗ Укажи тег, который нужно удалить. Пример: /deltag #важно");
      return;
    }

    String tag = parts[1].trim();
    if (!tag.startsWith("#")) {
      messageUtils.sendText(chatId, "❗ Тег должен начинаться с #. Пример: /deltag #вопрос");
      return;
    }

    if (tags.remove(tag)) {
      saveTagsToFile();
      messageUtils.sendText(chatId, "🗑️ Тег " + tag + " удалён.");
    } else {
      messageUtils.sendText(chatId, "⚠️ Такого тега нет.");
    }
  }

  private void saveUsersToFile() {
    if (!usersChanged) {
      return; // не сохраняем если ничего не менялось
    }

    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(new File(STORAGE_FILE), groupUsers);
      usersChanged = false; // после успешного сохранения сбрасываем флаг
    } catch (IOException e) {
      System.out.println("Ошибка при сохранении навозников: " + e.getMessage());
    }
  }

  private void sendTop(Long chatId) {
    Map<Long, SimpleUser> users = groupUsers.get(chatId);
    if (users == null || users.isEmpty()) {
      messageUtils.sendText(chatId, MessageUtils.noActiveUserMessage());
      return;
    }

    List<SimpleUser> top = new ArrayList<>(users.values());
    top.sort((a, b) -> Integer.compare(b.messageCount, a.messageCount));

    StringBuilder sb = new StringBuilder("🔥 Топ активных навозников:\n");

    int limit = Math.min(10, top.size());
    for (int i = 0; i < limit; i++) {
      SimpleUser u = top.get(i);
      sb.append(i + 1).append(". ").append(u.toMention())
          .append(" — ").append(u.messageCount).append(" сообщений\n");
    }

    messageUtils.sendText(chatId, sb.toString());
  }

  // Простой сериализуемый пользователь
  public static class SimpleUser {

    public Long id;
    public String username;
    public String firstName;
    public String lastName;
    public int messageCount = 0; //

    public SimpleUser(User user) {
      this.id = user.getId();
      this.username = user.getUserName();
      this.firstName = user.getFirstName();
      this.lastName = user.getLastName();
    }

    public String toMention() {
      if (username != null) {
        return "@" + username;
      } else {
        String name = (firstName != null ? firstName : "??") +
            (lastName != null ? " " + lastName : "");
        return "<a href=\"tg://user?id=" + id + "\">" + name + "</a>";
      }
    }
  }
}