package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;

import controllers.datacontroller.Data;
import controllers.menus.secondarymenus.LeaderBoard;
import models.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LeaderBoardView extends View {

    private SortColumn sortColumn =
        SortColumn.SCORE;

    private boolean descending =
        true;

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
    protected void buildContent(
        Table table
    ) {

        Label subtitle =
            mediumTitle(
                "TOP PLAYERS"
            );

        subtitle.setAlignment(
            Align.center
        );

        table.add(subtitle)
            .padBottom(10f)
            .row();

        Table hintPanel =
            pvzInnerPanel();

        Label hint =
            wrappedLabel(
                "Click a column header to sort. "
                    + "Click the same header again to reverse the order.",
                760f
            );

        hint.setAlignment(
            Align.center
        );

        hintPanel.add(hint)
            .width(760f);

        table.add(hintPanel)
            .width(820f)
            .padBottom(16f)
            .row();

        List<User> users =
            new ArrayList<>(
                Data.getAllUsers()
            );

        Comparator<User> comparator =
            buildComparator();

        if (descending) {
            comparator =
                comparator.reversed();
        }

        users.sort(comparator);

        Table outerPanel =
            pvzPanel();

        Table board =
            new Table();

        addHeader(
            board,
            "USERNAME",
            SortColumn.USERNAME,
            145f
        );

        addHeader(
            board,
            "PROGRESS",
            SortColumn.PROGRESS,
            185f
        );

        addHeader(
            board,
            "MINIGAMES",
            SortColumn.MINIGAMES,
            120f
        );

        addHeader(
            board,
            "DAILY Q",
            SortColumn.DAILY_QUESTS,
            105f
        );

        addHeader(
            board,
            "OTHER Q",
            SortColumn.OTHER_QUESTS,
            105f
        );

        addHeader(
            board,
            "MEOWPOINT",
            SortColumn.SCORE,
            125f
        );

        board.row();

        if (users.isEmpty()) {

            Label empty =
                mediumTitle(
                    "NO REGISTERED USERS"
                );

            empty.setAlignment(
                Align.center
            );

            board.add(empty)
                .colspan(6)
                .pad(25f);

        } else {

            int rank = 1;

            for (User user : users) {

                addCell(
                    board,
                    rank + ". " + user.getName(),
                    145f
                );

                addCell(
                    board,
                    user.getLastProgressText(),
                    185f
                );

                addCell(
                    board,
                    String.valueOf(
                        user.getMinigamesWon()
                    ),
                    120f
                );

                addCell(
                    board,
                    String.valueOf(
                        user.getDailyQuestsCompleted()
                    ),
                    105f
                );

                addCell(
                    board,
                    String.valueOf(
                        user.getOtherQuestsCompleted()
                    ),
                    105f
                );

                addCell(
                    board,
                    String.valueOf(
                        user.getHighestScore()
                    ),
                    125f
                );

                board.row();

                rank++;
            }
        }

        outerPanel.add(board);

        table.add(outerPanel)
            .padTop(8f);
    }

    private Comparator<User>
    buildComparator() {

        return switch (sortColumn) {

            case USERNAME ->
                Comparator.comparing(
                    User::getName,
                    String.CASE_INSENSITIVE_ORDER
                );

            case PROGRESS ->
                Comparator.comparingInt(
                    User::getLevelsPassed
                );

            case MINIGAMES ->
                Comparator.comparingInt(
                    User::getMinigamesWon
                );

            case DAILY_QUESTS ->
                Comparator.comparingInt(
                    User::getDailyQuestsCompleted
                );

            case OTHER_QUESTS ->
                Comparator.comparingInt(
                    User::getOtherQuestsCompleted
                );

            case SCORE ->
                Comparator.comparingInt(
                    User::getHighestScore
                );
        };
    }

    private void addHeader(
        Table table,
        String title,
        SortColumn column,
        float width
    ) {

        String suffix = "";

        if (sortColumn == column) {
            suffix =
                descending
                    ? "  v"
                    : "  ^";
        }

        TextButton header =
            brownButton(
                title + suffix,
                () -> {

                    if (
                        sortColumn
                            == column
                    ) {

                        descending =
                            !descending;

                    } else {

                        sortColumn =
                            column;

                        descending =
                            true;
                    }

                    content
                        .clearChildren();

                    buildContent(
                        content
                    );
                }
            );

        table.add(header)
            .width(width)
            .height(48f)
            .pad(2f);
    }

    private void addCell(
        Table table,
        String text,
        float width
    ) {

        Label label =
            new Label(
                text == null
                    ? ""
                    : text,
                skin
            );

        label.setAlignment(
            Align.center
        );

        table.add(label)
            .width(width)
            .height(42f)
            .center()
            .pad(5f);
    }
}
