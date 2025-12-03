package com.flowbrain.viewx.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserActionMessage {
    private Long userId;
    private Long videoId;
    private String actionType; // view / like / comment / share
    private Long timestamp;
}
