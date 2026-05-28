package com.skyfl.pfm.report.service;

import com.skyfl.pfm.transaction.entity.Transaction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CsvExportService {

    public byte[] export(List<Transaction> transactions, LocalDate start, LocalDate end) {
        StringBuilder builder = new StringBuilder();
        builder.append("type,account,category,amount,currency,transactionDate,note\n");
        for (Transaction tx : transactions) {
            builder.append(tx.getType()).append(',')
                    .append(escape(tx.getAccount().getName())).append(',')
                    .append(escape(tx.getCategory().getName())).append(',')
                    .append(tx.getAmount()).append(',')
                    .append(tx.getCurrency()).append(',')
                    .append(tx.getTransactionDate()).append(',')
                    .append(escape(tx.getNote()))
                    .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }
}
