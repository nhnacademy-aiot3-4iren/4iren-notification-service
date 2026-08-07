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
