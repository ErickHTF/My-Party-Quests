package com.Sprint.Sprint.Controller;

import com.Sprint.Sprint.DTO.Request.CreateQuestDTO;
import com.Sprint.Sprint.Entity.Quest;
import com.Sprint.Sprint.Entity.User;
import com.Sprint.Sprint.Service.QuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        public ResponseEntity<Quest> createQuest(@RequestBody CreateQuestDTO data, @AuthenticationPrincipal User user) {
            Quest newQuest = questService.createQuest(data, user);

            return ResponseEntity.ok(newQuest);
        }

}
