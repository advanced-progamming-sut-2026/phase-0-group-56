package controllers.menus.secondarymenus;

import controllers.datacontroller.Data;
import controllers.menus.Menu;
import models.App;
import models.NewsItem;
import models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class News implements Menu {
    @Override
    public String ChangeMenu(String menuName) {
        return "Invalid menu transition from this menu.";
    }

    @Override
    public String exitMenu() {
        App.setScreen(new view.HomeView());
        return "Returned to Home Menu.";
    }

    @Override
    public String ShowCurrentMenu() {
        return "--- News Menu ---";
    }

    public static void pushNewsToUser(User user, String message) {
        pushNewsToUser(
            user,
            NewsItem.inferTitle(message),
            message
        );
    }

    public static void pushNewsToUser(
        User user,
        String title,
        String message
    ) {
        if (queueNewsForUser(user, title, message)) {
            Data.saveUser();
        }
    }

    public static boolean queueNewsForUser(
        User user,
        String message
    ) {
        return queueNewsForUser(
            user,
            NewsItem.inferTitle(message),
            message
        );
    }

    public static boolean queueNewsForUser(
        User user,
        String title,
        String message
    ) {
        if (user == null || message == null || message.isBlank()) {
            return false;
        }

        NewsItem item = NewsItem.create(
            title,
            message,
            true
        );

        user.getUnreadNews().add(item.toStorage());
        return true;
    }

    public String ShowNews() {
        User user = Data.getCurrentUser();

        if (user == null) {
            return "Error: Please log in.";
        }

        boolean normalized = normalizeStoredNews(user);

        ArrayList<NewsItem> unread = toItems(
            user.getUnreadNews(),
            true,
            false
        );

        if (unread.isEmpty()) {
            if (normalized) {
                Data.saveUser();
            }

            return "No new unread news.";
        }

        String result = formatNews("--- Unread News ---", unread);

        user.getReadNews().addAll(user.getUnreadNews());
        user.getUnreadNews().clear();

        Data.saveUser();

        return result;
    }

    public String ShowAllNews() {
        User user = Data.getCurrentUser();

        if (user == null) {
            return "Error: Please log in.";
        }

        List<NewsItem> allNews = getAllNewsItems(user);

        if (allNews.isEmpty()) {
            return "No news available.";
        }

        return formatNews("--- All News ---", allNews);
    }

    public String markAllAsRead() {
        User user = Data.getCurrentUser();

        if (user == null) {
            return "Error: Please log in.";
        }

        boolean normalized = normalizeStoredNews(user);
        int unreadCount = user.getUnreadNews().size();

        if (unreadCount == 0) {
            if (normalized) {
                Data.saveUser();
            }

            return "No new unread news.";
        }

        user.getReadNews().addAll(user.getUnreadNews());
        user.getUnreadNews().clear();
        Data.saveUser();

        return unreadCount + " news item(s) marked as read.";
    }

    public List<NewsItem> getAllNewsItems() {
        User user = Data.getCurrentUser();

        if (user == null) {
            return Collections.emptyList();
        }

        return getAllNewsItems(user);
    }

    private List<NewsItem> getAllNewsItems(User user) {
        boolean normalized = normalizeStoredNews(user);
        ArrayList<NewsItem> result = new ArrayList<>();

        result.addAll(
            toItems(
                user.getUnreadNews(),
                true,
                true
            )
        );

        result.addAll(
            toItems(
                user.getReadNews(),
                false,
                true
            )
        );

        if (normalized) {
            Data.saveUser();
        }

        return Collections.unmodifiableList(result);
    }

    private boolean normalizeStoredNews(User user) {
        return normalizeList(user.getUnreadNews(), true)
            | normalizeList(user.getReadNews(), false);
    }

    private boolean normalizeList(
        ArrayList<String> storedNews,
        boolean unread
    ) {
        boolean changed = false;

        for (int index = 0; index < storedNews.size(); index++) {
            String storedValue = storedNews.get(index);
            NewsItem item = NewsItem.fromStorage(storedValue, unread);
            String normalized = item.toStorage();

            if (!item.isStructuredStorage(storedValue)) {
                storedNews.set(index, normalized);
                changed = true;
            }
        }

        return changed;
    }

    private ArrayList<NewsItem> toItems(
        ArrayList<String> storedNews,
        boolean unread,
        boolean newestFirst
    ) {
        ArrayList<NewsItem> result = new ArrayList<>();

        if (newestFirst) {
            for (int index = storedNews.size() - 1; index >= 0; index--) {
                result.add(
                    NewsItem.fromStorage(
                        storedNews.get(index),
                        unread
                    )
                );
            }
        } else {
            for (String storedValue : storedNews) {
                result.add(
                    NewsItem.fromStorage(
                        storedValue,
                        unread
                    )
                );
            }
        }

        return result;
    }

    private String formatNews(
        String heading,
        List<NewsItem> items
    ) {
        StringBuilder result = new StringBuilder(heading);

        for (NewsItem item : items) {
            result.append("\n")
                .append(item.isUnread() ? "[NEW] " : "")
                .append(item.getDate())
                .append(" | ")
                .append(item.getTitle())
                .append("\n")
                .append(item.getBody())
                .append("\n");
        }

        return result.toString().trim();
    }
}
