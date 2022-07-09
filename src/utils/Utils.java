package utils;

import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public class Utils {

    public static int playerNotTeam(Player player) {
        return playerNotTeam(player.team());
    }

    public static int playerNotTeam(Team team) {
        int count = 0;

        for (Player player : Groups.player) {
            if (player.team() != team) {
                count++;
            }
        }

        return count;
    }

    @Nullable
    public static Player getFirstPlayer(Team team) {
        for (Player player : Groups.player) {
            if (player.team() == team) {
                return player;
            }
        }

        return null;
    }

    public static int playerInTeam(Player player) {
        return playerInTeam(player.team());
    }

    public static int playerInTeam(Team team) {
        int count = 0;

        for (Player player : Groups.player) {
            if (player.team() == team) {
                count++;
            }
        }

        return count;
    }

    @Nullable
    public static Player playerFind(String name) {
        for (Player pl : Groups.player) {

            if (pl.name.equals(name)) {
                return pl;
            }

            if (Strings.stripColors(pl.name).equals(name)) {
                return pl;
            }
        }

        return null;
    }

    public static String teamToString(Team team) {
        return "[#" + team.color + "]Team#" + team.id + "[white]";
    }
}