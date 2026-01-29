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

import java.util.List;

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


    @Operation(summary = "Get All Parties",
            description = "Returns all Parties.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = PartyController.class))}),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(value = "/all")
    public List<Party> findAllParties() {

        return partyService.findAllParties();
    }

    @Operation(summary = "Approve a pending member request")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Member approved successfully",
                    content = @Content
            ),
            @ApiResponse(responseCode = "404", description = "Party or User not found", content = @Content),
            @ApiResponse(responseCode = "400", description = "Party is full or User is not in pending list", content = @Content)
    })
    @PostMapping("/{partyId}/approve/{userId}")
    public ResponseEntity<Void> approveRequest(@PathVariable Long partyId, @PathVariable Long userId) {
        partyService.approveRequest(partyId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reject a pending member request")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Member request rejected successfully",
                    content = @Content
            ),
            @ApiResponse(responseCode = "404", description = "Party or User not found", content = @Content)
    })
    @PostMapping("/{partyId}/reject/{userId}")
    public ResponseEntity<Void> rejectRequest(@PathVariable Long partyId, @PathVariable Long userId) {
        partyService.rejectRequest(partyId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Start Planning Phase", description = "Moves the party from LOBBY to PLANNING. Only the owner can do this.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Phase changed to PLANNING"),
            @ApiResponse(responseCode = "403", description = "User is not the owner"),
            @ApiResponse(responseCode = "400", description = "Invalid previous status (Must be LOBBY or REVIEW)")
    })
    @PostMapping("/{id}/start-planning")
    public ResponseEntity<PartyResponseDTO> startPlanning(@PathVariable Long id,
                                                          @AuthenticationPrincipal User user) {

        Party party = partyService.startPlanningPhase(id, user);
        return ResponseEntity.ok(new PartyResponseDTO(party));
    }

    @Operation(summary = "Start Execution Phase", description = "Moves the party from PLANNING to EXECUTION and assigns reviewers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Phase changed to EXECUTION, reviewers assigned"),
            @ApiResponse(responseCode = "403", description = "User is not the owner"),
            @ApiResponse(responseCode = "400", description = "Invalid previous status (Must be PLANNING)")
    })
    @PostMapping("/{id}/start-execution")
    public ResponseEntity<PartyResponseDTO> startExecution(@PathVariable Long id,
                                                           @AuthenticationPrincipal User user) {

        Party party = partyService.startExecutionPhase(id, user);
        return ResponseEntity.ok(new PartyResponseDTO(party));
    }

    @Operation(summary = "Start Review Phase",
            description = "Ends the execution and moves the party to Review status to see results.")
    @PostMapping("/{id}/start-review")
    public ResponseEntity<PartyResponseDTO> startReview(@PathVariable Long id,
                                                        @AuthenticationPrincipal User user) {


        return ResponseEntity.ok(new PartyResponseDTO(partyService.startReviewPhase(id, user)));
    }

    @Operation(summary = "Reset to Lobby",
            description = "Clears all quests and returns the party to the Lobby for a new Sprint.")
    @PostMapping("/{id}/reset-lobby")
    public ResponseEntity<PartyResponseDTO> resetLobby(@PathVariable Long id,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new PartyResponseDTO(partyService.resetToLobby(id, user)));
    }

    @Operation(
            summary = "Kick a member from the Party",
            description = "Removes a specific member from the party. Only the owner can perform this action."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Member kicked successfully",
                    content = @Content(schema = @Schema(implementation = PartyResponseDTO.class))
            ),
            @ApiResponse(responseCode = "403", description = "Only the owner can kick members"),
            @ApiResponse(responseCode = "404", description = "Party or User not found")
    })
    @PostMapping("/{partyId}/kick/{userId}")
    public ResponseEntity<PartyResponseDTO> kickMember(@PathVariable Long partyId,
                                                       @PathVariable Long userId,
                                                       @AuthenticationPrincipal User loggedUser) {

        Party updatedParty = partyService.kickMember(partyId, userId, loggedUser);
        return ResponseEntity.ok(new PartyResponseDTO(updatedParty));
    }
}
