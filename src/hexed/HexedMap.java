package hexed;

import arc.Core;
import arc.util.Log;
import mindustry.game.Gamemode;
import mindustry.game.Rules;
import mindustry.gen.Call;
import mindustry.io.SaveIO;
import mindustry.maps.Map;

import static mindustry.Vars.*;
import static mindustry.Vars.netServer;

public class HexedMap {

    public static void load(Rules rules) {
        // Load map
        Map map = maps.getShuffleMode().next(Gamemode.pvp, state.map);
        Log.info("Map next: @.", map.name());
        Log.info("Map loading...");

        logic.reset();
        Call.worldDataBegin();

        Core.settings.put("lastServerMode", Gamemode.pvp.name());
        SaveIO.load(map.file, world.filterContext(map));

        state.map = map;
        state.rules = rules.copy();

        Log.info("Map loaded.");
        logic.play();
        netServer.openServer();
    }
}
