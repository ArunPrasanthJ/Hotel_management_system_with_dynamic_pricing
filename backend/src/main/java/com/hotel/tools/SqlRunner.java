package com.hotel.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class SqlRunner {
    public static void main(String[] args) throws Exception {
        String file = args.length > 0 ? args[0] : "src/main/resources/update_prices_fix.sql";
        String url = "jdbc:mysql://localhost:3306/hotel_management";
        String user = "root";
        String pass = "masterarun1";

        String sql = Files.readString(Path.of(file));

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {

            System.out.println("Connected to DB, executing script: " + file);

            // naive split on semicolon; ok for simple statements
            String[] parts = sql.split(";");
            for (String part : parts) {
                String s = part.trim();
                if (s.isEmpty()) continue;
                System.out.println("Executing statement: " + (s.length() > 120 ? s.substring(0, 120) + "..." : s));
                stmt.execute(s);
            }

            System.out.println("Script executed. Previewing rows:");
            ResultSet rs = stmt.executeQuery("SELECT id, room_number, type, base_price, current_price, price FROM rooms ORDER BY id LIMIT 50");
            while (rs.next()) {
                System.out.printf("%d %s %s base=%s current=%s price=%s%n",
                        rs.getLong("id"), rs.getString("room_number"), rs.getString("type"), rs.getObject("base_price"), rs.getObject("current_price"), rs.getObject("price"));
            }
        }
    }
}
