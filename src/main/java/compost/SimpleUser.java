package compost;

import org.telegram.telegrambots.meta.api.objects.User;

public class SimpleUser {

  public Long id;
  public String username;
  public String firstName;
  public String lastName;
  public int messageCount = 0;

  public SimpleUser(User user) {
    this.id = user.getId();
    this.username = user.getUserName();
    this.firstName = user.getFirstName();
    this.lastName = user.getLastName();
  }
}
