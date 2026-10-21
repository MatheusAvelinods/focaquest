package com.focaquest.content;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.focaquest.content.ContentModel.OptionDef;
import com.focaquest.content.ContentModel.QuestionDef;

class ContentValidatorTest {

    static final Path CONTENT = Path.of("../../content/tjpb-2026");

    @Test
    void conteudoVersionadoDoRepositorioEhValido() {
        var bundle = ContentLoader.load(CONTENT);
        assertThat(ContentValidator.validate(bundle)).isEmpty();
        assertThat(bundle.lessons()).isNotEmpty();
        assertThat(bundle.questions()).isNotEmpty();
    }

    @Test
    void todaLicaoPublicadaTemObjetivoExplicacaoExemploEAtividades() {
        var bundle = ContentLoader.load(CONTENT);
        bundle.lessons().forEach(l -> {
            assertThat(l.value().objective()).as(l.value().id()).isNotBlank();
            assertThat(l.value().explanation()).as(l.value().id()).isNotBlank();
            assertThat(l.value().example()).as(l.value().id()).isNotBlank();
            assertThat(l.value().activities()).as(l.value().id()).isNotEmpty();
        });
    }

    private static List<OptionDef> fiveOptions() {
        List<OptionDef> out = new ArrayList<>();
        for (String l : List.of("A", "B", "C", "D", "E")) {
            out.add(new OptionDef(l, "Texto " + l, "Comentário " + l));
        }
        return out;
    }

    private static QuestionDef question(List<OptionDef> options, String correct, String explanation) {
        return new QuestionDef("q1", "t1", "FACIL", "FGV", "fonte", "autor", "PUBLISHED", null, "Enunciado?",
                options, correct, explanation);
    }

    private static List<String> errorsOf(QuestionDef q) {
        List<String> errors = new ArrayList<>();
        ContentValidator.validateQuestion(errors, "arquivo.yaml", q, Set.of("t1"));
        return errors;
    }

    @Test
    void questaoCompletaEhAceita() {
        assertThat(errorsOf(question(fiveOptions(), "C", "Explicação"))).isEmpty();
    }

    @Test
    void rejeitaGabaritoInexistente() {
        assertThat(errorsOf(question(fiveOptions(), "F", "Explicação"))).anyMatch(e -> e.contains("gabarito"));
    }

    @Test
    void rejeitaQuestaoSemExplicacao() {
        assertThat(errorsOf(question(fiveOptions(), "A", " "))).anyMatch(e -> e.contains("explanation"));
    }

    @Test
    void formatoFgvExigeCincoAlternativas() {
        var four = fiveOptions().subList(0, 4);
        assertThat(errorsOf(question(four, "A", "Explicação"))).anyMatch(e -> e.contains("FGV"));
    }

    @Test
    void exigeComentarioEmCadaAlternativa() {
        var options = new ArrayList<>(fiveOptions());
        options.set(2, new OptionDef("C", "Texto C", ""));
        assertThat(errorsOf(question(options, "A", "Explicação"))).anyMatch(e -> e.contains("rationale"));
    }

    @Test
    void rejeitaAlternativasDuplicadas() {
        var options = new ArrayList<>(fiveOptions());
        options.set(1, new OptionDef("B", "Texto A", "x"));
        assertThat(errorsOf(question(options, "A", "Explicação"))).anyMatch(e -> e.contains("idêntico"));
    }

    @Test
    void rejeitaTopicoInexistente() {
        var q = new QuestionDef("q1", "nao-existe", "FACIL", "FGV", "fonte", "autor", "PUBLISHED", null, "Enunciado",
                fiveOptions(), "A", "Explicação");
        assertThat(errorsOf(q)).anyMatch(e -> e.contains("tópico inexistente"));
    }
}
