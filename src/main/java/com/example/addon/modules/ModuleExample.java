package com.example.addon.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleExample extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");

    private final Setting<Integer> bpt = sgGeneral.add(new IntSetting.Builder().name("blocks-per-tick").defaultValue(5).min(1).sliderMax(100).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").defaultValue(false).build());
    private final Setting<Integer> hRadius = sgGeneral.add(new IntSetting.Builder().name("horizontal-radius").defaultValue(4).build());
    private final Setting<Integer> vUp = sgGeneral.add(new IntSetting.Builder().name("vertical-up").defaultValue(3).build());
    private final Setting<Integer> vDown = sgGeneral.add(new IntSetting.Builder().name("vertical-down").defaultValue(1).build());
    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder().name("blocks").build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").defaultValue(true).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").defaultValue(new SettingColor(255, 0, 0, 75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").defaultValue(new SettingColor(255, 0, 0, 255)).build());

    private final List<BlockPos> blocksToClick = new ArrayList<>();
    private final List<BlockPos> clickedThisTick = new ArrayList<>();

    public ModuleExample() {
        super(Categories.World, "click-aura", "Instant-break packet spammer. Set blocks in Whitelist.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        blocksToClick.clear();
        clickedThisTick.clear();
        BlockPos p = mc.player.getBlockPos();
        Vec3d eyePos = mc.player.getEyePos();

        for (int x = -hRadius.get(); x <= hRadius.get(); x++) {
            for (int z = -hRadius.get(); z <= hRadius.get(); z++) {
                for (int y = -vDown.get(); y <= vUp.get(); y++) {
                    BlockPos targetPos = p.add(x, y, z);
                    BlockState state = mc.world.getBlockState(targetPos);
                    if (!state.isAir() && whitelist.get().contains(state.getBlock())) {
                        blocksToClick.add(targetPos);
                    }
                }
            }
        }

        blocksToClick.sort(Comparator.comparingDouble(pos -> Vec3d.ofCenter(pos).squaredDistanceTo(eyePos)));

        int count = 0;
        for (BlockPos pos : blocksToClick) {
            if (count >= bpt.get()) break;
            if (rotate.get()) {
                Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), () -> click(pos));
            } else {
                click(pos);
            }
            clickedThisTick.add(pos);
            count++;
        }
    }

    private void click(BlockPos pos) {
        mc.interactionManager.attackBlock(pos, Direction.UP);
        // FORCE client-side air to prevent ghost blocks
        //fak you
        mc.world.setBlockState(pos, Blocks.AIR.getDefaultState());
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || clickedThisTick.isEmpty()) return;
        for (BlockPos pos : clickedThisTick) {
            event.renderer.box(pos, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
        }
    }
}
