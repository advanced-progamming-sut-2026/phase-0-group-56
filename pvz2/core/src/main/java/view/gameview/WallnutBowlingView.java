package view.gameview;

import commands.GameCommands;
import controllers.menus.gamecontroller.WallnutController;
import view.View;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WallnutBowlingView extends View {
    private final int level;
    private WallnutController wallnutController;

    public WallnutBowlingView(int level) {
        this.level = Math.max(1, level);
    }

    @Override
    public void show() {
        wallnutController = new WallnutController(level);
        super.show();
    }

    @Override
    protected String getScreenTitle() {
        return "Wall-nut Bowling";
    }

    @Override
    protected com.badlogic.gdx.Screen getBackScreen() {
        return new view.TravelLogView();
    }

    @Override
    protected void buildContent(com.badlogic.gdx.scenes.scene2d.ui.Table table) {
        table.add(mediumTitle("LEVEL " + level))
            .padBottom(12f)
            .row();
        table.add(wrappedLabel(
            "Choose a Wall-nut or Explode-o-nut, place it on the board, then use TICK.",
            720f
        )).width(720f).padBottom(12f).row();

        com.badlogic.gdx.scenes.scene2d.ui.Table controls = pvzInnerPanel();
        controls.add(greenButton("TICK", () ->
            showMessage(wallnutController.playGame(0.1f))
        )).width(150f).height(48f).padRight(12f);
        controls.add(brownButton("BACK TO TRAVEL LOG", () ->
            models.App.setScreen(new view.TravelLogView())
        )).width(230f).height(48f);
        table.add(controls).padBottom(14f).row();

        com.badlogic.gdx.scenes.scene2d.ui.Table board = pvzPanel();
        for (int row = 0; row < 5; row++) {
            for (int column = 0; column < 5; column++) {
                final int selectedRow = row;
                final int selectedColumn = column;
                com.badlogic.gdx.scenes.scene2d.ui.Table cell = pvzInnerPanel();
                cell.add(greenSmallButton("WALLNUT", () ->
                    showMessage(wallnutController.plant("Wallnut", selectedColumn, selectedRow))
                )).width(92f).height(36f).row();
                cell.add(purpleButton("EXPLODE", () ->
                    showMessage(wallnutController.plant("Explod'O nut", selectedColumn, selectedRow))
                )).width(92f).height(36f);
                board.add(cell).width(130f).height(100f).pad(4f);
            }
            board.row();
        }
        table.add(board).width(760f);
    }

    @Override
    public void input() {
        super.input();
        if(input.matches(GameCommands.PLANT.getRegex())){
            Matcher m = Pattern.compile(GameCommands.PLANT.getRegex()).matcher(input);
            if(m.find()){
                int x =  Integer.parseInt(m.group("x"));
                int y = Integer.parseInt(m.group("y"));
                String plantName = m.group("plant");
                System.out.println(wallnutController.plant(plantName , x , y));
            }
        }
        else if(input.matches(GameCommands.PLAY.getRegex())){
            Matcher m = Pattern.compile(GameCommands.PLAY.getRegex()).matcher(input);
            if(m.find()){
                float tick =  Float.parseFloat(m.group("delta"));
                System.out.println(wallnutController.playGame(tick * 0.1f));
            }
        }
        else System.out.println("The input is invalid.");
    }
}
