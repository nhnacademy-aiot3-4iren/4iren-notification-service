package com.siren.notificationservice.core.dlq;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Document(indexName = "notification-dlq-failures")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DlqFailureDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String queue;

    @Field(type = FieldType.Keyword)
    private String reason;

    @Field(type = FieldType.Text)
    private String exceptionMessage;

    @Field(type = FieldType.Text)
    private String stackTrace;

    @Field(type = FieldType.Text)
    private String payload;

    @Field(type = FieldType.Date)
    private Instant occurredAt;
}
