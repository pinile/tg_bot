package compost.storage;

import compost.model.SimpleUser;
import java.util.Collection;

public interface UserRepository {
  void saveUser(Long chatId, SimpleUser user);
  SimpleUser getUser(Long chatId, Long userId);
  Collection<SimpleUser> getAllUsers(Long chatId);
  void incrementMessageCount(Long chatId, Long userId);
  void load();    // загрузка из хранилища
  void persist(); // сохранение из хранилища
}
