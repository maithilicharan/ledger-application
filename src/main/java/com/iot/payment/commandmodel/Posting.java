package com.iot.payment.commandmodel;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class Posting {
    private String id;
    private BigDecimal amount;
    private PostingState state;
    private LocalDateTime dateTime;
}
