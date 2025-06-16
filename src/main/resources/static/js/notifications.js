document.addEventListener('DOMContentLoaded', function () {
    const notificationBell = document.getElementById('notificationBell');
    const notificationCountBadge = document.getElementById('notificationCountBadge');
    const notificationDropdown = document.getElementById('notificationDropdown');
    const notificationList = document.getElementById('notificationList');
    const markAllAsReadBtn = document.getElementById('markAllAsReadBtn');
    const noNotificationsLi = notificationList ? notificationList.querySelector('.no-notifications') : null;

    // Ensure we're using the correct API endpoint path
    const API_BASE_URL = '/api/notifications';

    async function fetchUnreadCount() {
        try {
            const response = await fetch(`${API_BASE_URL}/unread-count`);
            if (!response.ok) {
                console.error('Failed to fetch unread count:', response.status);
                return;
            }
            const count = await response.json();
            updateBadge(count);
        } catch (error) {
            console.error('Error fetching unread count:', error);
        }
    }

    async function fetchRecentNotifications() {
        try {
            const response = await fetch(`${API_BASE_URL}/recent?limit=5`); // Fetch 5 recent
            if (!response.ok) {
                console.error('Failed to fetch recent notifications:', response.status);
                if (notificationList) {
                    notificationList.innerHTML = '<li class="no-notifications">Could not load notifications.</li>';
                    if (noNotificationsLi) noNotificationsLi.style.display = 'block';
                }
                return;
            }
            const notifications = await response.json();
            renderNotifications(notifications);
        } catch (error) {
            console.error('Error fetching recent notifications:', error);
            if (notificationList) {
                notificationList.innerHTML = '<li class="no-notifications">Error loading notifications.</li>';
                if (noNotificationsLi) noNotificationsLi.style.display = 'block';
            }
        }
    }

    function updateBadge(count) {
        if (notificationCountBadge) {
            if (count > 0) {
                notificationCountBadge.textContent = count > 9 ? '9+' : count;
                notificationCountBadge.style.display = 'inline-block';
            } else {
                notificationCountBadge.style.display = 'none';
            }
        }
    }

    function renderNotifications(notifications) {
        if (!notificationList) return;

        notificationList.innerHTML = ''; // Clear existing
        if (notifications.length === 0) {
            if (noNotificationsLi) noNotificationsLi.style.display = 'block';
            notificationList.appendChild(noNotificationsLi || createNoNotificationsMessage());
            return;
        }
        if (noNotificationsLi) noNotificationsLi.style.display = 'none';

        notifications.forEach(notif => {
            const li = document.createElement('li');
            li.classList.add('notification-item');

            // Fixed property check: Proper handling of isRead property
            const isNotificationRead = notif.read === true || notif.isRead === true;
            if (!isNotificationRead) {
                li.classList.add('unread');
            }

            const a = document.createElement('a');
            a.href = notif.link || '#'; // Fallback href
            a.dataset.notificationId = notif.notificationId;

            const messageDiv = document.createElement('div');
            messageDiv.classList.add('notification-message');
            messageDiv.textContent = notif.message;

            const timeDiv = document.createElement('div');
            timeDiv.classList.add('notification-time');
            timeDiv.textContent = notif.sentDateFormatted;

            a.appendChild(messageDiv);
            a.appendChild(timeDiv);
            li.appendChild(a);
            notificationList.appendChild(li);

            a.addEventListener('click', async function(event) {
                event.preventDefault(); // Prevent default navigation first
                // Check if notification is unread
                if (!isNotificationRead) {
                    await markAsRead(notif.notificationId);
                }
                window.location.href = this.href; // Navigate after marking as read
            });
        });
    }

    function createNoNotificationsMessage() {
        const li = document.createElement('li');
        li.classList.add('no-notifications');
        li.textContent = 'No new notifications.';
        return li;
    }

    async function markAsRead(notificationId) {
        try {
            const response = await fetch(`${API_BASE_URL}/${notificationId}/read`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                console.error('Failed to mark notification as read:', response.status);
                return false;
            }

            // Optimistically update UI or refetch count/list
            fetchUnreadCount();

            // Find the item in the list and remove 'unread' class
            if (notificationList) {
                const itemLink = notificationList.querySelector(`a[data-notification-id="${notificationId}"]`);
                if (itemLink && itemLink.parentElement) {
                    itemLink.parentElement.classList.remove('unread');
                    console.log('Removed unread class from notification', notificationId);
                }
            }

            return true;
        } catch (error) {
            console.error('Error marking notification as read:', error);
            return false;
        }
    }

    async function markAllAsRead() {
        try {
            const response = await fetch(`${API_BASE_URL}/read-all`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                console.error('Failed to mark all notifications as read:', response.status);
                return;
            }

            console.log('Successfully marked all notifications as read');

            // Update unread count badge
            fetchUnreadCount();

            // Remove unread class from all notification items in the dropdown
            if (notificationList) {
                const unreadItems = notificationList.querySelectorAll('.notification-item.unread');
                unreadItems.forEach(item => {
                    item.classList.remove('unread');
                });
            }

            // Refresh the notifications list to ensure UI is in sync with server state
            fetchRecentNotifications();

            // Close dropdown after marking all as read
            if (notificationDropdown) {
                notificationDropdown.classList.remove('show');
            }
        } catch (error) {
            console.error('Error marking all notifications as read:', error);
        }
    }

    if (notificationBell) {
        notificationBell.addEventListener('click', function (event) {
            event.stopPropagation();
            const isShown = notificationDropdown.classList.toggle('show');
            if (isShown) {
                fetchRecentNotifications(); // Fetch when opening
            }
        });
    }

    if (markAllAsReadBtn) {
        markAllAsReadBtn.addEventListener('click', function(event) {
            event.stopPropagation();
            markAllAsRead();
        });
    }

    // Close dropdown if clicking outside
    document.addEventListener('click', function (event) {
        if (notificationDropdown && !notificationDropdown.contains(event.target) &&
            notificationBell && !notificationBell.contains(event.target)) {
            notificationDropdown.classList.remove('show');
        }
    });

    // Initial fetch of unread count
    fetchUnreadCount();

    // Optional: Poll for new notifications periodically (e.g., every 30 seconds)
    // setInterval(fetchUnreadCount, 30000);
});