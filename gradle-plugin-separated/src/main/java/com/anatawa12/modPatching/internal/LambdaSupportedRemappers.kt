package com.anatawa12.modPatching.internal

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.H_INVOKESTATIC
import org.objectweb.asm.Type
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.MethodRemapper
import org.objectweb.asm.commons.Remapper

class LambdaSupportedClassRemapper : ClassRemapper {
    constructor(classVisitor: ClassVisitor?, remapper: Remapper) : super(classVisitor, remapper)
    constructor(api: Int, classVisitor: ClassVisitor?, remapper: Remapper) : super(api, classVisitor, remapper)

    override fun createMethodRemapper(methodVisitor: MethodVisitor?): MethodVisitor {
        return LambdaSupportedMethodRemapper(methodVisitor, this.remapper)
    }
}

class LambdaSupportedMethodRemapper : MethodRemapper {
    constructor(methodVisitor: MethodVisitor?, remapper: Remapper?) : super(methodVisitor, remapper)
    constructor(api: Int, methodVisitor: MethodVisitor?, remapper: Remapper?) : super(api, methodVisitor, remapper)

    override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?,
    ) {
        mv?.visitInvokeDynamicInsn(
            /* name = */
            if (bootstrapMethodHandle in lambdaMetaFactories) {
                remapper.mapMethodName(
                    Type.getReturnType(descriptor).internalName,
                    name,
                    (bootstrapMethodArguments[0] as Type).descriptor,
                )
            } else {
                remapper.mapInvokeDynamicMethodName(name, descriptor)
            },
            remapper.mapMethodDesc(descriptor),
            remapper.mapValue(bootstrapMethodHandle) as Handle,
            *Array(bootstrapMethodArguments.size) {
                remapper.mapValue(bootstrapMethodArguments[it])
            },
        )
    }

    companion object {
        private val lambdaMetaFactories = setOf(
            Handle(H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory",
                "metafactory",
                "(" +
                        "Ljava/lang/invoke/MethodHandles\$Lookup;" +
                        "Ljava/lang/String;" +
                        "Ljava/lang/invoke/MethodType;" +
                        "Ljava/lang/invoke/MethodType;" +
                        "Ljava/lang/invoke/MethodHandle;" +
                        "Ljava/lang/invoke/MethodType;" +
                        ")Ljava/lang/invoke/CallSite;",
                false),
            Handle(H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory",
                "altMetafactory",
                "(" +
                        "Ljava/lang/invoke/MethodHandles\$Lookup;" +
                        "Ljava/lang/String;" +
                        "Ljava/lang/invoke/MethodType;" +
                        "[Ljava/lang/Object;" +
                        ")Ljava/lang/invoke/CallSite;",
                false)
        )
    }

}
