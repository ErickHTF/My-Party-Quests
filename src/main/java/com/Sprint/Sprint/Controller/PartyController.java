package com.Sprint.Sprint.Controller;

import com.Sprint.Sprint.DTO.Request.CreatePartyDTO;
import com.Sprint.Sprint.DTO.Response.PartyResponseDTO;
import com.Sprint.Sprint.Entity.Party;
import com.Sprint.Sprint.Entity.User;
import com.Sprint.Sprint.Service.PartyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parties")
public class PartyController {

    @Autowired
    private PartyService partyService;

    @PostMapping
    public ResponseEntity<PartyResponseDTO> create(@RequestBody @Valid CreatePartyDTO data,
                                        @AuthenticationPrincipal User user) {

        var newParty = partyService.createParty(data, user);

        return ResponseEntity.ok(new PartyResponseDTO(newParty));
    }

}
