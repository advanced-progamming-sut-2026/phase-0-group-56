package view.gameview;

import commands.GameCommands;
import controllers.menus.gamecontroller.VaseBreakerController;
import view.View;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VaseBreakerView extends View {
    private final int level;
    private VaseBreakerController controller;

    public VaseBreakerView(int level) {
        this.level = Math.max(1, level);
    }

    @Override
    public void show() {
        controller = new VaseBreakerController(level);
        super.show();
    }

    @Override
    protected String getScreenTitle() {
        return "Vase Breaker";
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
            "Break vases by selecting a board cell. Use TICK to advance the game.",
            720f
        )).width(720f).padBottom(12f).row();

        com.badlogic.gdx.scenes.scene2d.ui.Table controls = pvzInnerPanel();
        controls.add(greenButton("TICK", () -> {
            String result = controller.playGame(0.1f);
            showMessage(result);
        })).width(150f).height(48f).padRight(12f);
        controls.add(brownButton("BACK TO TRAVEL LOG", () ->
            models.App.setScreen(new view.TravelLogView())
        )).width(230f).height(48f);
        table.add(controls).padBottom(14f).row();

        com.badlogic.gdx.scenes.scene2d.ui.Table board = pvzPanel();
        for (int row = 0; row < 5; row++) {
            for (int column = 0; column < 9; column++) {
                final int selectedRow = row;
                final int selectedColumn = column;
                board.add(brownButton(
                    "BREAK",
                    () -> showMessage(controller.breakVase(selectedColumn, selectedRow))
                )).width(78f).height(42f).pad(3f);
            }
            board.row();
        }
        table.add(board).width(820f);
    }

    @Override
    public void input() {
        super.input();
        if(input.matches(GameCommands.PLAY.getRegex())){
            Matcher m = Pattern.compile(GameCommands.PLAY.getRegex()).matcher(input);
            if(m.find()){
                float tick = Float.parseFloat(m.group("delta"));
                System.out.println(controller.playGame(tick * 0.1f));
            }
        }
        else if(input.matches(GameCommands.PLANT.getRegex())){
            Matcher m = Pattern.compile(GameCommands.PLANT.getRegex()).matcher(input);
            if(m.find()){
                int x = Integer.parseInt(m.group("x"));
                int y = Integer.parseInt(m.group("y"));
                String name =  m.group("plant");
                System.out.println(controller.plant(name,x,y));
            }
        }
        else if(input.matches("show\\s+vases")){
            System.out.println(controller.showVases());
        }
        else System.out.println("Invalid input. Valid inputs:\n" +
                "--> Play\n--> Plant \n Show vases");
    }
}
