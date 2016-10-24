package hazi.security.bme.hu;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ParallelTaskExecutor {
    private static final int NULL_LIST_SIZE = 0;
    private static final String RETURNING_EMPTY_LIST = ", returning empty list!";
    private static final int MAIN_THREAD_COUNT = 1;
    private static final Duration TASK_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration BARRIER_TIMEOUT = Duration.ofSeconds(10);
    private static final TimeUnit TIMEUNIT = TimeUnit.MILLISECONDS;

    private ParallelTaskExecutor() {
    }

    public static <T, R> List<R> executeParallelTask(Collection<T> taskInputList, Function<T, R> task) {
        return executeParallelTask(TASK_TIMEOUT, TIMEUNIT, taskInputList, task);
    }

    public static <T, R> List<R> executeParallelTask(final Duration taskTimeout, final TimeUnit taskTimeUnit,
            final Collection<T> taskInputList, final Function<T, R> task) {
        List<R> results = Collections.emptyList();
        final int numOfThreads = Optional.ofNullable(taskInputList).map((list) -> list.size()).orElse(NULL_LIST_SIZE);

        System.out.println("Initiating executor service...");
        try (final AutoCloseableForkJoinPool threadPool = new AutoCloseableForkJoinPool(numOfThreads)) {
            final CyclicBarrier barrier = new CyclicBarrier(numOfThreads + MAIN_THREAD_COUNT);

            System.out.println("Creating parallel tasks...");
            List<Callable<R>> threads = new ArrayList<>();
            taskInputList.forEach((taskInput) -> {
                threads.add(new Callable<R>() {

                    @Override
                    public R call() throws Exception {
                        barrier.await();
                        return task.apply(taskInput);
                    }

                });
            });

            System.out.println("Starting all threads...");
            List<Future<R>> futureResults = threads.parallelStream().map((thread) -> {
                return threadPool.submit(thread);
            }).collect(Collectors.toList());
            barrier.await(BARRIER_TIMEOUT.toMillis(), TIMEUNIT);

            System.out.println("Waiting for all tasks to finish...");
            results = futureResults.parallelStream().map((futureResult) -> {
                try {
                    return futureResult.get(taskTimeout.toMillis(), taskTimeUnit);
                } catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
                    return null;
                }
            }).collect(Collectors.toList());

            System.out.println("Returning results list...");
        } catch (IllegalArgumentException e) {
        	System.err.println("Invalid input list size: " + taskInputList.size() + RETURNING_EMPTY_LIST);
        } catch (NullPointerException e) {
            System.err.println("Null reference found" + RETURNING_EMPTY_LIST);
        } catch (Exception e) {
        	System.err.println("Unexpected exception happened during scheduling/executing parallel tasks" + RETURNING_EMPTY_LIST);
        }

        return results;
    }

    private static class AutoCloseableForkJoinPool extends ForkJoinPool implements AutoCloseable {
        public AutoCloseableForkJoinPool(int parallelism) {
            super(parallelism);
        }

        @Override
        public void close() throws Exception {
            shutdownNow();
        }
    }
}

