package com.iafenvoy.random.economy.item.block;

import com.iafenvoy.random.economy.RandomEconomy;
import com.iafenvoy.random.economy.screen.handler.ChequeTableScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class ChequeTableBlock extends WorkStationBlock {
    public ChequeTableBlock() {
        super(Settings.copy(Blocks.CRAFTING_TABLE));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) player.openHandledScreen(new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.translatable("screen.%s.cheque_table".formatted(RandomEconomy.MOD_ID));
            }

            @Override
            public @NotNull ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                return new ChequeTableScreenHandler(syncId, playerInventory, ScreenHandlerContext.create(world, pos));
            }
        });
        return ActionResult.SUCCESS;
    }
}
