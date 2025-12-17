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

import javax.swing.text.html.parser.Entity;

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

}
