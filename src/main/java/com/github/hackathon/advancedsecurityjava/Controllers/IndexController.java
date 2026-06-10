package com.github.hackathon.advancedsecurityjava.Controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.github.hackathon.advancedsecurityjava.Application;
import com.github.hackathon.advancedsecurityjava.Models.Book;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class IndexController {

  @GetMapping("/")
  @ResponseBody
  public List<Book> getBooks(@RequestParam(name = "name", required = false) String bookname,
      @RequestParam(name = "author", required = false) String bookauthor,
      @RequestParam(name = "read", required = false) Boolean bookread) {
    List<Book> books = new ArrayList<>();

    try (Connection connection = DriverManager.getConnection(Application.connectionString)) {

      String query;
      if (bookname != null) {
        // Filter by book name
        query = "SELECT * FROM Books WHERE name LIKE ? ESCAPE '\\'";
      } else if (bookauthor != null) {
        // Filter by book author
        query = "SELECT * FROM Books WHERE author LIKE ? ESCAPE '\\'";
      } else if (bookread != null) {
        // Filter by if the book has been read or not
        query = "SELECT * FROM Books WHERE read = ?";
      } else {
        // All books
        query = "SELECT * FROM Books";
      }

      try (PreparedStatement statement = connection.prepareStatement(query)) {
        if (bookname != null) {
          statement.setString(1, "%" + escapeLikeWildcards(bookname) + "%");
        } else if (bookauthor != null) {
          statement.setString(1, "%" + escapeLikeWildcards(bookauthor) + "%");
        } else if (bookread != null) {
          statement.setInt(1, bookread ? 1 : 0);
        }

        try (ResultSet results = statement.executeQuery()) {
          while (results.next()) {
            Book book = new Book(results.getString("name"), results.getString("author"), (results.getInt("read") == 1));
            books.add(book);
          }
        }
      }

    } catch (SQLException error) {
      Application.logger.error("Database error in getBooks", error);
    }
    return books;
  }

  private String escapeLikeWildcards(String input) {
    return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}