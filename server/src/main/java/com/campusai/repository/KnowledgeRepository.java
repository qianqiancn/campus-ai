package com.campusai.repository;

import com.campusai.model.Knowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {

    @Query(value = "SELECT * FROM knowledge_base WHERE MATCH(question, keywords) AGAINST(:keyword IN NATURAL LANGUAGE MODE)", nativeQuery = true)
    List<Knowledge> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT k FROM Knowledge k WHERE k.question LIKE CONCAT('%', :keyword, '%') OR k.keywords LIKE CONCAT('%', :keyword, '%')")
    List<Knowledge> searchByLike(@Param("keyword") String keyword);

    List<Knowledge> findByCategory(String category);

    boolean existsByQuestion(String question);

    @Query("SELECT DISTINCT k.category FROM Knowledge k")
    List<String> findAllCategories();

    @Query("SELECT k.category, COUNT(k) FROM Knowledge k GROUP BY k.category")
    List<Object[]> countByCategory();
}
