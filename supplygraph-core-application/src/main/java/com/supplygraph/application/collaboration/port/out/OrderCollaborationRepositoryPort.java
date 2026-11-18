package com.supplygraph.application.collaboration.port.out;

import com.supplygraph.domain.model.WorkspaceId;
import com.supplygraph.domain.model.collaboration.AdvanceShippingNotice;
import com.supplygraph.domain.model.collaboration.AdvanceShippingNoticeId;
import com.supplygraph.domain.model.collaboration.PoCollaborationId;
import com.supplygraph.domain.model.collaboration.PoCollaborationRecord;

import java.util.List;
import java.util.Optional;

public interface OrderCollaborationRepositoryPort {
    void savePo(PoCollaborationRecord po);
    Optional<PoCollaborationRecord> findPoById(PoCollaborationId id);
    List<PoCollaborationRecord> findPosByWorkspaceId(WorkspaceId workspaceId);

    void saveAsn(AdvanceShippingNotice asn);
    Optional<AdvanceShippingNotice> findAsnById(AdvanceShippingNoticeId id);
    List<AdvanceShippingNotice> findAsnsByWorkspaceId(WorkspaceId workspaceId);
}

