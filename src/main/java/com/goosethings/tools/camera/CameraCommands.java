package com.goosethings.tools.camera;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.ArrayList;

public final class CameraCommands {
    private CameraCommands() {}
    @FunctionalInterface private interface Action { void run(CommandContext<CommandSourceStack> context) throws Exception; }
    private static int execute(CommandContext<CommandSourceStack> c, Action action) {
        try {
            action.run(c);
            c.getSource().sendSuccess(() -> Component.translatableWithFallback(
                    "command.goosetools.camera.saved", "Camera configuration saved."), false);
            return 1;
        } catch (Exception e) {
            c.getSource().sendFailure(Component.translatableWithFallback(
                    "command.goosetools.camera.failed", "Camera operation failed: %s", errorText(e)));
            return 0;
        }
    }
    private static Component errorText(Exception error) {
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        if (message.startsWith("Unknown camera: ")) return Component.translatableWithFallback(
                "command.goosetools.camera.unknown", "Unknown camera: %s", message.substring(16));
        if (message.startsWith("Unknown screen: ")) return Component.translatableWithFallback(
                "command.goosetools.camera.unknown_screen", "Unknown screen: %s", message.substring(16));
        String key = switch (message) {
            case "Camera ID already exists" -> "duplicate";
            case "Screen ID already exists" -> "duplicate_screen";
            case "Remove or rebind screens using this camera first" -> "in_use";
            case "Look at a vertical wall within 16 blocks" -> "wall_required";
            case "ID must contain 1-48 letters, digits, _, . or -" -> "invalid_id";
            case "Screen width and height must be between 0.1 and 128 blocks" -> "invalid_size";
            case "Corners must share a vertical plane matching the facing" -> "invalid_corners";
            case "Repair the invalid camera catalog before editing" -> "invalid_storage";
            case "Invalid camera catalog" -> "catalog_limit";
            default -> "detail";
        };
        if (key.equals("detail")) return Component.translatableWithFallback(
                "command.goosetools.camera.detail", "%s", message);
        return Component.translatableWithFallback("command.goosetools.camera." + key, message);
    }
    private static String arg(CommandContext<CommandSourceStack> c, String name) {
        return StringArgumentType.getString(c, name);
    }
    public static LiteralArgumentBuilder<CommandSourceStack> cameras() {
        return Commands.literal("camera")
                .then(Commands.literal("create").then(Commands.argument("id", StringArgumentType.word())
                        .executes(c -> execute(c, ctx -> capture(ctx, false)))))
                .then(Commands.literal("update").then(cameraArgument("id")
                        .executes(c -> execute(c, ctx -> capture(ctx, true)))))
                .then(Commands.literal("remove").then(cameraArgument("id")
                        .executes(c -> execute(c, ctx -> {
                            var service = CameraService.get(); var catalog = service.catalog(); String id = arg(ctx,"id");
                            catalog.camera(id);
                            if (catalog.screens().stream().anyMatch(s -> s.cameraId().equals(id)))
                                throw new IllegalArgumentException("Remove or rebind screens using this camera first");
                            service.replace(new CameraCatalog(1, catalog.cameras().stream().filter(a -> !a.id().equals(id)).toList(), catalog.screens()));
                        }))))
                .then(Commands.literal("list").executes(c -> list(c, true)));
    }
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> cameraArgument(String name) {
        return Commands.argument(name, StringArgumentType.word()).suggests((c,b) -> {
            CameraService.get().catalog().cameras().forEach(a -> b.suggest(a.id())); return b.buildFuture();
        });
    }
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> screenArgument() {
        return Commands.argument("screen", StringArgumentType.word()).suggests((c,b) -> {
            CameraService.get().catalog().screens().forEach(s -> b.suggest(s.id())); return b.buildFuture();
        });
    }
    public static LiteralArgumentBuilder<CommandSourceStack> screens() {
        return Commands.literal("screen")
                .then(Commands.literal("create").then(Commands.argument("screen", StringArgumentType.word())
                        .then(cameraArgument("camera")
                                .then(Commands.argument("from", Vec3Argument.vec3(false))
                                        .then(Commands.argument("to", Vec3Argument.vec3(false))
                                                .then(Commands.argument("facing", StringArgumentType.word())
                                                        .suggests((c,b) -> { for (String face : java.util.List.of("north","south","east","west")) b.suggest(face); return b.buildFuture(); })
                                                        .executes(c -> execute(c, CameraCommands::placeCorners))))))))
                .then(Commands.literal("look").then(Commands.argument("screen", StringArgumentType.word())
                        .then(cameraArgument("camera").then(Commands.argument("width", FloatArgumentType.floatArg(.1f,128))
                                .then(Commands.argument("height", FloatArgumentType.floatArg(.1f,128))
                                        .executes(c -> execute(c, CameraCommands::place)))))))
                .then(Commands.literal("resize").then(screenArgument()
                        .then(Commands.argument("width", FloatArgumentType.floatArg(.1f,128))
                                .then(Commands.argument("height", FloatArgumentType.floatArg(.1f,128))
                                        .executes(c -> execute(c, ctx -> change(ctx, true)))))))
                .then(Commands.literal("bind").then(screenArgument().then(cameraArgument("camera")
                        .executes(c -> execute(c, ctx -> change(ctx, false))))))
                .then(Commands.literal("remove").then(screenArgument().executes(c -> execute(c, ctx -> {
                    var service = CameraService.get(); var catalog = service.catalog(); String id = arg(ctx,"screen");
                    catalog.screen(id);
                    service.replace(new CameraCatalog(1, catalog.cameras(), catalog.screens().stream().filter(s -> !s.id().equals(id)).toList()));
                }))))
                .then(Commands.literal("list").executes(c -> list(c, false)));
    }
    private static void capture(CommandContext<CommandSourceStack> c, boolean update) throws Exception {
        var player = c.getSource().getPlayerOrException();
        var service = CameraService.get(); var catalog = service.catalog(); String id = arg(c,"id");
        if (update) catalog.camera(id);
        else if (catalog.cameras().stream().anyMatch(a -> a.id().equals(id))) throw new IllegalArgumentException("Camera ID already exists");
        var cameras = new ArrayList<>(catalog.cameras()); cameras.removeIf(a -> a.id().equals(id));
        cameras.add(new CameraDefinition(id, player.level().dimension().identifier().toString(), player.getX(),
                player.getEyeY(), player.getZ(), player.getYRot(), player.getXRot()));
        service.replace(new CameraCatalog(1, cameras, catalog.screens()));
    }
    private static void place(CommandContext<CommandSourceStack> c) throws Exception {
        var player = c.getSource().getPlayerOrException();
        var service = CameraService.get(); var catalog = service.catalog();
        String id = arg(c,"screen"), camera = arg(c,"camera"); catalog.camera(camera);
        if (catalog.screens().stream().anyMatch(s -> s.id().equals(id))) throw new IllegalArgumentException("Screen ID already exists");
        var eye = player.getEyePosition();
        BlockHitResult hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(16)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection().getAxis().isVertical())
            throw new IllegalArgumentException("Look at a vertical wall within 16 blocks");
        var p = hit.getLocation().add(hit.getDirection().getStepX() * .003, 0, hit.getDirection().getStepZ() * .003);
        var screens = new ArrayList<>(catalog.screens());
        screens.add(new ScreenDefinition(id, camera, player.level().dimension().identifier().toString(),
                p.x, p.y, p.z, hit.getDirection().getSerializedName(),
                FloatArgumentType.getFloat(c,"width"), FloatArgumentType.getFloat(c,"height")));
        service.replace(new CameraCatalog(1, catalog.cameras(), screens));
    }
    private static void change(CommandContext<CommandSourceStack> c, boolean resize) throws Exception {
        var service = CameraService.get(); var catalog = service.catalog(); var old = catalog.screen(arg(c,"screen"));
        var next = resize ? old.resize(FloatArgumentType.getFloat(c,"width"), FloatArgumentType.getFloat(c,"height"))
                : old.bind(catalog.camera(arg(c,"camera")).id());
        service.replace(new CameraCatalog(1, catalog.cameras(), catalog.screens().stream().map(s -> s.id().equals(old.id()) ? next : s).toList()));
    }
    private static void placeCorners(CommandContext<CommandSourceStack> c) throws Exception {
        var service = CameraService.get(); var catalog = service.catalog();
        String id = arg(c,"screen"), camera = arg(c,"camera"); catalog.camera(camera);
        if (catalog.screens().stream().anyMatch(s -> s.id().equals(id))) throw new IllegalArgumentException("Screen ID already exists");
        var a = Vec3Argument.getVec3(c,"from"); var b = Vec3Argument.getVec3(c,"to");
        var screen = ScreenDefinition.corners(id,camera,c.getSource().getLevel().dimension().identifier().toString(),
                a.x,a.y,a.z,b.x,b.y,b.z,arg(c,"facing"));
        var screens = new ArrayList<>(catalog.screens()); screens.add(screen);
        service.replace(new CameraCatalog(1,catalog.cameras(),screens));
    }
    private static int list(CommandContext<CommandSourceStack> c, boolean camera) {
        var catalog = CameraService.get().catalog();
        String value = camera ? String.join(", ", catalog.cameras().stream().map(CameraDefinition::id).toList())
                : String.join(", ", catalog.screens().stream().map(s -> s.id() + " -> " + s.cameraId() + " (" + s.width() + " x " + s.height() + ")").toList());
        c.getSource().sendSuccess(() -> Component.translatableWithFallback(
                "command.goosetools.camera.list", "Camera / screen entries: %s", value), false);
        return camera ? catalog.cameras().size() : catalog.screens().size();
    }
}
