package info.kgeorgiy.ja.ilyin.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author Yaroslav Ilin
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final WaitingQueue tasks = new WaitingQueue();
    private final List<Thread> workers;

    /**
     * Implementation of {@link ParallelMapper}
     *
     * @param threads to parallel
     */
    public ParallelMapperImpl(int threads) {
        workers = new ArrayList<>();
        Runnable task = () -> {
            try {
                while (!Thread.interrupted()) {
                    tasks.get().run();
                }
            } catch (InterruptedException ignored) {
            }
        };
        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(task);
            workers.add(thread);
            thread.start();
        }
    }


    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        return new Mapper<R>(f, args).get();
    }

    @Override
    public void close() {
        tasks.close();
        for (Thread thread : workers) {
            thread.interrupt();
        }
        try {
            IterativeParallelism.joinThreads(workers);
        } catch (InterruptedException ignored) {
        }
        // :NOTE: Р·Р°С‡РµРј РІС‹РїРѕР»РЅРёС‚СЊ РІСЃРµ Р·Р°РґР°С‡Рё, РєРѕРіРґР° РїРѕРїСЂРѕСЃРёР»Рё Р·Р°РєСЂС‹С‚СЊ?
        // :FIX: РЎРѕРіР»Р°СЃРЅРѕ РґРѕРєСѓРјРµРЅС‚Р°С†РёРё РЅРµ Р·Р°С‡РµРј. Р’СЃРµ РјРѕР¶РµС‚ Р±С‹С‚СЊ РІ undefined СЃРѕСЃС‚РѕСЏРЅРёРё, РЅРµ РїСЂРѕС‡РёС‚Р°Р» РґРѕРєСѓ РґРѕ СЌС‚РѕРіРѕ
    }

    private class Mapper<R> {
        private final List<R> result;
        private int waiters;
        private final RuntimeException exception = new RuntimeException();
        private boolean isException;

        <T> Mapper(Function<? super T, ? extends R> fun, List<? extends T> args) {
            waiters = args.size();
            result = new ArrayList<>(Collections.nCopies(waiters, null));
            for (int i = 0; i < args.size(); i++) {
                final int pos = i;
                tasks.add(() -> {
                    try {
                        setResult(pos, fun.apply(args.get(pos)));
                    } catch (RuntimeException e) {
                        synchronized (exception) {
                            isException = true;
                            exception.addSuppressed(e);
                        }
                    } finally {
                        done();
                    }
                });
            }
        }

        public synchronized List<R> get() throws InterruptedException {
            if (waiters > 0) {
                wait();
            }
            if (isException) {
                throw exception;
            }
            return result;
        }

        private synchronized void setResult(final int pos, R value) {
            result.set(pos, value);
        }

        private synchronized void done() {
            waiters--;
            if (waiters == 0) {
                notify();
            }
        }
    }

}
