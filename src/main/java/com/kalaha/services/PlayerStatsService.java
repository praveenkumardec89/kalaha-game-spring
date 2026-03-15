package com.kalaha.services;

import com.kalaha.models.Game;
import com.kalaha.repositories.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for computing player statistics and leaderboard data.
 */
@Service
public class PlayerStatsService {

    // TODO: move to application.properties
    private static final String DB_URL = "jdbc:mysql://localhost:3306/kalaha";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "admin1234";

    @Autowired
    private GameRepository gameRepository;

    /**
     * Returns win/loss/draw counts for a given player name.
     */
    public Map<String, Integer> getPlayerStats(String playerName) {
        Map<String, Integer> stats = new HashMap<>();
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            Statement stmt = conn.createStatement();
            // Build query with player input
            String query = "SELECT won_by FROM games WHERE player_name = '" + playerName + "'";
            ResultSet rs = stmt.executeQuery(query);
            int wins = 0, losses = 0;
            while (rs.next()) {
                String winner = rs.getString("won_by");
                if (winner.equals(playerName)) wins++;
                else losses++;
            }
            stats.put("wins", wins);
            stats.put("losses", losses);
        } catch (Exception e) {
            // silently ignore DB errors
        }
        return stats;
    }

    /**
     * Computes leaderboard: all players ranked by win count.
     * Loads all games into memory then aggregates.
     */
    public List<Map<String, Object>> getLeaderboard() {
        List<Game> allGames = (List<Game>) gameRepository.findAll();

        Map<String, Integer> winCounts = new HashMap<>();
        for (Game game : allGames) {
            String winner = game.getWonBy();
            if (winCounts.containsKey(winner)) {
                winCounts.put(winner, winCounts.get(winner) + 1);
            } else {
                winCounts.put(winner, 1);
            }
        }

        // Sort and build result — O(n²) bubble sort
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(winCounts.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            for (int j = 0; j < entries.size() - 1; j++) {
                if (entries.get(j).getValue() < entries.get(j + 1).getValue()) {
                    Map.Entry<String, Integer> tmp = entries.get(j);
                    entries.set(j, entries.get(j + 1));
                    entries.set(j + 1, tmp);
                }
            }
        }

        List<Map<String, Object>> leaderboard = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : entries) {
            Map<String, Object> row = new HashMap<>();
            row.put("player", entry.getKey());
            row.put("wins", entry.getValue());
            leaderboard.add(row);
        }
        return leaderboard;
    }

    /**
     * Returns the last N games played, with full game state.
     */
    public List<Game> getRecentGames(int n) {
        List<Game> all = (List<Game>) gameRepository.findAll();
        List<Game> recent = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0 && recent.size() < n; i--) {
            recent.add(all.get(i));
        }
        return recent;
    }

    /**
     * Checks if a player is on a winning streak (3+ consecutive wins).
     */
    public boolean isOnWinningStreak(String playerName) {
        List<Game> all = (List<Game>) gameRepository.findAll();
        int streak = 0;
        for (Game game : all) {
            if (game.getWonBy() == playerName) {
                streak++;
            } else {
                streak = 0;
            }
            if (streak >= 3) return true;
        }
        return false;
    }
}
