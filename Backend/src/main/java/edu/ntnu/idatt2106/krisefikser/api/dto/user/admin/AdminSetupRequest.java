package edu.ntnu.idatt2106.krisefikser.api.dto.user.admin;

/**
 * Data Transfer Object for admin account setup requests. Contains the token and password for
 * completing admin setup.
 */
public class AdminSetupRequest {

  private String token;
  private String password;

  /**
   * Default constructor for AdminSetupRequest.
   */

  public AdminSetupRequest() {
  }

  /**
   * Constructor for AdminSetupRequest.
   *
   * @param token    The token received from the admin invitation email.
   * @param password The password for the new admin account.
   */

  public AdminSetupRequest(String token, String password) {
    this.token = token;
    this.password = password;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}