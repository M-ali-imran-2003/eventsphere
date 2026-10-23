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
import com.example.eventsphere.repository.LandingPageRepository;
import com.example.eventsphere.entity.LandingPage;

import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class RagIndexListener {

    private final EventService eventService;
    private final ChatbotService chatbotService;
    private final LandingPageRepository landingPageRepository;


    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handleRagIndexRequest(
            RagIndexRequestedEvent event
    ) {

        try {

            UUID eventId =
                    event.eventId();


            LandingPage landingPage =
                    landingPageRepository
                            .findByEventId(eventId)
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Landing page not found."
                                    )
                            );


            String eventSlug =
                    landingPage.getSlug();


            PublicEventResponse eventData =
                    eventService.getPublicEventDetails(
                            eventSlug
                    );


            chatbotService.indexEvent(
                    eventId,
                    eventSlug,
                    eventData
            );


            log.info(
                    "RAG indexing completed for Event ID: {}",
                    eventId
            );

        } catch (Exception e) {

            log.error(
                    "RAG indexing failed for Event ID: {}",
                    event.eventId(),
                    e
            );
        }
    }
}