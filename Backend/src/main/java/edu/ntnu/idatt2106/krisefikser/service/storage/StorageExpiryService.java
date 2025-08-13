package edu.ntnu.idatt2106.krisefikser.service.storage;

import edu.ntnu.idatt2106.krisefikser.persistance.repository.storage.StorageItemRepository;
import edu.ntnu.idatt2106.krisefikser.service.notification.NotificationService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for handling storage expiry notifications.
 */

@Service
@EnableScheduling
public class StorageExpiryService {

  private final StorageItemRepository storageRepository;
  private final NotificationService notificationService;
  private final Logger logger = LoggerFactory.getLogger(StorageExpiryService.class);

  /**
   * Constructor for StorageExpiryService.
   *
   * @param storageRepository   the repository for handling storage items
   * @param notificationService the service for sending notifications
   */
  public StorageExpiryService(StorageItemRepository storageRepository,
      NotificationService notificationService) {
    this.storageRepository = storageRepository;
    this.notificationService = notificationService;
  }

  /**
   * Scheduled method to check for items expiring in the next 7 days and send notifications. This
   * method runs every day at 08:00 AM.
   */
  @Scheduled(cron = "0 0 8 * * ?")
  public void checkForExpiringItems() {
    logger.info("Checking for items expiring in the next 7 days...");

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime sevenDaysLater = now.plusDays(7);

    storageRepository.findExpiringItems(now, sevenDaysLater)
        .forEach(item -> {
          try {
            notificationService.sendExpiryNotification(item);
            logger.info("Sent notification for item: {}", item.getItem().getName());
          } catch (Exception e) {
            logger.error("Failed to send notification for item: {}", item.getItem().getName(), e);
          }
        });

    logger.info("Finished checking for expiring items.");
  }
}
