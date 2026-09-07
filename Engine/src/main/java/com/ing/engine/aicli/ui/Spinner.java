package com.ing.engine.aicli.ui;

/**
 * Lightweight animated spinner for streaming progress
 * ({@code Thinking... → ✓ done} lines). Falls back to plain prints when ANSI
 * is unavailable.
 */
public final class Spinner {
    private static final String[] FRAMES = {
        "\u280b",
        "\u2819",
        "\u2839",
        "\u2838",
        "\u283c",
        "\u2834",
        "\u2826",
        "\u2827",
        "\u2807",
        "\u280f"
    };

    private final Theme t;
    private volatile String message = "";
    private volatile boolean running;
    private Thread thread;

    public Spinner(Theme theme) {
        this.t = theme;
    }

    public synchronized void start(String msg) {
        this.message = msg;
        if (!t.ansiEnabled()) {
            System.out.println("· " + msg);
            return;
        }
        if (running) {
            return;
        }
        running = true;
        thread =
            new Thread(
                () -> {
                    int i = 0;
                    while (running) {
                        render(FRAMES[i++ % FRAMES.length]);
                        try {
                            Thread.sleep(80);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                },
                "aicli-spinner"
            );
        thread.setDaemon(true);
        thread.start();
    }

    public void update(String msg) {
        this.message = msg;
        if (!t.ansiEnabled()) {
            System.out.println("· " + msg);
        }
    }

    public synchronized void succeed(String msg) {
        stop();
        System.out.println(t.ok(msg));
    }

    public synchronized void fail(String msg) {
        stop();
        System.out.println(t.fail(msg));
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        if (thread != null) {
            try {
                thread.join(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            thread = null;
        }
        clearLine();
    }

    private void render(String frame) {
        String line = t.purple(frame) + " " + message;
        System.out.print("\r\u001b[2K" + line);
        System.out.flush();
    }

    private void clearLine() {
        if (t.ansiEnabled()) {
            System.out.print("\r\u001b[2K");
            System.out.flush();
        }
    }
}
