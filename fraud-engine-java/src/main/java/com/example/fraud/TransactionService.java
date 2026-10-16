package com.example.fraud;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    public record Tx(long accountId, double amount, String country) {}

    static final double HIGH_AMOUNT_LIMIT = 5000;
    static final int VELOCITY_LIMIT = 5;   // transactions per minute

    private final JdbcTemplate jdbc;

    public TransactionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Applies the fraud rules, stores the transaction, and raises an alert if any rule fires. */
    public Map<String, Object> process(Tx tx) {
        List<String> reasons = new ArrayList<>();

        // Rule 1: unusually large amount
        if (tx.amount() > HIGH_AMOUNT_LIMIT) {
            reasons.add("HIGH_AMOUNT");
        }

        // Rule 2: country differs from the account's last transaction within the past hour
        List<String> last = jdbc.query(
                "SELECT country FROM transactions WHERE account_id = ? " +
                "AND tx_time > now() - interval '1 hour' ORDER BY tx_time DESC LIMIT 1",
                (rs, i) -> rs.getString(1), tx.accountId());
        if (!last.isEmpty() && !last.get(0).equals(tx.country())) {
            reasons.add("LOCATION_CHANGE");
        }

        // Rule 3: too many transactions in the last minute
        Integer recent = jdbc.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE account_id = ? " +
                "AND tx_time > now() - interval '1 minute'",
                Integer.class, tx.accountId());
        if (recent != null && recent >= VELOCITY_LIMIT) {
            reasons.add("HIGH_VELOCITY");
        }

        Long txId = jdbc.queryForObject(
                "INSERT INTO transactions(account_id, amount, country) VALUES (?,?,?) RETURNING id",
                Long.class, tx.accountId(), tx.amount(), tx.country());

        if (!reasons.isEmpty()) {
            jdbc.update("INSERT INTO alerts(tx_id, account_id, amount, country, reason) VALUES (?,?,?,?,?)",
                    txId, tx.accountId(), tx.amount(), tx.country(), String.join(", ", reasons));
        }
        return Map.of("transactionId", txId, "flagged", !reasons.isEmpty(), "reasons", reasons);
    }
}
