package com.siren.notificationservice.core.repository;

import java.util.List;

public interface RoomSubscriptionRepositoryCustom {

    /**
     * 특정 강의실에 대해 구독이 승인된 유저 id 목록을 조회한다.
     * 관리자에게 자동부여된 구독도 포함된다 (일반유저/관리자 구분 없음).
     *
     * @param roomId 대상 강의실 id
     * @return 승인 상태(APPROVED)인 구독의 유저 id 목록
     */
    List<Long> findApprovedUserIdsByRoomId(Long roomId);

    /**
     * 특정 유저가 구독 승인된 강의실 id 목록을 조회한다 (조회 범위 판단용).
     *
     * @param userId 대상 유저 id
     * @return 승인 상태(APPROVED)인 구독의 강의실 id 목록
     */
    List<Long> findApprovedRoomIdsByUserId(Long userId);
}
