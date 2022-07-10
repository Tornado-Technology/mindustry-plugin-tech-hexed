package hexed;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import content.Badge;
import data.DataBase;
import data.PlayerData;
import data.TeamData;
import hexed.HexData.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.core.NetServer.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.game.Teams;
import mindustry.gen.*;
import mindustry.gen.Player;
import mindustry.mod.*;
import mindustry.net.Packets.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import org.jetbrains.annotations.NotNull;
import utils.Utils;

import java.util.Objects;

import static mindustry.Vars.*;
import static mindustry.Vars.player;
import static mindustry.game.Team.derelict;

public class HexedMod extends Plugin {

    // Server info
    public static final String serverName = "[red]\uE83A TechHexes";
    public static final String serverDesc = "[orange]\uE861 TeamHex[red]PvP";
    public static final String serverMode = "TeamHexPvP";
    public static final String discordLink = "https://discord.gg/hADs5A4X4V";

    public static final boolean customConnectMessages = true;
    public static final boolean welcomeMessage = true;
    public static final boolean captureCore = true;

    // Item requirement to capture a hex
    public static final int itemRequirement = 395;

    // region Time
    private final static int roundTime = 60 * 60 * 90;

    private final static int leaderboardTime = 60 * 60 * 2;

    private final static int updateTime = 60 * 2;
    // endregion

    private final static int winCondition = 10;

    private final static int timerBoard = 0, timerUpdate = 1, timerWinCheck = 2;

    private final Rules rules = new Rules();
    private final Interval interval = new Interval(5);

    private HexData hexData;
    private boolean restarting = false, registered = false;

    private Schematic start;
    private double counter = 0f;
    private int lastMin;

    public enum BadgeID {
        Creator,
        Moderator,
        Active,
    }

    public static final Badge[] Badges = {
      new Badge("\uE80E", "maroon"),
      new Badge("\uE817", "red"),
      new Badge("\uE86E", "gold"),
    };

    private void coreSettings() {
        Core.settings.put("servername", serverName);
        Core.settings.put("desc", serverDesc);
        Core.settings.put("showConnectMessages", !customConnectMessages);
        Core.settings.put("crashReport", true);
    }

    private void rulesSettings() {
        rules.attackMode = true;
        rules.pvp = false;
        rules.modeName = serverMode;
        rules.tags.put("hexed", "true");
        rules.buildSpeedMultiplier = 2f;
        rules.enemyCoreBuildRadius = 35f;
        rules.canGameOver = false;
        rules.coreCapture = true;

        // region Loadout
        rules.loadout = ItemStack.list(
            Items.copper, 1000,
            Items.lead, 1000,
            Items.graphite, 200,
            Items.silicon, 200,
            Items.metaglass, 120,
            Items.titanium, 250,
            Items.plastanium, 100
        );
        // endregion

        // region RevealedBlocks
        rules.revealedBlocks.addAll(
            Blocks.duct,
            Blocks.ductRouter,
            Blocks.ductBridge,
            Blocks.thruster,
            Blocks.scrapWall,
            Blocks.scrapWallLarge,
            Blocks.scrapWallHuge,
            Blocks.scrapWallGigantic
        );
        // endregion
    }

    @Override
    public void init() {
        coreSettings();
        rulesSettings();

        DataBase.Init();
        PlayerData.Init();
        TeamData.Init();

        start = Schematics.readBase64("bXNjaAF4nE1SW27bMBAckpJFUnKCHMQfac/REwT9YGS2MKCIgiQn8NWLAO4ONx8hJA33MbPkrtDj0aGZ01tG9/zjJx/057yN62XZL2UGcJjSa5422Jffj3gay7Lk9fSRpuk0pfVvRv/NBT+W+T3fygqX1hH9ViTptKQ5TxjWvKSLWOUy7+i2Me17XhGX8iH0uZwzhrGs+TRfxylfNzx9Y38VO7zl+Swkf52nkrjrKfknjXtZb+heq+YN7Zam9yKn/yUvDKwRcKirASzvpdApeI0FJgO9WgN5tI0TcHBWoPKM8gx5jPVwzHyAMVXONDXqCFZB6K1AQ02LlmJWxayKWYoxFuAOAlGtgee2OLICbyBiTqUdVTsK+6pUS9Itr5cKHaot/gCWtlGsBqaXT2wlaAbxD+Yuq2Y+8GwtCUeBhuItom/unzUsnbj/+yrTanKt2gs01AN3VA61TzWjq/0XkP4cwNOKv+Pl6DwyxeuMvDbFC9MSOgWvKYH39+yG4ZR0WNIfxwhVgw42KD0oPZDOWOCcAudEqw426Lii9jcqLyovatnIP+E/QcxTaQ==");

        // region Update
        Events.run(Trigger.update, () -> {
            // Update only if game running
            if (!gameActive()) {
                counter = 0;
                return;
            }

            hexData.updateStats();

            for (Player player : Groups.player) {
                if (player.team() != derelict && player.team().cores().isEmpty()) {
                    player.clearUnit();
                    kill(player, true);
                    Call.sendMessage("[yellow](!)[] [accent]" + player.name + "[lightgray] has been eliminated![yellow] (!)");
                    Call.infoMessage(player.con, "Your cores have been destroyed. You are defeated.");
                    player.team(derelict);
                }

                if(hexData.getControlled(player).size == hexData.hexes().size) {
                    gameEnd();
                    break;
                }
            }

            int minutesToGo = (int)(roundTime - counter) / 3600;
            if (minutesToGo != lastMin) {
                lastMin = minutesToGo;
            }

            if (interval.get(timerBoard, leaderboardTime)) {
                Call.infoToast(getLeaderboard(), 15f);
            }

            if (interval.get(timerUpdate, updateTime)) {
                hexData.updateControl();
            }

            if (interval.get(timerWinCheck, 120)) {
                Seq<Player> players = hexData.getLeaderboard();
                if (!players.isEmpty() && hexData.getControlled(players.first()).size >= winCondition && players.size > 1 && hexData.getControlled(players.get(1)).size <= 1){
                    gameEnd();
                }
            }

            counter += Time.delta;

            // Kick everyone and restart game
            if (counter > roundTime && !restarting) {
                gameEnd();
            }
        });
        // endregion

        // region Events
        Events.on(BlockDestroyEvent.class, (event) -> {
            // Reset last spawn times so this hex becomes vacant for a while.
            if (event.tile.block() instanceof CoreBlock) {
                Hex hex = hexData.getHex(event.tile.pos());

                if (hex != null) {
                    // Update state
                    hex.spawnTime.reset();
                    hex.updateController();
                }
            }
        });

        Events.on(PlayerJoin.class, (event) -> {
            if (!gameActive() || event.player.team() == derelict) return;
            Player player = event.player;

            if (customConnectMessages) {
                Call.sendMessage("[coral]<[green]+[coral]> " + event.player.coloredName());
            }

            PlayerData.playerInit(event.player);
            TeamData.teamInit(event.player);

            player.sendMessage("Welcome to " + serverName + " \n[blue]\uE80DDiscord: [white]" + discordLink);
            Seq<Hex> copy = hexData.hexes().copy();
            copy.shuffle();
            Hex hex = copy.find(h -> h.controller == null && h.spawnTime.get());

            if (hex != null) {
                loadout(player, hex.x, hex.y);
                Core.app.post(() ->  PlayerData.get(player).hexInfo.chosen = false);
                hex.findController();
            } else {
                Call.infoMessage(event.player.con, "There are currently no empty hex spaces available.\nAssigning into spectator mode.");
                player.unit().kill();
                player.team(derelict);
            }

            PlayerData.get(player).hexInfo.lastMessage.reset();
        });

        Events.on(PlayerLeave.class, (event) -> {
            if (!gameActive() || event.player.team() == derelict) return;
            Player player = event.player;

            TeamData teamData = TeamData.get(player);

            if (teamData.isOwner(player)) {
                if (teamData.temates.size > 0) {
                    Player newOwner = teamData.temates.get(0);
                    teamData.removeTeammate(newOwner);
                    teamData.setOwner(newOwner);
                }
            }

            if (teamData.isTeammate(player)) {
                teamData.removeTeammate(player);
            }

            PlayerData.playerUninit(player);
            TeamData.Uninit(player);

            if (customConnectMessages) {
                Call.sendMessage("[coral]<[red]-[coral]> " + event.player.coloredName());
            }

            kill(player, false);
        });

        Events.on(ProgressIncreaseEvent.class, (event) -> updateText(event.player));

        Events.on(HexCaptureEvent.class, (event) -> {
            updateText(event.player);
            world.tile(event.hex.x, event.hex.y).setNet(Blocks.coreShard, event.player.team(), 0);
        });

        Events.on(HexMoveEvent.class, (event) -> updateText(event.player));
        // endregion

        // region NetServer
        netServer.chatFormatter = (player, message) -> format(player, message);

        TeamAssigner prev = netServer.assigner;
        netServer.assigner = (player, players) -> {
            Seq<Player> arr = Seq.with(players);

            if (!gameActive()) {
                return prev.assign(player, players);
            }

            // Pick first inactive team
            for (Team team : Team.all) {
                if (team.id > 5 && !team.active() && Utils.playerInTeam(team) == 0) {
                    return team;
                }
            }

            Call.infoMessage(player.con, "There are currently no empty hex spaces available.\nAssigning into spectator mode.");
            return derelict;
        };

        // endregion

        gameStart();
    }

    public static String format(Player player, String message) {
        if (player.admin) {
            return HexedMod.Badges[HexedMod.BadgeID.Moderator.ordinal()].format(player, message);
        }

        return "[coral][[" + player.coloredName() + "[coral]]:[white] " + message;
    }

    public void gameStart() {
        Log.info("&ly--SERVER STARTING--");
        hexData = new HexData();
        hexData.initHexes(HexedGenerator.getHex());
        HexedMap.load(rules);
    }

    public void gameEnd() {
        if (restarting) return;

        restarting = true;

        Seq<Player> players = hexData.getLeaderboard();
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < players.size && i < 3; i++){
            if(hexData.getControlled(players.get(i)).size > 1){
                builder.append("[yellow]").append(i + 1).append(".[accent] ").append(players.get(i).name).append("[lightgray] (x").append(hexData.getControlled(players.get(i)).size).append(")[]\n");
            }
        }

        if (!players.isEmpty()) {
            boolean dominated = hexData.getControlled(players.first()).size == hexData.hexes().size;

            for (Player player : Groups.player) {
                Call.infoMessage(player.con,
                    "[accent]--ROUND OVER--\n\n[lightgray]" +
                    (player == players.first() ? "[accent]You[] were" : "[yellow]" + players.first().name + "[lightgray] was") +
                        " victorious, with [accent]" + hexData.getControlled(players.first()).size + "[lightgray] hexes conquered." + (dominated ? "" : "\n\nFinal scores:\n" + builder));
            }
        }

        Log.info("&ly--SERVER RESTARTING--");
        Time.runTask(60f * 10f, () -> {
            netServer.kickAll(KickReason.serverRestarting);

            // Clean building data
            for (Player player : Groups.player) {
                Team team = player.team();
                team.data().blocks.clear();
            }

            Time.runTask(5f, () -> {
                net.closeServer();
                state.set(State.menu);
                counter = 0;

                PlayerData.clear();
                TeamData.clear();

                gameStart(); // Start server
            });
        });
    }

    public boolean gameActive() {
        return state.rules.tags.getBool("hexed") && !state.is(State.menu);
    }

    // region Commands
    @Override
    public void registerServerCommands(@NotNull CommandHandler handler){
        handler.register("countdown", "Get the hexed restart countdown.", (args) -> Log.info("Time until round ends: &lc@ minutes", (int)(roundTime - counter) / 3600));
        handler.register("end", "End the game.", (args) -> gameEnd());
        handler.register("restart", "Restart the server.", (args) -> gameEnd());
    }

    @Override
    public void registerClientCommands(@NotNull CommandHandler handler){
        if (registered) return;
        registered = true;

        handler.removeCommand("a");
        handler.removeCommand("vote");
        handler.removeCommand("votekick");

        // Command: /discord
        handler.<Player>register("discord", "Discord link", (args, player) -> player.sendMessage("[blue]\uE80DDiscord: [white]" + discordLink));

        // Command: /spectate
        handler.<Player>register("spectate", "Enter spectator mode. This destroys your base.", (args, player) -> {
            if (player.team() == derelict) {
                player.sendMessage("[scarlet]You're already spectating.");
            } else {
                kill(player, false);
                player.unit().kill();
                player.team(derelict);
            }
        });

        // Command: /captured
        handler.<Player>register("captured", "Dispay the number of hexes you have captured.", (args, player) -> {
            if (player.team() == derelict) {
                player.sendMessage("[scarlet]You're spectating.");
            } else{
                player.sendMessage("[lightgray]You've captured[accent] " + hexData.getControlled(player).size + "[] hexes.");
            }
        });

        // Command: /leaderboard
        handler.<Player>register("leaderboard", "Display the leaderboard", (args, player) -> player.sendMessage(getLeaderboard()));

        // Command: /hexstatus
        handler.<Player>register("hexstatus", "Get hex status at your position.", (args, player) -> {
            Hex hex = PlayerData.get(player).hexInfo.location;
            if (hex != null) {
                hex.updateController();
                StringBuilder builder = new StringBuilder();
                builder.append("| [lightgray]Hex #").append(hex.id).append("[]\n");
                builder.append("| [lightgray]Owner:[] ").append(hex.controller != null && hexData.getPlayer(hex.controller) != null ? hexData.getPlayer(hex.controller).name : "<none>").append("\n");
                for (Teams.TeamData data : state.teams.getActive()){
                    if(hex.getProgressPercent(data.team) > 0){
                        builder.append("|> [accent]").append(this.hexData.getPlayer(data.team).name).append("[lightgray]: ").append((int)hex.getProgressPercent(data.team)).append("% captured\n");
                    }
                }
                player.sendMessage(builder.toString());
            } else {
                player.sendMessage("[scarlet]No hex found.");
            }
        });

        // Command: /respawn
        handler.<Player>register("respawn", "Destroys all buildings and transfers to the new core", (args, player) -> respawn(player, false));

        // Command: /join
        handler.<Player>register("join", "Accepts an invitation to join the team", (args, player) ->{
            PlayerData data = PlayerData.get(player);

            if (data.teamRequest == null) {
                player.sendMessage("[red]You have no invitations!");
                return;
            }

            Team team = player.team();
            player.team(data.teamRequest.team());
            changeTeam(team, data.teamRequest.team());
            TeamData.addTeamate(data.teamRequest.team(), player);

            player.sendMessage("[coral]<[green]+[coral]>[green] You joined to [coral]\"" + Utils.teamToString(data.teamRequest.team()) + "[coral]\"");
            data.teamRequest.sendMessage("[coral]\"" + player.coloredName() + "[coral]\" joined your team");
            data.teamRequest = null;
        });

        // Command: /reject
        handler.<Player>register("reject", "Rejects the invitation to join the team", (args, player) -> {
            PlayerData data = PlayerData.get(player);

            if (data.teamRequest == null) {
                player.sendMessage("[red]You have no invitations!");
                return;
            }

            player.sendMessage("[coral]<[red]-[coral]>[red] You rejected invite to [coral]\"" + Utils.teamToString(data.teamRequest.team()) + "[coral]\"");
            data.teamRequest.sendMessage("[coral]\"" + player.coloredName() + "[coral]\"[white] didn't want to join your team");
            data.teamRequest = null;
        });

        // Command: /myteam
        handler.<Player>register("myteam", "Show your team", (args, player) -> {
            player.sendMessage("Your team: [coral]\"" + Utils.teamToString(player.team()) + "[coral]\"");
        });

        // Command: /teammates
        handler.<Player>register("teammates", "Shows your teammates and owner", (args, player) -> {
            TeamData teamData = TeamData.get(player);

            StringBuilder builder = new StringBuilder();
            builder.append("Your Team:\n\n").append("Owner: [white]").append(teamData.owner == null ? "[red]Not" : teamData.owner.coloredName()).append("\n").append("[white]Teammates: [white]");

            if (teamData.temates.size == 0) {
                builder.append("[red]Not");
            } else {
                builder.append("\n");
                for (int i = 0; i < teamData.temates.size; i++) {
                    Player pl = teamData.temates.get(i);
                    builder.append("  [orange]").append(i + 1).append(". ").append(pl.coloredName()).append("\n");
                }
            }

            player.sendMessage(builder.toString());
        });

        // Command: /team
        handler.<Player>register("team", "<invite/kick> <playerName>", "For teaming and teamwork", (args, player) -> {
            String cmd = args[0];
            String name = args[1];

            if (Utils.playerFind(name) == null) {
                player.sendMessage("[coral]\"[white]" + name + "[coral]\"[red] doesn't exist! ");
                return;
            }

            Player findePlayer = Utils.playerFind(name);

            if (findePlayer.team() == derelict) {
                player.sendMessage("[coral]\"[white]" + name + "[coral]\"[red] spectator. You cannot work with that command!");
                return;
            }

            if (TeamData.get(player) == null) {
                player.sendMessage("[red]Team not exists!");
                return;
            }

            TeamData data = TeamData.get(player);

            switch (cmd) {
                case "invite":
                    if (findePlayer == player) {
                        player.sendMessage("[red]You can't invite yourself!");
                        break;
                    }

                    if (data.isTeammate(findePlayer)) {
                        player.sendMessage("[red]You can't invite your teammate!");
                        break;
                    }

                    PlayerData.teamRequest(player, findePlayer);
                    break;

                case "kick":
                    if (!data.isOwner(player)) {
                        player.sendMessage("[red]You are not the owner. only the owner can kick the player!");
                        break;
                    }

                    if (!data.isTeammate(findePlayer)) {
                        player.sendMessage("[coral]\"" + findePlayer.coloredName() + "[coral]\"[red] this is not your teammate. You can only kick out your teammate!");
                        break;
                    }

                    data.removeTeammate(findePlayer);
                    findePlayer.unit().kill();
                    findePlayer.team(derelict);
                    findePlayer.sendMessage("[red]You were kicked off the [coral]\"[#" + Utils.teamToString(player.team()) + "[coral]\"[red]!");
                    break;

                default:
                    player.sendMessage("[red]Team wrong argument!");
                    break;
            }
        });
    }
    // endregion

    // region Spawn
    void respawn(Player player, boolean forced) {
        kill(player, forced);
        spwan(player);
    }

    void spwan(Player player) {
        if (!gameActive() || player.team() == Team.derelict) return;

        Seq<Hex> copy = hexData.hexes().copy();
        copy.shuffle();
        Hex hex = copy.find(h -> h.controller == null && h.spawnTime.get());

        if (hex != null) {
            loadout(player, hex.x, hex.y);
            Core.app.post(() ->  PlayerData.get(player).hexInfo.chosen = false);
            hex.findController();
        } else {
            Call.infoMessage(player.con, "There are currently no empty hex spaces available.\nAssigning into spectator mode.");
            player.unit().kill();
            player.team(derelict);
        }

        PlayerData.get(player).hexInfo.lastMessage.reset();
    }
    // endregion

    // region Leaderboard
    public String getLeaderboard() {
        StringBuilder builder = new StringBuilder();
        builder.append("[accent]Leaderboard\n[scarlet]").append(lastMin).append("[lightgray] mins. remaining\n\n");
        int count = 0;

        Seq<Team> used = new Seq<>();
        for (Player player : hexData.getLeaderboard()) {
            Team team = player.team();
            String name;

            if (used.contains(team)) {
                continue;
            }

            used.add(team);

            name = getTeamName(team);

            int hexes = hexData.getControlled(player).size;
            builder.append("[yellow]").append(++count).append(".[white] ").append(name).append("[orange] (").append(hexes).append(hexes > 1 ? " hexes)" : " hex)").append("[white]\n");
            if (count > 4) break;
        }

        return builder.toString();
    }

    public void updateText(Player player){
        PlayerData.HexInfo team =  PlayerData.get(player).hexInfo;

        StringBuilder message = new StringBuilder("[white]Hex #" + team.location.id + "\n");

        if (!team.lastMessage.get()) return;

        if (team.location.controller == null){
            if (team.progressPercent > 0) {
                message.append("[lightgray]Capture progress: [accent]").append((int)(team.progressPercent)).append("%");
            } else {
                message.append("[lightgray][[Empty]");
            }
        } else if(team.location.controller == player.team()) {
            message.append("[yellow][[Captured]");
        } else if(team.location != null && team.location.controller != null && hexData.getPlayer(team.location.controller) != null) {
            message.append("Captured by ").append(getTeamName(hexData.getPlayer(team.location.controller)));
        } else {
            message.append("<Unknown>");
        }

        Call.setHudText(player.con, message.toString());
    }
    // endregion

    // region Team
    public void changeTeam(Player oldTeam, Player newTeam) {
        changeTeam(oldTeam.team(), newTeam.team());
    }

    public void changeTeam(Team oldTeam, Team newTeam) {
        transferTeamItems(oldTeam, newTeam);
        changeTeamUnits(oldTeam, newTeam);
        changeTeamTiles(oldTeam, newTeam);
    }

    public void transferTeamItems(Team oldTeam, Team newTeam) {
        newTeam.data().core().items.add(oldTeam.data().core().items());
        oldTeam.data().core().items().clear();
    }

    public void changeTeamUnits(Team oldTeam, Team newTeam) {
        for (Unit unit : oldTeam.data().units) {
            unit.team(newTeam);
        }
    }

    public void changeTeamTiles(Team oldTeam, Team newTeam) {
        for (int x = 0; x < world.width(); x++) {
            for (int y = 0; y < world.height(); y++) {
                Tile tile = world.tile(x, y);
                if (tile.build != null && tile.team() == oldTeam) {
                    if (tile.block() instanceof CoreBlock) {
                        Block block = tile.block();
                        tile.removeNet();
                        world.tile(x + (int)Math.ceil(block.size / 2), y + (int)Math.ceil(block.size / 2)).setNet(block, newTeam, 0);
                    } else {
                        Call.setTeam(tile.build, newTeam);
                    }
                }
            }
        }
    }
    // endregion

    // region Kill
    public void kill(Player player, boolean forced) {
        kill(player.team(), forced);
    }

    public void kill(Team team, boolean forced) {
        if (!forced && Utils.playerInTeam(team) > 1) {
            return;
        }

        killUnits(team);
        killTiles(team);
    }

    public void killUnits(Team team) {
        for (Unit unit : team.data().units) {
            unit.kill();
        }
    }

    public void killTiles(Team team) {
        for (int x = 0; x < world.width(); x++) {
            for (int y = 0; y < world.height(); y++) {
                Tile tile = world.tile(x, y);

                if (tile.build != null && tile.team() == team) {
                    if (tile.block() instanceof CoreBlock) {
                        tile.removeNet();
                    } else {
                        Time.run(Mathf.random(60f * 6), tile.build::kill);
                    }
                }
            }
        }
    }
    // endregion

    public void loadout(Player player, int x, int y) {
        Stile coreTile = start.tiles.find(s -> s.block instanceof CoreBlock);
        if (coreTile == null) throw new IllegalArgumentException("Schematic has no core tile. Exiting.");
        int ox = x - coreTile.x, oy = y - coreTile.y;
        start.tiles.each(st -> {
            Tile tile = world.tile(st.x + ox, st.y + oy);
            if(tile == null) return;

            if(tile.block() != Blocks.air){
                tile.removeNet();
            }

            tile.setNet(st.block, player.team(), st.rotation);

            if(st.config != null){
                tile.build.configureAny(st.config);
            }
            if(tile.block() instanceof CoreBlock){
                for(ItemStack stack : state.rules.loadout){
                    Call.setItem(tile.build, stack.item, stack.amount);
                }
            }
        });
    }

    public String getTeamName(Player player) {
        return getTeamName(player.team());
    }

    public String getTeamName(Team team) {
        if (Utils.playerInTeam(team) > 1) {
            return "[#" + team.color + "]Team#" + team.id;
        } else {
            return Utils.getFirstPlayer(team).coloredName();
        }
    }
}
