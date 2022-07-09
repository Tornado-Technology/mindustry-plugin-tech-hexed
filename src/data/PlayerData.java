package data;

import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Timekeeper;
import hexed.Hex;
import mindustry.game.Team;
import mindustry.gen.Player;
import utils.Utils;

public class PlayerData {

    public static Seq<PlayerData> playerData = new Seq<>();

    public Player player;
    public Player teamRequest;

    public HexInfo hexInfo;

    public PlayerData(Player player) {
        this.player = player;
        this.teamRequest = null;
        this.hexInfo = new HexInfo();
    }

    public static void Init() {
        Log.info("PlayerData initialized successfully!");
    }

    public static void playerInit(Player player) {
        if (get(player) == null) {
            Log.info("Player registered: " + player.name);
            playerData.add(new PlayerData(player));
        }
    }

    public static void playerUninit(Player player) {
        if (get(player) != null) {
            playerData.remove(get(player));
        }
    }

    @Nullable
    public static PlayerData get(Player player) {
        for (PlayerData data : playerData) {
            if (data.player == player) {
                return data;
            }
        }

        return null;
    }

    public static void teamRequest(Player player, Player request) {
        PlayerData requestData = PlayerData.get(request);

        if (player.team() == Team.derelict) {
            player.sendMessage("[red]Spectators can't invite people to their teams");
            return;
        }

        if (requestData.teamRequest == player) {
            player.sendMessage("[red]You have already invited this player");
            return;
        }

        if (requestData.teamRequest != null) {
            player.sendMessage("[red]This player has already been invited to the team. Wait for his decision on the passed invitation!");
            return;
        }

        player.sendMessage("[coral]\"" + request.coloredName() + "[coral]\" [white]The player was invited to");
        request.sendMessage("You were invited to join the [coral]\"" + Utils.teamToString(player.team()) + "[coral]\"[white]\n Type \"/team join\" to accept the invitation or \"/team reject\" to reject the offer");
        requestData.teamRequest = player;
    }

    public class HexInfo {
        public boolean chosen;
        public @Nullable Hex location;
        public float progressPercent;
        public boolean lastCaptured;
        public Timekeeper lastMessage = new Timekeeper(1);
    }
}
