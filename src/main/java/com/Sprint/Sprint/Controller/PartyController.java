package com.Sprint.Sprint.Controller;

import com.Sprint.Sprint.DTO.Request.CreatePartyDTO;
import com.Sprint.Sprint.DTO.Response.PartyResponseDTO;
import com.Sprint.Sprint.Entity.Party;
import com.Sprint.Sprint.Entity.User;
import com.Sprint.Sprint.Service.PartyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/parties")
public class PartyController {

    @Autowired
    private PartyService partyService;

    @Operation(
            summary = "Create a new Party",
            description = "Creates a new party with the logged user as the owner."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Party created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PartyResponseDTO.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping
    public ResponseEntity<PartyResponseDTO> create(@RequestBody @Valid CreatePartyDTO data,
                                        @AuthenticationPrincipal User user) {

        var newParty = partyService.createParty(data, user);

        return ResponseEntity.ok(new PartyResponseDTO(newParty));
    }

    @Operation(
            summary = "Join a Party",
            description = "Adds the logged user to the specified party by ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully joined the party",
                    content = @Content(schema = @Schema(implementation = PartyResponseDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Party not found"),
            @ApiResponse(responseCode = "400", description = "User already belongs to this party")
    })
    @PostMapping("/join/{id}")
    public ResponseEntity<PartyResponseDTO> joinParty(@PathVariable Long id,
                                        @AuthenticationPrincipal User user){

        Party partyJoined = partyService.joinParty(id, user);

        return ResponseEntity.ok(new PartyResponseDTO(partyJoined));
    }

    @Operation(
            summary = "Leave current Party",
            description = "Removes the logged user from their current Party."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully left the party"
            ),
            @ApiResponse(responseCode = "400", description = "User is not part of any party")
    })
    @PostMapping("/leave")
    public ResponseEntity<String> leaveParty(@AuthenticationPrincipal User loggedUser) {
        partyService.leaveParty(loggedUser);
        return ResponseEntity.ok("You left the party.");
    }

    @Operation(
            summary = "Delete a Party",
            description = "Deletes the specified party, but only the owner can perform this action."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Party deleted successfully"
            ),
            @ApiResponse(responseCode = "403", description = "Only the owner can delete this party"),
            @ApiResponse(responseCode = "404", description = "Party not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteParty(
            @PathVariable Long id,
            @AuthenticationPrincipal User loggedUser) {

        partyService.deleteParty(id, loggedUser);
        return ResponseEntity.ok("Party successfully deleted.");
    }
}
