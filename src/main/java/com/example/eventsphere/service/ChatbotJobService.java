package com.example.eventsphere.service;

import com.example.eventsphere.dto.ChatJobStartResponse;
import com.example.eventsphere.dto.ChatJobStatusResponse;
import com.example.eventsphere.dto.ChatRequest;
import com.example.eventsphere.entity.ChatbotJob;
import com.example.eventsphere.enums.ChatJobStatus;
import com.example.eventsphere.repository.ChatbotJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatbotJobService {

    private final ChatbotJobRepository chatbotJobRepository;
    private final ChatbotJobProcessor chatbotJobProcessor;


    // =====================================================
    // START NEW CHAT JOB
    // =====================================================

//    public ChatJobStartResponse startChatJob(
//            String eventSlug,
//            ChatRequest request
//    ) {
//
//        UUID jobId =
//                UUID.randomUUID();
//
//
//        ChatbotJob job =
//                new ChatbotJob();
//
//        job.setId(jobId);
//
//        job.setEventSlug(
//                eventSlug
//        );
//
//        job.setSessionId(
//                request.getSessionId()
//        );
//
//        job.setMessage(
//                request.getMessage()
//        );
//
//        job.setStatus(
//                ChatJobStatus.PENDING
//        );
//
//        job.setCreatedAt(
//                Instant.now()
//        );
//
//
//        // Save first.
//        //
//        // Repository save finishes before we start
//        // the background processor.
//        chatbotJobRepository.save(job);
//
//
//        // Run Gemini/RAG in background
//        chatbotJobProcessor.processJob(
//                jobId
//        );
//
//
//        // Return immediately
//        return new ChatJobStartResponse(
//                jobId,
//                ChatJobStatus.PENDING.name()
//        );
//    }


    public ChatJobStartResponse startChatJob(
            UUID eventId,
            ChatRequest request
    ) {

        UUID jobId =
                UUID.randomUUID();

        ChatbotJob job =
                new ChatbotJob();

        job.setId(jobId);

        job.setEventId(
                eventId
        );

        job.setSessionId(
                request.getSessionId()
        );

        job.setMessage(
                request.getMessage()
        );

        job.setStatus(
                ChatJobStatus.PENDING
        );

        job.setCreatedAt(
                Instant.now()
        );

        chatbotJobRepository.save(job);

        chatbotJobProcessor.processJob(
                job.getId()
        );

        return new ChatJobStartResponse(
                jobId,
                ChatJobStatus.PENDING.name()
        );
    }

    // =====================================================
    // POLLING
    // =====================================================

    public ChatJobStatusResponse getJob(
            UUID jobId
    ) {

        ChatbotJob job =
                chatbotJobRepository
                        .findById(jobId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Chat job not found."
                                )
                        );


        return new ChatJobStatusResponse(
                job.getId(),
                job.getStatus().name(),
                job.getAnswer(),
                job.getErrorMessage()
        );
    }
}