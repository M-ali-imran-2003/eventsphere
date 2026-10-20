package com.example.eventsphere.event;

import com.example.eventsphere.dto.PublicEventResponse;
import com.example.eventsphere.service.ChatbotService;
import com.example.eventsphere.service.EventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Slf4j
@Component
@RequiredArgsConstructor
public class RagIndexListener {

    private final EventService eventService;
    private final ChatbotService chatbotService;


    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handleRagIndexRequest(
            RagIndexRequestedEvent event
    ) {

        try {

            log.info(
                    "Starting RAG indexing for event: {}",
                    event.eventSlug()
            );


            PublicEventResponse eventData =
                    eventService.getPublicEventDetails(
                            event.eventSlug()
                    );


            chatbotService.indexEvent(
                    event.eventSlug(),
                    eventData
            );


            log.info(
                    "RAG indexing completed for event: {}",
                    event.eventSlug()
            );

        } catch (Exception e) {

            log.error(
                    "RAG indexing failed for event: {}",
                    event.eventSlug(),
                    e
            );
        }
    }
}