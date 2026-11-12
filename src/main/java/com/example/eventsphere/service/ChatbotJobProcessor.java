//package com.example.eventsphere.service;
//
//import com.example.eventsphere.dto.ChatRequest;
//import com.example.eventsphere.dto.ChatResponse;
//import com.example.eventsphere.entity.ChatbotJob;
//import com.example.eventsphere.enums.ChatJobStatus;
//import com.example.eventsphere.repository.ChatbotJobRepository;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//
//import java.time.Instant;
//import java.util.UUID;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class ChatbotJobProcessor {
//
//    private final ChatbotJobRepository chatbotJobRepository;
//    private final ChatbotService chatbotService;
//
//
//    @Async
//    public void processJob(UUID jobId) {
//
//        ChatbotJob job = chatbotJobRepository
//                .findById(jobId)
//                .orElse(null);
//
//        if (job == null) {
//            log.error(
//                    "Chatbot job not found: {}",
//                    jobId
//            );
//            return;
//        }
//
//        try {
//
//            // -----------------------------------------
//            // Mark as processing
//            // -----------------------------------------
//
//            job.setStatus(
//                    ChatJobStatus.PROCESSING
//            );
//
//            chatbotJobRepository.save(job);
//
//
//            log.info(
//                    "Processing chatbot job {} for event {}",
//                    jobId,
//                    job.getEventId()
//            );
//
//
//            // -----------------------------------------
//            // Build the request for your EXISTING
//            // ChatbotService
//            // -----------------------------------------
//
//            ChatRequest request =
//                    new ChatRequest();
//
//            request.setMessage(
//                    job.getMessage()
//            );
//
//            request.setSessionId(
//                    job.getSessionId()
//            );
//
//
//            // -----------------------------------------
//            // THIS IS THE SLOW PART
//            //
//            // It can take 10, 20, 40+ sec.
//            //
//            // But it is NOT the user's HTTP request.
//            // -----------------------------------------
//
//            ChatResponse chatbotResponse =
//                    chatbotService.chat(
//                            job.getEventId(),
//                            request
//                    );
//
//
//            // -----------------------------------------
//            // Store final answer
//            // -----------------------------------------
//
//            job.setAnswer(
//                    chatbotResponse.getAnswer()
//            );
//
//            job.setStatus(
//                    ChatJobStatus.COMPLETED
//            );
//
//            job.setCompletedAt(
//                    Instant.now()
//            );
//
//            chatbotJobRepository.save(job);
//
//
//            log.info(
//                    "Chatbot job {} completed.",
//                    jobId
//            );
//
//        } catch (Exception e) {
//
//            log.error(
//                    "Chatbot job {} failed.",
//                    jobId,
//                    e
//            );
//
//
//            job.setStatus(
//                    ChatJobStatus.FAILED
//            );
//
//            job.setErrorMessage(
//                    e.getMessage() != null
//                            ? e.getMessage()
//                            : "Chatbot processing failed."
//            );
//
//            job.setCompletedAt(
//                    Instant.now()
//            );
//
//            chatbotJobRepository.save(job);
//        }
//    }
//}


package com.example.eventsphere.service;

import com.example.eventsphere.dto.ChatRequest;
import com.example.eventsphere.dto.ChatResponse;
import com.example.eventsphere.entity.ChatbotJob;
import com.example.eventsphere.enums.ChatJobStatus;
import com.example.eventsphere.repository.ChatbotJobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * LEGACY polling path. The streaming endpoint (/chat/stream) does not need this class.
 * Changes: runs on its own "chatExecutor" pool, and the public error message is generic
 * (the real exception stays in the server log only).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotJobProcessor {

    private static final String PUBLIC_ERROR = "The assistant is unavailable right now. Please try again.";

    private final ChatbotJobRepository chatbotJobRepository;
    private final ChatbotService chatbotService;

    @Async("chatExecutor")
    public void processJob(UUID jobId) {

        ChatbotJob job = chatbotJobRepository.findById(jobId).orElse(null);

        if (job == null) {
            log.error("Chatbot job not found: {}", jobId);
            return;
        }

        try {
            job.setStatus(ChatJobStatus.PROCESSING);
            chatbotJobRepository.save(job);

            log.info("Processing chatbot job {} for event {}", jobId, job.getEventId());

            ChatRequest request = new ChatRequest();
            request.setMessage(job.getMessage());
            request.setSessionId(job.getSessionId());

            ChatResponse chatbotResponse = chatbotService.chat(job.getEventId(), request);

            job.setAnswer(chatbotResponse.getAnswer());
            job.setStatus(ChatJobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            chatbotJobRepository.save(job);

            log.info("Chatbot job {} completed.", jobId);

        } catch (Exception e) {

            log.error("Chatbot job {} failed.", jobId, e);

            job.setStatus(ChatJobStatus.FAILED);
            job.setErrorMessage(PUBLIC_ERROR);   // never expose e.getMessage() to visitors
            job.setCompletedAt(Instant.now());
            chatbotJobRepository.save(job);
        }
    }
}
