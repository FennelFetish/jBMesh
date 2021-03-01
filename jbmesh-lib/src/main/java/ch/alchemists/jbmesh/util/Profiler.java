package ch.alchemists.jbmesh.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Profiler implements AutoCloseable {
    private static class Entry implements Comparable<Entry> {
        public Map<String, Entry> children = new HashMap<>();

        public final String name;
        public final int order;
        public long time = 0;
        public long min = Long.MAX_VALUE;
        public long max = 0;
        public long runs = 0;
        
        public Entry(String name, int order) {
            this.name  = name;
            this.order = order;
        }

        @Override
        public int compareTo(Entry other) {
            return order - other.order;
        }
    }


    private static boolean ENABLED = true;
    private static final double NANO2MILLI = 1.0 / 1000000.0;

    // Synchronize on ROOT
    private static final Entry ROOT = new Entry("root", 0);
    private static final ThreadLocal<Profiler> CURRENT = new ThreadLocal<>(); // initial value = null

    private long tStart;
    private final Entry entry;
    private final Profiler parent;


    private Profiler(String name, Profiler parent) {
        this.parent = parent;
        Entry entry;

        synchronized(ROOT) {
            Entry parentEntry = (parent == null) ? ROOT : parent.entry;
            entry = parentEntry.children.get(name);
            if(entry == null) {
                entry = new Entry(name, parentEntry.children.size());
                parentEntry.children.put(name, entry);
            }
        }
        
        this.entry = entry;
    }


    private void start() {
        tStart = System.nanoTime();
    }


    public static Profiler start(String name) {
        if(!ENABLED) {
            return null;
        }

        Profiler p = new Profiler(name, CURRENT.get());
        CURRENT.set(p);

        p.start();
        return p;
    }


    public static Profiler disabled(String name) {
        return null;
    }


    public static void setEnabled(boolean enabled) {
        ENABLED = enabled;
    }


    @Override
    public void close() {
        long duration = System.nanoTime() - tStart;
        synchronized(ROOT) {
            entry.time += duration;
            entry.runs++;

            if(duration < entry.min)
                entry.min = duration;
            if(duration > entry.max)
                entry.max = duration;

            CURRENT.set(parent);
        }
    }


    private static void printEntry(int level, Entry entry) {
        Entry[] entries = new Entry[entry.children.size()];
        entries = entry.children.values().toArray(entries);
        Arrays.sort(entries);

        for(Entry e : entries) {
            double timeMs  = NANO2MILLI * e.time;
            double avgMs   = NANO2MILLI * e.time / (double)e.runs;
            double minMs   = NANO2MILLI * e.min;
            double maxMs   = NANO2MILLI * e.max;
            double percent = 100.0 * e.time / (double)entry.time;

            print(level, e.name, timeMs, percent, avgMs, minMs, maxMs, e.runs);
            printEntry(level+1, e);
        }
    }


    // This formats very low values (x.yyyyyE-4 as 0.000xyyy..)
    private static String formatDouble(double val) {
        return String.format("%-14.14f", val);
    }

    private static String formatPercent(double val) {
        return Double.isInfinite(val) ? "" : formatDouble(val);
    }


    private static void print(int level, String name, double timeMs, double percent, double avgMs, double minMs, double maxMs, long runs) {
        for(int i=0; i<level; ++i) {
            name = "·   " + name;
        }

        System.out.printf("%-50.50s     %-12.12s  %-7.7s%%     %-10.10s  %-10.10s  %-10.10s  %-12.12s %n",
            name, formatDouble(timeMs), formatPercent(percent), formatDouble(avgMs), formatDouble(minMs), formatDouble(maxMs), runs);
    }


    static {
        if(ENABLED) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.printf("%-50.50s     %-12.12s  %s  %-10.10s  %-10.10s  %-10.10s  %-12.12s %n",
                        "===== Profiler =====", "Total [ms]", "% of Parent", "Avg [ms]", "Min [ms]", "Max [ms]", "Runs");
                
                synchronized(ROOT) {
                    printEntry(0, ROOT);
                }
            }));
        }
    }
}
