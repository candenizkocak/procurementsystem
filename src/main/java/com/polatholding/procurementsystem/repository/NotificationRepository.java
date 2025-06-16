package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    // Changed from findByUserIdAndIsReadFalseOrderBySentDateDesc to findByUserUserIdAndIsReadFalseOrderBySentDateDesc
    List<Notification> findByUserUserIdAndIsReadFalseOrderBySentDateDesc(Integer userId);

    // Changed from countByUserIdAndIsReadFalse to countByUserUserIdAndIsReadFalse
    long countByUserUserIdAndIsReadFalse(Integer userId);

    // Method to get all notifications for a user with sort order
    List<Notification> findByUserUserIdOrderBySentDateDesc(Integer userId);

    // Changed method to remove the ordering from the method name and let Pageable handle sorting
    // This prevents duplicate ORDER BY columns in the SQL query
    List<Notification> findByUserUserId(Integer userId, Pageable pageable);

    // Updated to use native SQL query to directly set IsRead=1 in the database
    @Modifying
    @Query(value = "UPDATE Notifications SET IsRead = 1 WHERE UserID = :userId AND IsRead = 0", nativeQuery = true)
    void markAllAsReadForUser(@Param("userId") Integer userId);

    // Find by user and notification ID to ensure user owns the notification before marking as read
    Notification findByNotificationIdAndUserUserId(Integer notificationId, Integer userId);

    // Added direct SQL update for a single notification
    @Modifying
    @Query(value = "UPDATE Notifications SET IsRead = 1 WHERE NotificationID = :notificationId AND UserID = :userId", nativeQuery = true)
    void markAsReadNative(@Param("notificationId") Integer notificationId, @Param("userId") Integer userId);
}