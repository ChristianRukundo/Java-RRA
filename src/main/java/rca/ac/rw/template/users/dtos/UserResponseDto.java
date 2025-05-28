package rca.ac.rw.template.users.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto{
       private UUID id;
       private String firstName;
       private String lastName;
       private String email;
//       private LocalDateTime updatedAt;
//       private LocalDateTime createdAt;


}
