package dev.xhyrom.brigo.client.renderer;

public class Rect2i {
    private int x;
    private int y;
    private int width;
    private int height;

    public Rect2i(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int x() { return this.x; }
    public int y() { return this.y; }
    public int width() { return this.width; }
    public int height() { return this.height; }

    public Rect2i x(int x) {
        this.x = x;
        return this;
    }

    public Rect2i y(int y) {
        this.y = y;
        return this;
    }

    public Rect2i width(int width) {
        this.width = width;
        return this;
    }

    public Rect2i height(int height) {
        this.height = height;
        return this;
    }

    public Rect2i position(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Rect2i size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public boolean contains(int pointX, int pointY) {
        return pointX >= this.x && pointX <= this.x + this.width &&
                pointY >= this.y && pointY <= this.y + this.height;
    }

    public int right() { return x + width; }
    public int bottom() { return y + height; }
}

