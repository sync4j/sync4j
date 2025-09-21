package com.fathzer.sync4j.file;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.HashAlgorithm;

public class LocalProvider implements FileProvider {
    @Override
    public List<HashAlgorithm> getSupportedHash() {
        return List.of(HashAlgorithm.values());
    }

    @Override
    public File get(String path, boolean recursive) throws IOException {
        return new LocalFile(Paths.get(path));
    }
}
