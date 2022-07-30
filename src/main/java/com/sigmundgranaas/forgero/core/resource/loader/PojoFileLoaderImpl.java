package com.sigmundgranaas.forgero.core.resource.loader;

import com.sigmundgranaas.forgero.ForgeroInitializer;
import com.sigmundgranaas.forgero.core.data.ForgeroDataResource;
import com.sigmundgranaas.forgero.core.resource.PojoLoader;
import com.sigmundgranaas.forgero.core.util.JsonPOJOLoader;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

public class PojoFileLoaderImpl<T extends ForgeroDataResource> implements PojoLoader<T> {
    private final Collection<Path> paths;
    private final Class<T> type;

    public PojoFileLoaderImpl(Collection<Path> paths, Class<T> type) {
        this.paths = paths;
        this.type = type;
    }

    private Optional<String> getFilePath(Path path) {
        String[] elements = path.toString().split( "data");
        if (elements.length == 2) {
            return Optional.of("/" + "data" + elements[1]);
        }
        ForgeroInitializer.LOGGER.error("Unable to resolve path {}, as it could not be split using default split operator {}", path.toString(), File.separator );
        return Optional.empty();
    }

    @Override
    public Collection<T> loadPojos() {
        return paths.stream()
                .map(this::getFilePath)
                .flatMap(Optional::stream)
                .map(path -> JsonPOJOLoader.loadPOJO(path, type))
                .flatMap(Optional::stream)
                .toList();
    }
}
