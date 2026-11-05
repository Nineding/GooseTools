package com.goosethings.tools.client.meetingvoice;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

final class Diagnostics {
   static final AtomicLong incoming = new AtomicLong();
   static final AtomicLong microphone = new AtomicLong();
   static final AtomicLong accepted = new AtomicLong();
   static volatile String renderState = "not initialized";
   static volatile String error = "none";
   static volatile long renders;
   static volatile long submitted;
   static volatile long lastRender;
   private static Path logPath;

   static void init(Path var0) {
      logPath = var0.resolve("logs/meetingvoice.log");

      try {
         Files.createDirectories(logPath.getParent());
         Files.writeString(logPath, "Meeting Voice beta.9 session " + Instant.now() + "\n");
      } catch (IOException var2) {
      }
   }

   static synchronized void info(String var0) {
      write("INFO", var0, null);
   }

   static synchronized void fail(String var0, Throwable var1) {
      error = var0 + ": " + root(var1);
      write("ERROR", var0, var1);
   }

   private static Throwable root(Throwable var0) {
      while (var0.getCause() != null && var0.getCause() != var0) {
         var0 = var0.getCause();
      }

      return var0;
   }

   private static void write(String var0, String var1, Throwable var2) {
      String var3 = "[meetingvoice] " + var1;

      try {
         Class var4 = Class.forName("org.slf4j.LoggerFactory");
         Class var5 = Class.forName("org.slf4j.Logger");
         Object var6 = var4.getMethod("getLogger", String.class).invoke(null, "meetingvoice");
         if (var2 == null) {
            var5.getMethod("info", String.class).invoke(var6, var3);
         } else {
            var5.getMethod("error", String.class, Throwable.class).invoke(var6, var3, var2);
         }
      } catch (ReflectiveOperationException | LinkageError var8) {
         System.err.println(var3);
         if (var2 != null) {
            var2.printStackTrace();
         }
      }

      if (logPath != null) {
         try {
            StringWriter var9 = new StringWriter();
            if (var2 != null) {
               var2.printStackTrace(new PrintWriter(var9));
            }

            Files.writeString(logPath, Instant.now() + " " + var0 + " " + var3 + "\n" + var9, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
         } catch (IOException var7) {
         }
      }
   }

   static String summary() {
      return "beta.9 | gate="
         + MeetingVoice.blockReason(MeetingVoice.api)
         + " | group="
         + MeetingVoice.groupName()
         + " | showSelf="
         + MeetingVoice.config.showSelf
         + " | render="
         + renderState
         + " | icons="
         + VoiceIconCompatibility.mode
         + " | callbacks="
         + renders
         + " | bubbles="
         + submitted
         + " | mic="
         + microphone.get()
         + " | received="
         + incoming.get()
         + " | accepted="
         + accepted.get()
         + " | active="
         + PlayerVoiceStateCache.size()
         + " | error="
         + error;
   }
}
