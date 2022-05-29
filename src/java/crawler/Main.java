package crawler;

import arc.Events;
import arc.math.Mathf;
import arc.util.CommandHandler;
import arc.util.Strings;
import arc.util.Timer;
import crawler.ai.DefaultAI;
import crawler.boss.BossBullets;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Rules;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.Administration.ActionType;
import mindustry.type.UnitType;

import static arc.Core.app;
import static crawler.Bundle.*;
import static crawler.CrawlerVars.*;
import static mindustry.Vars.*;

public class Main extends Plugin {

    public static final Rules rules = new Rules();

    public static boolean isWaveGoing, firstWaveLaunched;
    public static float statScaling;

    @Override
    public void init() {
        CrawlerVars.load();
        CrawlerLogic.load();

        netServer.admins.addActionFilter(action -> action.type != ActionType.breakBlock && action.type != ActionType.placeBlock);

        content.units().each(unit -> unit.constructor.get() instanceof WaterMovec, unit -> unit.flying = true);
        content.units().each(type -> {
            type.payloadCapacity = 6f * 6f * tilePayload;
            type.maxRange = Float.MAX_VALUE;
            type.defaultController = DefaultAI::new;
        });

        Events.on(WorldLoadEvent.class, event -> app.post(CrawlerLogic::play));
        Events.on(PlayerJoin.class, event -> CrawlerLogic.join(event.player));

        Timer.schedule(() -> sendToChat("events.tip.info"), 90f, 180f);
        Timer.schedule(() -> sendToChat("events.tip.upgrades"), 180f, 180f);

        Timer.schedule(BossBullets::update, 0f, .1f);

        Timer.schedule(() -> {
            if (state.gameOver || Groups.player.isEmpty()) return;

            if (state.wave == 0 && !firstWaveLaunched) {
                firstWaveLaunched = true;
                sendToChat("events.first-wave", waveDelay);
                Timer.schedule(CrawlerLogic::runWave, waveDelay);
            }

            if (rules.defaultTeam.data().unitCount == 0 && isWaveGoing) {
                isWaveGoing = false;

                PlayerData.each(data -> Call.infoMessage(data.player.con, Bundle.get("events.lose", data.locale)));
                Timer.schedule(() -> Events.fire(new GameOverEvent(state.rules.waveTeam)), 3f);

                Call.hideHudText();
                return;
            } else if (rules.waveTeam.data().unitCount == 0 && isWaveGoing) {
                isWaveGoing = false;

                BossBullets.bullets.clear();

                if (state.wave >= winWave) {
                    PlayerData.each(data -> Call.infoMessage(data.player.con, Bundle.get("events.victory", data.locale)));
                    Timer.schedule(() -> Events.fire(new GameOverEvent(state.rules.defaultTeam)), 3f);
                    return;
                }

                int delay = waveDelay;
                if (state.wave >= helpMinWave && state.wave % helpSpacing == 0) {
                    CrawlerLogic.spawnReinforcement();
                    delay += helpExtraTime; // megas need time to deliver blocks
                }

                sendToChat("events.next-wave", delay);
                Timer.schedule(CrawlerLogic::runWave, delay);
                PlayerData.each(PlayerData::afterWave);
            }

            PlayerData.each(data -> Call.setHudText(data.player.con, format("ui.money", data.locale, data.money)));
        }, 0f, 1f);
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("upgrade", "<type> [amount]", "Upgrade your unit.", (args, player) -> {
            if (args.length == 2 && Strings.parseInt(args[1]) <= 0) {
                bundled(player, "commands.upgrade.invalid-amount");
                return;
            }

            UnitType type = costs.keys().toSeq().find(u -> u.name.equalsIgnoreCase(args[0]));
            if (type == null) {
                bundled(player, "commands.upgrade.unit-not-found");
                return;
            }

            int amount = args.length == 2 ? Strings.parseInt(args[1]) : 1;
            if (rules.defaultTeam.data().countType(type) + amount > unitCap) {
                bundled(player, "commands.upgrade.too-many-units");
                return;
            }

            PlayerData data = PlayerData.datas.get(player.uuid());
            if (data.money < costs.get(type) * amount) {
                bundled(player, "commands.upgrade.not-enough-money", costs.get(type) * amount, data.money);
                return;
            }

            for (int i = 0; i < amount; i++) {
                Unit unit = type.spawn(player.x + Mathf.range(8f), player.y + Mathf.range(8f));
                data.applyUnit(unit);
            }

            data.money -= costs.get(type) * amount;
            bundled(player, "commands.upgrade.success", amount, type.name);
        });

        handler.<Player>register("upgrades", "Show units you can upgrade to.", (args, player) -> {
            PlayerData data = PlayerData.datas.get(player.uuid());
            StringBuilder upgrades = new StringBuilder(format("commands.upgrades.header", data.locale));
            costs.each((type, cost) -> upgrades.append("[gold] - [accent]").append(type.name).append(" [lightgray](").append(data.money < cost ? "[scarlet]" : "[lime]").append(cost).append("[])\n"));
            player.sendMessage(upgrades.toString());
        });
    }
}
