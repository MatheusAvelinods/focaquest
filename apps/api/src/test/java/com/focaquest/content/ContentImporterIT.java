package com.focaquest.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import com.focaquest.questions.Question;
import com.focaquest.questions.QuestionRepository;
import com.focaquest.support.IntegrationTest;

class ContentImporterIT extends IntegrationTest {

    @Autowired
    ContentImporter importer;
    @Autowired
    QuestionRepository questions;

    @TempDir
    Path tmp;

    @Test
    void reimportarOMesmoConteudoNaoAlteraNada() {
        // O conteúdo já foi importado na inicialização da aplicação.
        var result = importer.importFrom(ContentValidatorTest.CONTENT);
        assertThat(result.created()).isZero();
        assertThat(result.updated()).isZero();
        assertThat(result.retired()).isZero();
        assertThat(result.unchanged()).isPositive();
    }

    @Test
    void questaoAlteradaGanhaNovaRevisaoERejeitaConteudoInvalido() throws IOException {
        Path copy = copyContent();
        Path file = copy.resolve("questions/algoritmos.yaml");
        Files.writeString(file, Files.readString(file)
                .replace("A complexidade de tempo do trecho acima, no pior caso, é",
                        "No pior caso, a complexidade de tempo do trecho acima é"));
        int before = questions.findById("q.alg.cx.01").orElseThrow().getRevision();

        var result = importer.importFrom(copy);
        assertThat(result.updated()).isEqualTo(1);
        Question q = questions.findById("q.alg.cx.01").orElseThrow();
        assertThat(q.getRevision()).isEqualTo(before + 1);

        // Gabarito inexistente: a importação inteira é rejeitada e nada muda.
        Files.writeString(file, Files.readString(file).replaceFirst("correct: D", "correct: Z"));
        assertThatThrownBy(() -> importer.importFrom(copy))
                .isInstanceOf(ContentValidationException.class)
                .hasMessageContaining("gabarito");
        assertThat(questions.findById("q.alg.cx.01").orElseThrow().getRevision()).isEqualTo(before + 1);

        // Restaura o conteúdo original para não afetar outros testes.
        importer.importFrom(ContentValidatorTest.CONTENT);
    }

    private Path copyContent() throws IOException {
        Path src = ContentValidatorTest.CONTENT;
        try (Stream<Path> s = Files.walk(src)) {
            for (Path p : s.sorted(Comparator.naturalOrder()).toList()) {
                Path target = tmp.resolve(src.relativize(p).toString());
                if (Files.isDirectory(p)) Files.createDirectories(target);
                else Files.copy(p, target);
            }
        }
        return tmp;
    }
}
