package compost.service;

import org.telegram.telegrambots.meta.api.objects.User;
import compost.storage.UserRepository;
import compost.model.SimpleUser;
import java.util.Collection;

public class UserService {
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public void handleUser(Long chatId, User telegramUser) {
    SimpleUser existing = userRepository.getUser(chatId, telegramUser.getId());
    if (existing == null) {
      existing = new SimpleUser(telegramUser);
    } else {
      existing.incrementMessageCount();
    }
    userRepository.saveUser(chatId, existing);
  }

  public Collection<SimpleUser> getAllUsers(Long chatId) {
    return userRepository.getAllUsers(chatId);
  }

  public SimpleUser getUser(Long chatId, Long userId) {
    return userRepository.getUser(chatId, userId);
  }

  public void saveUsers() {
    userRepository.persist();
  }

  public void loadUsers() {
    userRepository.load();
  }
}
