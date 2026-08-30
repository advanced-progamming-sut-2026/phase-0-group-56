package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import controllers.menus.secondarymenus.LeaderBoard;
import models.User;
import network.LeaderboardEntry;

import java.util.List;

public class LeaderBoardView extends View {

    private LeaderBoard.SortCriterion sortColumn =
        LeaderBoard.SortCriterion.SCORE;

    private boolean descending =
        true;

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

        table.add(
                menuSectionHeader(
                    "almanac",
                    "TOP PLAYERS",
                    "Compare progress, minigame wins, quests, and Meowpoint."
                )
            )
            .width(900f)
            .padBottom(12f)
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

        Image hintIcon = MenuVisualAssets.image("star");
        if (hintIcon != null) {
            hintIcon.setScaling(Scaling.fit);
            hintPanel.add(hintIcon)
                .size(34f)
                .padRight(8f);
        }

        hintPanel.add(hint)
            .width(710f);

        table.add(hintPanel)
            .width(820f)
            .padBottom(16f)
            .row();

        LeaderBoard controller =
            (LeaderBoard) menu;

        List<User> users =
            controller.getSortedUsers(
                sortColumn,
                descending
            );
        List<LeaderboardEntry> networkUsers = controller.getNetworkLeaderboard();

        Table outerPanel =
            pvzPanel();

        Table board =
            new Table();

        addHeader(
            board,
            "USERNAME",
            LeaderBoard.SortCriterion.USERNAME,
            145f
        );

        addHeader(
            board,
            "PROGRESS",
            LeaderBoard.SortCriterion.PROGRESS,
            185f
        );

        addHeader(
            board,
            "MINIGAMES",
            LeaderBoard.SortCriterion.MINIGAMES,
            120f
        );

        addHeader(
            board,
            "DAILY Q",
            LeaderBoard.SortCriterion.DAILY_QUESTS,
            105f
        );

        addHeader(
            board,
            "OTHER Q",
            LeaderBoard.SortCriterion.OTHER_QUESTS,
            105f
        );

        addHeader(
            board,
            "MEOWPOINT",
            LeaderBoard.SortCriterion.SCORE,
            125f
        );

        board.row();

        if (!networkUsers.isEmpty()) {
            int rank = 1;
            for (LeaderboardEntry entry : networkUsers) {
                addCell(board, rank + ". " + entry.username(), 145f);
                addCell(board, "SERVER SCORE", 185f);
                addCell(board, "-", 120f);
                addCell(board, "-", 105f);
                addCell(board, "-", 105f);
                addCell(board, String.valueOf(entry.score()), 125f);
                board.row();
                rank++;
            }
        } else if (users.isEmpty()) {

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
                    controller.getProgressText(user),
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

    private void addHeader(
        Table table,
        String title,
        LeaderBoard.SortCriterion column,
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
