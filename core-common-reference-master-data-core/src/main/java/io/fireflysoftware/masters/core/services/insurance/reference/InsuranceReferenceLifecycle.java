/* Copyright 2026 Firefly Software Foundation. Licensed under Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.insurance.reference;

import static io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.*;
import java.time.Instant;

/** Expected CAS data only. Stored approvals/current authority and actual persistence are separate obligations. */
public final class InsuranceReferenceLifecycle {
    private InsuranceReferenceLifecycle(){}
    public static Control publish(Control previous,Revision prior,Revision next,long expectedGeneration,Instant recordedAt){
        try{
            require(next!=null&&expectedGeneration>=0);requireTimestamp(recordedAt);
            require(!recordedAt.isBefore(next.effectiveFrom())&&(next.effectiveUntil()==null||recordedAt.isBefore(next.effectiveUntil())));
            if(previous==null){require(prior==null&&expectedGeneration==0&&next.revision()==1);return new Control(next.recordId(),next.kind(),next.publicationId(),1,0,State.ACTIVE);}
            require(prior!=null&&previous.generation()==expectedGeneration&&same(previous,prior));
            require(previous.recordId().equals(next.recordId())&&previous.kind()==next.kind()&&prior.code().equals(next.code())
                    &&next.revision()==Math.addExact(previous.revision(),1)&&!previous.publicationId().equals(next.publicationId()));
            return new Control(next.recordId(),next.kind(),next.publicationId(),next.revision(),Math.addExact(previous.generation(),1),State.ACTIVE);
        }catch(RuntimeException error){throw invalid();}
    }
    public static Control withdraw(Control previous,long expectedGeneration){
        try{require(previous!=null&&expectedGeneration>=0&&previous.generation()==expectedGeneration&&previous.state()==State.ACTIVE);
            return new Control(previous.recordId(),previous.kind(),previous.publicationId(),previous.revision(),Math.addExact(previous.generation(),1),State.WITHDRAWN);
        }catch(RuntimeException error){throw invalid();}
    }
    private static boolean same(Control control,Revision value){return control.recordId().equals(value.recordId())&&control.kind()==value.kind()&&control.publicationId().equals(value.publicationId())&&control.revision()==value.revision();}
    private static void require(boolean ok){if(!ok)throw invalid();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("INSURANCE_REFERENCE_LIFECYCLE");}
}
