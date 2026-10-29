package dev.romeo.blockheads;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public final class ArmorStandBlockHeads implements ModInitializer {
    private static final String PLACED_TAG = "asbh_placed_block_head_v1";

    @Override
    public void onInitialize() {
        UseEntityCallback.EVENT.register((Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult hitResult) -> {
            if (!(entity instanceof ArmorStand stand)) return InteractionResult.PASS;

            // Client must send the ordinary entity-interaction packet. All changes
            // and item consumption are authoritative on the server.
            if (world.isClientSide()) return InteractionResult.PASS;
            if (player.isSpectator() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

            ItemStack head = stand.getItemBySlot(EquipmentSlot.HEAD);
            if (stand.entityTags().contains(PLACED_TAG)) {
                if (!head.isEmpty()) {
                    ItemStack returned = head.copy();
                    returned.setCount(1);
                    stand.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    stand.removeTag(PLACED_TAG);
                    giveBackSafely(player, returned);
                } else {
                    stand.removeTag(PLACED_TAG);
                }
                return InteractionResult.SUCCESS;
            }

            ItemStack held = player.getItemInHand(hand);
            if (held.isEmpty() || !(held.getItem() instanceof BlockItem) || !head.isEmpty()) return InteractionResult.PASS;

            ItemStack placed = held.copy();
            placed.setCount(1);
            stand.setItemSlot(EquipmentSlot.HEAD, placed);
            stand.addTag(PLACED_TAG);
            if (!player.getAbilities().instabuild) held.shrink(1);
            return InteractionResult.SUCCESS;
        });
    }

    private static void giveBackSafely(Player player, ItemStack stack) {
        for (int i = 0; i < player.getInventory().getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack existing = player.getInventory().getItem(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(moved);
                stack.shrink(moved);
            }
        }
        if (stack.isEmpty()) return;

        int selected = player.getInventory().getSelectedSlot();
        ItemStack selectedStack = player.getInventory().getItem(selected);
        if (selectedStack.isEmpty()) {
            player.getInventory().setItem(selected, stack.copy());
            stack.setCount(0);
            return;
        }
        if (ItemStack.isSameItemSameComponents(selectedStack, stack)
                && selectedStack.getCount() < selectedStack.getMaxStackSize()) {
            int moved = Math.min(stack.getCount(), selectedStack.getMaxStackSize() - selectedStack.getCount());
            selectedStack.grow(moved);
            stack.shrink(moved);
        }
        if (!stack.isEmpty()) {
            player.getInventory().add(stack);
            if (!stack.isEmpty()) player.drop(stack, false, Prediction.SERVER_ONLY);
        }
    }
}
