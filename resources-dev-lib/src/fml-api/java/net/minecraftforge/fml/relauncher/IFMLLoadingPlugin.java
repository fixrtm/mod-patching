package net.minecraftforge.fml.relauncher;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

public interface IFMLLoadingPlugin {
    String[] getASMTransformerClass();

    String getModContainerClass();

    String getSetupClass();

    void injectData(Map<String, Object> data);

    String getAccessTransformerClass();

    @Retention(RetentionPolicy.RUNTIME)
    @interface TransformerExclusions {
        String[] value();
    }
}
