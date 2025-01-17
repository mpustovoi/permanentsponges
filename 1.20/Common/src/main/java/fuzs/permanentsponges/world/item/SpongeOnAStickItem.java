package fuzs.permanentsponges.world.item;

import fuzs.permanentsponges.PermanentSponges;
import fuzs.permanentsponges.config.ServerConfig;
import fuzs.permanentsponges.world.level.block.sponge.SetSpongeTask;
import fuzs.permanentsponges.world.level.block.sponge.SpongeMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class SpongeOnAStickItem extends Item {
    private final SpongeMaterial spongeMaterial;

    public SpongeOnAStickItem(Properties properties, SpongeMaterial spongeMaterial) {
        super(properties);
        this.spongeMaterial = spongeMaterial;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        BlockHitResult blockHitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (blockHitResult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemStack);
        } else if (blockHitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemStack);
        } else {
            BlockPos blockPos = blockHitResult.getBlockPos();
            Direction direction = blockHitResult.getDirection();
            BlockPos blockPos2 = blockPos.relative(direction);
            if (!level.mayInteract(player, blockPos) || !player.mayUseItemAt(blockPos2, direction, itemStack)) {
                return InteractionResultHolder.fail(itemStack);
            } else {
                BlockState blockState = level.getBlockState(blockPos);
                if (blockState.getBlock() instanceof BucketPickup bucketPickup) {
                    int cooldownTicks = PermanentSponges.CONFIG.get(ServerConfig.class).stickCooldownTicks;
                    if (cooldownTicks > 0) {
                        player.getCooldowns().addCooldown(this, cooldownTicks);
                    }
                    if (level instanceof ServerLevel serverLevel) {
                        int distance = this.spongeMaterial.getStickDistance();
                        boolean vanish = this.spongeMaterial.shouldDestroyTouchingHot();
                        boolean hasDestroyedSource = SetSpongeTask.instant(serverLevel, Blocks.AIR, blockPos, distance, vanish);
                        if (hasDestroyedSource) itemStack.setDamageValue(itemStack.getMaxDamage() - 1);
                        itemStack.hurtAndBreak(1, player, playerx -> playerx.broadcastBreakEvent(usedHand));
                    }
                    player.awardStat(Stats.ITEM_USED.get(this));
                    bucketPickup.getPickupSound().ifPresent(soundEvent -> player.playSound(soundEvent, 1.0F, 1.0F));
                    level.gameEvent(player, GameEvent.FLUID_PICKUP, blockPos);
                    return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
                }
                return InteractionResultHolder.fail(itemStack);
            }
        }
    }
}
