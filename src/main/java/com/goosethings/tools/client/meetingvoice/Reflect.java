package com.goosethings.tools.client.meetingvoice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class Reflect {
   private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();
   private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();
   private static final Map<String, Class<?>> TYPES = new ConcurrentHashMap<>();

   static Class<?> type(String var0) throws ClassNotFoundException {
      Class var1 = TYPES.get(var0);
      if (var1 == null) {
         var1 = Class.forName(var0);
         TYPES.put(var0, var1);
      }

      return var1;
   }

   static Object field(Object var0, String var1) throws ReflectiveOperationException {
      Class var2 = var0 instanceof Class ? (Class)var0 : var0.getClass();
      String var3 = var2.getName() + "#" + var1;
      Field var4 = FIELDS.get(var3);
      if (var4 == null) {
         var4 = var2.getField(var1);
         var4.trySetAccessible();
         FIELDS.put(var3, var4);
      }

      return var4.get(var0 instanceof Class ? null : var0);
   }

   static Object call(Object var0, String var1, Object... var2) throws ReflectiveOperationException {
      Class var3 = var0 instanceof Class ? (Class)var0 : var0.getClass();
      StringBuilder var4 = new StringBuilder(var3.getName()).append('#').append(var1);

      for (Object var8 : var2) {
         var4.append(':').append(var8 == null ? "null" : var8.getClass().getName());
      }

      Method var13 = METHODS.get(var4.toString());
      if (var13 == null) {
         for (Method var9 : var3.getMethods()) {
            if (!var9.isBridge() && !var9.isSynthetic() && var9.getName().equals(var1) && var9.getParameterCount() == var2.length) {
               Class[] var10 = var9.getParameterTypes();
               boolean var11 = true;

               for (int var12 = 0; var12 < var10.length; var12++) {
                  if (var2[var12] == null ? var10[var12].isPrimitive() : !box(var10[var12]).isInstance(var2[var12])) {
                     var11 = false;
                     break;
                  }
               }

               if (var11) {
                  if (var13 != null && !moreSpecific(var9, var13)) {
                     if (!moreSpecific(var13, var9)) {
                        throw new NoSuchMethodException("Ambiguous " + var4);
                     }
                  } else {
                     var13 = var9;
                  }
               }
            }
         }

         if (var13 == null) {
            throw new NoSuchMethodException(var4.toString());
         }

         var13.trySetAccessible();
         METHODS.put(var4.toString(), var13);
      }

      return var13.invoke(var0 instanceof Class ? null : var0, var2);
   }

   private static boolean moreSpecific(Method var0, Method var1) {
      Class[] var2 = var0.getParameterTypes();
      Class[] var3 = var1.getParameterTypes();

      for (int var4 = 0; var4 < var2.length; var4++) {
         if (!box(var3[var4]).isAssignableFrom(box(var2[var4]))) {
            return false;
         }
      }

      return true;
   }

   private static Class<?> box(Class<?> var0) {
      if (var0 == boolean.class) {
         return Boolean.class;
      } else if (var0 == int.class) {
         return Integer.class;
      } else if (var0 == float.class) {
         return Float.class;
      } else if (var0 == double.class) {
         return Double.class;
      } else {
         return var0 == long.class ? Long.class : var0;
      }
   }

   static double coord(Object var0, String var1) throws ReflectiveOperationException {
      return ((Number)field(var0, var1)).doubleValue();
   }

   static Object vec(double var0, double var2, double var4) throws ReflectiveOperationException {
      return type("net.minecraft.world.phys.Vec3").getConstructor(double.class, double.class, double.class).newInstance(var0, var2, var4);
   }
}
