package org.group1.coffeeshopapi.barista.dto.request;

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
public class BaristaRequest {
    private String name;
    private String username;
    private String email;
    private String phone;
    private String password;
    private Boolean enabled;
}
