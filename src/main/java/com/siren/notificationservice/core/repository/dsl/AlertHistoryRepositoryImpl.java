package com.siren.notificationservice.core.repository.dsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.siren.notificationservice.core.dto.request.AlertHistorySearchCondition;
import com.siren.notificationservice.core.entity.table.AlertHistory;
import com.siren.notificationservice.core.entity.table.QAlertHistory;
import com.siren.notificationservice.core.repository.AlertHistoryRepositoryCustom;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.ZoneId;
import java.util.List;

public class AlertHistoryRepositoryImpl implements AlertHistoryRepositoryCustom {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final JPAQueryFactory jpaQueryFactory;

    public AlertHistoryRepositoryImpl(EntityManager entityManager) {
        this.jpaQueryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Page<AlertHistory> search(Long userId, AlertHistorySearchCondition filter, Pageable pageable) {
        QAlertHistory q = QAlertHistory.alertHistory;

        BooleanBuilder bb = new BooleanBuilder();
        bb.and(q.userId.eq(userId));
        if (filter.roomId() != null) bb.and(q.roomId.eq(filter.roomId()));
        if (filter.botType() != null) bb.and(q.botType.eq(filter.botType()));
        if (filter.alertType() != null) bb.and(q.alertType.eq(filter.alertType()));
        if (filter.from() != null) bb.and(q.sendAt.goe(filter.from().atStartOfDay(ZONE)));
        if (filter.to() != null) bb.and(q.sendAt.lt(filter.to().plusDays(1).atStartOfDay(ZONE)));

        List<AlertHistory> content = jpaQueryFactory
                .selectFrom(q)
                .where(bb)
                .orderBy(q.sendAt.desc(), q.alertHistoryId.desc()) // sendAt은 유니크 아님 -> PK로 tie-break해 페이지 경계 안정화
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(q.count())
                .from(q)
                .where(bb);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
