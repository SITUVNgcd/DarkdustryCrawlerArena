package arena.ai;

import arc.math.geom.Vec2;
import mindustry.entities.units.AIController;

import static mindustry.Vars.*;
import static mindustry.ai.Pathfinder.fieldCore;

public class CrawlerAI extends AIController {

    public int pathID = controlPath.nextTargetId();
    public Vec2 out = new Vec2();

    @Override
    public void updateUnit() {
        if (retarget() || invalid(target))
            target = findMainTarget(unit.x, unit.y, unit.range() * 2f, unit.type.targetAir, unit.type.targetGround);

        boolean shouldShoot = target != null && unit.within(target, unit.range() * 1.25f);
        if (shouldShoot) unit.aimLook(target);
        unit.controlWeapons(shouldShoot);

        if (unit.dst(world.tile(world.width() / 2, world.height() / 2)) < 32 * tilesize && target != null) {
            controlPath.getPathPosition(unit, pathID, new Vec2(target.x(), target.y()), out);
            moveTo(out, 0f, 0f);
        } else pathfind(fieldCore);

        faceTarget();
    }
}