package compost.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.telegram.telegrambots.meta.api.objects.User;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleUser {

  private final Long id;
  private final String username;
  private final String firstName;
  private final String lastName;

  public SimpleUser(Long id, String username, String firstName, String lastName) {
    this.id = id;
    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public SimpleUser(User user) {
    this.id = user.getId();
    this.username = user.getUserName();
    this.firstName = user.getFirstName();
    this.lastName = user.getLastName();
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  @Override
  public String toString() {
    return "SimpleUser{" +
        "id=" + id +
        ", username='" + username + '\'' +
        ", firstName='" + firstName + '\'' +
        ", lastName='" + lastName + '\'' +
        '}';
  }
}
