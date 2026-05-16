package com.campusai.service;

import com.campusai.dto.KnowledgeDTO;
import com.campusai.model.Knowledge;
import com.campusai.repository.KnowledgeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeRepository knowledgeRepository;

    public Page<Knowledge> findAll(Pageable pageable) {
        return knowledgeRepository.findAll(pageable);
    }

    public Knowledge findById(Long id) {
        return knowledgeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("知识条目不存在"));
    }

    @Transactional
    public Knowledge create(KnowledgeDTO dto) {
        if (knowledgeRepository.existsByQuestion(dto.getQuestion())) {
            throw new RuntimeException("该问题已存在，请勿重复添加");
        }
        Knowledge knowledge = Knowledge.builder()
                .question(dto.getQuestion())
                .answer(dto.getAnswer())
                .category(dto.getCategory() != null ? dto.getCategory() : "通用")
                .keywords(dto.getKeywords())
                .build();
        return knowledgeRepository.save(knowledge);
    }

    @Transactional
    public Knowledge update(Long id, KnowledgeDTO dto) {
        Knowledge knowledge = findById(id);
        knowledge.setQuestion(dto.getQuestion());
        knowledge.setAnswer(dto.getAnswer());
        if (dto.getCategory() != null) {
            knowledge.setCategory(dto.getCategory());
        }
        knowledge.setKeywords(dto.getKeywords());
        return knowledgeRepository.save(knowledge);
    }

    @Transactional
    public void delete(Long id) {
        knowledgeRepository.deleteById(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        knowledgeRepository.deleteAllById(ids);
    }

    public List<Knowledge> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String trimmed = keyword.trim();
        if (trimmed.length() > 100) {
            trimmed = trimmed.substring(0, 100);
        }
        List<Knowledge> results = knowledgeRepository.searchByKeyword(trimmed);
        if (results.isEmpty()) {
            results = tokenSearch(keyword);
        }
        return results;
    }

    private List<Knowledge> tokenSearch(String keyword) {
        List<Knowledge> allResults = new java.util.ArrayList<>();
        for (int i = 0; i < keyword.length(); i++) {
            for (int j = i + 1; j <= Math.min(i + 4, keyword.length()); j++) {
                String token = keyword.substring(i, j);
                if (token.length() < 2) continue;
                List<Knowledge> partial = knowledgeRepository.searchByLike(token);
                for (Knowledge k : partial) {
                    if (!allResults.contains(k)) {
                        allResults.add(k);
                    }
                }
            }
        }
        return allResults;
    }

    public List<String> getAllCategories() {
        return knowledgeRepository.findAllCategories();
    }

    public Map<String, Object> importFromExcel(MultipartFile file) throws IOException {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        int imported = 0;
        int skipped = 0;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String question = getCellString(row.getCell(0));
            String answer = getCellString(row.getCell(1));
            String category = getCellString(row.getCell(2));
            String keywords = getCellString(row.getCell(3));

            if (question.isBlank() || answer.isBlank()) continue;
            if (knowledgeRepository.existsByQuestion(question)) {
                skipped++;
                continue;
            }

            Knowledge knowledge = Knowledge.builder()
                    .question(question)
                    .answer(answer)
                    .category(category.isBlank() ? "通用" : category)
                    .keywords(keywords)
                    .build();
            knowledgeRepository.save(knowledge);
            imported++;
        }
        workbook.close();
        return Map.of("imported", imported, "skipped", skipped);
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
