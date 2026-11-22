package com.recycler.mixin;

import com.recycler.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(Ingredient.class)
public abstract class IngredientMixin {

		@Unique
		private boolean recycler$isExplicitCobblestone() {
			List<Holder<Item>> items =
					((Ingredient) (Object) this).items().toList();

			return items.size() == 1
					&& items.get(0).value() == Items.COBBLESTONE;
		}

    @Inject(
            method = "test(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void recycler$allowGenericStone(
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!stack.isEmpty()
                && stack.is(ModItems.GENERIC_STONE)
                && recycler$isExplicitCobblestone()) {

            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "acceptsItem(Lnet/minecraft/core/Holder;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void recycler$allowGenericStoneHolder(
            Holder<Item> item,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (item.value() == ModItems.GENERIC_STONE
                && recycler$isExplicitCobblestone()) {

            cir.setReturnValue(true);
        }
    }
}
