package com.alfy.budget.controller;

import com.alfy.budget.model.Transaction;
import com.alfy.budget.service.TransactionsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;

@RestController
@RequestMapping(path = "/transactions/{transactionId}/notes")
public class TransactionsIdNotesController {

    private final TransactionsService transactionsService;

    @Autowired
    public TransactionsIdNotesController(TransactionsService transactionsService) {
        this.transactionsService = transactionsService;
    }

    @GetMapping
    public void showSelection(
            @PathVariable(name = "transactionId") int transactionId,
            HttpServletResponse response
    ) throws IOException {
        final String postUrl = "/transactions/" + transactionId + "/notes";

        response.setContentType("text/html");
        try (PrintWriter printWriter = response.getWriter()) {
            printWriter.print("<html lang=\"en-US\">");
            printWriter.print("<head>");
            printWriter.print("  <title>Add Note</title>");
            printWriter.print("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>");
            printWriter.print("</head>");
            printWriter.print("<body>");

            Transaction transaction = transactionsService.getTransaction(transactionId);
            String notes = transaction.notes;
            if (notes == null) {
                notes = "";
            }

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

    @PostMapping
    public void update(
            @PathVariable("transactionId") int transactionId,
            @RequestParam(name = "note", required = false) String note,
            HttpServletResponse response
    ) throws IOException {
        if (note == null || note.isEmpty()) {
            note = null;
        }

        transactionsService.updateNotes(transactionId, note);
        response.sendRedirect("/transactions/" + transactionId);
    }

}
