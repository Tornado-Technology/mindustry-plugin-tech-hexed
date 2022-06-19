package data;

import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.serialization.Json;
import mindustry.game.Team;
import mindustry.gen.Player;
import utils.Utils;

import java.util.Arrays;

public class TeamData {
    public static final TeamData[] teamData = new TeamData[256];

    public Team team;

    @Nullable
    public Player owner;
    public Seq<Player> temates = new Seq<>();

    public TeamData() {
        this.team = null;
        this.owner = null;
        this.temates.clear();
    }

    public static void Init() {
        for (int i = 0; i < teamData.length; i++) {
            teamData[i] = new TeamData();
        }

        Log.info("TeamData initialized successfully!");
    }

    // region team Init & Uniti
    public static void teamInit (Player player) {
        teamInit(player.team());
    }

    public static void teamInit(Team team) {
        if (Utils.playerInTeam(team) > 1) {
            return;
        }

        TeamData data = teamData[team.id];
        data.team = team;
        data.owner = Utils.getFirstPlayer(team);
    }

    public static void Uninit (Player player) {
        Uninit(player.team());
    }

    public static void Uninit(Team team) {
        if (Utils.playerInTeam(team) > 1) {
            return;
        }

        TeamData data = teamData[team.id];
        data.team = null;
        data.owner = null;
        data.temates.clear();
    }
    // endregion

    @Nullable
    public static TeamData get(Player player) {
        return get(player.team());
    }

    @Nullable
    public static TeamData get(Team team) {
        TeamData result = teamData[team.id];
        return result.team == null ? null : result;
    }

    public static boolean isTeammate(Team team, Player player) {
        if (get(team) == null) return false;

        TeamData data = get(team);

        return data.temates.contains(player);
    }

    public static boolean isOwner(Team team, Player player) {
        if (get(team) == null) {
            Log.info("Undefined team");
            return false;
        }

        TeamData data = get(team);

        if (data.owner == null) {
            return false;
        }

        return data.owner == player;
    }

    public boolean isOwner(Player player) {
        return TeamData.isOwner(this.team, player);
    }

    public boolean isTeammate(Player player) {
        return TeamData.isTeammate(this.team, player);
    }

    public void setOwner(Player player) {
        this.owner = player;
    }

    public Player getOwner() {
        return this.owner;
    }

    // region Teamates
    public static void addTeamate(Team team, Player player) {
        if (get(team) == null) {
            Log.info("Undefined team");
            return;
        }

        TeamData data = get(team);

        if (!data.temates.contains(player)) {
            data.temates.add(player);
            Log.info("Teammate added");
        } else {
            Log.info("Already a teammate");
        }
    }

    public static void removeTeammate(Team team, Player player) {
        if (get(team) == null) {
            Log.info("Undefined team");
            return;
        }

        TeamData data = get(team);

        if (data.temates.contains(player)) {
            data.temates.remove(player);
        }
    }

    public void addTeamate(Player player) {
        TeamData.addTeamate(this.team, player);
    }

    public void removeTeammate(Player player) {
        TeamData.removeTeammate(this.team, player);
    }
    // endregion
}
