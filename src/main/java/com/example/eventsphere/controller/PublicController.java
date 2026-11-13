//package com.example.eventsphere.controller;
//
//import com.example.eventsphere.dto.*;
//import com.example.eventsphere.service.*;
//import jakarta.validation.Valid;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import com.example.eventsphere.prediction.DemandPredictionService;
//
//
//
//
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/api/public")
//public class PublicController {
//
//    private final EventService eventService;
//    private final CheckoutService checkoutService;
//    private final TicketService ticketService;
//    private final ChatbotService chatbotService;
//    private final ChatbotJobService chatbotJobService;
////    private final DemandPredictionService predictionService;
//
//
//    public PublicController(EventService eventService, CheckoutService checkoutService, TicketService ticketService, ChatbotService chatbotService, ChatbotJobService chatbotJobService) {
//        this.eventService = eventService;
//        this.checkoutService = checkoutService;
//        this.ticketService = ticketService;
//        this.chatbotService = chatbotService;
//        this.chatbotJobService = chatbotJobService;
//    }
//
//    @GetMapping("/event/{slug}")
//    public ResponseEntity<PublicEventResponse> getPublicLandingPage(@PathVariable String slug) {
//        PublicEventResponse response = eventService.getPublicEventDetails(slug);
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/get-published-events")
//    public ResponseEntity<List<PublicEventCardDTO>> getAllActiveEvents() {
//        return ResponseEntity.ok(eventService.getExplorePageEvents());
//    }
//
//    @PostMapping("/checkout/{eventId}")
//    public ResponseEntity<CheckoutResponse> processCheckout(
//            @PathVariable UUID eventId,
//            @Valid @RequestBody CheckoutRequest request) {
//
//        // The @Transactional service handles the math, money, and database saves
//        CheckoutResponse result = checkoutService.processCheckout(eventId, request);
//
//        // Return a 200 OK with the transaction reference
//        return ResponseEntity.ok(result);
//    }
//
//    @GetMapping("/discount-code/validate/{eventId}")
//    public ResponseEntity<Map<String, Object>> validatePromoCode(
//            @PathVariable UUID eventId,
//            @RequestParam String code) {
//
//        try {
//            BigDecimal discountPercentage = eventService.validatePromoCodeForPublic(eventId, code);
//
//            // Return it in a nice JSON object for the frontend
//            return ResponseEntity.ok(Map.of(
//                    "isValid", true,
//                    "percentage", discountPercentage
//            ));
//        } catch (RuntimeException e) {
//            // Return a 400 Bad Request if the code is expired, full, or invalid
//            return ResponseEntity.badRequest().body(Map.of(
//                    "isValid", false,
//                    "message", e.getMessage()
//            ));
//        }
//    }
//
//    @PostMapping("/tickets/recover/{eventId}")
//    public ResponseEntity<Map<String, String>> recoverLostTicket(
//            @PathVariable UUID eventId,
//            @Valid @RequestBody LostTicketRecoveryRequest request) {
//
//        String message = ticketService.recoverLostTickets(eventId, request);
//        return ResponseEntity.ok(Map.of("message", message));
//    }
//
//    @PostMapping("/event/{eventId}/chat")
//    public ResponseEntity<ChatJobStartResponse> chatWithEvent(
//            @PathVariable UUID eventId,
//            @RequestBody ChatRequest request
//    ) {
//
//        ChatJobStartResponse response =
//                chatbotJobService.startChatJob(
//                        eventId,
//                        request
//                );
//
//        return ResponseEntity
//                .accepted()
//                .body(response);
//    }
//
//    @GetMapping("/event/chat/jobs/{jobId}")
//    public ResponseEntity<ChatJobStatusResponse> getChatJob(
//            @PathVariable UUID jobId
//    ) {
//
//        ChatJobStatusResponse response =
//                chatbotJobService.getJob(
//                        jobId
//                );
//
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/event/chat/history/{sessionId}")
//    public ResponseEntity<List<ChatMessagesResponse>> getChatHistory(
//            @PathVariable UUID sessionId
//    ) {
//
//        List<ChatMessagesResponse> response =
//                chatbotService.getChatHistory(
//                        sessionId
//                );
//        return ResponseEntity.ok(response);
//    }
//
//
////    @GetMapping("/event/chat/history/{sessionId}")
////    public ResponseEntity<List<ChatResponse>> getChatHistory(
////            @PathVariable UUID sessionId
////    ) {
////        List<ChatMessagesResponse> response = chatbotService.getChatHistory(sessionId);
////        return ResponseEntity.ok(response);
////    }
//
//
//
//
//
//
//    @PostMapping("/event/{eventId}/reindex")
//    public ResponseEntity<?> reindexEvent(
//            @PathVariable UUID eventId
//    ) {
//
//        PublicEventResponse eventData =
//                eventService.getPublicEventDetailsByEventId(
//                        eventId
//                );
//
//        String eventSlug =
//                eventService.getEventSlugById(
//                        eventId
//                );
//
//        chatbotService.indexEvent(
//                eventId,
//                eventSlug,
//                eventData
//        );
//
//        return ResponseEntity.ok(
//                Map.of(
//                        "status", "indexed",
//                        "event_id", eventId.toString()
//                )
//        );
//    }
//
//
//}



package com.example.eventsphere.controller;

import com.example.eventsphere.dto.*;
import com.example.eventsphere.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final EventService eventService;
    private final CheckoutService checkoutService;
    private final TicketService ticketService;
    private final ChatbotService chatbotService;
    private final ChatbotJobService chatbotJobService;
    private final ChatStreamService chatStreamService;
    private final ChatRateLimiter chatRateLimiter;

    public PublicController(EventService eventService,
                            CheckoutService checkoutService,
                            TicketService ticketService,
                            ChatbotService chatbotService,
                            ChatbotJobService chatbotJobService,
                            ChatStreamService chatStreamService,
                            ChatRateLimiter chatRateLimiter) {
        this.eventService = eventService;
        this.checkoutService = checkoutService;
        this.ticketService = ticketService;
        this.chatbotService = chatbotService;
        this.chatbotJobService = chatbotJobService;
        this.chatStreamService = chatStreamService;
        this.chatRateLimiter = chatRateLimiter;
    }

    @GetMapping("/event/{slug}")
    public ResponseEntity<PublicEventResponse> getPublicLandingPage(@PathVariable String slug) {
        PublicEventResponse response = eventService.getPublicEventDetails(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-published-events")
    public ResponseEntity<List<PublicEventCardDTO>> getAllActiveEvents() {
        return ResponseEntity.ok(eventService.getExplorePageEvents());
    }

    @PostMapping("/checkout/{eventId}")
    public ResponseEntity<CheckoutResponse> processCheckout(
            @PathVariable UUID eventId,
            @Valid @RequestBody CheckoutRequest request) {

        // The @Transactional service handles the math, money, and database saves
        CheckoutResponse result = checkoutService.processCheckout(eventId, request);

        // Return a 200 OK with the transaction reference
        return ResponseEntity.ok(result);
    }

    @GetMapping("/discount-code/validate/{eventId}")
    public ResponseEntity<Map<String, Object>> validatePromoCode(
            @PathVariable UUID eventId,
            @RequestParam String code) {

        try {
            BigDecimal discountPercentage = eventService.validatePromoCodeForPublic(eventId, code);

            // Return it in a nice JSON object for the frontend
            return ResponseEntity.ok(Map.of(
                    "isValid", true,
                    "percentage", discountPercentage
            ));
        } catch (RuntimeException e) {
            // Return a 400 Bad Request if the code is expired, full, or invalid
            return ResponseEntity.badRequest().body(Map.of(
                    "isValid", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/tickets/recover/{eventId}")
    public ResponseEntity<Map<String, String>> recoverLostTicket(
            @PathVariable UUID eventId,
            @Valid @RequestBody LostTicketRecoveryRequest request) {

        String message = ticketService.recoverLostTickets(eventId, request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    // =====================================================
    // CHATBOT - STREAMING (use this one from the widget)
    //
    // Returns Server-Sent Events:
    //   data: {"t":"Tickets "}           pieces of the answer
    //   data: {"done":true,...}          finished
    //   data: {"error":"..."}            failed
    // =====================================================

    @PostMapping(value = "/event/{eventId}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamChatWithEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody ChatRequest request,
            HttpServletRequest http
    ) {
        if (!eventService.isEventPublished(eventId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!chatRateLimiter.allow(http, request.getSessionId())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "30")
                    .build();
        }

        SseEmitter emitter = chatStreamService.stream(eventId, request);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }

    // =====================================================
    // CHATBOT - LEGACY POLLING (kept so the old widget keeps working)
    // =====================================================

    @PostMapping("/event/{eventId}/chat")
    public ResponseEntity<?> chatWithEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody ChatRequest request,
            HttpServletRequest http
    ) {
        if (!eventService.isEventPublished(eventId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!chatRateLimiter.allow(http, request.getSessionId())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "30")
                    .body(Map.of("message", "Too many messages. Please wait a moment."));
        }

        ChatJobStartResponse response = chatbotJobService.startChatJob(eventId, request);

        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/event/chat/jobs/{jobId}")
    public ResponseEntity<ChatJobStatusResponse> getChatJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(chatbotJobService.getJob(jobId));
    }

    @GetMapping("/event/chat/history/{sessionId}")
    public ResponseEntity<List<ChatMessagesResponse>> getChatHistory(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(chatbotService.getChatHistory(sessionId));
    }

    // NOTE: POST /event/{eventId}/reindex was REMOVED from this public controller.
    // Anyone on the internet could call it. It now lives in AdminReindexController
    // as POST /api/admin/event/{eventId}/reindex (ADMIN only).
}




