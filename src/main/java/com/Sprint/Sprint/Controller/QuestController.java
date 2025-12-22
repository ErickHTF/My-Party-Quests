package com.Sprint.Sprint.Controller;

import com.Sprint.Sprint.DTO.Request.CreateQuestDTO;
import com.Sprint.Sprint.DTO.Request.ReviewQuestDTO;
import com.Sprint.Sprint.DTO.Response.QuestResponseDTO;
import com.Sprint.Sprint.Entity.Quest;
import com.Sprint.Sprint.Entity.User;
import com.Sprint.Sprint.Service.QuestService;
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
@RequestMapping("/quests")
public class QuestController {

        @Autowired
        private QuestService questService;

    @Operation(
            summary = "Create a new Quest",
            description = "Creates a new quest/activity for the logged user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Quest created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Quest.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
        @PostMapping
        public ResponseEntity<QuestResponseDTO> createQuest(@RequestBody CreateQuestDTO data, @AuthenticationPrincipal User user) {
            Quest newQuest = questService.createQuest(data, user);

            return ResponseEntity.ok(new QuestResponseDTO(newQuest));
        }



    @Operation(
            summary = "Review a Quest",
            description = "Approve or Reject a quest. Only the assigned reviewer can do this."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Quest reviewed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QuestResponseDTO.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request or illegal state"),
            @ApiResponse(responseCode = "403", description = "User is not the assigned reviewer"),
            @ApiResponse(responseCode = "404", description = "Quest not found")
    })
    @PostMapping("/{id}/review")
    public ResponseEntity<QuestResponseDTO> reviewQuest(@PathVariable Long id,
                                                        @RequestBody @Valid ReviewQuestDTO data,
                                                        @AuthenticationPrincipal User user) {

        Quest reviewedQuest = questService.reviewQuest(id, data, user);

        return ResponseEntity.ok(new QuestResponseDTO(reviewedQuest));
    }



    @Operation(
            summary = "Complete a Quest",
            description = "Mark an approved quest as completed. Only the quest owner can do this."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Quest completed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QuestResponseDTO.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Quest is not in APPROVED state"),
            @ApiResponse(responseCode = "403", description = "User is not the quest owner"),
            @ApiResponse(responseCode = "404", description = "Quest not found")
    })
    @PostMapping("/{id}/complete")
    public ResponseEntity<QuestResponseDTO> completeQuest(@PathVariable Long id,
                                                          @AuthenticationPrincipal User user) {

        Quest completedQuest = questService.completeQuest(id, user);

        return ResponseEntity.ok(new QuestResponseDTO(completedQuest));
    }


    @Operation(summary = "Get All Quests",
            description = "Returns all Quests.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = PartyController.class))}),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(value = "/all")
    public List<Quest> findAllQuests() {

        return questService.findAllQuests();
    }

    @Operation(summary = "Get My Quests",
            description = "Returns all quests created by the authenticated user in their current party.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of user's quests returned successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = QuestResponseDTO.class))}),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/my-quests")
    public ResponseEntity<List<QuestResponseDTO>> getMyQuests(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(questService.getMyQuests(user));
    }


    @Operation(summary = "Get Quests to Review",
            description = "Returns all quests where the authenticated user is the assigned reviewer and the status is PENDING_APPROVAL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of quests to review returned successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = QuestResponseDTO.class))}),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/to-review")
    public ResponseEntity<List<QuestResponseDTO>> getQuestsToReview(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(questService.getQuestsToReview(user));
    }

    @Operation(summary = "Update Quest", description = "Allows the user to fix a quest after a rejection.")
    @PutMapping("/{id}")
    public ResponseEntity<QuestResponseDTO> updateQuest(
            @PathVariable Long id,
            @RequestBody @Valid CreateQuestDTO data,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(new QuestResponseDTO(questService.updateQuest(id, data, user)));
    }

    @Operation(summary = "Get Quest Details", description = "Get details of a specific quest for editing.")
    @GetMapping("/{id}")
    public ResponseEntity<QuestResponseDTO> getQuestById(@PathVariable Long id) {
        Quest quest = questService.findById(id);
        return ResponseEntity.ok(new QuestResponseDTO(quest));
    }

    @Operation(
            summary = "Get Party Pending Quests (Owner Only)",
            description = "Returns all quests with status PENDING_APPROVAL for the party owned by the authenticated user. Used for the Guild Master Log."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of pending quests returned successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = QuestResponseDTO.class))
            ),
            @ApiResponse(responseCode = "403", description = "User is not a party owner"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/party-pending")
    public ResponseEntity<List<QuestResponseDTO>> getPartyPendingQuests(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(questService.getPartyPendingQuests(user));
    }
}
