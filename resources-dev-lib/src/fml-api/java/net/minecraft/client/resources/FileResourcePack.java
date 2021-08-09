package net.minecraft.client.resources;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

// signatures only for extend
public abstract class FileResourcePack {
    // extend from parent
    protected final File resourcePackFile;

    // getPackName
    public abstract String func_130077_b();

    public FileResourcePack(File resourcePackFileIn) {
        this.resourcePackFile = resourcePackFileIn;
    }

    // getInputStreamByName
    protected InputStream func_110591_a(String name) throws IOException {
        throw new IOException();
    }

    // getPackImage
    public abstract BufferedImage func_110586_a() throws IOException;
}
