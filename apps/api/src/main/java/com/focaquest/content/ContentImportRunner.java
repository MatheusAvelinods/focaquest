package com.focaquest.content;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Importa o conteúdo na inicialização quando {@code focaquest.content.import-on-startup=true}. */
@Component
@ConditionalOnProperty(name = "focaquest.content.import-on-startup", havingValue = "true")
public class ContentImportRunner implements ApplicationRunner {

    private final ContentImporter importer;
    private final Path path;

    public ContentImportRunner(ContentImporter importer, @Value("${focaquest.content.path}") String path) {
        this.importer = importer;
        this.path = Path.of(path);
    }

    @Override
    public void run(ApplicationArguments args) {
        importer.importFrom(path);
    }
}
