package com.siren.notificationservice.core.dto.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum Operator {
    GT(">", "초과"), // Greater Than
    GTE(">=", "이상"), // Greater Than Equal
    LT("<","미만"), // Less Than
    LTE("<=","이하"), // Less Than Equal
    EQ("=", "같음"), // Equal
    NEQ("!=","같지 않음"); // Not Equal

    private final String symbol;
    private final String label;

    Operator(String symbol,String label){
        this.symbol= symbol;
        this.label= label;
    }

    /**
     * RuleEngine이 심볼(예: "&lt;")로 보내는 값을 우리 enum으로 변환한다. Jackson이 역직렬화 시 자동 호출.
     * 모르는 심볼이면 예외 대신 null을 반환한다 — 메시지 역직렬화 단계에서 예외가 나면 리스너까지
     * 닿지도 못하고 무한 재큐잉으로 이어지기 때문.
     */
    @JsonCreator
    public static Operator fromSymbol(String symbol){
        for(Operator operator : Operator.values()){
            if(operator.getSymbol().equals(symbol)){
                return operator;
            }
        }
        return null;
    }

    /**
     * 메시지 조립 시 사용하는 null-safe 라벨 조회. operator가 없거나(nullable 필드) 모르는 값이면 "값"으로 대체한다.
     */
    public static String labelOf(Operator operator){
        return operator != null ? operator.getLabel() : "값";
    }
}
