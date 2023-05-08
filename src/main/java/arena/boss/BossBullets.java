package arena.boss;

import arc.func.Floatc2;
import arc.graphics.Color;
import arc.struct.Seq;
import arc.util.Timer;
import mindustry.content.*;
import mindustry.entities.Damage;
import mindustry.gen.*;
import mindustry.world.blocks.defense.turrets.*;

import static mindustry.Vars.state;

public class BossBullets {

    public static final Seq<BossBullet> bullets = new Seq<>();

    public static void update() {
        bullets.each(BossBullet::update);
        bullets.filter(BossBullet::alive);
    }

    // #region bullets

    public static void toxopidMount(float x, float y) {
        var weapon = UnitTypes.toxopid.weapons.get(0);
        new StarBullet(x, y, 180, 3, 10f, weapon.bullet, weapon.shootSound);
    }

    public static void corvusLaser(float x, float y) {
        var weapon = UnitTypes.corvus.weapons.get(0);
        new SnakeBullet(x, y, 120, 20f, 12f, weapon.bullet, weapon.shootSound);
    }

    public static void titaniumFuse(float x, float y) {
        var turret = (ItemTurret) Blocks.fuse;
        new StarBullet(x, y, 90, 5, 8f, turret.ammoTypes.get(Items.titanium), turret.shootSound);
    }

    public static void thoriumFuse(float x, float y) {
        var turret = (ItemTurret) Blocks.fuse;
        new StarBullet(x, y, 90, 5, 8f, turret.ammoTypes.get(Items.thorium), turret.shootSound);
    }

    public static void arcLight(float x, float y) {
        var turret = (PowerTurret) Blocks.arc;
        new StarBullet(x, y, 90, 8, 12f, turret.shootType, turret.shootSound);
    }

    public static void atomic(float x, float y) {
        new AtomicBullet(x, y);
    }

    // #endregion
    // #region visual effects

    public static void timer(float x, float y, Floatc2 cons, float delay) {
        for (int i = 0; i < delay; i++) Timer.schedule(() -> inst(x, y), i);
        Timer.schedule(() -> cons.get(x, y), delay);
    }

    public static void timer(float x, float y, Floatc2 cons) {
        timer(x, y, cons, 3f);
    }

    public static void inst(float x, float y) {
        Call.effect(Fx.instBomb, x, y, 0, Color.white);
        Call.soundAt(Sounds.railgun, x, y, 0.8f, 1f);
    }

    public static void impact(float x, float y) {
        Call.effect(Fx.impactReactorExplosion, x, y, 0, Color.white);
        Call.soundAt(Sounds.explosionbig, x, y, 0.8f, 1f);
    }

    public static void thorium(float x, float y) {
        Call.effect(Fx.reactorExplosion, x, y, 0, Color.white);
        Call.soundAt(Sounds.explosionbig, x, y, 0.8f, 1f);
        Damage.damage(state.rules.waveTeam, x, y, 240f, 4800f);
    }

    // #endregion
}