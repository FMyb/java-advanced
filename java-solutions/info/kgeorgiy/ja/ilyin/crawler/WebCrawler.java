package info.kgeorgiy.ja.ilyin.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Recursive downloader pages from website.
 *
 * @author Yaroslav Ilin
 */
public class WebCrawler implements AdvancedCrawler {
    private final Downloader downloader;
    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private final int perHost;
    private final Map<String, DownloadQueue> downloadQueues = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<String> nextLayer;


    /**
     * Create {@link WebCrawler}
     *
     * @param downloader  allows you to download pages and extract links from them
     * @param downloaders the maximum number of simultaneously loaded pages
     * @param extractors  the maximum number of pages from which links are retrieved at the same time
     * @param perHost     maximum number of pages simultaneously loaded from one host
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    /**
     * running {@link WebCrawler} with args
     *
     * @param args arguments to WebCrawler: expected url [depth [downloads [extractors [perHost]]]]
     */
    public static void main(String[] args) {
        checkArgs(args);
        String url = args[0];
        int i = 1;
        int depth = parseArg(i++, args, 3);
        int downloads = parseArg(i++, args, 2);
        int extractors = parseArg(i++, args, 2);
        int perHost = parseArg(i, args, 2);
        try (WebCrawler webCrawler = new WebCrawler(new CachingDownloader(), downloads, extractors, perHost)) {
            Result result = webCrawler.download(url, depth);
            System.out.println("Downloaded " + result.getDownloaded().size() + " pages");
            System.out.println("With error " + result.getErrors().size() + " pages");
        } catch (IOException e) {
            System.err.println("Failed " + e.getMessage());
        }
    }

    private static int parseArg(int ind, String[] args, int defaultValue) {
        if (ind < args.length) {
            return Integer.parseInt(args[ind]);
        }
        return defaultValue;
    }

    private static void checkArgs(String[] args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull) || args.length == 0 || args.length > 5) {
            throw new IllegalArgumentException("Expected : WebCrawler url [depth [downloads [extractors [perHost]]]]");
        }
    }

    private void downloading(String url, boolean isLast, Set<String> downloaded, Map<String, IOException> errors, Phaser phaser, Set<String> hosts) {  // :TODO: Check for last level
        String host;
        try {
            host = URLUtils.getHost(url);
            if ((hosts != null && !hosts.contains(host)) || !downloaded.add(url) || errors.containsKey(url)) {
                phaser.arrive();
                return;
            }
            DownloadQueue queue = downloadQueues.computeIfAbsent(host, key -> new DownloadQueue());
            queue.add(() -> {
                try {
                    Document document = downloader.download(url);
                    if (!isLast) {
                        extractors.submit(() -> {
                            try {
                                nextLayer.addAll(document.extractLinks());
                            } catch (IOException e) {
                                errors.put(url, e);
                            } finally {
                                phaser.arrive();
                            }
                        });
                    } else {
                        phaser.arrive();
                    }
                } catch (IOException e) {
                    errors.put(url, e);
                    // :NOTE-2: you might miss that arrive if it's not IOException but some other exception
                    phaser.arrive();
                }
            });
        } catch (IOException e) {
            errors.put(url, e);
            phaser.arrive();
        }
    }

    private void download(String url, int depth, Set<String> downloaded, Map<String, IOException> errors, Set<String> hosts) {
        if (depth <= 0) {
            return;
        }
        nextLayer.add(url);
        for (int i = 0; i < depth; i++) {
            Phaser phaser = new Phaser(nextLayer.size() + 1);
            ConcurrentLinkedQueue<String> tmp = new ConcurrentLinkedQueue<>(nextLayer);
            nextLayer = new ConcurrentLinkedQueue<>();
            int finalI = i;
            tmp.forEach(link -> {
                try {
                    downloading(link, (finalI == depth - 1), downloaded, errors, phaser, hosts);
                } finally {
                    phaser.arrive();
                }
            });
            phaser.arriveAndAwaitAdvance();
        }
    }

    @Override
    public Result download(String url, int depth) {
        return getResult(url, depth, null);
    }

    @Override
    public void close() {
        shutdownExecutor(downloaders);
        shutdownExecutor(extractors);
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("Executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = executor.shutdownNow();
                System.err.println("Executor was abruptly shut down. " + droppedTasks.size() + " tasks will not be executed.");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Result download(String url, int depth, List<String> hosts) {
        return getResult(url, depth, new HashSet<>(hosts));
    }

    private Result getResult(String url, int depth, Set<String> hosts) {
        Set<String> downloaded = ConcurrentHashMap.newKeySet();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        nextLayer = new ConcurrentLinkedQueue<>();
        download(url, depth, downloaded, errors, hosts);
        downloaded.removeAll(errors.keySet());
        return new Result(new ArrayList<>(downloaded), errors);
    }

    private class DownloadQueue {
        private final Queue<Runnable> tasks = new ArrayDeque<>();
        private final AtomicInteger executing = new AtomicInteger(0);

        private synchronized void execute() {
            if (executing.get() < perHost && !tasks.isEmpty()) {
                Runnable task = tasks.poll();
                executing.incrementAndGet();
                downloaders.submit(() -> {
                    task.run();
                    // :NOTE:
                    executing.decrementAndGet();
                    execute();
                });
            }
        }

        synchronized void add(Runnable task) {
            tasks.add(task);
            execute();
        }
    }
}