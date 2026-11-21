package com.bookstore.bookstore.dto.response;

public class OrderAdjustment {

    private String bookTitle;
    private int requested;
    private int fulfilled;
    private String reason;

    public OrderAdjustment(String bookTitle, int requested, int fulfilled, String reason) {
        this.bookTitle = bookTitle;
        this.requested = requested;
        this.fulfilled = fulfilled;
        this.reason = reason;
    }

    public String getBookTitle() { return bookTitle; }
    public int getRequested() { return requested; }
    public int getFulfilled() { return fulfilled; }
    public String getReason() { return reason; }
}
