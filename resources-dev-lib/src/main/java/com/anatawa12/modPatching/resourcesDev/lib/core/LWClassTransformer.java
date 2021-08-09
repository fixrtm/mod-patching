package com.anatawa12.modPatching.resourcesDev.lib.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class LWClassTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        if (!"net.minecraft.client.Minecraft".equals(transformedName)) return basicClass;
        ClassReader classReader = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(0);
        classReader.accept(new MinecraftVisitor(cw), 0);
        return cw.toByteArray();
    }

    private static boolean isSrg = false;
    private static final int ASM_VERSION;

    static {
        Class<?> opcodes = Opcodes.class;
        // at least
        int asm_version = Opcodes.ASM5;
        for (int i = 9; i >= 5; i--) {
            try {
                asm_version = opcodes.getField("ASM" + i).getInt(null);
                break;
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
        ASM_VERSION = asm_version;
    }

    private static class MinecraftVisitor extends ClassVisitor {
        public MinecraftVisitor(ClassWriter cv) {
            super(ASM_VERSION, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (mv == null) return null;
            if ("()V".equals(descriptor) &&
                    ("init".equals(name) || "startGame".equals(name) || "func_71384_a".equals(name))) {
                if (name.startsWith("func_")) isSrg = true;
                mv = new InitMethodVisitor(mv);
            }
            return mv;
        }
    }

    private static class InitMethodVisitor extends MethodVisitor {
        public InitMethodVisitor(MethodVisitor mv) {
            super(ASM_VERSION, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (opcode == Opcodes.INVOKEVIRTUAL
                    && ("cpw/mods/fml/client/FMLClientHandler".equals(owner)
                    || "net/minecraftforge/fml/client/FMLClientHandler".equals(owner))
                    && "beginMinecraftLoading".equals(name)) {
                super.visitIntInsn(Opcodes.ALOAD, 0);
                super.visitFieldInsn(Opcodes.GETFIELD, "net/minecraft/client/Minecraft",
                        isSrg ? "field_110449_ao" : "defaultResourcePacks", "Ljava/util/List;");
                super.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "com/anatawa12/modPatching/resourcesDev/lib/ResourcePackManager",
                        "init", "(Ljava/util/List;)V", false);
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }
    // in
    // net/minecraft/client/Minecraft/func_71384_a: init ()V
    // net/minecraft/client/Minecraft/func_71384_a: startGame ()V
    //
    // the code before calling virtual either
    //           cpw/mods/fml/client/FMLClientHandler/beginMinecraftLoading 
    // net/minecraftforge/fml/client/FMLClientHandler/beginMinecraftLoading
    //
    // insert invokestatic
    // com/anatawa12/modPatching/resourcesDev/lib/ResourcePackManager/init (Ljava/util/List;)V
    // 
    // with this.
    // net/minecraft/client/Minecraft/field_110449_ao: defaultResourcePacks
}
