package com.Sprint.Sprint.Service;

import org.springframework.stereotype.Service;

@Service
public class LevelProgressionService {
    private static final double DIFFICULTY_FACTOR = 100.0;

    public int calculateLevel(Integer totalXp) {
        if (totalXp == null || totalXp < 0) return 1;
        return (int) Math.floor(Math.sqrt(totalXp / DIFFICULTY_FACTOR)) + 1;
    }

    public int calculateNextLevelThreshold(int currentLevel) {
        int targetLevel = currentLevel + 1;
        return (int) (Math.pow(targetLevel, 2) * DIFFICULTY_FACTOR);
    }

    public int calculateProgressPercentage(Integer totalXp) {
        if (totalXp == null || totalXp < 0) return 0;

        int currentLevel = calculateLevel(totalXp);
        int nextLevel = currentLevel + 1;
        int xpFloor = (int) (Math.pow(currentLevel - 1, 2) * DIFFICULTY_FACTOR);
        int xpCeiling = (int) (Math.pow(currentLevel, 2) * DIFFICULTY_FACTOR);

        int range = xpCeiling - xpFloor;
        int progress = totalXp - xpFloor;

        if (range == 0) return 100;

        int percentage = (progress * 100) / range;
        return Math.min(100, Math.max(0, percentage));
    }
}
