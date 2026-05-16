package com.campusai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeDTO {
    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题不能超过500字")
    private String question;

    @NotBlank(message = "答案不能为空")
    private String answer;

    @Size(max = 100, message = "分类不能超过100字")
    private String category;

    @Size(max = 500, message = "关键词不能超过500字")
    private String keywords;
}
