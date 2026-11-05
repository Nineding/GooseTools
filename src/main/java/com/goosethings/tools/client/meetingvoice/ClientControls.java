package com.goosethings.tools.client.meetingvoice;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.function.Consumer;

final class ClientControls {
   static volatile long previewUntil;
   private static long nextReport;

   static void install() throws ReflectiveOperationException {
      String var0 = "net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback";
      Object var1 = listener(Reflect.type(var0), var0x -> {
         try {
            registerCommands(var0x[0]);
         } catch (Exception var2x) {
            Diagnostics.fail("Command registration failed", var2x);
         }
      });
      Reflect.call(Reflect.field(Reflect.type(var0), "EVENT"), "register", var1);
      String var2 = "net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents";
      Reflect.call(Reflect.field(Reflect.type(var2), "END_CLIENT_TICK"), "register", listener(Reflect.type(var2 + "$EndTick"), var0x -> {
         try {
            Object var1x = Reflect.field(var0x[0], "player");
            MeetingVoice.refreshContext(var0x[0]);
            long var2x = System.nanoTime();
            if (var1x != null && var2x >= nextReport) {
               nextReport = var2x + 15000000000L;
               Diagnostics.info(Diagnostics.summary());
            }
         } catch (Exception var4) {
            if (nextReport != Long.MAX_VALUE) {
               nextReport = Long.MAX_VALUE;
               Diagnostics.fail("Client tick diagnostics failed", var4);
            }
         }
      }));
   }

   private static void registerCommands(Object var0) throws ReflectiveOperationException {
      Object var1 = literal("meetingvoice");
      Reflect.call(var1, "executes", command("status"));

      for (String var5 : new String[]{"status", "test", "reload"}) {
         Reflect.call(var1, "then", Reflect.call(literal(var5), "executes", command(var5)));
      }

      for (String var14 : new String[]{"self", "meeting"}) {
         Object var6 = literal(var14);

         for (String var10 : new String[]{"on", "off"}) {
            Reflect.call(var6, "then", Reflect.call(literal(var10), "executes", command(var14 + " " + var10)));
         }

         Reflect.call(var1, "then", var6);
      }

      Reflect.call(var0, "register", var1);
   }

   private static Object literal(String var0) throws ReflectiveOperationException {
      return Reflect.call(Reflect.type("net.fabricmc.fabric.api.client.command.v2.ClientCommands"), "literal", var0);
   }

   private static Object command(String var0) throws ReflectiveOperationException {
      Class var1 = Reflect.type("com.mojang.brigadier.Command");
      return Proxy.newProxyInstance(
         var1.getClassLoader(),
         new Class[]{var1},
         (var1x, var2, var3) -> {
            if (var2.getDeclaringClass() == Object.class) {
               return objectMethod(var1x, var2.getName(), var3);
            }

            Object var4 = Reflect.call(var3[0], "getSource");

            try {
               String var5 = switch (var0) {
                  case "test" -> {
                     MeetingVoice.refreshContext(Reflect.call(Reflect.type("net.minecraft.client.Minecraft"), "getInstance"));
                     if (!MeetingVoice.allowed(MeetingVoice.api)) yield "测试仅在会议阶段且加入会议语音组时可用：" + MeetingVoice.blockReason(MeetingVoice.api);
                     HeadRenderer.retry();
                     previewUntil = System.nanoTime() + 15000000000L;
                     yield "会议气泡预览已开启 15 秒（模拟声波）。按 F5 切换第三人称；离开会议立即结束。";
                  }
                  case "reload" -> {
                     MeetingVoice.reload();
                     HeadRenderer.retry();
                     yield "配置已重新载入。";
                  }
                  case "self on", "self off" -> {
                     MeetingVoice.saveOption("showSelf", var0.endsWith("on"));
                     yield "显示自己：" + MeetingVoice.config.showSelf + "（第三人称）。";
                  }
                  case "meeting on", "meeting off" -> {
                     // Meeting scope is mandatory; legacy command cannot bypass it.
                     yield "D 方案强制仅在会议阶段及会议语音组显示，不能关闭此限制。";
                  }
                  default -> Diagnostics.summary();
               };
               Diagnostics.info("Command " + var0 + ": " + Diagnostics.summary());
               Reflect.call(var4, "sendFeedback", Reflect.call(Reflect.type("net.minecraft.network.chat.Component"), "literal", "[MeetingVoice] " + var5));
               return 1;
            } catch (Exception var8) {
               Diagnostics.fail("Command failed: " + var0, var8);
               Reflect.call(
                  var4,
                  "sendError",
                  Reflect.call(Reflect.type("net.minecraft.network.chat.Component"), "literal", "[MeetingVoice] 执行失败，请提供 logs/meetingvoice.log")
               );
               return 0;
            }
         }
      );
   }

   static Object listener(Class<?> var0, Consumer<Object[]> var1) {
      return Proxy.newProxyInstance(var0.getClassLoader(), new Class[]{var0}, (var1x, var2, var3) -> {
         if (var2.getDeclaringClass() == Object.class) {
            return objectMethod(var1x, var2.getName(), var3);
         }

         var1.accept(var3);
         return null;
      });
   }

   private static Object objectMethod(Object proxy, String method, Object[] args) {
      return switch (method) {
         case "hashCode" -> System.identityHashCode(proxy);
         case "equals" -> proxy == args[0];
         default -> "MeetingVoice callback";
      };
   }
}
