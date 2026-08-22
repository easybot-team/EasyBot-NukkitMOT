package com.springwater.easybot.nukkitmot.service;

import com.springwater.easybot.nukkitmot.EasyBotNukkitMOT;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class MainThreadExecutor {
    private static final long TIMEOUT_SECONDS = 10;

    private final EasyBotNukkitMOT plugin;

    public MainThreadExecutor(EasyBotNukkitMOT plugin) {
        this.plugin = plugin;
    }

    public <T> T call(Callable<T> callable) {
        if (plugin.getServer().isPrimaryThread()) {
            return invoke(callable);
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
            try {
                future.complete(callable.call());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });

        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待 Nukkit 主线程时被中断", exception);
        } catch (TimeoutException exception) {
            throw new IllegalStateException("等待 Nukkit 主线程处理超时", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Nukkit 主线程任务执行失败", cause);
        }
    }

    private static <T> T invoke(Callable<T> callable) {
        try {
            return callable.call();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
