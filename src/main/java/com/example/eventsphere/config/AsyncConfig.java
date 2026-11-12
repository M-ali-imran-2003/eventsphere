//package com.example.eventsphere.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//
//import java.util.concurrent.ThreadPoolExecutor;
//
///**
// * Separate thread pools, so a slow chatbot can never block emails or indexing.
// *
// * IMPORTANT: once you define your own Executor beans, Spring Boot stops creating its
// * default pool. TicketService uses plain @Async (emails), so we MUST provide a bean
// * named "taskExecutor" - that is the name plain @Async looks for.
// */
//@Configuration
//public class AsyncConfig {
//
//    /** Default pool for plain @Async (TicketService emails). Never drops work. */
//    @Bean(name = "taskExecutor")
//    public ThreadPoolTaskExecutor taskExecutor() {
//        ThreadPoolTaskExecutor ex = build("async-", 4, 8, 200);
//        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
//        return ex;
//    }
//
//    /** Chat requests (streaming + legacy polling jobs). Each one waits on Gemini, so keep it roomy. */
//    @Bean(name = "chatExecutor")
//    public ThreadPoolTaskExecutor chatExecutor() {
//        ThreadPoolTaskExecutor ex = build("chat-", 16, 32, 100);
//        // Default policy = reject when full (we answer "busy" instead of hanging forever).
//        return ex;
//    }
//
//    /** RAG indexing runs here, so re-indexing never steals threads from visitors chatting. */
//    @Bean(name = "indexExecutor")
//    public ThreadPoolTaskExecutor indexExecutor() {
//        ThreadPoolTaskExecutor ex = build("index-", 2, 4, 50);
//        return ex;
//    }
//
//    private ThreadPoolTaskExecutor build(String prefix, int core, int max, int queue) {
//        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
////
////        ThreadPoolTaskExecutor ex = build("async-", 8, 8, Integer.MAX_VALUE);
//        ex.setThreadNamePrefix(prefix);
//        ex.setCorePoolSize(core);
//        ex.setMaxPoolSize(max);
//        ex.setQueueCapacity(queue);
//        ex.setWaitForTasksToCompleteOnShutdown(true);
//        ex.setAwaitTerminationSeconds(20);
//        return ex;
//    }
//}

package com.example.eventsphere.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Separate thread pools, so a slow chatbot can never block emails or indexing.
 *
 * IMPORTANT: once you define your own Executor beans, Spring Boot stops creating its
 * default pool. TicketService uses plain @Async (emails), so we MUST provide a bean
 * named "taskExecutor" - that is the name plain @Async looks for.
 */
@Configuration
public class AsyncConfig {

    /** Default pool for plain @Async (TicketService emails). Never drops work. */
    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor ex = build("async-", 4, 8, 200);
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return ex;
    }

    /** Chat requests (streaming + legacy polling jobs). Each one waits on Gemini, so keep it roomy. */
    @Bean(name = "chatExecutor")
    public ThreadPoolTaskExecutor chatExecutor() {
        ThreadPoolTaskExecutor ex = build("chat-", 16, 32, 100);
        // Default policy = reject when full (we answer "busy" instead of hanging forever).
        return ex;
    }

    /** RAG indexing runs here, so re-indexing never steals threads from visitors chatting. */
    @Bean(name = "indexExecutor")
    public ThreadPoolTaskExecutor indexExecutor() {
        ThreadPoolTaskExecutor ex = build("index-", 2, 4, 50);
        return ex;
    }

    private ThreadPoolTaskExecutor build(String prefix, int core, int max, int queue) {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix(prefix);
        ex.setCorePoolSize(core);
        ex.setMaxPoolSize(max);
        ex.setQueueCapacity(queue);
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(20);
        return ex;
    }
}
