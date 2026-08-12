package com.siren.notificationservice.core.dto.response;

import java.util.List;

public record AlertHistoryFilterOptionsResponse(
        List<String> botTypeList,
        List<String> alertTypeList
) {
}
