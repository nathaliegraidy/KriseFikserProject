package edu.ntnu.idatt2106.krisefikser.service.household;

import edu.ntnu.idatt2106.krisefikser.api.dto.position.PositionResponseDto;
import edu.ntnu.idatt2106.krisefikser.api.dto.household.CreateHouseholdRequestDto;
import edu.ntnu.idatt2106.krisefikser.api.dto.household.EditHouseholdRequestDto;
import edu.ntnu.idatt2106.krisefikser.api.dto.household.HouseholdBasicResponseDto;
import edu.ntnu.idatt2106.krisefikser.api.dto.household.HouseholdResponseDto;
import edu.ntnu.idatt2106.krisefikser.api.dto.notification.NotificationDto;
import edu.ntnu.idatt2106.krisefikser.api.dto.unregisteredmembers.EditMemberDto;
import edu.ntnu.idatt2106.krisefikser.api.dto.unregisteredmembers.UnregisteredMemberHouseholdAssignmentRequestDto;
import edu.ntnu.idatt2106.krisefikser.api.dto.unregisteredmembers.UnregisteredMemberResponseDto;
import edu.ntnu.idatt2106.krisefikser.api.dto.user.UserHouseholdAssignmentRequestDto;
import edu.ntnu.idatt2106.krisefikser.api.dto.user.UserResponseDto;
import edu.ntnu.idatt2106.krisefikser.persistance.entity.household.Household;
import edu.ntnu.idatt2106.krisefikser.persistance.entity.unregisteredhouseholdmember.UnregisteredHouseholdMember;
import edu.ntnu.idatt2106.krisefikser.persistance.entity.user.User;
import edu.ntnu.idatt2106.krisefikser.persistance.enums.NotificationType;
import edu.ntnu.idatt2106.krisefikser.persistance.repository.household.HouseholdRepository;
import edu.ntnu.idatt2106.krisefikser.persistance.repository.membershiprequest.MembershipRequestRepository;
import edu.ntnu.idatt2106.krisefikser.persistance.repository.unregisteredhouseholdmember.UnregisteredHouseholdMemberRepository;
import edu.ntnu.idatt2106.krisefikser.persistance.repository.user.UserRepository;
import edu.ntnu.idatt2106.krisefikser.service.notification.NotificationService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing household-related operations. This service handles the creation,
 * management, and modification of households, including validation and persistence operations.
 */
@Service
public class HouseholdService {

  /**
   * Logger for this class to log important events and errors.
   */
  private static final Logger logger = LoggerFactory.getLogger(HouseholdService.class);

  /**
   * Repository for household entity operations.
   */
  private final HouseholdRepository householdRepository;

  /**
   * Notification service for sending notifications.
   */
  private final NotificationService notificationService;

  /**
   * Repository for membership request entity operations.
   */
  private final MembershipRequestRepository membershipRequestRepository;

  /**
   * Repository for user entity operations.
   */
  private final UserRepository userRepository;

  /**
   * Repository for unregistered household member entity operations.
   */
  private final UnregisteredHouseholdMemberRepository unregisteredHouseholdMemberRepository;

  /**
   * Constructs a new HouseholdService with required repositories.
   *
   * @param householdRepository                   Repository for household operations.
   * @param notificationService                   the notification service
   * @param userRepository                        Repository for user operations.
   * @param unregisteredHouseholdMemberRepository Repository for unregistered household member
   *                                              <p>
   *                                              operations.
   */
  public HouseholdService(HouseholdRepository householdRepository,
      NotificationService notificationService,
      MembershipRequestRepository membershipRequestRepository, UserRepository userRepository,
      UnregisteredHouseholdMemberRepository unregisteredHouseholdMemberRepository) {
    this.householdRepository = householdRepository;
    this.notificationService = notificationService;
    this.membershipRequestRepository = membershipRequestRepository;
    this.userRepository = userRepository;
    this.unregisteredHouseholdMemberRepository = unregisteredHouseholdMemberRepository;
    logger.info("HouseholdService initialized");
  }

  /**
   * Creates a new household with the given name, address, and creator's user ID. The creator
   * automatically becomes the owner of the household.
   *
   * @param request DTO containing household name, address, and owner ID.
   * @throws IllegalArgumentException if a household with the same name already exists.
   * @throws IllegalArgumentException if the specified owner ID does not match any existing user.
   */
  public void createHousehold(CreateHouseholdRequestDto request) {
    logger.info("Creating household with name: {}", request.getName());

    String householdId = generateHouseholdId();
    logger.debug("Generated household ID: {}", householdId);

    Household household = new Household();
    household.setId(householdId);  // Add this line to set the ID
    household.setName(request.getName());
    household.setAddress(request.getAddress());
    household.setNumberOfMembers(1);

    // Find current user
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String email = authentication.getName();

    User owner = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          logger.warn("Cannot create household: No user logged in with email: {}", email);
          return new IllegalArgumentException("User not found");
        });

    household.setOwner(owner);

    householdRepository.save(household);
    logger.debug("Household saved to database with ID: {}", household.getId());

    userRepository.updateHouseholdId(owner.getId(), household.getId());

    logger.debug("User {} associated with household {}", owner.getEmail(), household.getId());

    logger.info("Household created successfully: {}",
        householdRepository.findByName(request.getName()));

    NotificationDto notification = new NotificationDto();
    notification.setMessage("Household created successfully");
    notification.setType(NotificationType.HOUSEHOLD);
    notification.setRecipientId(household.getOwner().getId());
    notification.setTimestamp(LocalDateTime.now());
    notification.setRead(false);

    notificationService.saveNotification(notification);
    notificationService.sendPrivateNotification(household.getOwner().getId(), notification);
    logger.debug("Notification sent to household owner");
  }

  /**
   * Generates a unique household ID.
   *
   * @return a unique id.
   */
  private static String generateHouseholdId() {
    String id = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8).toUpperCase();
    logger.debug("Generated household ID: {}", id);
    return id;
  }

  /**
   * Adds a registered member to a household.
   *
   * @param request DTO containing the id of the user and the ID of the household.
   * @throws IllegalArgumentException if the user is not found.
   * @throws IllegalArgumentException if the household is not found.
   * @throws IllegalArgumentException if the user is already a member of the specified household.
   */
  public void addUserToHousehold(UserHouseholdAssignmentRequestDto request) {
    logger.info("Adding user with ID {} to current household",
        request.getUserId());

    logger.debug("Finding user with ID: {}", request.getUserId());
    User user = userRepository.findById(request.getUserId())
        .orElseThrow(() -> {
          logger.warn("Cannot add user to household: User not found with ID: {}",
              request.getUserId());
          return new IllegalArgumentException("User not found");
        });

    Household household = householdRepository.findById(request.getHouseholdId())
        .orElseThrow(() -> {
          logger.warn("Cannot add user to household: Household not found with ID: {}",
              request.getHouseholdId());
          return new IllegalArgumentException("Household not found");
        });

    if (user.getHousehold() != null
        && Objects.equals(user.getHousehold().getId(), household.getId())) {
      logger.warn("User {} is already a member of household {}",
          user.getFullName(), household.getName());
      throw new IllegalArgumentException("User is already a member of this household");
    }

    if (user.getHousehold() != null) {
      logger.debug("User is leaving previous household: {}",
          user.getHousehold().getId());
      householdRepository.updateNumberOfMembers(user.getHousehold().getId(),
          user.getHousehold().getNumberOfMembers() - 1);
    }

    userRepository.updateHouseholdId(user.getId(), household.getId());
    householdRepository.updateNumberOfMembers(household.getId(),
        household.getNumberOfMembers() + 1);
    logger.debug("Updated household member count to: {}",
        household.getNumberOfMembers() + 1);

    NotificationDto notification =
        new NotificationDto(NotificationType.HOUSEHOLD,
            household.getOwner().getId(),
            LocalDateTime.now(), false, user.getFullName() + " has joined your household.");

    NotificationDto notification2 =
        new NotificationDto(NotificationType.INFO, request.getUserId(), LocalDateTime.now(), false,
            "You have been added to household " + household.getName() + ".");

    notificationService.saveHouseholdNotification(notification, household.getId());
    notificationService.sendPrivateNotification(request.getUserId(), notification2);
    logger.info("User {} successfully added to household {}",
        user.getFullName(), household.getName());
  }

  /**
   * Removes a registered member from a household.
   *
   * @param userId the user id
   * @throws IllegalArgumentException if the user with a specified id is not found.
   * @throws IllegalArgumentException if the user is not a member of any household.
   */
  public void removeUserFromHousehold(String userId) {
    logger.info("Removing user with ID {} from household", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          logger.warn("Cannot remove user from household: User not found with ID: {}", userId);
          return new IllegalArgumentException("User not found");
        });

    if (user.getHousehold() == null) {
      logger.warn("User {} is not a member of any household", user.getFullName());
      throw new IllegalArgumentException("User is not a member of any household");
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String email = authentication.getName();

    User currentUser = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          logger.warn("Cannot remove user from household: No user logged in with email: {}", email);
          return new IllegalArgumentException("User not found");
        });
    Household household = householdRepository.findById(currentUser.getHousehold().getId())
        .orElseThrow(() -> {
          logger.warn("Cannot remove user from household: Household not found with ID: {}",
              currentUser.getHousehold().getId());
          return new IllegalArgumentException("Household not found");
        });

    if (!user.getHousehold().getId().equals(household.getId())) {
      logger.warn("User {} is not a member of household with ID {}",
          user.getFullName(), household.getId());
      throw new IllegalArgumentException("User is not a member of this household");
    }

    householdRepository.updateNumberOfMembers(user.getHousehold().getId(),
        user.getHousehold().getNumberOfMembers() - 1);
    userRepository.updateHouseholdId(user.getId(), null);
    logger.debug("Updated household member count to: {}",
        user.getHousehold().getNumberOfMembers() - 1);

    NotificationDto notification =
        new NotificationDto(NotificationType.HOUSEHOLD, null, LocalDateTime.now(), false,
            user.getFullName() + " has been removed from household.");

    notificationService.saveHouseholdNotification(notification, household.getId());
    logger.info("User {} successfully removed from household", user.getFullName());
  }

  /**
   * Removes the current authenticated user from their household.
   *
   * @throws IllegalArgumentException if the user is not found or is not a member of any household.
   */
  public void leaveCurrentUserFromHousehold() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    UserDetails userDetails = (UserDetails) auth.getPrincipal();
    String email = userDetails.getUsername();

    logger.info("User attempting to leave household: {}", email);

    User user = userRepository.findByEmail(email).orElseThrow(() -> {
      logger.warn("Authenticated user not found in database: {}", email);
      return new IllegalArgumentException("Authenticated user not found");
    });

    if (user.getHousehold() == null) {
      logger.warn("User {} is not a member of any household", user.getFullName());
      throw new IllegalArgumentException("You are not a member of any household.");
    }

    if (user.getHousehold().getOwner().getId().equals(user.getId())) {
      logger.warn(
          "User {} is the owner and cannot leave the household without transferring ownership",
          user.getFullName());
      throw new IllegalArgumentException(
          "Owner cannot leave the household. Transfer ownership first.");
    }

    logger.info("User {} is leaving household with ID {}", user.getFullName(),
        user.getHousehold().getId());

    String householdId = user.getHousehold().getId();

    householdRepository.updateNumberOfMembers(householdId,
        user.getHousehold().getNumberOfMembers() - 1);
    userRepository.updateHouseholdId(user.getId(), null);
    logger.info("User {} has been removed from the household", user.getFullName());

    String ownerId = user.getHousehold().getOwner().getId();

    NotificationDto notification =
        new NotificationDto(NotificationType.HOUSEHOLD, ownerId,
            LocalDateTime.now(), false, user.getFullName() + " has left the household.");
    notificationService.saveHouseholdNotification(notification, householdId);
  }

  /**
   * Adds a new unregistered member to a household.
   *
   * <p>This method checks if an unregistered member with the given full name already exists in the
   * specified household. If the member does not exist, it creates a new
   * `UnregisteredHouseholdMember` entity, associates it with the household, and updates the
   * household's number of members.
   *
   * @param request The DTO containing the full name of the unregistered member and the ID of the
   *                household to which the member should be added.
   * @throws IllegalArgumentException if the unregistered member already exists in the specified
   *                                  household or if the household is not found.
   */
  public void addUnregisteredMemberToHousehold(
      UnregisteredMemberHouseholdAssignmentRequestDto request) {
    logger.info("Adding unregistered member {} ",
        request.getFullName());

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String email = authentication.getName();

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          logger.warn("Cannot add unregistered member: No user logged in with email: {}", email);
          return new IllegalArgumentException("User not found");
        });
    Household household = user.getHousehold();

    if (household == null) {
      logger.warn("User {} does not belong to a household", user.getFullName());
      throw new IllegalArgumentException("User does not belong to a household");
    }

    UnregisteredHouseholdMember member = new UnregisteredHouseholdMember();
    member.setFullName(request.getFullName());
    member.setHousehold(user.getHousehold());

    unregisteredHouseholdMemberRepository.save(member);
    logger.debug("Unregistered member saved to database");

    householdRepository.updateNumberOfMembers(user.getHousehold().getId(),
        user.getHousehold().getNumberOfMembers() + 1);

    logger.info("Unregistered member {} added to household {}",
        request.getFullName(), user.getHousehold().getName());
  }

  /**
   * Removes an unregistered member from a household.
   *
   * <p>This method deletes the unregistered member from the
   * system and updates the household's member count. If the member doesn't belong to any household,
   * a warning is logged but the member is still deleted.
   *
   * @param memberId the member id
   */
  public void removeUnregisteredMemberFromHousehold(Long memberId) {
    logger.info("Removing unregistered member with ID {}", memberId);

    UnregisteredHouseholdMember member = unregisteredHouseholdMemberRepository.findById(memberId)
        .orElseThrow(() -> {
          logger.warn("Cannot remove unregistered member: Member not found with ID: {}", memberId);
          return new IllegalArgumentException("Unregistered member not found");
        });

    if (member.getHousehold() == null) {
      logger.warn("Unregistered member doesn't belong to any household");
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String email = authentication.getName();
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          logger.warn("Cannot remove unregistered member: No user logged in with email: {}", email);
          return new IllegalArgumentException("User not found");
        });

    if (!user.getHousehold().getId().equals(member.getHousehold().getId())) {
      logger.warn("User {} is not authorized to remove unregistered member {}",
          user.getFullName(), member.getFullName());
      throw new IllegalArgumentException("You are not authorized to remove this member");
    }

    String householdId = member.getHousehold().getId();

    unregisteredHouseholdMemberRepository.delete(member);
    logger.debug("Unregistered member deleted from database");

    householdRepository.updateNumberOfMembers(householdId,
        member.getHousehold().getNumberOfMembers() - 1);
    logger.debug("Updated household member count");

    String memberName = member.getFullName();

    logger.info("Unregistered member {} successfully removed from household", memberName);
  }

  /**
   * Gets the details of the current user's household.
   *
   * @return A map containing household details, registered users, and unregistered members.
   * @throws IllegalArgumentException if the user is not found or does not belong to a household
   */
  public Map<String, Object> getHouseholdDetails() {
    logger.info("Getting household details for current user");

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String email = authentication.getName();

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          logger.warn("Cannot get household details: No user logged in with email: {}", email);
          return new IllegalArgumentException("User not found");
        });

    Household household = user.getHousehold();

    if (household == null) {
      logger.warn("User {} does not belong to a household", user.getFullName());
      throw new IllegalArgumentException("User does not belong to a household");
    }

    Map<String, Object> resultMap = new HashMap<>();

    // Create owner DTO only if owner exists
    UserResponseDto ownerDto = null;
    if (household.getOwner() != null) {
      User owner = household.getOwner();
      ownerDto = new UserResponseDto(
          owner.getId(),
          owner.getEmail(),
          owner.getFullName(),
          owner.getTlf(),
          owner.getRole()
      );
    }

    resultMap.put("household",
        new HouseholdResponseDto(
            household.getId(),
            household.getName(),
            household.getAddress(),
            ownerDto));  // This can now be null safely

    List<UserResponseDto> userResponseDtos = userRepository.getUsersByHousehold(household).stream()
        .map(u -> new UserResponseDto(
            u.getId(),
            u.getEmail(),
            u.getFullName(),
            u.getTlf(),
            u.getRole()))
        .collect(Collectors.toList());
    logger.debug("Found {} registered users in household", userResponseDtos.size());

    List<UnregisteredMemberResponseDto> unregisteredMemberResponseDtos =
        unregisteredHouseholdMemberRepository.findUnregisteredHouseholdMembersByHousehold(household)
            .stream()
            .map(unregisteredMember -> new UnregisteredMemberResponseDto(
                unregisteredMember.getId(),
                unregisteredMember.getFullName()))
            .collect(Collectors.toList());
    logger.debug("Found {} unregistered members in household",
        unregisteredMemberResponseDtos.size());

    resultMap.put("users", userResponseDtos);
    resultMap.put("unregisteredMembers", unregisteredMemberResponseDtos);

    logger.info("Successfully retrieved household details for {}", household.getName());
    return resultMap;
  }

  /**
   * Edits an unregistered member in a household.
   *
   * @param request The request containing the full name of the unregistered member and the new full
   *                name.
   */
  public void editUnregisteredMemberInHousehold(EditMemberDto request) {
    logger.info("Editing unregistered member with ID {}",
        request.getMemberId());

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String email = authentication.getName();
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          logger.warn("Cannot edit unregistered member: No user logged in with email: {}", email);
          return new IllegalArgumentException("User not found");
        });
    Household household = user.getHousehold();

    UnregisteredHouseholdMember member =
        unregisteredHouseholdMemberRepository.findById(request.getMemberId()).orElseThrow(
            () -> {
              logger.warn("Cannot edit unregistered member: Member not found with ID: {}",
                  request.getMemberId());
              return new IllegalArgumentException("Unregistered member not found in household");
            });

    if (!member.getHousehold().getId().equals(household.getId())) {
      logger.warn("User {} is not authorized to edit unregistered member {}",
          user.getFullName(), member.getFullName());
      throw new IllegalArgumentException("You are not authorized to edit this member");
    }

    String oldName = member.getFullName();

    if (request.getNewFullName() != null) {
      member.setFullName(request.getNewFullName());
      logger.debug("Changing member name from {} to {}", oldName, request.getNewFullName());
    }

    unregisteredHouseholdMemberRepository.save(member);
    logger.info("Unregistered member {} edited in household {}",
        request.getNewFullName(), member.getHousehold().getName());
  }

  /**
   * Changes the owner of a household.
   *
   * @param request the request
   */
  public void changeHouseholdOwner(UserHouseholdAssignmentRequestDto request) {
    logger.info("Changing household owner for current household");

    User newOwner = userRepository.findById(request.getUserId())
        .orElseThrow(() -> {
          logger.warn("Cannot change household owner: User not found with ID: {}",
              request.getUserId());
          return new IllegalArgumentException("User not found");
        });

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String email = authentication.getName();

    User currentUser = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          logger.warn("Cannot change household owner: No user logged in with email: {}", email);
          return new IllegalArgumentException("User not found");
        });
    Household household = householdRepository.findById(currentUser.getHousehold().getId())
        .orElseThrow(() -> new IllegalArgumentException("Household not found"));

    if (!household.getOwner().getId().equals(currentUser.getId())) {
      logger.warn("User {} is not authorized to transfer ownership of household {}",
          currentUser.getFullName(), household.getName());
      throw new IllegalArgumentException("Only the owner can transfer ownership");
    }

    if (household.getOwner().getId().equals(newOwner.getId())) {
      logger.warn("User {} is already the owner of household {}", newOwner.getFullName(),
          household.getName());
      throw new IllegalArgumentException("User is already the owner of this household");
    }

    User previousOwner = household.getOwner();
    logger.debug("Changing owner from {} to {}",
        previousOwner.getFullName(), newOwner.getFullName());

    household.setOwner(newOwner);
    householdRepository.save(household);
    logger.info("Household owner changed to {}", newOwner.getFullName());

    NotificationDto notification =
        new NotificationDto(NotificationType.HOUSEHOLD, newOwner.getId(), LocalDateTime.now(),
            false, "You are now the owner of household " + household.getName());
    notificationService.saveNotification(notification);
    notificationService.sendPrivateNotification(newOwner.getId(), notification);
    logger.debug("Ownership change notification sent to new owner");
  }

  /**
   * Searches for a household by its ID.
   *
   * @param householdId The ID of the household to search for.
   * @return A DTO containing the household's basic information.
   */
  public HouseholdBasicResponseDto searchHouseholdById(String householdId) {
    logger.info("Searching for household with ID {}", householdId);

    Household household = householdRepository.findById(householdId)
        .orElseThrow(() -> {
          logger.warn("Cannot find household with ID: {}", householdId);
          return new IllegalArgumentException("Household not found");
        });

    logger.info("Found household: {}", household.getName());
    return new HouseholdBasicResponseDto(household.getId(), household.getName());
  }

  /**
   * Edit household.
   *
   * @param request the request
   */
  public void editHousehold(EditHouseholdRequestDto request) {
    logger.info("Editing household");

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String email = authentication.getName();

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("No user logged in"));
    Household household =
        householdRepository.findById(user.getHousehold().getId()).orElseThrow(() ->
            new IllegalArgumentException("Household not found"));

    if (user.getHousehold() == null) {
      throw new IllegalArgumentException("User is not associated with any household");
    }

    if (!household.getOwner().getId().equals(user.getId())) {
      logger.warn("User {} is not authorized to edit household {}", user.getId(),
          household.getId());
      throw new IllegalArgumentException("Only the owner can edit the household");
    }
    if (request.getName() != null) {
      logger.debug("Changing household name from {} to {}", household.getName(), request.getName());
      household.setName(request.getName());
    }

    if (request.getAddress() != null) {
      logger.debug("Changing household address from {} to {}", household.getAddress(),
          request.getAddress());
      household.setAddress(request.getAddress());
    }

    householdRepository.save(household);
    logger.info("Household {} updated successfully", household.getName());

    NotificationDto notification =
        new NotificationDto(NotificationType.HOUSEHOLD, household.getOwner().getId(),
            LocalDateTime.now(), false, "Household " + household.getName() + " has been updated.");
    notificationService.saveNotification(notification);
    notificationService.sendPrivateNotification(household.getOwner().getId(), notification);
    logger.debug("Household update notification sent to owner");
  }

  /**
   * Deletes a household and removes all associated users and unregistered members.
   *
   * @throws IllegalArgumentException if the household is not found or if the user is not the
   *                                  owner.
   */
  @Transactional
  public void deleteHousehold() {
    logger.info("Deleting household");

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String email = authentication.getName();

    // Finding the current user
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          logger.warn("Cannot delete household: User not found with email: {}", email);
          return new IllegalArgumentException("User not found");
        });

    // Getting the current users household id
    Household household = householdRepository.findById(user.getHousehold().getId())
        .orElseThrow(() -> {
          logger.warn("Cannot delete household: Household not found with ID: {}",
              user.getHousehold().getId());
          return new IllegalArgumentException("Household not found");
        });

    if (!household.getOwner().getId().equals(user.getId())) {
      logger.warn("User {} is not authorized to delete household {}", user.getId(),
          household.getId());
      throw new IllegalArgumentException("Only the owner can delete the household");
    }

    membershipRequestRepository.deleteAllByHouseholdId(household.getId());
    logger.debug("Deleted membership requests for household {}", household.getId());

    // Moving all registered users from the household
    List<User> users = userRepository.getUsersByHousehold(household);
    logger.debug("Removing {} registered users from household", users.size());
    for (User u : users) {
      u.setHousehold(null);
      userRepository.save(u);
    }

    // Deleting all unregistered members from the household
    List<UnregisteredHouseholdMember> unregistered =
        unregisteredHouseholdMemberRepository.findUnregisteredHouseholdMembersByHousehold(
            household);
    logger.debug("Removing {} unregistered members from household", unregistered.size());
    unregisteredHouseholdMemberRepository.deleteAll(unregistered);

    String householdName = household.getName();

    NotificationDto notification =
        new NotificationDto(NotificationType.HOUSEHOLD, household.getOwner().getId(),
            LocalDateTime.now(), false, "Household " + householdName + " has been deleted.");

    householdRepository.delete(household);
    logger.info("Household {} deleted successfully", householdName);

    notificationService.saveHouseholdNotification(notification, household.getId());
    logger.debug("Household deletion notification sent");
  }

  /**
   * Gets a household by its ID.
   *
   * @param householdId the id of the household
   * @return HouseholdResponseDto containing household details.
   */
  public HouseholdResponseDto getHousehold(String householdId) {
    logger.info("Getting household with ID {}", householdId);

    Household household = householdRepository.findById(householdId)
        .orElseThrow(() -> {
          logger.warn("Cannot get household: Household not found with ID: {}", householdId);
          return new IllegalArgumentException("Household not found");
        });

    logger.info("Successfully retrieved household: {}", household.getName());
    return new HouseholdResponseDto(household.getId(), household.getName(), household.getAddress(),
        new UserResponseDto(household.getOwner().getId(), household.getOwner().getEmail(),
            household.getOwner().getFullName(), household.getOwner().getTlf(),
            household.getOwner().getRole()));
  }

  /**
   * Gets positions of the users in the current users household.
   *
   * @return the household positions
   */
  public List<PositionResponseDto> getHouseholdPositions() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String email = authentication.getName();

    logger.info("Fetching household positions for user: {}", email);

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          logger.warn("Cannot get household positions: No user logged in with email: {}", email);
          return new IllegalArgumentException("No user logged in");
        });

    if (user.getHousehold() == null) {
      logger.warn("User {} does not belong to a household", user.getFullName());
      throw new IllegalArgumentException("User does not belong to a household");
    }

    String householdId = user.getHousehold().getId();
    List<User> users = userRepository.getUsersByHouseholdId(householdId);
    logger.debug("Found {} users in household {}", users.size(), user.getHousehold().getName());

    List<PositionResponseDto> positions = users.stream().map(
            u -> new PositionResponseDto(u.getId(), u.getFullName(),
                u.getLongitude(), u.getLatitude()))
        .toList();

    logger.info("Successfully retrieved positions for {} household members", positions.size());
    return positions;
  }
}