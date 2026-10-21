package com.focaquest.catalog;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.focaquest.catalog.CatalogDtos.ExamDto;
import com.focaquest.catalog.CatalogDtos.RoleDto;
import com.focaquest.catalog.CatalogDtos.SyllabusDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catálogo curricular")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/exams")
    @Operation(summary = "Listar concursos")
    public List<ExamDto> exams() {
        return catalog.listExams();
    }

    @GetMapping("/exams/{id}/roles")
    @Operation(summary = "Consultar cargos de um concurso")
    public List<RoleDto> roles(@PathVariable String id) {
        return catalog.listRoles(id);
    }

    @GetMapping("/roles/{id}/syllabus")
    @Operation(summary = "Consultar trilhas e assuntos do edital vigente do cargo")
    public SyllabusDto syllabus(@PathVariable String id) {
        return catalog.syllabus(id);
    }
}
