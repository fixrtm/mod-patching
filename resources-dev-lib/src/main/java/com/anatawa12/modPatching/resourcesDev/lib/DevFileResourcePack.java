package com.anatawa12.modPatching.resourcesDev.lib;


import net.minecraft.client.resources.FileResourcePack;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class DevFileResourcePack extends FileResourcePack {

    public DevFileResourcePack(File file) {
        super(file);
    }

    @Override
    public String func_130077_b() {
        return "DevFileResourcePack by mod-patching:" + resourcePackFile.getName();
    }

    @Override
    protected InputStream func_110591_a(String resourceName) throws IOException {
        try {
            return super.func_110591_a(resourceName);
        } catch (IOException ioe) {
            if ("pack.mcmeta".equals(resourceName)) {
                return new ByteArrayInputStream(("{\n" +
                        " \"pack\": {\n" +
                        "   \"description\": \"dummy Dev pack by mod-patching " + resourcePackFile.getName() + "\",\n" +
                        "   \"pack_format\": 2\n" +
                        "}\n" +
                        "}").getBytes(StandardCharsets.UTF_8));
            } else {
                throw ioe;
            }
        }
    }

    @Override
    public BufferedImage func_110586_a() throws IOException {
        throw new FileNotFoundException();
    }
}
