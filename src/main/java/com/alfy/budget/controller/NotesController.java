package com.alfy.budget.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.*;

@RestController
@RequestMapping(path = "/notes")
public class NotesController {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public NotesController(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @GetMapping(path = "/enter")
    public void selectCategory(
            @RequestParam(name = "transactionId") int transactionId,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("text/html");
        try (PrintWriter printWriter = response.getWriter()) {
            printWriter.print("<html lang=\"en-US\">");
            printWriter.print("<head>");
            printWriter.print("  <title>Add Note</title>");
            printWriter.print("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>");
            printWriter.print("  <link rel=\"stylesheet\" href=\"/transactions.css?cache-buster=" + new Random().nextInt() + "\">");
            printWriter.print("</head>");
            printWriter.print("<body>");

            String query = "SELECT notes FROM transactions" +
                    " WHERE id=:id";
            HashMap<String, Object> paramMap = new HashMap<>();
            paramMap.put("id", transactionId);
            String notes = namedParameterJdbcTemplate.queryForObject(query, paramMap, String.class);
            if (notes == null) {
                notes = "";
            }

            String postUrl = "/notes/update";
            printWriter.print("<form method=\"POST\" action=\"" + postUrl + "\" enctype=\"application/x-www-form-urlencoded\">");
            printWriter.print("<input type=\"hidden\" name=\"transactionId\" value=\"" + transactionId + "\"/>");
            printWriter.print("<textarea name=\"note\" style=\"width: 100%; height: 200px;\">" + notes + "</textarea>");
            printWriter.print("<br>");
            printWriter.print("<br>");
            printWriter.print("<input type=\"submit\">");
            printWriter.print("</form>");

            printWriter.print("</body>");
            printWriter.print("</html>");
        }
    }

    @PostMapping(path = "/update")
    public void updateNote(
            @RequestParam("transactionId") int transactionId,
            @RequestParam(name = "note", required = false) String note,
            HttpServletResponse response
    ) throws IOException {
        if (note == null || note.isEmpty()) {
            note = null;
        }

        String s = "UPDATE transactions"
                + " SET notes = :notes"
                + " WHERE id = :id";

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("id", transactionId);
        paramMap.put("notes", note);
        namedParameterJdbcTemplate.update(s, paramMap);

        response.sendRedirect("/transactions/" + transactionId);
    }

}
