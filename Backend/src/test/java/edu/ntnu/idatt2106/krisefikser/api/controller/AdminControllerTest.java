package edu.ntnu.idatt2106.krisefikser.api.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ntnu.idatt2106.krisefikser.api.controller.admin.AdminController;
import edu.ntnu.idatt2106.krisefikser.api.dto.auth.LoginResponse;
import edu.ntnu.idatt2106.krisefikser.api.dto.auth.TwoFactorVerifyRequest;
import edu.ntnu.idatt2106.krisefikser.api.dto.user.UserResponseDto;
import edu.ntnu.idatt2106.krisefikser.api.dto.user.admin.AdminInviteRequest;
import edu.ntnu.idatt2106.krisefikser.api.dto.user.admin.AdminSetupRequest;
import edu.ntnu.idatt2106.krisefikser.persistance.enums.Role;
import edu.ntnu.idatt2106.krisefikser.service.admin.AdminInvitationService;
import edu.ntnu.idatt2106.krisefikser.service.auth.AuthService;
import edu.ntnu.idatt2106.krisefikser.service.auth.TwoFactorService;
import edu.ntnu.idatt2106.krisefikser.service.user.UserService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for the AdminController class.
 */

class AdminControllerTest {

  private MockMvc mockMvc;
  private AdminInvitationService adminInvitationService;
  private TwoFactorService twoFactorService;
  private AuthService authService;
  private ObjectMapper objectMapper;
  private UserService userService;

  @BeforeEach
  void setUp() {
    // Create mocks without MockBean annotation
    adminInvitationService = mock(AdminInvitationService.class);
    twoFactorService = mock(TwoFactorService.class);
    authService = mock(AuthService.class);
    userService = mock(UserService.class);
    objectMapper = new ObjectMapper();

    // Setup controller with mocked services
    AdminController adminController = new AdminController(
        adminInvitationService, twoFactorService, authService, userService);

    // Setup MockMvc with the controller
    mockMvc = MockMvcBuilders
        .standaloneSetup(adminController)
        .build();
  }

  @Test
  void inviteAdmin_shouldSendInvitation() throws Exception {
    // Arrange
    AdminInviteRequest request = new AdminInviteRequest();
    request.setEmail("admin@example.com");
    request.setFullName("Admin User");

    doNothing().when(adminInvitationService).createAdminInvitation(anyString(), anyString());

    // Act & Assert
    mockMvc.perform(post("/api/admin/invite")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Admin invitation sent successfully"));
  }

  @Test
  void setupAdmin_withValidRequest_shouldSetupAccount() throws Exception {
    // Arrange
    AdminSetupRequest request = new AdminSetupRequest();
    request.setToken("valid-token");
    request.setPassword("StrongP@ss1");

    doNothing().when(adminInvitationService).completeAdminSetup(anyString(), anyString());

    // Act & Assert
    mockMvc.perform(post("/api/admin/setup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Admin account setup completed"));
  }

  @Test
  void setupAdmin_withInvalidRequest_shouldReturnBadRequest() throws Exception {
    // Arrange
    AdminSetupRequest request = new AdminSetupRequest();
    request.setToken("invalid-token");
    request.setPassword("weak");

    // Use doThrow instead of when for void methods
    doThrow(new IllegalArgumentException("Invalid token or password"))
        .when(adminInvitationService).completeAdminSetup(anyString(), anyString());

    // Act & Assert
    mockMvc.perform(post("/api/admin/setup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Invalid token or password"));
  }

  @Test
  void generateTwoFactorCode_shouldSendCode() throws Exception {
    // Arrange
    Map<String, String> request = Map.of("email", "admin@example.com");

    // Mock the return value with a valid OTP code
    when(twoFactorService.generateAndSendOtp(anyString())).thenReturn("123456");

    // Act & Assert
    mockMvc.perform(post("/api/admin/login/2fa/generate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("2FA code sent to your email"));
  }

  @Test
  void verifyTwoFactor_withValidCode_shouldReturnToken() throws Exception {
    // Arrange
    TwoFactorVerifyRequest request = new TwoFactorVerifyRequest();
    request.setEmail("admin@example.com");
    request.setOtp("123456");

    LoginResponse loginResponse = new LoginResponse("jwt-token");
    when(authService.verify2Fa(anyString(), anyString())).thenReturn(loginResponse);

    // Act & Assert
    mockMvc.perform(post("/api/admin/login/2fa/verify")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt-token"))
        .andExpect(jsonPath("$.message").value("2FA verification successful"));
  }

  @Test
  void verifyTwoFactor_withInvalidCode_shouldReturnBadRequest() throws Exception {
    // Arrange
    TwoFactorVerifyRequest request = new TwoFactorVerifyRequest();
    request.setEmail("admin@example.com");
    request.setOtp("invalid");

    when(authService.verify2Fa(anyString(), anyString()))
        .thenThrow(new IllegalArgumentException("Invalid verification code"));

    // Act & Assert
    mockMvc.perform(post("/api/admin/login/2fa/verify")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Invalid verification code"));
  }

  @Test
  void initiateAdminPasswordReset_shouldResetPassword_whenRequestIsValid() throws Exception {
    // Arrange
    Map<String, String> request = Map.of("email", "admin@example.com");

    doNothing().when(authService).initiatePasswordReset(anyString());

    // Act & Assert
    mockMvc.perform(post("/api/admin/reset-password/initiate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Password reset email sent successfully"));
  }

  @Test
  void initiateAdminPasswordReset_shouldReturnBadRequest_whenEmailIsInvalid() throws Exception {
    // Arrange
    Map<String, String> request = Map.of("email", "invalid@example.com");

    doThrow(new IllegalArgumentException("No user registered with that email."))
        .when(authService).initiateAdminPasswordReset(anyString());

    // Act & Assert
    mockMvc.perform(post("/api/admin/reset-password/initiate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("No user registered with that email."));
  }

  @Test
  void getAllAdmins_shouldReturnAdminList() throws Exception {
    // Arrange
    List<UserResponseDto> adminList = Arrays.asList(
        new UserResponseDto("1L", "admin1@example.com", "Admin One", null, Role.ADMIN),
        new UserResponseDto("2L", "admin2@example.com", "Admin Two", null, Role.ADMIN)
    );

    when(userService.getAllAdmins()).thenReturn(adminList);

    // Act & Assert
    mockMvc.perform(get("/api/admin"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].email").value("admin1@example.com"))
        .andExpect(jsonPath("$[1].email").value("admin2@example.com"));
  }

  @Test
  void deleteAdmin_shouldDeleteAdmin_whenIdIsValid() throws Exception {
    String adminId = "1L";
    Map<String, String> requestBody = Map.of("adminId", adminId);
    ObjectMapper objectMapper = new ObjectMapper();
    String requestJson = objectMapper.writeValueAsString(requestBody);

    doNothing().when(adminInvitationService).deleteAdmin(adminId);

    mockMvc.perform(post("/api/admin/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Admin deleted successfully"));
  }

  @Test
  void deleteAdmin_shouldReturnBadRequest_whenIdIsInvalid() throws Exception {
    String invalidId = "999L";
    Map<String, String> requestBody = Map.of("adminId", invalidId);
    ObjectMapper objectMapper = new ObjectMapper();
    String requestJson = objectMapper.writeValueAsString(requestBody);

    doThrow(new IllegalArgumentException("Admin user not found"))
        .when(adminInvitationService).deleteAdmin(invalidId);

    mockMvc.perform(post("/api/admin/delete") // Added the leading slash here
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Admin user not found"));
  }
}

