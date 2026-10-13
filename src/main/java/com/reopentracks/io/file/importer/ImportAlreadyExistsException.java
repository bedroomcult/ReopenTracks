package com.reopentracks.io.file.importer;

class ImportAlreadyExistsException extends RuntimeException {
    public ImportAlreadyExistsException(Exception e) {
        super(e);
    }

    public ImportAlreadyExistsException(String msg) {
        super(msg);
    }
}
