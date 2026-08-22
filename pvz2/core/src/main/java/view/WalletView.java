package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

import controllers.menus.secondarymenus.Wallet;
import controllers.datacontroller.Data;
import models.User;
import models.utils.RegexHelper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WalletView extends View {
    public WalletView() { menu = new Wallet(); }

    @Override
    protected String getScreenTitle() {
        return "Wallet";
    }

    @Override
    protected Screen getBackScreen() {
        return new HomeView();
    }

    @Override
    protected void buildContent(Table table) {
        User user = Data.getCurrentUser();

        if (user == null) {
            table.add(mediumTitle("PLEASE LOG IN"));
            return;
        }

        table.add(
                menuSectionHeader(
                    "hud_zg",
                    "WALLET",
                    "Manage your Coins and Gems in one place."
                )
            )
            .width(720f)
            .padTop(8f)
            .padBottom(16f)
            .row();

        Table balances = pvzPanel();
        balances.defaults()
            .width(270f)
            .height(145f)
            .pad(8f);

        balances.add(balanceCard("COINS", user.getCoins()));
        balances.add(balanceCard("GEMS", user.getDiamonds()));

        table.add(balances)
            .width(650f)
            .padBottom(16f)
            .row();

        if (user.isDebugMode()) {
            Table debugPanel = pvzInnerPanel();

            Label debugTitle = mediumTitle("DEBUG WALLET CONTROLS");
            debugTitle.setAlignment(Align.center);

            debugPanel.add(debugTitle)
                .colspan(2)
                .padBottom(10f)
                .row();

            debugPanel.add(
                    greenButton(
                        "+1000 COINS",
                        () -> addDebugResource("coin", 1000)
                    )
                )
                .width(220f)
                .height(50f)
                .pad(5f);

            debugPanel.add(
                    purpleButton(
                        "+10 GEMS",
                        () -> addDebugResource("diamond", 10)
                    )
                )
                .width(220f)
                .height(50f)
                .pad(5f);

            table.add(debugPanel)
                .width(520f)
                .padBottom(12f);
        }
    }

    private Table balanceCard(
        String labelText,
        int amount
    ) {
        Table card = pvzInnerPanel();

        Label label = new Label(labelText, skin, "medium_outline");
        label.setAlignment(Align.center);

        Label amountLabel = new Label(
            String.valueOf(amount),
            skin,
            "big_outline"
        );
        amountLabel.setAlignment(Align.center);

        card.add(label)
            .center()
            .padBottom(8f)
            .row();

        card.add(amountLabel).center();
        return card;
    }

    private void addDebugResource(String type, int amount) {
        String result = ((Wallet) menu).cheatAdd(amount, type);
        showMessage(result);
        rebuildWallet();
    }

    private void rebuildWallet() {
        if (content == null) {
            return;
        }

        content.clearChildren();
        buildContent(content);
        refreshResourceLabels();
    }

    @Override
    public void input() {
        System.out.println("=== Wallet Menu ===");
        super.input();
        if (handleGlobalCommands(input)) return;

        Matcher coinMatcher = Pattern.compile(RegexHelper.WALLET_SHOW_COIN).matcher(input);
        Matcher gemMatcher = Pattern.compile(RegexHelper.WALLET_SHOW_GEM).matcher(input);
        Matcher cheatMatcher = Pattern.compile(RegexHelper.WALLET_CHEAT).matcher(input);

        if (coinMatcher.matches()) System.out.println(((Wallet) menu).showCoinWallet());
        else if (gemMatcher.matches()) System.out.println(((Wallet) menu).showGemWallet());
        else if (cheatMatcher.matches()) {
            System.out.println(((Wallet) menu).cheatAdd(Integer.parseInt(cheatMatcher.group("amount")), cheatMatcher.group("type")));
        } else {
            System.out.println("Invalid command!");
        }
    }
}
