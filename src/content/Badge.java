package content;

import arc.struct.Seq;
import mindustry.gen.Player;

import java.util.HashMap;
import java.util.Map;

public class Badge {

    // public final static Map<String, Badge> List = new HashMap<String, Badge>();

    private String symbol;
    private String color;

    public Badge(String symbol, String color) {
        this.symbol = symbol;
        this.color = color;
    }

    public String symbol() {
        return this.symbol;
    }

    public String color() {
        return this.color;
    }

    public String format() {
        return "[coral]<[" + this.color + "]" + this.symbol + "[coral]>[white]";
    }

    public String format(Player player, String message) {
        return "[coral]<[" + this.color + "]" + this.symbol + "[coral]> [coral][[" + player.coloredName() + "[coral]]:[white] " + message;
    }
}
