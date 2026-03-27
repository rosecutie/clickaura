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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class ModuleExample extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");

    // --- General Settings ---
    private final Setting<Integer> bpt = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("How many blocks to click every tick.")
        .defaultValue(1)
        .min(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Face the block before clicking (helps bypass some anti-cheats).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
        .name("horizontal-radius")
        .description("How far left, right, forward, and back to reach.")
        .defaultValue(4)
        .min(1)
        .build()
    );

    private final Setting<Integer> verticalUp = sgGeneral.add(new IntSetting.Builder()
        .name("vertical-up")
        .description("How many blocks above your feet to reach.")
        .defaultValue(3)
        .min(0)
        .build()
    );

    private final Setting<Integer> verticalDown = sgGeneral.add(new IntSetting.Builder()
        .name("vertical-down")
        .description("How many blocks below your feet to reach.")
        .defaultValue(1)
        .min(0)
        .build()
    );

    // --- Whitelist Settings ---
    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Only click these blocks.")
        .build()
    );

    // --- Render Settings ---
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a box around the blocks being clicked.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the box.")
        .defaultValue(new SettingColor(255, 0, 0, 75))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the box.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );

    private final List<BlockPos> activeBlocks = new ArrayList<>();

    public ModuleExample() {
        // You can change "world-origin" to "click-aura" here if you want
        super(Categories.World, "click-aura", "Bypasses Nuker limits by sending single-click packets. Requires Haste.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        activeBlocks.clear();
        int count = 0;
        BlockPos p = mc.player.getBlockPos();

        // Box iteration logic
        for (int x = -horizontalRadius.get(); x <= horizontalRadius.get(); x++) {
            for (int z = -horizontalRadius.get(); z <= horizontalRadius.get(); z++) {
                for (int y = -verticalDown.get(); y <= verticalUp.get(); y++) {
                    
                    if (count >= bpt.get()) return;

                    BlockPos targetPos = p.add(x, y, z);
                    Block block = mc.world.getBlockState(targetPos).getBlock();

                    // Only target whitelisted blocks
                    if (whitelist.get().contains(block)) {
                        activeBlocks.add(targetPos);
                        
                        if (rotate.get()) {
                            Rotations.rotate(Rotations.getYaw(targetPos), Rotations.getPitch(targetPos), () -> clickBlock(targetPos));
                        } else {
                            clickBlock(targetPos);
                        }
                        
                        count++;
                    }
                }
            }
        }
    }

    private void clickBlock(BlockPos pos) {
        // Sends the attack/start-break packet
        mc.interactionManager.attackBlock(pos, Direction.UP);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || activeBlocks.isEmpty()) return;
        
        for (BlockPos pos : activeBlocks) {
            event.renderer.box(pos, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
        }
    }
}
