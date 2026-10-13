# Architecture and game flow

[← Back to README](../README.md)

## Game Concepts

- **User (Adventurer)**: A registered player with level, XP, and gold.
- **Party**: A team led by an owner (Guild Master).
- **Quest**: A task created by a party member and reviewed by another member.
- **Phases**:

  - **LOBBY** – Party setup and member management
  - **PLANNING** – Quest creation, review, approval, and refinement
  - **EXECUTION** – Quest execution
  - **REVIEW** – Results overview and wrap-up

## Party (Sprint) Flow

1. Create a **Party** (owner becomes Guild Master)
2. Members request to join and get approved
3. Move from **LOBBY → PLANNING**
4. Create **Quests**
5. Move to **EXECUTION** (reviewers assigned)
6. Review, approve, and complete quests
7. Move to **REVIEW** and finalize the sprint
8. Reset to **LOBBY** for a new cycle

## Project Structure

```
├── controller   # REST & MVC controllers
├── service      # Business logic
├── repository   # JPA repositories
├── model        # Entities
├── dto          # Data Transfer Objects
├── security     # JWT & Spring Security config
├── exception    # Global exception handling
└── resources
    └── static    # HTML, CSS, JS, images, assets
```

## Security

- JWT-based authentication
- Role-based access control
- Protected endpoints for party ownership and quest review
