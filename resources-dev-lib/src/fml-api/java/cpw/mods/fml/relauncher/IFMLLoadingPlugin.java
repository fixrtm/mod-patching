package cpw.mods.fml.relauncher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

public interface IFMLLoadingPlugin {
    String[] getASMTransformerClass();

    String getModContainerClass();

    String getSetupClass();

    void injectData(Map<String, Object> data);

    String getAccessTransformerClass();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface TransformerExclusions {
        String[] value() default "";
    }
}
