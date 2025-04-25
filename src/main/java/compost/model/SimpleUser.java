package compost.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.telegram.telegrambots.meta.api.objects.User;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleUser {

  private final Long id;
  private final String username;
  private final String firstName;
  private final String lastName;
  private final int messageCount;

  @JsonCreator
  public SimpleUser(
      @JsonProperty("id") Long id,
      @JsonProperty("username") String username,
      @JsonProperty("firstName") String firstName,
      @JsonProperty("lastName") String lastName,
      @JsonProperty("messageCount") int messageCount
  ) {
    this.id = id;
    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
    this.messageCount = messageCount;
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

  public SimpleUser withIncrementedMessageCount() {
    return new SimpleUser(id, username, firstName, lastName, messageCount + 1);
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
