package com.reopentracks.data;

public record SelectionData(
        String selection,
        String[] selectionArgs
) {

    public SelectionData() {
        this(null, null);
    }
}
