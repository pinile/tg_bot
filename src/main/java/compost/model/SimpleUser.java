package compost.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.telegram.telegrambots.meta.api.objects.User;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleUser {

  @JsonProperty("id")
  private Long id;

  @JsonProperty("username")
  private String username;

  @JsonProperty("firstName")
  private String firstName;

  @JsonProperty("lastName")
  private String lastName;

  @JsonProperty("messageCount")
  private int messageCount;

  public SimpleUser() {
    // нужен для Jackson
  }

  public SimpleUser(User user) {
    this.id = user.getId();
    this.username = user.getUserName();
    this.firstName = user.getFirstName();
    this.lastName = user.getLastName();
    this.messageCount = 0;
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

  public int getMessageCount() {
    return messageCount;
  }

  public void incrementMessageCount() {
    this.messageCount++;
  }

  @Override
  public String toString() {
    return "SimpleUser{" +
        "id=" + id +
        ", username='" + username + '\'' +
        ", firstName='" + firstName + '\'' +
        ", lastName='" + lastName + '\'' +
        ", messageCount=" + messageCount +
        '}';
  }
}