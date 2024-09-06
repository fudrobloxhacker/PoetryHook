/**
 * Created: 11.05.2024 (MM:DD:YYY)
 */

package net.poetryhack.poetryhook;

import net.poetryhack.poetryhook.exceptions.PoetryHookException;
import net.poetryhack.poetryhook.util.MixinClassFileTransformer;
import net.poetryhack.poetryhook.util.MixinMethod;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Utility class to handle the boilerplate of injection and ejection
 *
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public final class PoetryHookInjector {
    /**
     * @param inst {@link Instrumentation} object of the agent
     * @param mixinBases ArrayList of {@link MixinMethod} subclass objects to inject
     * @return ArrayList of {@link ClassFileTransformer} objects which can be used for ejection
     * @see #ejectMixins(Instrumentation, ArrayList)
     * @since 1.0.0
     */
    public static ArrayList<ClassFileTransformer> injectMixins(Instrumentation inst, ArrayList<MixinMethod> mixinBases) {
        ArrayList<ClassFileTransformer> transformers = new ArrayList<>();
        ArrayList<Class<?>> classesToRetransform = new ArrayList<>();
        HashMap<Class<?>, MixinMethod[]> mixinsForClass = new HashMap<>();

        for (MixinMethod mixin : mixinBases) {
            try {
                MixinClassFileTransformer transformer = new MixinClassFileTransformer(mixin);
                transformers.add(transformer);
                inst.addTransformer(transformer, true);

                Class<?> injectTo = mixin.injectTo;
                classesToRetransform.add(injectTo);

                MixinMethod[] mms = mixinsForClass.getOrDefault(injectTo, new MixinMethod[]{});
                MixinMethod[] to = new MixinMethod[mms.length + 1];
                System.arraycopy(mms, 0, to, 0, mms.length);
                to[mms.length] = mixin;
                mixinsForClass.put(injectTo, to);
            } catch (Throwable e) {
                e.printStackTrace(System.err);
            }
        }
        for (Class<?> clazz : classesToRetransform) {
            try {
                inst.retransformClasses(clazz);
            } catch (Throwable e) {
                throw new PoetryHookException(e);
            }
        }

        for (MixinMethod mixin : mixinBases) {
            if (!mixin.loaded) {
                throw new PoetryHookException("Failed to inject Mixin: " + mixin.methodToCall.getDeclaringClass().getName() + " / " + mixin.methodToCall.getName());
            }
        }

        return transformers;
    }

    /**
     * @param inst {@link Instrumentation} object that created the transformers
     * @param transformers ArrayList of {@link ClassFileTransformer} objects created by the agent
     * @see #injectMixins(Instrumentation, ArrayList)
     * @since 1.0.0
     */
    public static void ejectMixins(Instrumentation inst, ArrayList<ClassFileTransformer> transformers) {
        for (ClassFileTransformer transformer : transformers) {
            try {
                inst.removeTransformer(transformer);
            } catch (Throwable e) {
                throw new PoetryHookException(e);
            }
        }
    }
}
