package hexed;

import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;

import mindustry.world.*;

public class HexedGenerator {
    public int width = Hex.size;
    public int height = Hex.size;

    public IntSeq getHex(){
        IntSeq array = new IntSeq();
        double h = Math.sqrt(3) * Hex.spacing / 2;

        // Base horizontal spacing 1.5w
        // Offset 3/4w
        for(int x = 0; x < width / Hex.spacing - 2; x++) {
            for (int y = 0; y < height / (h / 2) - 2; y++) {
                int cx = (int)(x * Hex.spacing * 1.5 + (y % 2) * Hex.spacing * 3.0 / 4) + Hex.spacing / 2;
                int cy = (int)(y * h / 2) + Hex.spacing / 2;
                array.add(Point2.pack(cx, cy));
            }
        }
        return array;
    }
}
