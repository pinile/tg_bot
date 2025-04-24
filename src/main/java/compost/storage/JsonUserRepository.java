package compost.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import compost.model.SimpleUser;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JsonUserRepository implements UserRepository {

  private final Map<Long, Map<Long, SimpleUser>> groupUsers = new HashMap<>();
  private final ObjectMapper mapper = new ObjectMapper();
  private final File file;

  public JsonUserRepository(String filePath) {
    this.file = new File(filePath);
    load();
  }

  @Override
  public void saveUser(Long chatId, SimpleUser user) {
    groupUsers.computeIfAbsent(chatId, k -> new HashMap<>()).put(user.getId(), user);
  }

  @Override
  public SimpleUser getUser(Long chatId, Long userId) {
    Map<Long, SimpleUser> users = groupUsers.get(chatId);
    return users != null ? users.get(userId) : null;
  }

  @Override
  public Collection<SimpleUser> getAllUsers(Long chatId) {
    return groupUsers.getOrDefault(chatId, Collections.emptyMap()).values();
  }

  @Override
  public void incrementMessageCount(Long chatId, Long userId) {
    SimpleUser user = getUser(chatId, userId);
    if (user != null) {
      user.incrementMessageCount();
    }
  }

  @Override
  public void load() {
    if (!file.exists()) {
      return;
    }
    try {
      Map<Long, Map<Long, SimpleUser>> data = mapper.readValue(file, new TypeReference<>() {
      });
      groupUsers.putAll(data);
    } catch (IOException e) {
      System.out.println("Ошибка при загрузке пользователей: " + e.getMessage());
    }
  }

  @Override
  public void persist() {
    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(file, groupUsers);
    } catch (IOException e) {
      System.out.println("Ошибка при сохранении пользователей: " + e.getMessage());
    }
  }
}
