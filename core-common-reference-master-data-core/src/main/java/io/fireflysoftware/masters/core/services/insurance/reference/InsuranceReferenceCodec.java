/* Copyright 2026 Firefly Software Foundation. Licensed under Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.insurance.reference;

import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Revision;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Wording;

/** Compatibility entry point; canonical data does not prove current evidence or permission. */
public final class InsuranceReferenceCodec {
    private InsuranceReferenceCodec() {}

    public static Revision decode(String raw) {
        return io.fireflysoftware.masters.interfaces.protocol.insurance.reference.InsuranceReferenceCodec.decode(raw);
    }

    public static String encode(Revision value) {
        return io.fireflysoftware.masters.interfaces.protocol.insurance.reference.InsuranceReferenceCodec.encode(value);
    }

    public static byte[] bytes(Revision value) {
        return io.fireflysoftware.masters.interfaces.protocol.insurance.reference.InsuranceReferenceCodec.bytes(value);
    }

    public static String digest(Revision value) {
        return io.fireflysoftware.masters.interfaces.protocol.insurance.reference.InsuranceReferenceCodec.digest(value);
    }

    public static String contentDigest(Wording value) {
        return io.fireflysoftware.masters.interfaces.protocol.insurance.reference.InsuranceReferenceCodec.contentDigest(value);
    }

    public static Revision decodeBytes(byte[] value) {
        return io.fireflysoftware.masters.interfaces.protocol.insurance.reference.InsuranceReferenceCodec.decodeBytes(value);
    }
}
