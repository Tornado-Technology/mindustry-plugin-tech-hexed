package hexed;

import java.util.*;

import arc.math.geom.*;
import arc.util.*;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

public class Hex {
    private float[] progress = new float[256];

    public final static int size     = 516;
    public final static int diameter = 74;
    public final static int radius   = diameter / 2;
    public final static int spacing  = 78;

    public final int id;
    public final int x;
    public final int y;
    public final int wx;
    public final int wy;
    public final float tileRadius = radius * tilesize;
    public final float tileDiameter = tileRadius * 2;

    public @Nullable Team controller;
    public Timekeeper spawnTime = new Timekeeper(0);

    public Hex(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        wx = x * tilesize;
        wy = y * tilesize;
    }

    // Updates
    public void updateController() {
        controller = findController();
    }

    @Nullable
    public Team findController() {
        if (hasCore()) {
            return world.tile(x, y).team();
        }

        for (int i = 0; i < progress.length; i++) {
            progress[i] = 0F;
        }

        for (int cx = x - radius; cx < x + radius; cx++) {
            for (int cy = y - radius; cy < y + radius; cy++) {
                Tile tile = world.tile(cx, cy);

                if (tile != null && tile.synthetic() && contains(tile) && tile.block().requirements != null) {
                    for (ItemStack stack : tile.block().requirements) {
                        progress[tile.team().id] += (stack.amount * stack.item.cost) / (Math.pow(tile.block().size, 2));
                    }
                }
            }
        }

        TeamData data = state.teams.getActive().max(t -> progress[t.team.id]);
        if (data != null && data.team != Team.derelict && progress[data.team.id] >= HexedMod.itemRequirement) {
            return data.team;
        }

        return null;
    }

    // Blocks check
    public boolean hasCore(){
        return world.tile(x, y).team() != Team.derelict && world.tile(x, y).block() instanceof CoreBlock;
    }

    public boolean contains(Tile tile) {
        return contains(tile.worldx(), tile.worldy());
    }

    public boolean contains(float x, float y) {
        return Intersector.isInsideHexagon(wx, wy, tileDiameter, x, y);
    }

    // Progress
    public float getProgress(Team team){
        return progress[team.id];
    }

    public float getProgressPercent(Team team){
        return progress[team.id] / HexedMod.itemRequirement * 100;
    }
}
