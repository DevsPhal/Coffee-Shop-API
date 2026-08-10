package org.group1.coffeeshopapi.barista.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaristaResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String status;
    private String joinDate;
}
