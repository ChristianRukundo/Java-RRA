package rca.ac.rw.template.commons.response;

import lombok.Getter;
import lombok.Setter;
import rca.ac.rw.template.users.User;

@Getter
@Setter
public class JwtAuthenticationResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private User user;

    public JwtAuthenticationResponse(String accessToken, User user) {
        this.accessToken = accessToken;
        this.user = user;
    }
}




