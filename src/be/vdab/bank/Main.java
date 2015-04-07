package be.vdab.bank;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Scanner;

/**
 * Created by Samuel Engelen on 3/04/2015.
 */
public class Main {
    private static final String URL = "jdbc:mysql://localhost/bank";
    private static final String USER = "bediende";
    private static final String PASSWORD = "keytrade";

    private static final String SQL_INSERT = "INSERT INTO rekeningen (rekeningnr) VALUE (?)";
    private static final String SQL_SELECT = "SELECT rekeningnr, saldo FROM rekeningen WHERE rekeningnr = ?";
    private static final String SQL_TEST = "SELECT rekeningnr FROM rekeningen WHERE rekeningnr = ?";
    private static final String SQL_UPDATE1 = "UPDATE rekeningen SET saldo = saldo - ? WHERE rekeningnr = ? AND saldo >= ?";
    private static final String SQL_UPDATE2 = "UPDATE rekeningen SET saldo = saldo + ? WHERE rekeningnr = ?";


    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            int keuze = 1;
            while (keuze != 0) {
                System.out.println("Maak een keuze uit volgend menu:\n\n1. Nieuwe rekening\n2. Saldo consulteren\n3. Overschrijven\n0. Stoppen\n");
                keuze = scanner.nextInt();
                switch (keuze) {
                    case 1:
                        rekeningAanmaken(scanner);
                        break;
                    case 2:
                        saldoConsulteren(scanner);
                        break;
                    case 3:
                        bedragOverschrijven(scanner);
                        break;
                    case 0:
                        break;
                    default:
                        System.out.println("\nOngeldige keuze\n");
                        break;
                }
            }
        }
    }

    public static boolean valideerNummer(long nummer) {
        return String.valueOf(nummer).length() == 12 && (nummer / 100) % 97 == nummer % 100;
    }


    private static void rekeningAanmaken(Scanner scanner) {
            System.out.println("\nGeef het nieuwe rekeningnummer: \n");
            long nummer = scanner.nextLong();

            if (valideerNummer(nummer)) {
                try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                     PreparedStatement statement = connection.prepareStatement(SQL_INSERT)) {
                    statement.setLong(1, nummer);
                    statement.execute();
                    System.out.println("\nNieuwe rekening met nummer " + nummer + " succesvol aangemaakt\n");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } else {
                System.out.println("\nOngeldig rekeningnummer.\n");
            }
    }

    private static void saldoConsulteren(Scanner scanner) {
            System.out.println("\nGeef het rekeningnummer: \n");
            long nummer = scanner.nextLong();

            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement statement = connection.prepareStatement(SQL_SELECT)) {
                statement.setLong(1, nummer);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        System.out.println("\nSaldo van rekening " + resultSet.getString("rekeningnr") + " bedraagt " + resultSet.getBigDecimal("saldo") + " euro\n");
                    } else {
                        System.out.println("\nRekening staat niet in database.\n");
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
    }

    private static void bedragOverschrijven(Scanner scanner) {
            System.out.println("\nVan welke rekening wil je overschrijven?\n");
            long vanRekening = scanner.nextLong();
            System.out.println("\nNaar welke rekening wil je overschrijven?\n");
            long naarRekening = scanner.nextLong();
            System.out.println("\nWelk bedrag wil je overschrijven?\n");
            BigDecimal bedrag = scanner.nextBigDecimal();

            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement statementTest = connection.prepareStatement(SQL_TEST);
                PreparedStatement statementUpdate1 = connection.prepareStatement(SQL_UPDATE1);
                PreparedStatement statementUpdate2 = connection.prepareStatement(SQL_UPDATE2)) {

                // Zet de isolation op serializable voor de veiligheid
                connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                // Zet de vermindering van de van-rekening klaar
                statementUpdate1.setBigDecimal(1, bedrag);
                statementUpdate1.setLong(2, vanRekening);
                statementUpdate1.setBigDecimal(3, bedrag);
                // Zet autocommit uit
                connection.setAutoCommit(false);

                // Test of de eerste update een record heeft aangepast; zoniet, controleer of er onvoldoende saldo was of dat de rekening niet bestaat
                if (statementUpdate1.executeUpdate() == 0) {
                        statementTest.setLong(1, vanRekening);
                        try (ResultSet resultSet = statementTest.executeQuery()) {
                            System.out.println(resultSet.next() ? "\nOnvoldoende saldo\n" : "\nVan-rekening niet gevonden\n");
                        }
                } else {
                    // Controleer of de naar-rekening bestaat en voer de vermeerdering uit.
                    statementTest.setLong(1, naarRekening);
                    try (ResultSet resultSet = statementTest.executeQuery()) {
                        if (resultSet.next()) {
                            statementUpdate2.setBigDecimal(1, bedrag);
                            statementUpdate2.setLong(2, naarRekening);
                            statementUpdate2.executeUpdate();
                            connection.commit();
                            System.out.println("\n" + bedrag + " euro overgeschreven van rekening " + vanRekening + " naar rekening " + naarRekening);
                        } else {
                            System.out.println("\nNaar-rekening niet gevonden\n");
                        }
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
    }
}
