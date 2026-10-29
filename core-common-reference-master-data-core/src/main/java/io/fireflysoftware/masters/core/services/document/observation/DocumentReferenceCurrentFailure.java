/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation;

public final class DocumentReferenceCurrentFailure extends RuntimeException {
    public enum Kind {FORBIDDEN,NOT_CURRENT,UNAVAILABLE}
    private final Kind kind;
    public DocumentReferenceCurrentFailure(Kind kind){super("DOCUMENT_REFERENCE_CURRENT_"+java.util.Objects.requireNonNull(kind));this.kind=kind;}
    public Kind kind(){return kind;}
}
