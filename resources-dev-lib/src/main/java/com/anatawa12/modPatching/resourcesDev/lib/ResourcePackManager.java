package com.anatawa12.modPatching.resourcesDev.lib;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ResourcePackManager {
    private static final List<File> files = new ArrayList<>();

    public static void addFile(File file) {
        files.add(file);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void init(List list) {
        ReflectionHelper.initializeHelper();
        for (File file : files) {
            list.add(new DevFileResourcePack(file));
        }
    }

    private static class ReflectionHelper {
        private static final Constructor<?> ctor;

        static {
            try {
                ctor = Class.forName("net.minecraft.client.resources.FileResourcePack")
                        .getConstructor(File.class);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        public static Object newResourcePack(File file) {
            try {
                return ctor.newInstance(file);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public static void initializeHelper() {
            try {
                Class.forName("com.anatawa12.modPatching.resourcesDev.lib.GeneratedHelper")
                        .getMethod("init")
                        .invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
