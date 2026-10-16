using Npgsql;

var builder = WebApplication.CreateBuilder(args);
var connStr = builder.Configuration["DB_CONN"]
    ?? "Host=localhost;Port=5433;Database=fraud;Username=postgres;Password=postgres";
builder.Services.AddSingleton(NpgsqlDataSource.Create(connStr));

var app = builder.Build();
app.UseDefaultFiles();
app.UseStaticFiles();

// List alerts. status = PENDING (default) | APPROVED | BLOCKED | ALL
app.MapGet("/api/alerts", async (NpgsqlDataSource ds, string? status) =>
{
    status = (status ?? "PENDING").ToUpperInvariant();
    const string cols = "SELECT id, tx_id, account_id, amount, country, reason, status, created_at FROM alerts";
    var sql = status == "ALL"
        ? cols + " ORDER BY created_at DESC LIMIT 50"
        : cols + " WHERE status = @s ORDER BY created_at DESC LIMIT 50";
    await using var cmd = ds.CreateCommand(sql);
    if (status != "ALL") cmd.Parameters.AddWithValue("s", status);
    return await Read(cmd);
});

// Analyst decision: { "decision": "APPROVED" | "BLOCKED" }
app.MapPost("/api/alerts/{id:int}/decision", async (NpgsqlDataSource ds, int id, DecisionRequest req) =>
{
    var d = req.Decision?.ToUpperInvariant();
    if (d != "APPROVED" && d != "BLOCKED")
        return Results.BadRequest(new { error = "decision must be APPROVED or BLOCKED" });

    await using var cmd = ds.CreateCommand(
        "UPDATE alerts SET status = @s, reviewed_at = now() WHERE id = @id AND status = 'PENDING'");
    cmd.Parameters.AddWithValue("s", d);
    cmd.Parameters.AddWithValue("id", id);
    var changed = await cmd.ExecuteNonQueryAsync();
    return changed == 0
        ? Results.NotFound(new { error = "alert not found or already reviewed" })
        : Results.Ok(new { id, status = d });
});

app.MapGet("/api/stats", async (NpgsqlDataSource ds) =>
{
    await using var cmd = ds.CreateCommand(
        @"SELECT (SELECT COUNT(*) FROM transactions) AS transactions,
                 (SELECT COUNT(*) FROM alerts WHERE status='PENDING') AS pending,
                 (SELECT COUNT(*) FROM alerts WHERE status='APPROVED') AS approved,
                 (SELECT COUNT(*) FROM alerts WHERE status='BLOCKED') AS blocked");
    return (await Read(cmd)).First();
});

app.MapGet("/api/health", () => new { status = "UP" });

app.Run();

static async Task<List<Dictionary<string, object?>>> Read(NpgsqlCommand cmd)
{
    await using var r = await cmd.ExecuteReaderAsync();
    var rows = new List<Dictionary<string, object?>>();
    while (await r.ReadAsync())
    {
        var row = new Dictionary<string, object?>();
        for (int i = 0; i < r.FieldCount; i++)
            row[r.GetName(i)] = r.IsDBNull(i) ? null : r.GetValue(i);
        rows.Add(row);
    }
    return rows;
}

record DecisionRequest(string? Decision);
