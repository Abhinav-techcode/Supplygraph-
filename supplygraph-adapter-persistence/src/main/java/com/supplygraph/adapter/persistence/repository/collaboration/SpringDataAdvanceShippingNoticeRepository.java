package com.supplygraph.adapter.persistence.repository.collaboration;

import com.supplygraph.adapter.persistence.entity.collaboration.AdvanceShippingNoticeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataAdvanceShippingNoticeRepository extends JpaRepository<AdvanceShippingNoticeJpaEntity, String> {
    List<AdvanceShippingNoticeJpaEntity> findByWorkspaceId(String workspaceId);
    List<AdvanceShippingNoticeJpaEntity> findByPoId(String poId);
}

