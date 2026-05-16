package com.campusai.controller;

import com.campusai.dto.KnowledgeDTO;
import com.campusai.model.Knowledge;
import com.campusai.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping
    public ResponseEntity<Page<Knowledge>> list(Pageable pageable) {
        return ResponseEntity.ok(knowledgeService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Knowledge> getById(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeService.findById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody KnowledgeDTO dto) {
        try {
            return ResponseEntity.ok(knowledgeService.create(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody KnowledgeDTO dto) {
        try {
            return ResponseEntity.ok(knowledgeService.update(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<?> deleteBatch(@RequestBody List<Long> ids) {
        knowledgeService.deleteBatch(ids);
        return ResponseEntity.ok(Map.of("message", "批量删除成功", "count", ids.size()));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(knowledgeService.search(keyword));
    }

    @GetMapping("/categories")
    public ResponseEntity<?> categories() {
        return ResponseEntity.ok(knowledgeService.getAllCategories());
    }

    @PostMapping("/import")
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = knowledgeService.importFromExcel(file);
            return ResponseEntity.ok(Map.of("message", "导入完成", "imported", result.get("imported"), "skipped", result.get("skipped")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "导入失败: " + e.getMessage()));
        }
    }
}
