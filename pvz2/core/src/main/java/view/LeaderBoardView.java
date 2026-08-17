package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

import controllers.datacontroller.Data;
import controllers.menus.secondarymenus.LeaderBoard;
import models.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LeaderBoardView extends View {
    private SortColumn sortColumn = SortColumn.SCORE;
    private boolean descending = true;

    private enum SortColumn {
        USERNAME,
        PROGRESS,
        MINIGAMES,
        DAILY_QUESTS,
        OTHER_QUESTS,
        SCORE
    }

    public LeaderBoardView() {
        menu = new LeaderBoard();
    }

    @Override
    protected String getScreenTitle() {
        return "Leaderboard";
    }

    @Override
    protected Screen getBackScreen() {
        return new HomeView();
    }

    @Override
    protected void buildContent(Table table) {
        table.add(wrappedLabel(
                "Click a column header to sort. Clicking the same column again reverses the order.", 900f))
            .width(900f).padBottom(12f).row();

        List<User> users = new ArrayList<>(Data.getAllUsers());
        Comparator<User> comparator = buildComparator();
        if (descending) {
            comparator = comparator.reversed();
        }
        users.sort(comparator);

        Table board = new Table();
        addHeader(board, "Username", SortColumn.USERNAME, 150f);
        addHeader(board, "Progress", SortColumn.PROGRESS, 210f);
        addHeader(board, "Minigames", SortColumn.MINIGAMES, 120f);
        addHeader(board, "Daily Q", SortColumn.DAILY_QUESTS, 105f);
        addHeader(board, "Other Q", SortColumn.OTHER_QUESTS, 105f);
        addHeader(board, "MeowPoint", SortColumn.SCORE, 120f);
        board.row();

        if (users.isEmpty()) {
            board.add(new Label("No registered users.", skin)).colspan(6).pad(20f);
        } else {
            for (User user : users) {
                addCell(board, user.getName(), 150f);
                addCell(board, user.getLastProgressText(), 210f);
                addCell(board, String.valueOf(user.getMinigamesWon()), 120f);
                addCell(board, String.valueOf(user.getDailyQuestsCompleted()), 105f);
                addCell(board, String.valueOf(user.getOtherQuestsCompleted()), 105f);
                addCell(board, String.valueOf(user.getHighestScore()), 120f);
                board.row();
            }
        }
        table.add(board).padTop(8f);
    }

    private Comparator<User> buildComparator() {
        return switch (sortColumn) {
            case USERNAME -> Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER);
            case PROGRESS -> Comparator.comparingInt(User::getLevelsPassed);
            case MINIGAMES -> Comparator.comparingInt(User::getMinigamesWon);
            case DAILY_QUESTS -> Comparator.comparingInt(User::getDailyQuestsCompleted);
            case OTHER_QUESTS -> Comparator.comparingInt(User::getOtherQuestsCompleted);
            case SCORE -> Comparator.comparingInt(User::getHighestScore);
        };
    }

    private void addHeader(Table table, String title, SortColumn column, float width) {
        String suffix = sortColumn == column ? (descending ? " v" : " ^") : "";
        TextButton header = button(title + suffix, () -> {
            if (sortColumn == column) {
                descending = !descending;
            } else {
                sortColumn = column;
                descending = true;
            }
            content.clearChildren();
            buildContent(content);
        });
        table.add(header).width(width).height(44f).pad(2f);
    }

    private void addCell(Table table, String text, float width) {
        table.add(new Label(text == null ? "" : text, skin)).width(width).left().pad(7f);
    }
}
