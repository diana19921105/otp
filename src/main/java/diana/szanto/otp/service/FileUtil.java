package diana.szanto.otp.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import diana.szanto.otp.model.CustomerData;
import diana.szanto.otp.model.WebShopIncomeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FileUtil {

    private static final Set<String> SEEN_PAIRS = new HashSet<>();

    @Value("${otp.out.payment.path}")
    private String transactionFile;

    @Value("${otp.out.customer.path}")
    private String customerFile;

    public void loadCustomerData(Map<String, CustomerData> customerMap) {
        try (var reader = new BufferedReader(new FileReader(customerFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                var parts = line.split(";");
                if (parts.length < 4) {
                    log.warn("Faulty line: " + line);
                    continue;
                }

                var webShopId = parts[0].trim();
                var customerId = parts[1].trim();
                var name = parts[2].trim();
                var address = parts[3].trim();

                var key = webShopId + ";" + customerId;
                customerMap.putIfAbsent(key, new CustomerData(webShopId, customerId, name, address));
            }
        } catch (IOException e) {
            log.debug("File processing failed", e);
        }
    }

    public void loadTransactionData(Map<String, CustomerData> customerMap, Map<String, WebShopIncomeData> webShopIncomeDataMap) {
        try (var reader = new BufferedReader(new FileReader(transactionFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                var parts = line.split(";");
                if (parts.length < 7) {
                    log.warn("Faulty line: " + line);
                    continue;
                }

                var webShopId = parts[0].trim();
                var customerId = parts[1].trim();
                var transactionType = parts[2].trim();
                var transactionAmount = Integer.parseInt(parts[3].trim());

                var key = webShopId + ";" + customerId;
                var customerData = customerMap.get(key);

                if (customerData != null) {
                    customerData.addPurchase(transactionAmount);
                } else {
                    log.warn("Ineligible customer data for id: " + key);
                }

                if (!webShopIncomeDataMap.containsKey(webShopId)) {
                    webShopIncomeDataMap.put(webShopId, new WebShopIncomeData(webShopId));
                }

                var income = webShopIncomeDataMap.get(webShopId);
                if ("card".equalsIgnoreCase(transactionType)) {
                    income.addCardPayment(transactionAmount);
                } else if ("transfer".equalsIgnoreCase(transactionType)) {
                    income.addBankTransfer(transactionAmount);
                } else {
                    log.warn("Transaction type is not supported: " + transactionType);
                }
            }
        } catch (IOException e) {
            log.error("Failed to process file: {}", transactionFile, e);
        } catch (NumberFormatException e) {
            log.error("Faulty amount", e);
        }
    }

    private boolean validateInputFile(String line, String filename) {
        var parts = line.split(";");
        var webShopId = parts[0].trim();
        var customerId = parts[1].trim();

        if (filename.contains("payments")) {
            var transactionType = parts[2].trim();
            var accountNumber = parts[4].trim();
            var cardNumber = parts[5].trim();
            var dateColumn = parts[6].trim();

            if ("card".equalsIgnoreCase(transactionType)) {
                if (!accountNumber.isEmpty() || cardNumber.isEmpty()) {
                    return false;
                }
            } else if ("transfer".equalsIgnoreCase(transactionType)) {
                if (!cardNumber.isEmpty() || accountNumber.isEmpty()) {
                    return false;
                }
            }

            var dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
            try {
                var date = LocalDate.parse(dateColumn, dateFormatter);
                log.debug("Date is valid: {}", date);
                return true;
            } catch (DateTimeParseException ex) {
                return false;
            }
        }

        var pair = webShopId + ";" + customerId;
        if (SEEN_PAIRS.contains(pair)) {
            return false;
        } else {
            SEEN_PAIRS.add(pair);
            return true;
        }
    }

    public void readFile(String inputFile, String outPutFile, String errorLogFile) throws IOException {
        Files.deleteIfExists(Paths.get(outPutFile));
        Files.deleteIfExists(Paths.get(errorLogFile));

        try (var reader = new BufferedReader(new FileReader(inputFile));
             var fileWriter = new BufferedWriter(new FileWriter(outPutFile));
             var errorWriter = new BufferedWriter(new FileWriter(errorLogFile, true))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (validateInputFile(line, inputFile)) {
                    fileWriter.write(line + System.lineSeparator());
                } else {
                    var errorMessage = "Faulty line: " + line;
                    log.error(errorMessage);
                    errorWriter.write(errorMessage + System.lineSeparator());
                }
            }
        }
    }

}
