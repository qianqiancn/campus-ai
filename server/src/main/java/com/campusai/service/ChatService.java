package com.campusai.service;

import com.campusai.dto.ChatResponse;
import com.campusai.model.Conversation;
import com.campusai.model.Knowledge;
import com.campusai.model.Message;
import com.campusai.model.User;
import com.campusai.repository.ConversationRepository;
import com.campusai.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final KnowledgeService knowledgeService;
    private final OllamaService ollamaService;
    private final UserService userService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    private static final String SYSTEM_PROMPT = """
            你是四川工商职业技术学院的校园智能客服助手。你的职责是帮助学生解答关于学校的各类问题。
            
            回答规则:
            1. 优先使用下面提供的【知识库参考内容】来回答问题
            2. 如果知识库中有相关内容，请基于知识库内容来回答，保持回答准确、有条理
            3. 如果知识库中没有相关内容，请根据你的知识给出合理的引导性回复
            4. 始终保持友好、热情的语气
            5. 如果学生的问题不明确，可以主动询问更多细节
            6. 回答使用中文，简洁明了
            """;

    public ChatResponse chat(String studentId, String question, Long conversationId) {
        User user = userService.findByStudentId(studentId);

        List<Knowledge> relevantKnowledge = knowledgeService.search(question);
        List<Knowledge> topKnowledge = relevantKnowledge.stream()
                .limit(5)
                .collect(Collectors.toList());

        String context = buildContext(topKnowledge);
        String prompt = buildPrompt(context, question);

        log.info("RAG检索到 {} 条相关知识，取前 {} 条", relevantKnowledge.size(), topKnowledge.size());
        String answer = ollamaService.generate(prompt);

        Conversation conversation = getOrCreateConversation(user, question, conversationId);
        saveMessage(conversation, "USER", question);
        saveMessage(conversation, "AI", answer);

        String sources = topKnowledge.stream()
                .map(k -> "[" + k.getCategory() + "] " + k.getQuestion())
                .collect(Collectors.joining("; "));

        return new ChatResponse(answer, conversation.getId(), sources);
    }

    private String buildContext(List<Knowledge> knowledgeList) {
        if (knowledgeList.isEmpty()) {
            return "暂无相关知识库内容。";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < knowledgeList.size(); i++) {
            Knowledge k = knowledgeList.get(i);
            sb.append("【知识").append(i + 1).append("】\n");
            sb.append("问题: ").append(k.getQuestion()).append("\n");
            sb.append("答案: ").append(k.getAnswer()).append("\n");
            sb.append("分类: ").append(k.getCategory()).append("\n\n");
        }
        return sb.toString();
    }

    private String buildPrompt(String context, String question) {
        return SYSTEM_PROMPT + """
                
                【知识库参考内容】
                %s
                
                【学生问题】
                %s
                
                请根据以上信息回答学生的问题:
                """.formatted(context, question);
    }

    private Conversation getOrCreateConversation(User user, String question, Long conversationId) {
        if (conversationId != null) {
            return conversationRepository.findById(conversationId)
                    .orElseGet(() -> createConversation(user, question));
        }
        return createConversation(user, question);
    }

    private Conversation createConversation(User user, String question) {
        String title = question.length() > 30 ? question.substring(0, 30) + "..." : question;
        Conversation conversation = Conversation.builder()
                .user(user)
                .title(title)
                .build();
        return conversationRepository.save(conversation);
    }

    private void saveMessage(Conversation conversation, String role, String content) {
        Message message = Message.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .build();
        messageRepository.save(message);
    }
}
