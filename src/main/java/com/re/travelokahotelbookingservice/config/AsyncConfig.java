package com.re.travelokahotelbookingservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * ThreadPool chuyên biệt cho việc ghi Audit Log ngầm,
     * tách hoàn toàn khỏi luồng HTTP chính để không làm nghẽn phản hồi API của khách hàng.
     */
    @Bean(name = "auditLogExecutor")
    public Executor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("audit-log-");
        // Khi hàng đợi đầy, tác vụ sẽ được thực thi trên chính luồng gọi thay vì vứt bỏ
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        log.info("Khởi tạo thành công ThreadPool [auditLogExecutor] cho tác vụ ngầm");
        return executor;
    }
}
