package com.payangar.immersivecompanions.entity.combat;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.ai.CompanionAssistOwnerGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionDefendVillageGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionTeamCoordinationGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;

import java.util.List;

/**
 * Aggressive combat stance: actively seeks and engages threats.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Retaliates when attacked</li>
 *   <li>Defends teammates under attack</li>
 *   <li>Assists teammates in attacking their targets</li>
 *   <li>Assists owner by attacking what the owner attacks</li>
 *   <li>Proactively attacks nearby monsters</li>
 *   <li>Defends villages from players with bad reputation</li>
 * </ul>
 */
public final class AggressiveStance implements CombatStance {

    public static final AggressiveStance INSTANCE = new AggressiveStance();

    private AggressiveStance() {}

    @Override
    public String getId() {
        return "aggressive";
    }

    @Override
    public List<TargetGoalEntry> getTargetGoals() {
        return List.of(
            // Priority 2: Retaliate when hurt (but not against owner or same-team companions)
            new TargetGoalEntry(2, c -> new HurtByTargetGoal(c) {
                @Override
                public boolean canUse() {
                    if (!super.canUse()) return false;
                    LivingEntity attacker = this.mob.getLastHurtByMob();
                    // Don't retaliate against owner
                    if (attacker != null && attacker.equals(c.getOwner())) {
                        return false;
                    }
                    return !c.isOnSameTeam(attacker);
                }
            }.setAlertOthers()),
            // Priority 3: Defend and assist teammates
            new TargetGoalEntry(3, CompanionTeamCoordinationGoal::new),
            // Priority 4: Assist owner
            new TargetGoalEntry(4, CompanionAssistOwnerGoal::new),
            // Priority 5: Proactively attack monsters
            new TargetGoalEntry(5, c -> new NearestAttackableTargetGoal<>(c, Monster.class, 10, true, false,
                    target -> shouldAttackEntity(c, target))),
            // Priority 6: Defend village
            new TargetGoalEntry(6, CompanionDefendVillageGoal::new)
        );
    }

    /**
     * Determines if the companion should attack a given entity.
     * Filters out dangerous targets like Creepers and Endermen.
     */
    private static boolean shouldAttackEntity(CompanionEntity companion, LivingEntity entity) {
        // Don't attack Creepers or Endermen
        if (entity instanceof Creeper || entity instanceof EnderMan) {
            return false;
        }
        return entity instanceof Monster;
    }
}
