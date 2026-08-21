package io.reqover.cli;

/** A problem the user can fix by changing the command line or the input files. */
final class CliException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    CliException(String message) {
        super(message);
    }

    CliException(String message, Throwable cause) {
        super(message, cause);
    }
}
