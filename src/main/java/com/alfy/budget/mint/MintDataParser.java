package com.alfy.budget.mint;

import com.alfy.budget.model.BankTransaction;
import com.alfy.budget.tools.Tools;
import com.google.gson.Gson;
import com.webcohesion.ofx4j.domain.data.common.TransactionType;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.ZonedDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MintDataParser {

    private final Gson gson;

    public MintDataParser() {
        gson = new Gson();
    }

    private void parseZip(Path path) throws IOException {
        try (PrintWriter writer = new PrintWriter(Files.newOutputStream(Paths.get("C:/Users/Christensen/Downloads/mintTransactions.csv")))) {
            writer.println(String.join(",", new String[]{
                    "CSV",
                    "Type",
                    "Amount",
                    "Transaction Date",
                    "Post Date",
                    "Account",
                    "Category",
                    "Original Description",
                    "Updated Description",
                    "Memo",
                    "Provider Description",
                    "Comments",
                    "Check Number",
                    "Balance",

                    "Tags",
                    "Notes",
            }));

            try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(path))) {
                ZipEntry zipEntry;
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    System.out.println(zipEntry.getName());
                    if (zipEntry.getName().contains("SharedData/FinancialData/transactions_users_")) {
                        MintTransactionJson[] mintTransactions = gson.fromJson(new InputStreamReader(zipInputStream), MintTransactionJson[].class);
                        for (MintTransactionJson mintTransaction : mintTransactions) {

                            BankTransaction bankTransaction = new BankTransaction();
                            bankTransaction.csv = mintTransaction.id;
                            bankTransaction.account = getAccountName(mintTransaction);
                            bankTransaction.transactionType = Tools.isLessThanZero(mintTransaction.amount) ? "debit" : "credit";
                            bankTransaction.transactionDate = mintTransaction.transactionDate != null ? ZonedDateTime.parse(mintTransaction.transactionDate).toLocalDate() : null;
                            bankTransaction.postDate = mintTransaction.postedDate != null ? ZonedDateTime.parse(mintTransaction.postedDate).toLocalDate() : null;
                            bankTransaction.description = mintTransaction.description;
                            bankTransaction.comments = "TODO comments";
                            bankTransaction.checkNumber = "TODO check number";
                            bankTransaction.amount = mintTransaction.amount.abs();
                            bankTransaction.balance = null;

                            if (bankTransaction.transactionDate == null) {
                                bankTransaction.transactionDate = bankTransaction.postDate;
                            }

                            if (bankTransaction.postDate == null) {
                                bankTransaction.postDate = bankTransaction.transactionDate;
                            }

                            String providerDescription = "";
                            if (mintTransaction.memo != null) {
                                providerDescription = mintTransaction.memo;
                            } else if (mintTransaction.L10_provider_description != null) {
                                providerDescription = mintTransaction.L10_provider_description.trim();
                            }

                            System.out.println("Transaction:");
                            System.out.println("status: " + mintTransaction.status);
                            System.out.println("type: " + bankTransaction.transactionType);
                            System.out.println("date: " + bankTransaction.transactionDate);
                            System.out.println("postDate: " + bankTransaction.postDate);
                            System.out.println("description: " + bankTransaction.description);
                            System.out.println("description 2: " + providerDescription);
                            System.out.println("amount: " + bankTransaction.amount);
                            System.out.println("account: " + bankTransaction.account);
                            System.out.println("category: " + mintTransaction.categoryName);
                            System.out.println(zipEntry.getName());
                            System.out.println();

                            writer.println("\"" + bankTransaction.csv + "\""
                                    + ",\"" + bankTransaction.transactionType + "\""
                                    + ",\"" + bankTransaction.amount + "\""
                                    + ",\"" + bankTransaction.transactionDate + "\""
                                    + ",\"" + bankTransaction.postDate + "\""
                                    + ",\"" + bankTransaction.account + "\""
                                    + ",\"" + mintTransaction.categoryName + "\""
                                    + ",\"" + providerDescription + "\""
                                    + ",\"" + mintTransaction.description + "\""
                                    + ",\"" + mintTransaction.memo + "\""
                                    + ",\"" + mintTransaction.L10_provider_description + "\""
                                    + ",\"" + bankTransaction.comments + "\""
                                    + ",\"" + bankTransaction.checkNumber + "\""
                                    + ",\"" + bankTransaction.balance + "\""

                                    + ",\"" + "TODO Tag" + "\""
                                    + ",\"" + "TODO Notes" + "\""
                                    + ",\"" + zipEntry.getName() + "\""
                            );
                        }
                    }
                }
            }
        }
    }

    private String getAccountName(MintTransactionJson mintTransaction) {
        String account = getAccountName(mintTransaction.transactionAccountId);
        if (account != null) {
            return account;
        }

        if (mintTransaction.associations != null) {
            for (String association : mintTransaction.associations) {
                account = getAccountName(association);
                if (account != null) {
                    return account;
                }
            }
        }

        return null;
    }

    private String getAccountName(String value) {
        if (value != null) {
            switch (value) {
                case "urn:account:fdp::accountid:d7a565c0-e264-11eb-ac32-3ea9baee151e":
                    // Xima Software 401(k) Plan
                    return "Retirement - The Standard";
                case "urn:account:fdp::accountid:37625a40-5a41-11ed-90e3-0291410a9c30":
                    // "The Standard - 401K"
                    return "Retirement - The Standard";

                case "urn:account:fdp::accountid:0f09a9d0-7462-11ea-9283-da34662b8b46":
                    // Mass Mutual - Retirement
                    return "Retirement - Mass Mutual";

                case "urn:account:fdp::accountid:25d69790-950f-11ea-a0e1-2a6b9194d980":
                    // "Alan D Christensen Roth IRA Brokerage Account"
                    // "Vanguard"
                    return "Retirement - Vanguard - Alan Roth IRA";
                case "urn:account:fdp::accountid:25d69791-950f-11ea-a0e1-2a6b9194d980":
                    // "Alan D Christensen Rollover IRA Brokerage Account XXXX4292"
                    // "Vanguard"
                    return "Retirement - Vanguard - Alan Rollover IRA";
                case "urn:account:fdp::accountid:289508b0-543f-11ea-8820-c28480da9934":
                    // "Alan D Christensen, Dixie Marie Christensen Brokerage Account XXXX9940"
                    return "Retirement - Vanguard - Mission";
                case "urn:account:fdp::accountid:289508b1-543f-11ea-8820-c28480da9934":
                    // "Alan D Christensen Roth IRA Brokerage Account XXXX5252"
                    return "Retirement - Vanguard - Alan Roth IRA";
                case "urn:account:fdp::accountid:cc36f1b0-def8-11ea-a515-02867c534c9d":
                    // "Dixie Marie Christensen Roth IRA Brokerage Account XXXX3082"
                    return "Retirement - Vanguard - Dixie Roth IRA";


                case "urn:account:fdp::accountid:d00f4f3a-05a7-30e9-bc31-c197fc9575d0":
                    return "Zions - Alan and Dixie Check (1465)";
                case "urn:account:fdp::accountid:b6b95b20-b961-3ec4-8982-b1bdc86cfe98":
                    return "Zions - Alan and Dixie Check (1465) - Line of Credit";
                case "urn:account:fdp::accountid:07b0d1e8-1a65-3eb3-9a42-f8b03c1b6160":
                    return "Zions - Alan Check (3174)";
                case "urn:account:fdp::accountid:f7855dd7-565c-3327-9a5c-4a98ae588947":
                    return "Zions - Alan Check (3174) - Line of Credit";
                case "urn:account:fdp::accountid:4e6f6146-cce1-3430-aee5-dfc864ac4d0f":
                    // Old main card
                    return "Zions - Cash Back Visa (5622)";
                case "urn:account:fdp::accountid:892ba8a6-da23-3289-a9d5-d240d551aade":
                    return "Zions - Student Visa (7747)";
                case "urn:account:fdp::accountid:7f734e4d-3625-3d36-a329-17eefcc4d851":
                    return "Zions - Savings (3793)";
                case "urn:account:fdp::accountid:fcd617f0-9512-11e9-95d6-06d003d87fce":
                    return "Zions - Savings (0113) - Long Term Projects";
                case "urn:account:fdp::accountid:fcd617f1-9512-11e9-95d6-06d003d87fce":
                    return "Zions - Savings (0719) - Gift";
                case "urn:account:fdp::accountid:6b1c8750-f52c-11eb-b55d-caba1ee2a7af":
                    return "Zions - HELOC";
                case "urn:account:fdp::accountid:f0c905c0-669a-3ea3-bfb2-951a566fe6bf":
                    return "Zions - HELOC";


                case "urn:account:fdp::accountid:396434f0-6699-11eb-a478-cae05e75aded":
                    return "Discover - Checking (8449)";
                case "urn:account:fdp::accountid:0836e242-eadc-3153-986d-02405eb3ef10":
                    return "Discover - Car Bank (7642)";
                case "urn:account:fdp::accountid:0e64640d-a771-30d5-90ec-7433d39fc37e":
                    return "Discover - Car Bills (1876)";
                case "urn:account:fdp::accountid:7391fc2e-6677-3110-981b-1c6867becf36":
                    return "Discover - Savings (2021)";
                case "urn:account:fdp::accountid:f304ef0a-d920-33dc-898e-b370a34b55d8":
                    return "Discover - Savings (2012)";
                case "urn:account:fdp::accountid:af999330-95ff-11ed-8b69-8a7f8e4efa60":
                    return "Discover - Yearly (0291)";
                case "urn:account:fdp::accountid:c01aa940-6a64-11ed-853e-7a9fc71ea109":
                    return "Discover - Emergency (2038)";
                case "urn:account:fdp::accountid:f585c5c3-b0d7-33a3-a56b-c62c60875d25":
                    return "Discover - Medical (2003)";


                case "urn:account:fdp::accountid:0a264f50-a8c4-11e9-b131-0285bd89e4be":
                    return "Gunnison - Checking (9332)";
                case "urn:account:fdp::accountid:8edd4950-a99c-11e9-baae-02c9ed290658":
                    return "Gunnison - Checking (5333)";
                case "urn:account:fdp::accountid:9b289460-bba4-11ea-ae7a-ceaaf9364bd8":
                    return "Gunnison - Checking ()";
                case "urn:account:fdp::accountid:9b28bb70-bba4-11ea-ae7a-ceaaf9364bd8":
                    return "Gunnison - Checking ()";


                case "urn:account:fdp::accountid:26561545-b154-3972-a274-5a4f7b1ce687":
                    return "DFCU - Credit Card (4372)";
                case "urn:account:fdp::accountid:e6efed9a-6915-3b83-8295-391b65e16cc7":
                    return "DFCU - Checking (3424)";
                case "urn:account:fdp::accountid:8ea5c278-8c60-308b-8fc6-11f1b5bdf8c4":
                    return "DFCU - Savings (3416)";
                case "urn:account:fdp::accountid:f949290a-5a37-36c7-9b84-922463c41a55":
                    return "DFCU - Nicole Mission Savings";
                case "urn:account:fdp::accountid:f29d7db0-a666-342e-912f-f53b4e856966":
                    return "DFCU - Nicole Youth Savings";
                case "urn:account:fdp::accountid:287b3c94-e60d-33c1-ac43-4fc7abba8ebc":
                    return "DFCU - Max Mission Savings (0031)";
                case "urn:account:fdp::accountid:d07cb528-00de-3503-9207-a3a8d96a1d9a":
                    return "DFCU - Max Youth Savings (0023)";
                case "urn:account:fdp::accountid:747ad8fb-3327-3fa6-84d7-77e83dbf23f7":
                    return "DFCU - Tyson Mission Savings (1251)";
                case "urn:account:fdp::accountid:a0604528-861e-3e20-b46b-15fa6440da6d":
                    return "DFCU - Tyson Youth Savings";
            }
        }

        return null;
    }

    public static void main(final String[] args) throws IOException {
        MintDataParser parser = new MintDataParser();
        parser.parseZip(Paths.get("D:/mint backup 3-1-2024.zip"));
    }

}
